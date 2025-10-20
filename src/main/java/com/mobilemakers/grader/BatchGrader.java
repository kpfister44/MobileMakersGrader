package com.mobilemakers.grader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates batch grading across multiple assignments.
 * Handles downloading, extracting, and grading all configured assignments.
 */
public class BatchGrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchGrader.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final SwiftFileReader swiftFileReader;
    private final OpenAIGrader openAIGrader;
    private final LMStudioGrader lmStudioGrader;
    private final SchoologySubmissionDownloader submissionDownloader;
    private final SubmissionCache submissionCache;
    private final List<AssignmentSummary> completedAssignments;
    private final List<AssignmentSummary> skippedAssignments;

    public BatchGrader() {
        this.swiftFileReader = new SwiftFileReader();
        this.openAIGrader = new OpenAIGrader();
        this.lmStudioGrader = new LMStudioGrader();
        this.submissionCache = new SubmissionCache("results");
        this.completedAssignments = new ArrayList<>();
        this.skippedAssignments = new ArrayList<>();

        // Initialize Schoology submission downloader if needed
        boolean hasSchoologyConfig = hasSchoologyConfiguration();
        if (hasSchoologyConfig) {
            String baseUrl = Config.get("SCHOOLOGY_BASE_URL");
            String sessionCookie = readSchoologyCookie();
            String csrfKey = Config.get("SCHOOLOGY_CSRF_KEY");
            String csrfToken = Config.get("SCHOOLOGY_CSRF_TOKEN");

            this.submissionDownloader = new SchoologySubmissionDownloader(
                    baseUrl, sessionCookie, csrfKey, csrfToken, submissionCache);
            LOGGER.info("Schoology submission downloader initialized");
        } else {
            this.submissionDownloader = null;
            LOGGER.info("Schoology not configured - expecting manual submission uploads");
        }
    }

    /**
     * Grades all configured assignments in batch mode.
     */
    public void gradeAllAssignments() {
        Instant startTime = Instant.now();
        LOGGER.info("═══════════════════════════════════════════");
        LOGGER.info("Starting Batch Grading");
        LOGGER.info("═══════════════════════════════════════════");

        // Load assignment configurations
        List<AssignmentConfig> assignments;
        try {
            assignments = AssignmentConfig.loadFromEnvironment();
            LOGGER.info("Loaded {} assignment(s) from configuration", assignments.size());
        } catch (Exception ex) {
            LOGGER.error("Failed to load assignment configuration: {}", ex.getMessage());
            printSummary(startTime);
            return;
        }

        // Process each assignment
        for (int i = 0; i < assignments.size(); i++) {
            AssignmentConfig assignment = assignments.get(i);
            LOGGER.info("");
            LOGGER.info("═══════════════════════════════════════════");
            LOGGER.info("Processing assignment {}/{}: {}", i + 1, assignments.size(), assignment.getName());
            LOGGER.info("  ID: {}", assignment.getId());
            LOGGER.info("  Prompt: {}", assignment.getPromptClassName());
            LOGGER.info("═══════════════════════════════════════════");

            try {
                processAssignment(assignment);
                completedAssignments.add(new AssignmentSummary(assignment.getName(), true, null));
                LOGGER.info("✓ Completed: {}", assignment.getName());
            } catch (Exception ex) {
                String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                skippedAssignments.add(new AssignmentSummary(assignment.getName(), false, errorMsg));
                LOGGER.error("✗ Skipped: {} - {}", assignment.getName(), errorMsg);
                LOGGER.debug("Full error details:", ex);
                // Continue with next assignment
            }
        }

        // Print final summary
        printSummary(startTime);
    }

    /**
     * Processes a single assignment: download, extract, load prompt, grade.
     */
    private void processAssignment(AssignmentConfig assignment) throws Exception {
        // Step 1: Get submissions directory
        Path submissionsDir = getSubmissionsDirectory(assignment);

        // Step 2: Verify submissions directory exists and has content
        if (!Files.exists(submissionsDir)) {
            throw new Exception("Submissions directory does not exist: " + submissionsDir);
        }

        long submissionCount = Files.list(submissionsDir).count();
        if (submissionCount == 0) {
            throw new Exception("Submissions directory is empty: " + submissionsDir);
        }

        LOGGER.info("→ Found {} student submission(s)", submissionCount);

        // Step 3: Load prompt dynamically
        String promptText;
        try {
            promptText = PromptLoader.loadPrompt(assignment.getPromptClassName());
            LOGGER.info("→ Loaded prompt: {}", assignment.getPromptClassName());
        } catch (ClassNotFoundException ex) {
            throw new Exception("Prompt class not found: " + assignment.getPromptClassName() +
                    ". Verify class exists in com.mobilemakers.grader.prompts package", ex);
        }

        // Step 4: Create assignment-specific components
        AssignmentPrompt assignmentPrompt = new AssignmentPrompt(promptText);

        // Step 5: Create GradeProcessor with assignment-specific parameters
        GradeProcessor processor = new GradeProcessor(
                swiftFileReader,
                assignmentPrompt,
                openAIGrader,
                lmStudioGrader,
                assignment.getName(),
                assignment.getId(),
                assignment.getName()
        );

        // Step 6: Create results directory for this assignment
        String timestamp = TIMESTAMP.format(LocalDateTime.now());
        Path resultsDir = Path.of("results", assignment.getSanitizedName() + "-" + timestamp);
        Files.createDirectories(resultsDir);

        LOGGER.info("→ Grading submissions...");

        // Step 7: Grade all students
        processor.gradeAll(submissionsDir, resultsDir);

        LOGGER.info("→ Results saved to: {}", resultsDir);
    }

    /**
     * Gets the submissions directory for an assignment.
     * Downloads from Schoology if configured, otherwise expects manual upload.
     */
    private Path getSubmissionsDirectory(AssignmentConfig assignment) throws Exception {
        if (submissionDownloader != null) {
            // Download and extract from Schoology
            LOGGER.info("→ Downloading submissions from Schoology...");
            return submissionDownloader.downloadAndExtractIfNeeded(
                    assignment.getId(),
                    assignment.getName()
            );
        } else {
            // Expect manual upload to submissions/{AssignmentName}/
            Path manualPath = Path.of("submissions", assignment.getSanitizedName());
            LOGGER.info("→ Using manually uploaded submissions from: {}", manualPath);
            return manualPath;
        }
    }

    /**
     * Checks if Schoology configuration is present.
     */
    private boolean hasSchoologyConfiguration() {
        String baseUrl = Config.get("SCHOOLOGY_BASE_URL");
        String csrfKey = Config.get("SCHOOLOGY_CSRF_KEY");
        String csrfToken = Config.get("SCHOOLOGY_CSRF_TOKEN");
        String cookie = readSchoologyCookie();

        return baseUrl != null && csrfKey != null && csrfToken != null && cookie != null;
    }

    /**
     * Reads Schoology session cookie from file or environment variable.
     */
    private String readSchoologyCookie() {
        // Try reading from file first
        Path cookieFile = Path.of(".schoology-cookie");
        if (Files.exists(cookieFile)) {
            try {
                String cookie = Files.readString(cookieFile).trim();
                if (!cookie.isEmpty()) {
                    return cookie;
                }
            } catch (Exception ex) {
                LOGGER.debug("Failed to read .schoology-cookie file: {}", ex.getMessage());
            }
        }

        // Fall back to environment variable
        return Config.get("SCHOOLOGY_SESSION_COOKIE");
    }

    /**
     * Prints final summary of batch grading run.
     */
    private void printSummary(Instant startTime) {
        Duration elapsed = Duration.between(startTime, Instant.now());
        long minutes = elapsed.toMinutes();
        long seconds = elapsed.minusMinutes(minutes).getSeconds();

        LOGGER.info("");
        LOGGER.info("═══════════════════════════════════════════");
        LOGGER.info("Batch Grading Summary");
        LOGGER.info("═══════════════════════════════════════════");
        LOGGER.info("Total assignments: {}", completedAssignments.size() + skippedAssignments.size());
        LOGGER.info("Successfully graded: {}", completedAssignments.size());
        LOGGER.info("Skipped (errors): {}", skippedAssignments.size());
        LOGGER.info("Total time: {}m {}s", minutes, seconds);

        if (!completedAssignments.isEmpty()) {
            LOGGER.info("");
            LOGGER.info("Completed assignments:");
            for (AssignmentSummary summary : completedAssignments) {
                LOGGER.info("  ✓ {}", summary.name);
            }
        }

        if (!skippedAssignments.isEmpty()) {
            LOGGER.info("");
            LOGGER.info("Skipped assignments:");
            for (AssignmentSummary summary : skippedAssignments) {
                LOGGER.info("  ✗ {} - {}", summary.name, summary.error);
            }
        }

        LOGGER.info("═══════════════════════════════════════════");

        // Save submission cache if used
        if (submissionCache != null) {
            submissionCache.save();
        }
    }

    /**
     * Summary record for a single assignment.
     */
    private record AssignmentSummary(String name, boolean success, String error) {
    }
}
