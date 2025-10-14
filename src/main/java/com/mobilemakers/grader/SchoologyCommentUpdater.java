package com.mobilemakers.grader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Posts feedback comments to Schoology assignment submissions via reverse-engineered web API.
 * Requires valid session cookies and CSRF tokens from an active Schoology session.
 */
public class SchoologyCommentUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchoologyCommentUpdater.class);
    private static final MediaType FORM_URLENCODED = MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8");

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String courseId;
    private final String assignmentId;
    private final String sessionCookie;
    private final String csrfKey;
    private final String csrfToken;
    private final String gradingPeriodId;
    private final String sectionId;

    // Cache for school_uid -> uid mapping
    private Map<String, String> studentUidCache;

    public SchoologyCommentUpdater(String baseUrl, String courseId, String assignmentId,
                                    String sessionCookie, String csrfKey, String csrfToken) {
        this(baseUrl, courseId, assignmentId, sessionCookie, csrfKey, csrfToken, null, null);
    }

    public SchoologyCommentUpdater(String baseUrl, String courseId, String assignmentId,
                                    String sessionCookie, String csrfKey, String csrfToken,
                                    String gradingPeriodId, String sectionId) {
        this.baseUrl = baseUrl;
        this.courseId = courseId;
        this.assignmentId = assignmentId;
        this.sessionCookie = sessionCookie;
        this.csrfKey = csrfKey;
        this.csrfToken = csrfToken;
        this.gradingPeriodId = gradingPeriodId;
        this.sectionId = sectionId;
        this.objectMapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(10))
                .build();
        this.studentUidCache = null;
    }

    /**
     * Fetches student enrollment data and builds school_uid -> uid mapping.
     * This must be called before posting comments.
     */
    public void fetchStudentMappings() throws IOException {
        String url = baseUrl + "/iapi/enrollment/member_enrollments/course/" + courseId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Cookie", sessionCookie)
                .header("x-csrf-key", csrfKey)
                .header("x-csrf-token", csrfToken)
                .header("accept", "*/*")
                .header("referer", baseUrl + "/course/" + courseId + "/grades")
                .build();

        LOGGER.info("Fetching student enrollment data from Schoology...");
        LOGGER.debug("Request URL: {}", url);
        LOGGER.debug("Cookie length: {} chars", sessionCookie.length());
        LOGGER.debug("CSRF Key: {}", csrfKey.substring(0, Math.min(10, csrfKey.length())) + "...");
        LOGGER.debug("CSRF Token: {}", csrfToken.substring(0, Math.min(10, csrfToken.length())) + "...");

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                LOGGER.error("Schoology enrollment API returned status {}", response.code());
                LOGGER.error("Response body (first 500 chars): {}", responseBody.substring(0, Math.min(500, responseBody.length())));
                throw new IOException("Failed to fetch student enrollments. Status: " + response.code() +
                    ". Response starts with: " + responseBody.substring(0, Math.min(200, responseBody.length())));
            }

            // Check if we got HTML instead of JSON (indicates auth failure)
            if (responseBody.trim().startsWith("<")) {
                LOGGER.error("Received HTML instead of JSON from Schoology API");
                LOGGER.error("HTTP Status: {}", response.code());
                LOGGER.error("This usually means session cookies are invalid or expired");
                LOGGER.error("Response starts with: {}", responseBody.substring(0, Math.min(300, responseBody.length())));
                LOGGER.error("Full cookie being sent has {} characters", sessionCookie.length());
                throw new IOException("Authentication failed: Schoology returned HTML instead of JSON (Status: " + response.code() + "). " +
                    "Session cookie may be expired. Please refresh SCHOOLOGY_SESSION_COOKIE, " +
                    "SCHOOLOGY_CSRF_KEY, and SCHOOLOGY_CSRF_TOKEN from browser DevTools.");
            }
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode enrollmentsNode = root.path("body");

            if (enrollmentsNode.isMissingNode() || !enrollmentsNode.isObject()) {
                throw new IOException("Invalid enrollment response format");
            }

            studentUidCache = new HashMap<>();
            enrollmentsNode.fields().forEachRemaining(entry -> {
                JsonNode student = entry.getValue();
                String schoolUid = student.path("school_uid").asText();
                String uid = student.path("uid").asText();

                if (!schoolUid.isEmpty() && !uid.isEmpty()) {
                    studentUidCache.put(schoolUid, uid);
                }
            });

            LOGGER.info("Loaded {} student UID mappings from Schoology", studentUidCache.size());
        }
    }

    /**
     * Posts a feedback comment to a student's assignment submission.
     *
     * This uses a two-step process:
     * 1. GET the student's dropbox page to extract CSRF tokens (form_token, form_build_id, sid)
     * 2. POST the comment with all required fields and tokens
     *
     * Schoology's comment form requires these tokens for security (CSRF protection).
     * Without them, the POST returns HTML instead of JSON and the comment is not saved.
     *
     * @param schoolUid The student's school_uid (e.g., "s486002")
     * @param comment The feedback comment to post
     * @throws IOException if the request fails
     */
    public void postComment(String schoolUid, String comment) throws IOException {
        if (studentUidCache == null) {
            throw new IllegalStateException("Student mappings not loaded. Call fetchStudentMappings() first.");
        }

        String studentUid = studentUidCache.get(schoolUid);
        if (studentUid == null) {
            throw new IOException("Student UID not found for school_uid: " + schoolUid);
        }

        // ============================================================================
        // STEP 1: Fetch the dropbox page to extract form tokens
        // ============================================================================
        // We need to GET the page first because Schoology generates unique CSRF tokens
        // for each form submission. These tokens are embedded in the HTML as hidden inputs.
        String dropboxUrl = baseUrl + "/assignment/" + assignmentId + "/dropbox/view/" + studentUid;

        Request getRequest = new Request.Builder()
                .url(dropboxUrl)
                .get()
                .header("Cookie", sessionCookie)
                .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build();

        String formToken = null;      // CSRF token for form submission
        String formBuildId = null;    // Drupal form build ID
        String sid = null;            // Session identifier (hex-encoded)

        try (Response getResponse = client.newCall(getRequest).execute()) {
            if (!getResponse.isSuccessful()) {
                throw new IOException("Failed to fetch dropbox page for " + schoolUid + ". Status: " + getResponse.code());
            }

            String pageHtml = getResponse.body() != null ? getResponse.body().string() : "";

            // Save HTML for debugging (first student only, if tokens fail to extract)
            // This helps troubleshoot if Schoology changes their HTML structure
            if (studentUidCache.size() > 0 && formToken == null) {
                try {
                    java.nio.file.Files.writeString(
                        java.nio.file.Path.of("dropbox-page-debug.html"),
                        pageHtml,
                        java.nio.charset.StandardCharsets.UTF_8
                    );
                    LOGGER.info("Saved dropbox HTML to dropbox-page-debug.html for inspection");
                } catch (Exception ex) {
                    LOGGER.warn("Could not save debug HTML: {}", ex.getMessage());
                }
            }

            // Extract form tokens from HTML
            // These appear as hidden input fields like:
            // <input type="hidden" name="form_token" id="edit-..." value="625ba8761..." />
            // Note: The 'id' attribute appears between 'name' and 'value', so we use
            // extractInputValue() which handles arbitrary attributes between them
            formToken = extractInputValue(pageHtml, "form_token");
            formBuildId = extractInputValue(pageHtml, "form_build_id");
            sid = extractInputValue(pageHtml, "sid");

            if (formToken == null || formBuildId == null) {
                LOGGER.warn("Could not extract form tokens from dropbox page for {}", schoolUid);
                LOGGER.warn("form_token found: {}, form_build_id found: {}, sid found: {}",
                    formToken != null, formBuildId != null, sid != null);

                // Check if comment form exists at all
                if (pageHtml.contains("s_drop_item_add_comment_form")) {
                    LOGGER.info("Comment form found in HTML, but tokens missing");
                } else {
                    LOGGER.warn("Comment form 's_drop_item_add_comment_form' not found in HTML");
                }
            } else {
                LOGGER.info("Successfully extracted form tokens for {}", schoolUid);
                LOGGER.info("  form_token: {}...", formToken.substring(0, Math.min(10, formToken.length())));
                LOGGER.info("  form_build_id: {}...", formBuildId.substring(0, Math.min(20, formBuildId.length())));
                if (sid != null) {
                    LOGGER.info("  sid: {}...", sid.substring(0, Math.min(10, sid.length())));
                }
            }
        }

        // ============================================================================
        // STEP 2: POST comment with extracted tokens
        // ============================================================================
        // Build the POST request with all required form fields. The field order and
        // presence of empty fields matters - Schoology expects this exact structure.
        String postUrl = baseUrl + "/assignment/" + assignmentId + "/dropbox/view/" + studentUid
                       + "?destination=assignment%2F" + assignmentId + "%2Finfo";

        FormBody.Builder formBuilder = new FormBody.Builder()
                // destination: Where to redirect after successful comment post
                .add("destination", "assignment/" + assignmentId + "/info")

                // comment: The actual feedback text
                .add("comment", comment)

                // target_DOM_id: JavaScript uses this to update the UI (format: upload-btn-{uid})
                .add("target_DOM_id", "upload-btn-" + studentUid)

                // sid: Session identifier (hex-encoded, extracted from form)
                .add("sid", (sid != null) ? sid : "")

                // File upload fields (empty for text-only comments, but required by form)
                .add("file[files]", "")
                .add("file[recording]", "")
                .add("annotation_files", "");

        // Add CSRF protection tokens (required for form submission to succeed)
        if (formBuildId != null) {
            formBuilder.add("form_build_id", formBuildId);
        }
        if (formToken != null) {
            formBuilder.add("form_token", formToken);
        }

        // Form metadata fields
        formBuilder
                .add("form_id", "s_drop_item_add_comment_form")  // Identifies which form is being submitted
                .add("op", "Post")                                 // Operation type (button clicked)
                .add("drupal_ajax", "1");                          // Tells Schoology to return JSON, not HTML

        FormBody formBody = formBuilder.build();

        // Build HTTP request with proper headers
        // Key headers:
        // - Cookie: Session authentication
        // - x-csrf-key/x-csrf-token: Additional CSRF protection (from browser DevTools)
        // - x-requested-with: Identifies this as an AJAX request
        // - drupal_ajax=1 in form body: Makes Schoology return JSON instead of HTML
        Request request = new Request.Builder()
                .url(postUrl)
                .post(formBody)
                .header("Cookie", sessionCookie)
                .header("x-csrf-key", csrfKey)
                .header("x-csrf-token", csrfToken)
                .header("x-requested-with", "XMLHttpRequest")
                .header("accept", "application/json, text/javascript, */*; q=0.01")
                .header("referer", baseUrl + "/assignment/" + assignmentId + "/info")
                .header("origin", baseUrl)
                .build();

        LOGGER.info("Posting comment for school_uid={} (uid={}) with form tokens", schoolUid, studentUid);

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                LOGGER.error("Failed to post comment. Status: {}", response.code());
                LOGGER.error("Response body: {}", responseBody.substring(0, Math.min(500, responseBody.length())));
                throw new IOException("Failed to post comment for " + schoolUid + ". Status: " + response.code());
            }

            // ========================================================================
            // Validate response format
            // ========================================================================
            // Success: Schoology returns JSON with {"status":true, ...}
            // Failure: Schoology returns full HTML page (means CSRF tokens were wrong/missing)
            if (responseBody.contains("\"status\":true") || responseBody.contains("\"status\": true")) {
                LOGGER.info("✓ Successfully posted comment for {} - Schoology confirmed status:true", schoolUid);
            } else if (responseBody.trim().startsWith("{") && responseBody.contains("\"status\"")) {
                LOGGER.warn("Comment POST returned JSON but status is not true for {}", schoolUid);
                LOGGER.warn("Response preview: {}", responseBody.substring(0, Math.min(200, responseBody.length())));
            } else if (responseBody.contains("<!DOCTYPE") || responseBody.contains("<html")) {
                LOGGER.error("✗ Received HTML instead of JSON for {} - comment NOT saved", schoolUid);
                LOGGER.error("Response length: {} chars (full HTML page returned)", responseBody.length());
                LOGGER.error("This means form submission failed - check form_token/form_build_id/sid extraction");
            } else {
                LOGGER.warn("Unexpected response format for {} - length: {} chars", schoolUid, responseBody.length());
                LOGGER.warn("Response preview: {}", responseBody.substring(0, Math.min(200, responseBody.length())));
            }
        }
    }

    /**
     * Extracts a value from HTML between start and end markers.
     */
    private String extractValue(String html, String startMarker, String endMarker) {
        int startIndex = html.indexOf(startMarker);
        if (startIndex == -1) {
            return null;
        }
        startIndex += startMarker.length();
        int endIndex = html.indexOf(endMarker, startIndex);
        if (endIndex == -1) {
            return null;
        }
        return html.substring(startIndex, endIndex);
    }

    /**
     * Extracts form input value by name attribute, handling any attributes between name and value.
     *
     * This is necessary because Schoology's HTML has attributes in between name and value:
     * Example: <input type="hidden" name="form_token" id="edit-..." value="625ba8761..." />
     *
     * A simple string search for 'name="form_token" value="' would fail because the 'id'
     * attribute appears in between. This method:
     * 1. Finds the input tag by name attribute
     * 2. Extracts just that input tag (up to closing >)
     * 3. Searches for value attribute within that isolated tag
     *
     * @param html The HTML page content
     * @param inputName The name attribute to search for (e.g., "form_token")
     * @return The value attribute, or null if not found
     */
    private String extractInputValue(String html, String inputName) {
        // Find the input tag with this name (try both double and single quotes)
        String namePattern = "name=\"" + inputName + "\"";
        int nameIndex = html.indexOf(namePattern);
        if (nameIndex == -1) {
            namePattern = "name='" + inputName + "'";
            nameIndex = html.indexOf(namePattern);
            if (nameIndex == -1) {
                return null;
            }
        }

        // Find the closing > of this input tag (limit search to 500 chars to avoid matching other tags)
        int closingBracket = html.indexOf(">", nameIndex);
        if (closingBracket == -1 || closingBracket - nameIndex > 500) {
            return null;
        }

        // Extract the substring for this input tag only
        // Example result: 'name="form_token" id="edit-..." value="625ba8761..."'
        String inputTag = html.substring(nameIndex, closingBracket);

        // Now find value="..." or value='...' within this isolated tag
        String value = extractValue(inputTag, "value=\"", "\"");
        if (value == null) {
            value = extractValue(inputTag, "value='", "'");
        }

        return value;
    }

    /**
     * Posts a grade to a student's assignment in Schoology.
     *
     * This uses Schoology's grading API endpoint which is much simpler than comment posting:
     * - No form token extraction needed (uses existing CSRF tokens)
     * - Single HTTP PUT request with JSON payload
     * - Returns JSON with updated grade statistics
     *
     * @param schoolUid The student's school_uid (e.g., "s519167")
     * @param grade The numeric grade to assign (e.g., 10.0)
     * @throws IOException if the request fails
     */
    public void postGrade(String schoolUid, double grade) throws IOException {
        if (studentUidCache == null) {
            throw new IllegalStateException("Student mappings not loaded. Call fetchStudentMappings() first.");
        }

        if (gradingPeriodId == null || sectionId == null) {
            throw new IllegalStateException("Grade posting requires SCHOOLOGY_GRADING_PERIOD_ID and SCHOOLOGY_SECTION_ID");
        }

        String studentUid = studentUidCache.get(schoolUid);
        if (studentUid == null) {
            throw new IOException("Student UID not found for school_uid: " + schoolUid);
        }

        // ============================================================================
        // Build the grading API request
        // ============================================================================
        // Schoology's grading API uses a nested JSON structure:
        // {"grades": {"{studentUid}": {"{assignmentId}": {"grade": "10", ...}}}, "sequence": 1}
        // The csm_section_nid is sent as a query parameter in the URL, not in the body

        String url = baseUrl + "/iapi/grades/grader_grade_data/" + courseId + "/" + gradingPeriodId
                   + "?csm_section_nid=" + sectionId;

        // Format grade as string (Schoology expects string without unnecessary decimals)
        // Use integer format if it's a whole number, otherwise one decimal place
        String gradeStr;
        if (grade == Math.floor(grade)) {
            gradeStr = String.format("%.0f", grade);  // "10" not "10.0"
        } else {
            gradeStr = String.format("%.1f", grade);  // "9.5" for half points
        }

        // Build the nested JSON payload
        // Inner level: grade details for this assignment
        String gradeJson = String.format(
            "{\"grade\":\"%s\",\"exception\":0,\"comment\":null,\"comment_status\":null,\"flags\":null,\"updateSequence\":0}",
            gradeStr
        );

        // Middle level: assignments for this student
        String assignmentJson = String.format(
            "{\"%s\":%s}",
            assignmentId,
            gradeJson
        );

        // Outer level: students being graded
        String studentJson = String.format(
            "{\"%s\":%s}",
            studentUid,
            assignmentJson
        );

        // Top level: complete payload
        String payloadJson = String.format(
            "{\"grades\":%s,\"sequence\":1}",
            studentJson
        );

        // Build request body as raw JSON (not form-encoded)
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(payloadJson, JSON);

        // Build HTTP PUT request
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .header("Cookie", sessionCookie)
                .header("x-csrf-key", csrfKey)
                .header("x-csrf-token", csrfToken)
                .header("accept", "*/*")
                .header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")  // Keep as form-urlencoded
                .header("referer", baseUrl + "/course/" + courseId + "/grades")
                .header("origin", baseUrl)
                .build();

        LOGGER.info("Posting grade {} for school_uid={} (uid={})", gradeStr, schoolUid, studentUid);
        LOGGER.info("Request URL: {}", url);
        LOGGER.info("Request body: {}", payloadJson);

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                LOGGER.error("Failed to post grade. Status: {}", response.code());
                LOGGER.error("Response body: {}", responseBody.substring(0, Math.min(500, responseBody.length())));
                throw new IOException("Failed to post grade for " + schoolUid + ". Status: " + response.code());
            }

            // ========================================================================
            // Validate response format
            // ========================================================================
            // Success: Schoology returns JSON with {"response_code":200, "body":{...}}
            if (responseBody.contains("\"response_code\":200") || responseBody.contains("\"response_code\": 200")) {
                LOGGER.info("✓ Successfully posted grade for {} - Schoology confirmed", schoolUid);
            } else if (responseBody.contains("response_code")) {
                LOGGER.warn("Grade POST returned unexpected response_code for {}", schoolUid);
                LOGGER.warn("Response preview: {}", responseBody.substring(0, Math.min(200, responseBody.length())));
            } else {
                LOGGER.warn("Unexpected grade response format for {} - length: {} chars", schoolUid, responseBody.length());
                LOGGER.warn("Response preview: {}", responseBody.substring(0, Math.min(200, responseBody.length())));
            }
        }
    }

    /**
     * Posts a comment with additional form tokens (if the simple version fails).
     */
    public void postCommentWithTokens(String schoolUid, String comment, String formToken, String formBuildId) throws IOException {
        if (studentUidCache == null) {
            throw new IllegalStateException("Student mappings not loaded. Call fetchStudentMappings() first.");
        }

        String studentUid = studentUidCache.get(schoolUid);
        if (studentUid == null) {
            throw new IOException("Student UID not found for school_uid: " + schoolUid);
        }

        String url = baseUrl + "/assignment/" + assignmentId + "/dropbox/view/" + studentUid
                   + "?destination=assignment%2F" + assignmentId + "%2Finfo";

        FormBody formBody = new FormBody.Builder()
                .add("comment", comment)
                .add("form_token", formToken)
                .add("form_build_id", formBuildId)
                .add("form_id", "s_drop_item_add_comment_form")
                .add("op", "Post")
                .add("drupal_ajax", "1")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .header("Cookie", sessionCookie)
                .header("x-csrf-key", csrfKey)
                .header("x-csrf-token", csrfToken)
                .header("x-requested-with", "XMLHttpRequest")
                .header("accept", "application/json, text/javascript, */*; q=0.01")
                .build();

        LOGGER.debug("Posting comment with tokens for school_uid={} (uid={})", schoolUid, studentUid);

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new IOException("Failed to post comment for " + schoolUid + ". Status: " + response.code() + " - " + body);
            }
            LOGGER.info("Successfully posted comment for {}", schoolUid);
        }
    }
}
