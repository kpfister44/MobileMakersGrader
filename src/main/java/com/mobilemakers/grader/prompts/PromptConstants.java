package com.mobilemakers.grader.prompts;

/**
 * Shared constants used across all assignment prompts.
 */
public final class PromptConstants {

    private PromptConstants() {
    }

    /**
     * Lenient grading philosophy that prioritizes functional behavior over strict implementation details.
     * This should be inserted at the beginning of every assignment prompt to establish the grading mindset.
     */
    public static final String LENIENT_GRADING_PHILOSOPHY = """
            GRADING PHILOSOPHY - READ FIRST:

            Your primary goal is to evaluate whether the student's app FUNCTIONS as expected according to the MVP requirements.
            If the MVP works correctly when run, the student should receive full MVP credit (8/10 points minimum).
            Stretch goals add bonus points (up to +2 for a total of 10/10).

            KEY PRINCIPLES:

            1. FUNCTIONALITY FIRST: If the app's behavior matches the requirement, award full credit for that requirement.
               - Focus on "Does it work?" not "Is it implemented exactly as I would do it?"
               - Accept alternative implementations that achieve the same functional result
               - Example: A student might use different view layouts or variable names - if the app functions correctly, that's what matters

            2. BENEFIT OF THE DOUBT: When code is present and appears to implement a requirement (even with minor issues):
               - If the code compiles and the behavior is present, award full credit
               - Don't penalize for stylistic choices or alternative (but valid) Swift approaches
               - Don't deduct points for extra features or additional code beyond requirements

            3. ONLY DEDUCT WHEN:
               - A required feature is completely missing or non-functional
               - The code has major syntax errors that prevent compilation
               - The app's behavior clearly does NOT match what the MVP requirement describes

            4. EQUIVALENCE & EVIDENCE: If a student uses different-but-valid SwiftUI code that produces the required behavior:
               - Award full credit (e.g., custom styling instead of built-in modifiers, alternative state management patterns)
               - Prefer observing functional behavior over checking for exact code patterns

            5. COMMENTED CODE DOESN'T COUNT: Ignore all commented-out code when evaluating completeness

            REMEMBER: These are intro-level high school students. If their app works and meets the MVP requirements,
            they've succeeded. Be generous when the functionality is present, strict only when it's clearly absent or broken.

            """;

    /**
     * Standard critical grading rules that apply to all assignments.
     * These are more specific technical rules that complement the lenient philosophy.
     */
    public static final String CRITICAL_GRADING_RULES = """
            CRITICAL GRADING RULES:
            1. ONLY count stretch goals as complete if the code contains the actual working implementation
            2. IGNORE all commented-out code - it does NOT count toward any points
            3. IGNORE Package.swift files - only grade actual Swift source code (.swift files)
            4. Be CONSISTENT: if you mark a stretch goal as complete in stretchGoalsCompleted, do NOT mention it as missing in feedback
            5. If any MVP requirement is missing or non-functional, mvpComplete MUST be false
            """;
}
