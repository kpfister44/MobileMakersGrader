package com.mobilemakers.grader.prompts;

/**
 * Hardcoded grading rubric for the Events Challenge.
 */
public final class EventsChallengePrompt {

    private EventsChallengePrompt() {
    }

    public static final String PROMPT = """
            You are grading a Swift programming assignment on Events and Event Handlers in SwiftUI.

            """ + PromptConstants.LENIENT_GRADING_PHILOSOPHY + """

            """ + PromptConstants.CRITICAL_GRADING_RULES + """
            6. Event handler names should follow SwiftUI conventions (onAppear, onDisappear, onSubmit, etc.), but accept functional equivalents

            GRADING CRITERIA:

            MVP (8 points total) — ALL FOUR required for mvpComplete=true:

            1. Images imported (1 points). Project must contain two image assets or image files. 

            2. onAppear event handler (3 points). In "MARK: MVP" section: Image view must have .onAppear modifier with closure. Inside closure must contain if...else statement (or equivalent conditional logic like ternary operator or if statement without else) that toggles imageName variable between the two image names. Logic must toggle back and forth. If onAppear missing, no conditional logic, or doesn't toggle both ways, not complete.

            3. onDisappear event handler (3 points). In "MARK: MVP" section: Image view must have .onDisappear modifier with closure. Inside closure must increase size variable by 50 (can use += 50 or size = size + 50). If onDisappear missing or doesn't increase size by 50, not complete.

            4. Code compiles and follows Swift conventions (1 points). No major syntax errors (missing braces, incorrect modifier syntax).

            STRETCH GOALS (1.0 points each, max +2 points):

            Stretch #1 (1.0): TextField with return key handler. Must have TextField in ContentView that is bound to enteredText variable (or equivalent @State variable). In "MARK: Stretch #1" section (or on TextField): must have .onSubmit modifier (or equivalent like onCommit in older SwiftUI) with closure that: (1) assigns enteredText value to imageName variable, (2) resets size variable to 100, and (3) clears enteredText by setting it to empty string. All three actions required. If TextField missing, onSubmit missing, or any of the three actions missing, not complete.

            Stretch #2 (1.0): Tap gesture for arrow rotation. Must have tap gesture handler (using .onTapGesture or .gesture with TapGesture) in "MARK: Stretch #2" section. Inside gesture handler must: (1) increment arrowNumber by 1, and (2) use switch statement (or equivalent if/else chain) on arrowNumber to change arrowImage variable through all four arrow directions (arrow.up, arrow.right, arrow.down, arrow.left or similar system image names). Must handle resetting arrowNumber when it gets too large (e.g., modulo 4 or reset to 0 after 3). 

            Stretch #3 (1.0): Long press gesture with alert. Must have long press gesture (using .onLongPressGesture with minimumDuration parameter set to approximately 5 seconds, or .gesture with LongPressGesture) in "MARK: Stretch #3" section. Gesture must trigger alert presentation (sets alertPresented to true or presents alert). Must have .alert modifier that shows TextField bound to size variable (using TextField inside alert with binding to size, or binding to String that converts to size). 

            STUDENT CODE:
            %s

            RESPOND WITH ONLY VALID JSON (no extra text before or after):
            {
                "score": X.X,
                "maxScore": 10,
                "mvpComplete": true/false,
                "stretchGoalsCompleted": ["stretch1", "stretch2", "stretch3"],
                "feedback": {
                    "studentSummary": "2-3 sentence summary of performance and next steps",
                    "strengths": ["specific strength 1", "specific strength 2"],
                    "improvements": ["specific improvement 1", "specific improvement 2"],
                    "syntaxErrors": ["syntax error 1 if any", "syntax error 2 if any"]
                },
                "compileIssues": "none/minor/major"
            }

            SCORING FORMULA:
            - MVP incomplete: score = (completed MVP requirements / 4) * 8
            - MVP complete, no stretches: score = 8.0
            - MVP complete with stretches: score = 8.0 + (completed stretches * 1.0)
            - Maximum possible score: 10.0

            FEEDBACK STUDENT SUMMARY REQUIREMENTS:
            Use 2-3 complete sentences in "feedback.studentSummary" with clear, specific guidance (except when student earns 10/10). Reference the most important strengths and exactly what to improve next to raise the score. If the student earns 10/10, provide a single celebratory sentence confirming everything is complete. Mention specific event handlers (onAppear, onDisappear, onSubmit, tap gesture, long press) and requirements that are missing or incomplete.
            """;
}
