package com.mobilemakers.grader;

import com.mobilemakers.grader.model.GradingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Coordinates grading across all student submissions and generates CSV output.
 */
public class GradeProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradeProcessor.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final SwiftFileReader fileReader;
    private final AssignmentPrompt assignmentPrompt;
    private final OpenAIGrader openAIGrader;
    private final LMStudioGrader lmStudioGrader;
    private final SchoologyCommentUpdater schoologyCommentUpdater;
    private final String schoologyAssignmentColumnName;
    private final String assignmentId;
    private final String assignmentName;
    private final boolean useLocalModel;
    private final boolean enableSchoologyComments;
    private final boolean enableSchoologyGrades;
    private GradingCache gradingCache;

    public GradeProcessor(SwiftFileReader fileReader, AssignmentPrompt assignmentPrompt, OpenAIGrader openAIGrader) {
        this(fileReader, assignmentPrompt, openAIGrader, null, "Assignment", null, null);
    }

    public GradeProcessor(SwiftFileReader fileReader,
                          AssignmentPrompt assignmentPrompt,
                          OpenAIGrader openAIGrader,
                          LMStudioGrader lmStudioGrader,
                          String schoologyAssignmentColumnName) {
        this(fileReader, assignmentPrompt, openAIGrader, lmStudioGrader, schoologyAssignmentColumnName, null, null);
    }

    public GradeProcessor(SwiftFileReader fileReader,
                          AssignmentPrompt assignmentPrompt,
                          OpenAIGrader openAIGrader,
                          LMStudioGrader lmStudioGrader,
                          String schoologyAssignmentColumnName,
                          String assignmentId,
                          String assignmentName) {
        this.fileReader = fileReader;
        this.assignmentPrompt = assignmentPrompt;
        this.openAIGrader = openAIGrader;
        this.lmStudioGrader = lmStudioGrader;
        if (schoologyAssignmentColumnName == null || schoologyAssignmentColumnName.isBlank()) {
            this.schoologyAssignmentColumnName = "Assignment";
        } else {
            this.schoologyAssignmentColumnName = schoologyAssignmentColumnName.trim();
        }

        // Assignment ID and name for batch mode
        // Falls back to config if not provided (backward compatibility)
        this.assignmentId = (assignmentId != null && !assignmentId.isBlank())
                ? assignmentId
                : Config.get("SCHOOLOGY_ASSIGNMENT_ID", "unknown_assignment");
        this.assignmentName = (assignmentName != null && !assignmentName.isBlank())
                ? assignmentName
                : schoologyAssignmentColumnName;

        // Determine which model to use based on environment variable
        this.useLocalModel = Config.getBoolean("USE_LOCAL_MODEL");

        if (useLocalModel) {
            LOGGER.info("Using LM Studio local model for grading");
            if (lmStudioGrader == null) {
                throw new IllegalStateException("USE_LOCAL_MODEL is true but LMStudioGrader was not provided");
            }
        } else {
            LOGGER.info("Using OpenAI API for grading");
            if (openAIGrader == null) {
                throw new IllegalStateException("USE_LOCAL_MODEL is false but OpenAIGrader was not provided");
            }
        }

        // Initialize Schoology integration (comments and/or grades)
        this.enableSchoologyComments = Config.getBoolean("ENABLE_SCHOOLOGY_COMMENTS");
        this.enableSchoologyGrades = Config.getBoolean("ENABLE_SCHOOLOGY_GRADES");

        if (enableSchoologyComments || enableSchoologyGrades) {
            String baseUrl = Config.get("SCHOOLOGY_BASE_URL");
            String courseId = Config.get("SCHOOLOGY_COURSE_ID");
            String sessionCookie = readSchoologyCookie();
            String csrfKey = Config.get("SCHOOLOGY_CSRF_KEY");
            String csrfToken = Config.get("SCHOOLOGY_CSRF_TOKEN");

            // Grade posting requires additional parameters
            String gradingPeriodId = Config.get("SCHOOLOGY_GRADING_PERIOD_ID");
            String sectionId = Config.get("SCHOOLOGY_SECTION_ID");

            if (baseUrl == null || courseId == null || this.assignmentId == null ||
                sessionCookie == null || csrfKey == null || csrfToken == null) {
                throw new IllegalStateException(
                    "ENABLE_SCHOOLOGY_COMMENTS/GRADES is true but required configuration is missing. " +
                    "Required env vars: SCHOOLOGY_BASE_URL, SCHOOLOGY_COURSE_ID, SCHOOLOGY_ASSIGNMENT_ID, " +
                    "SCHOOLOGY_CSRF_KEY, SCHOOLOGY_CSRF_TOKEN. " +
                    "Cookie: Create .schoology-cookie file in project root OR set SCHOOLOGY_SESSION_COOKIE env var."
                );
            }

            if (enableSchoologyGrades && (gradingPeriodId == null || sectionId == null)) {
                throw new IllegalStateException(
                    "ENABLE_SCHOOLOGY_GRADES is true but required configuration is missing. " +
                    "Required env vars: SCHOOLOGY_GRADING_PERIOD_ID, SCHOOLOGY_SECTION_ID"
                );
            }

            this.schoologyCommentUpdater = new SchoologyCommentUpdater(
                baseUrl, courseId, this.assignmentId, sessionCookie, csrfKey, csrfToken,
                gradingPeriodId, sectionId
            );

            if (enableSchoologyComments && enableSchoologyGrades) {
                LOGGER.info("Schoology grade posting and comment posting are ENABLED");
            } else if (enableSchoologyGrades) {
                LOGGER.info("Schoology grade posting is ENABLED");
            } else {
                LOGGER.info("Schoology comment posting is ENABLED");
            }
        } else {
            this.schoologyCommentUpdater = null;
            LOGGER.info("Schoology integration is DISABLED");
        }
    }

    /**
     * Reads Schoology session cookie from file or environment variable.
     * Tries .schoology-cookie file first, falls back to SCHOOLOGY_SESSION_COOKIE env var.
     */
    private String readSchoologyCookie() {
        // Try reading from file first (better for values with semicolons)
        Path cookieFile = Path.of(".schoology-cookie");
        if (Files.exists(cookieFile)) {
            try {
                String cookie = Files.readString(cookieFile, StandardCharsets.UTF_8).trim();
                if (!cookie.isEmpty()) {
                    LOGGER.debug("Read Schoology cookie from .schoology-cookie file");
                    return cookie;
                }
            } catch (IOException ex) {
                LOGGER.warn("Failed to read .schoology-cookie file: {}", ex.getMessage());
            }
        }

        // Fall back to environment variable
        String envCookie = Config.get("SCHOOLOGY_SESSION_COOKIE");
        if (envCookie != null && !envCookie.isEmpty()) {
            LOGGER.debug("Read Schoology cookie from SCHOOLOGY_SESSION_COOKIE env var");
            return envCookie;
        }

        return null;
    }

    public void gradeAll(Path submissionsPath, Path resultsDir) throws IOException {
        // Initialize grading cache in persistent location (not timestamped folder)
        // This ensures cache persists across runs for cost protection
        gradingCache = new GradingCache("results");

        Map<String, String> submissions = fileReader.readStudentSubmissions(submissionsPath);
        List<GradeRecord> records = new ArrayList<>();
        List<SchoologyRecord> schoologyRecords = new ArrayList<>();

        // Fetch student UID mappings from Schoology if any integration is enabled
        if ((enableSchoologyComments || enableSchoologyGrades) && schoologyCommentUpdater != null) {
            try {
                schoologyCommentUpdater.fetchStudentMappings();
            } catch (Exception ex) {
                LOGGER.error("Failed to fetch Schoology student mappings. Schoology integration will be skipped.", ex);
                // Continue with grading - integration will fail individually but grading will work
            }
        }

        int totalStudents = submissions.size();
        int skippedCount = 0;
        int gradedCount = 0;

        for (Map.Entry<String, String> entry : submissions.entrySet()) {
            String studentKey = entry.getKey();
            String schoolUid = extractUniqueUserId(studentKey);
            Path studentDir = submissionsPath.resolve(studentKey);

            // Check revision status before processing
            int highestRevision = 0;
            try {
                highestRevision = fileReader.findHighestRevision(studentDir);
                int lastGradedRevision = gradingCache.getLastGradedRevision(schoolUid, this.assignmentId);

                if (highestRevision <= lastGradedRevision) {
                    LOGGER.info("⊘ Skipped {} - Revision {} already graded (last graded: revision {})",
                            studentKey, highestRevision, lastGradedRevision);
                    skippedCount++;
                    continue;
                } else if (lastGradedRevision > 0) {
                    LOGGER.info("→ New submission detected for {} - Revision {} (previously graded: revision {})",
                            studentKey, highestRevision, lastGradedRevision);
                }
            } catch (Exception ex) {
                LOGGER.warn("Failed to check revision for {}: {}. Will attempt to grade.", studentKey, ex.getMessage());
                highestRevision = 1; // Assume revision 1 if check fails
            }

            String swiftCode = entry.getValue();
            if (swiftCode.isBlank()) {
                LOGGER.warn("Skipping {} because no Swift code was extracted.", studentKey);
                records.add(GradeRecord.failed(studentKey, "No Swift files located in latest submission"));
                schoologyRecords.add(SchoologyRecord.failed(studentKey));
                continue;
            }
            String prompt = assignmentPrompt.buildPrompt(swiftCode);

            final int currentRevision = highestRevision;

            try {
                GradingResult result;
                if (useLocalModel) {
                    result = lmStudioGrader.gradeSubmission(studentKey, prompt);
                } else {
                    result = openAIGrader.gradeSubmission(studentKey, prompt);
                }
                records.add(GradeRecord.from(studentKey, result));
                schoologyRecords.add(SchoologyRecord.from(studentKey, result));
                logResult(studentKey, result);

                // Update cache with successful grading
                gradingCache.updateGrade(
                    schoolUid,
                    studentKey,
                    this.assignmentId,
                    schoologyAssignmentColumnName,
                    currentRevision,
                    result.score()
                );
                gradedCount++;

                // Post grade to Schoology if enabled
                if (enableSchoologyGrades && schoologyCommentUpdater != null) {
                    try {
                        schoologyCommentUpdater.postGrade(schoolUid, result.score());
                    } catch (Exception gradeEx) {
                        LOGGER.warn("Failed to post Schoology grade for {}: {}", studentKey, gradeEx.getMessage());
                        // Continue processing - CSV still works even if grade posting fails
                    }
                }

                // Post comment to Schoology if enabled
                if (enableSchoologyComments && schoologyCommentUpdater != null) {
                    String feedbackComment = result.feedback().studentSummary();

                    if (feedbackComment != null && !feedbackComment.isBlank()) {
                        try {
                            schoologyCommentUpdater.postComment(schoolUid, feedbackComment);
                        } catch (Exception commentEx) {
                            LOGGER.warn("Failed to post Schoology comment for {}: {}", studentKey, commentEx.getMessage());
                            // Continue processing - CSV still works even if comment posting fails
                        }
                    } else {
                        LOGGER.debug("No feedback summary available for {}, skipping comment post", studentKey);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Grading failed for {}", studentKey, ex);
                records.add(GradeRecord.failed(studentKey, ex.getMessage()));
                schoologyRecords.add(SchoologyRecord.failed(studentKey));
            }
        }

        // Save cache and print summary
        gradingCache.save();
        LOGGER.info("═══════════════════════════════════════════");
        LOGGER.info("Grading Summary:");
        LOGGER.info("  Total students: {}", totalStudents);
        LOGGER.info("  Newly graded: {}", gradedCount);
        LOGGER.info("  Skipped (already graded): {}", skippedCount);
        LOGGER.info("  " + gradingCache.getSummary());
        LOGGER.info("═══════════════════════════════════════════");

        if (!records.isEmpty()) {
            writeCsv(resultsDir, records);
            writeSchoologyCsv(resultsDir, schoologyRecords);
        } else {
            LOGGER.warn("No submissions processed. No CSV will be generated.");
        }
    }

    private void logResult(String studentKey, GradingResult result) {
        LOGGER.info("{} -> Score: {} / {} | MVP: {} | Stretch Goals: {} | Compile: {}",
                studentKey,
                result.score(),
                result.maxScore(),
                result.mvpComplete(),
                result.stretchGoalsCompleted(),
                result.compileIssues());
    }

    private void writeCsv(Path resultsDir, List<GradeRecord> records) throws IOException {
        Files.createDirectories(resultsDir);
        Path outputFile = resultsDir.resolve("grades-" + TIMESTAMP.format(LocalDateTime.now()) + ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write("FolderName,Score,MVP,Stretch1,Stretch2,Stretch3,CompileIssues,Feedback\n");
            for (GradeRecord record : records) {
                writer.write(record.toCsvRow());
                writer.write('\n');
            }
        }
        LOGGER.info("CSV results written to {}", outputFile.toAbsolutePath());
    }

    private record GradeRecord(String studentKey,
                               String score,
                               String mvp,
                               String stretch1,
                               String stretch2,
                               String stretch3,
                               String compileIssues,
                               String feedback) {

        private static GradeRecord from(String studentKey, GradingResult result) {
            List<String> stretchGoals = result.stretchGoalsCompleted();
            return new GradeRecord(
                    studentKey,
                    formatScore(result.score(), result.maxScore()),
                    Boolean.toString(result.mvpComplete()),
                    Boolean.toString(stretchGoals.contains("stretch1")),
                    Boolean.toString(stretchGoals.contains("stretch2")),
                    Boolean.toString(stretchGoals.contains("stretch3")),
                    sanitizeForCsv(result.compileIssues()),
                    sanitizeForCsv(joinFeedback(result))
            );
        }

        private static GradeRecord failed(String studentKey, String error) {
            return new GradeRecord(studentKey, "0/10", "false", "false", "false", "false",
                    sanitizeForCsv("error"), sanitizeForCsv(error));
        }

        private static String joinFeedback(GradingResult result) {
            String summary = normalizeSummary(result.feedback().studentSummary());
            if (!summary.isBlank()) {
                return summary;
            }

            List<String> sentences = new ArrayList<>();
            List<String> strengths = result.feedback().strengths();
            List<String> improvements = result.feedback().improvements();
            List<String> syntaxIssues = result.feedback().syntaxErrors();

            String strengthSummary = summarizeList(strengths);
            if (!strengthSummary.isBlank()) {
                sentences.add(ensurePeriod("You nailed: " + strengthSummary));
            }

            String improvementSummary = summarizeList(improvements);
            if (!improvementSummary.isBlank()) {
                sentences.add(ensurePeriod("To raise your score next time, focus on: " + improvementSummary));
            }

            String syntaxSummary = summarizeList(syntaxIssues);
            if (!syntaxSummary.isBlank()) {
                sentences.add(ensurePeriod("Fix syntax issues such as: " + syntaxSummary));
            }

            if (sentences.isEmpty()) {
                sentences.add("Keep going—you are close! Please review the rubric for any missing requirements.");
            }

            return sentences.stream().limit(3).collect(Collectors.joining(" "));
        }

        private static String summarizeList(List<String> items) {
            return items.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .limit(3)
                    .collect(Collectors.joining("; "));
        }

        private static String normalizeSummary(String summary) {
            if (summary == null) {
                return "";
            }
            String cleaned = summary.replaceAll("\\s+", " ").trim();
            if (cleaned.isEmpty()) {
                return "";
            }
            return ensurePeriod(cleaned);
        }

        private static String ensurePeriod(String sentence) {
            if (sentence == null || sentence.isBlank()) {
                return "";
            }
            String trimmed = sentence.trim();
            if (trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?")) {
                return trimmed;
            }
            return trimmed + ".";
        }

        private static String formatScore(double score, double maxScore) {
            return String.format(Locale.US, "%.1f/%.0f", score, maxScore);
        }

        public String toCsvRow() {
            return String.join(",",
                    sanitizeForCsv(studentKey),
                    score,
                    mvp,
                    stretch1,
                    stretch2,
                    stretch3,
                    sanitizeForCsv(compileIssues),
                    '"' + feedback + '"');
        }
    }

    private record SchoologyRecord(String uniqueUserId, String score) {

        private static SchoologyRecord from(String studentKey, GradingResult result) {
            return new SchoologyRecord(extractUniqueUserId(studentKey), formatSchoologyScore(result.score()));
        }

        private static SchoologyRecord failed(String studentKey) {
            return new SchoologyRecord(extractUniqueUserId(studentKey), formatSchoologyScore(0));
        }

        public String toCsvRow() {
            return String.join(",",
                    sanitizeForCsv(uniqueUserId),
                    sanitizeForCsv(score));
        }
    }

    private void writeSchoologyCsv(Path resultsDir, List<SchoologyRecord> records) throws IOException {
        Files.createDirectories(resultsDir);
        Path outputFile = resultsDir.resolve("schoology-grades-" + TIMESTAMP.format(LocalDateTime.now()) + ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write("Unique User ID," + sanitizeForCsv(schoologyAssignmentColumnName) + "\n");
            for (SchoologyRecord record : records) {
                writer.write(record.toCsvRow());
                writer.write('\n');
            }
        }
        LOGGER.info("Schoology CSV results written to {}", outputFile.toAbsolutePath());
    }

    private static String sanitizeForCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ').replace("\"", "'");
    }

    private static String extractUniqueUserId(String studentKey) {
        if (studentKey == null || studentKey.isBlank()) {
            return "";
        }
        int lastDash = studentKey.lastIndexOf('-');
        if (lastDash == -1) {
            return studentKey.trim();
        }
        return studentKey.substring(lastDash + 1).trim();
    }

    private static String formatSchoologyScore(double score) {
        if (Double.isNaN(score)) {
            return "0";
        }
        if (Math.abs(score - Math.rint(score)) < 1e-9) {
            return String.format(Locale.US, "%.0f", score);
        }
        return String.format(Locale.US, "%.1f", score);
    }
}
