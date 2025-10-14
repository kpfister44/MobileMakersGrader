package com.mobilemakers.grader.prompts;

/**
 * Grading rubric for the Buttons Challenge assignment.
 */
public final class ButtonsPrompt {

    private ButtonsPrompt() {
    }

    public static final String PROMPT = """
            You are grading a SwiftUI Buttons Challenge assignment.

            CRITICAL GRADING RULES:
            1. ONLY count stretch goals as complete if the code contains the actual working implementation
            2. IGNORE all commented-out code - it does NOT count toward any points
            3. IGNORE Package.swift or test files - only grade the actual SwiftUI View code
            4. Be CONSISTENT: if you mark a stretch goal as complete in stretchGoalsCompleted, do NOT mention it as missing in feedback
            5. If any MVP requirement is missing, mvpComplete MUST be false

            GRADING CRITERIA:

            MVP (8 points total) - ALL FOUR requirements must be present for mvpComplete=true:

            1. Display two square buttons side by side with red backgrounds and white numbers 1 and 2 (2 points)
               - Must have TWO buttons visible in the layout
               - Both buttons must have red background (.red or Color.red)
               - Buttons must display "1" and "2" (or similar numbering)
               - Buttons should be arranged horizontally (side by side)
               - If buttons are missing, wrong color, or stacked vertically, deduct points

            2. Use distinct SwiftUI button initializers for Button #1 and Button #2 (2 points)
               - Button #1 must use: Button("title") { action } OR Button("title", action: { })
               - Button #2 must use: Button(action: { }) { label/view }
               - The two buttons MUST use different initializer styles
               - If both use the same initializer style, deduct points

            3. Log "Button #1 Was Pressed" and "Button #2 Was Pressed" to console when tapped (2 points)
               - Must use print() statements in button actions
               - Messages must clearly indicate which button was pressed
               - Exact wording doesn't need to match, but must be distinct for each button
               - If print statements are missing or identical for both buttons, deduct points

            4. Keep layout inside the provided MARK section without altering scaffolded code (2 points)
               - Code should be organized and within the designated MARK: section
               - Scaffolded code outside the section should remain unchanged
               - If major structural changes break the template, deduct points

            STRETCH GOALS (0.5 points each, max 2 additional points total):

            Stretch #1 (0.5 points): Add a rounded blue "Change Background" button that toggles changeBackground state
            - REQUIRED:
              * Button with blue background (.blue or Color.blue)
              * Button has rounded corners (.cornerRadius or .clipShape)
              * Button label says "Change Background" (exact wording may vary)
              * Button action toggles the `changeBackground` boolean state variable
              * The toggle actually changes the background color of the root view
            - If ANY requirement is missing or commented out, DO NOT mark stretch1 as complete

            Stretch #2 (0.5 points): Add a gray capsule counter button that displays and increments counter
            - REQUIRED:
              * Button with gray background (.gray or Color.gray)
              * Button shape is capsule (.capsule or Capsule())
              * Button displays the `counter` value using string interpolation: "\\(counter)"
              * Button action increments counter (counter += 1 or similar)
            - If ANY requirement is missing or commented out, DO NOT mark stretch2 as complete

            Stretch #3 (0.5 points): Add a circular green button that shows alert using showAlert state
            - REQUIRED:
              * Button with green background (.green or Color.green)
              * Button is circular shape (.clipShape(Circle()) or similar)
              * Button action sets `showAlert = true`
              * .alert modifier present that shows "Stretch #3 Complete" title with OK button
              * Alert uses the `showAlert` state variable for presentation
            - If ANY requirement is missing or commented out, DO NOT mark stretch3 as complete

            Stretch #4 (0.5 points): Add a lightbulb button that toggles imagery and text between On/Off
            - REQUIRED:
              * Button with lightbulb image/icon (systemImage: "lightbulb" or similar)
              * Button toggles `lightBulbStatus` boolean state
              * Image changes between two states (e.g., "lightbulb.fill" and "lightbulb")
              * Nearby Text displays "On" or "Off" based on `lightBulbStatus` state
              * Text color changes between yellow (On) and white (Off)
            - If ANY requirement is missing or commented out, DO NOT mark stretch4 as complete

            SCORING FORMULA:
            - Start with 0 points
            - Award 2 points for each of the 4 MVP criteria met (max 8 points)
            - If all 4 MVP criteria are met: mvpComplete = true
            - If any MVP criteria is missing: mvpComplete = false
            - Add 0.5 points for each stretch goal actually completed
            - Maximum possible score: 10.0

            STUDENT CODE:
            %s

            RESPOND WITH ONLY VALID JSON (no extra text before or after):
            {
              "score": X.X,
              "maxScore": 10,
              "mvpComplete": true/false,
              "stretchGoalsCompleted": ["stretch1", "stretch2", "stretch3", "stretch4"],
              "feedback": {
                "studentSummary": "2-3 sentence summary of performance and next steps",
                "strengths": ["specific strength 1", "specific strength 2"],
                "improvements": ["specific improvement 1", "specific improvement 2"],
                "syntaxErrors": ["syntax error 1 if any", "syntax error 2 if any"]
              },
              "compileIssues": "none/minor/major"
            }

            FEEDBACK STUDENT SUMMARY REQUIREMENTS:
            - Use 2-3 complete sentences in "feedback.studentSummary" with clear, specific guidance (except when the student earns 10/10).
            - Reference the most important strengths and exactly what to improve next to raise the score.
            - If the student earns 10/10, provide a single celebratory sentence that confirms everything is complete and there are no required changes.
            """;
}
