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

        try {
            // Batch mode: Grade all configured assignments
            BatchGrader batchGrader = new BatchGrader();
            batchGrader.gradeAllAssignments();
        } catch (Exception ex) {
            LOGGER.error("Batch grading failed", ex);
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Swift Assignment Auto-Grader - Batch Mode");
        System.out.println();
        System.out.println("Usage: java -jar swift-grader.jar");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  - Configure assignments in .env file (ASSIGNMENT_1_ID, ASSIGNMENT_1_NAME, etc.)");
        System.out.println("  - Submissions will be downloaded from Schoology automatically");
        System.out.println("  - Results will be saved to results/{AssignmentName}-{timestamp}/");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help    Show this help message");
    }
}
