package com.mobilemakers.grader;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for a single assignment in batch grading mode.
 * Parses assignment configuration from .env file.
 */
public class AssignmentConfig {
    private final String id;
    private final String name;
    private final String promptClassName;

    public AssignmentConfig(String id, String name, String promptClassName) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Assignment ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Assignment name cannot be null or blank");
        }
        if (promptClassName == null || promptClassName.isBlank()) {
            throw new IllegalArgumentException("Prompt class name cannot be null or blank");
        }

        this.id = id;
        this.name = name;
        this.promptClassName = promptClassName;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPromptClassName() {
        return promptClassName;
    }

    /**
     * Sanitizes assignment name for use in folder paths.
     * Replaces spaces with underscores and removes special characters.
     *
     * @return Sanitized name safe for file system use
     */
    public String getSanitizedName() {
        return name.replaceAll("\\s+", "_")
                   .replaceAll("[^a-zA-Z0-9_-]", "");
    }

    /**
     * Loads all assignment configurations from environment variables.
     * Expects variables in format:
     * - ASSIGNMENT_1_ID
     * - ASSIGNMENT_1_NAME
     * - ASSIGNMENT_1_PROMPT
     *
     * Numbering must be sequential starting from 1.
     * All three fields must be present for each assignment.
     *
     * @return List of assignment configurations
     * @throws IllegalStateException if configuration is invalid
     */
    public static List<AssignmentConfig> loadFromEnvironment() {
        List<AssignmentConfig> assignments = new ArrayList<>();

        int assignmentNumber = 1;
        while (true) {
            String idKey = "ASSIGNMENT_" + assignmentNumber + "_ID";
            String nameKey = "ASSIGNMENT_" + assignmentNumber + "_NAME";
            String promptKey = "ASSIGNMENT_" + assignmentNumber + "_PROMPT";

            String id = Config.get(idKey);
            String name = Config.get(nameKey);
            String promptClassName = Config.get(promptKey);

            // Stop when we encounter the first missing assignment ID
            if (id == null || id.isBlank()) {
                break;
            }

            // Validate that all three fields are present
            if (name == null || name.isBlank()) {
                throw new IllegalStateException(
                    String.format("Missing %s for assignment %d. All three fields (ID, NAME, PROMPT) are required.",
                                nameKey, assignmentNumber)
                );
            }

            if (promptClassName == null || promptClassName.isBlank()) {
                throw new IllegalStateException(
                    String.format("Missing %s for assignment %d. All three fields (ID, NAME, PROMPT) are required.",
                                promptKey, assignmentNumber)
                );
            }

            assignments.add(new AssignmentConfig(id, name, promptClassName));
            assignmentNumber++;
        }

        if (assignments.isEmpty()) {
            throw new IllegalStateException(
                "No assignments configured. Please add ASSIGNMENT_1_ID, ASSIGNMENT_1_NAME, " +
                "and ASSIGNMENT_1_PROMPT to your .env file."
            );
        }

        return assignments;
    }

    @Override
    public String toString() {
        return String.format("Assignment[id=%s, name=%s, prompt=%s]", id, name, promptClassName);
    }
}
