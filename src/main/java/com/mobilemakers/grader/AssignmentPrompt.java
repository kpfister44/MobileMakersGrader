package com.mobilemakers.grader;

/**
 * Provides the assignment prompt injected with the student's Swift code.
 * Supports dynamic prompt loading for batch grading mode.
 */
public class AssignmentPrompt {
    private final String promptText;

    /**
     * Creates an assignment prompt with the provided prompt text.
     *
     * @param promptText The grading rubric/prompt text
     * @throws IllegalArgumentException if promptText is null or blank
     */
    public AssignmentPrompt(String promptText) {
        if (promptText == null || promptText.isBlank()) {
            throw new IllegalArgumentException("Prompt text cannot be null or blank");
        }
        this.promptText = promptText;
    }

    /**
     * Builds the complete prompt by injecting the student's Swift code.
     *
     * @param swiftCode The student's Swift code to grade
     * @return The complete prompt ready for AI grading
     */
    public String buildPrompt(String swiftCode) {
        String safeCode = swiftCode == null || swiftCode.isBlank() ? "// No code submitted" : swiftCode;
        return promptText.replace("%s", safeCode);
    }
}
