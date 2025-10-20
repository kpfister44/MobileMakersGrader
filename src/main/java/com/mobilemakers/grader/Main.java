package com.mobilemakers.grader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length == 1 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
            printUsage();
            return;
        }

        Path submissionsPath = args.length > 0 ? Path.of(args[0]) : Path.of("submissions");
        Path resultsDir = args.length > 1 ? Path.of(args[1]) : Path.of("results");

        // TODO: Phase 4 - Replace with BatchGrader
        // Temporary: Load first assignment's prompt for backward compatibility during Phase 1-3
        try {
            String promptText = PromptLoader.loadPrompt("MultiplyPrompt");
            SwiftFileReader reader = new SwiftFileReader();
            AssignmentPrompt prompt = new AssignmentPrompt(promptText);
            OpenAIGrader openAIGrader = new OpenAIGrader();
            LMStudioGrader lmStudioGrader = new LMStudioGrader();
            String schoologyAssignmentName = Config.get("SCHOOLOGY_ASSIGNMENT_NAME");
            GradeProcessor processor = (schoologyAssignmentName == null || schoologyAssignmentName.isBlank())
                    ? new GradeProcessor(reader, prompt, openAIGrader, lmStudioGrader, "Assignment")
                    : new GradeProcessor(reader, prompt, openAIGrader, lmStudioGrader, schoologyAssignmentName);

            processor.gradeAll(submissionsPath, resultsDir);
        } catch (Exception ex) {
            LOGGER.error("Grading run failed", ex);
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar swift-grader.jar [submissionsDir] [resultsDir]");
        System.out.println("Defaults: submissionsDir=./submissions, resultsDir=./results");
    }
}
