package com.mobilemakers.grader;

import static com.mobilemakers.grader.prompts.MultiplyPrompt.PROMPT;

/**
 * Provides the assignment prompt injected with the student's Swift code.
 */
public class AssignmentPrompt {

    public String buildPrompt(String swiftCode) {
        if (PROMPT == null || PROMPT.isBlank()) {
            throw new IllegalStateException("Active assignment prompt is empty. Set PROMPT constant before grading.");
        }
        String safeCode = swiftCode == null || swiftCode.isBlank() ? "// No code submitted" : swiftCode;
        return PROMPT.replace("%s", safeCode);
    }
}
