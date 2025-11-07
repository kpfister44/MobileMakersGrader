package com.mobilemakers.grader.prompts;

/**
 * Hardcoded grading rubric for the Views, SubViews, and Navigation Challenge.
 */
public final class ViewsSubViewsNavigationPrompt {

    private ViewsSubViewsNavigationPrompt() {
    }

    public static final String PROMPT = """
            You are grading a Swift programming assignment on Views, SubViews, and Navigation in SwiftUI.

            """ + PromptConstants.LENIENT_GRADING_PHILOSOPHY + """

            """ + PromptConstants.CRITICAL_GRADING_RULES + """

            GRADING CRITERIA:

            MVP (8 points total) — ALL FOUR required for mvpComplete=true:
            
            1. As a user, I want to see a view that is blue and has the text “Blue View” in the top of the canvas. (2 points)
            2. As a user, I want to see a green view and text that says “Green View” just below the blue view. (2 points)
            3. As a user, I want to see a red view and text that says “Red View” in just below the green view. (2 points)
            4. As a user, I want to see a navigation link that says “Go To The Yellow View” in the bottom of the screen. When I press the button, I should see a yellow view and text that says “Yellow View.” (2 points)
            
            STRETCH GOALS (1 points each, max +2 points):

            Stretch #1 (1 point): As a user, when I press on the Stretch #1 tab at the bottom of the canvas, I want to see a game of tic-tac-toe where the winning move is a different color than the other X’s and 0’s.
            Stretch #2 (1 point): As a user, when I press on the Stretch #2 tab at the bottom of the canvas, I want to see two system images that are links to other pages. When I visit the other pages, I want to see an image on the screen.
            
            STUDENT CODE:
            %s

            RESPOND WITH ONLY VALID JSON (no extra text before or after):
            {
                "score": X.X,
                "maxScore": 10,
                "mvpComplete": true/false,
                "stretchGoalsCompleted": ["stretch1", "stretch2"],
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
            Use 2-3 complete sentences in "feedback.studentSummary" with clear, specific guidance (except when student earns 10/10). Reference the most important strengths and exactly what to improve next to raise the score. If the student earns 10/10, provide a single celebratory sentence confirming everything is complete. Mention specific view names, MARK sections, and requirements that are missing or incomplete.
            """;
}
