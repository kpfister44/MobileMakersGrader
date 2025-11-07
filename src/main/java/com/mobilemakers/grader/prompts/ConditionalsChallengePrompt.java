package com.mobilemakers.grader.prompts;

/**
 * Hardcoded grading rubric for the Conditionals Challenge.
 */
public final class ConditionalsChallengePrompt {

    private ConditionalsChallengePrompt() {
    }

    public static final String PROMPT = """
            You are grading a Swift programming assignment on Conditionals (if/else statements) in Swift.

            """ + PromptConstants.LENIENT_GRADING_PHILOSOPHY + """

            """ + PromptConstants.CRITICAL_GRADING_RULES + """
            6. String outputs should generally match the expected text, but accept minor variations if the logic and behavior are correct
            7. MARK sections should contain the appropriate conditional logic
            8. Variable names are typically: totalMoney, firstChoiceCost, secondChoiceCost, thirdChoiceCost, fourthChoiceCost, stringToReturn (but accept reasonable variations)

            GRADING CRITERIA:

            MVP (8 points total) — ALL FOUR required for mvpComplete=true:

            1. If...else statement present (2 points). In "MARK: - MVP" section: contains if...else statement that compares totalMoney with firstChoiceCost.

            2. Correct purchase logic (3 points). If condition checks if totalMoney >= firstChoiceCost (or equivalent). If true, assigns "You can purchase your first choice." to stringToReturn (exact text, including period and spacing).

            3. Correct cannot purchase logic (2 points). Else block assigns "You cannot purchase your first choice." to stringToReturn (exact text, including period and spacing).

            4. Code compiles and follows Swift conventions (1 point). No major syntax errors. Proper if...else syntax. Code is readable.

            STRETCH GOALS (0.5 points each, max +2 points, only first 4 completed count):

            Stretch #1 (0.5): Three-way conditional check. In "MARK: - Stretch #1": Uses if...else if...else structure with THREE branches. First checks totalMoney > firstChoiceCost (plenty), assigns "You have plenty of money for your first choice" to stringToReturn. Second checks totalMoney == firstChoiceCost (just enough), assigns "You have just enough money for your first choice". Third else assigns "You do not have enough money for your first choice". All three messages must be present with correct logic. If structure wrong or messages incorrect, not complete.

            Stretch #2 (0.5): Multiple choice checking (CAN only). In "MARK: - Stretch #2": Uses multiple if statements (not if...else) to check each of the four choices independently. Each if statement checks if totalMoney is sufficient for that choice AFTER accounting for previous purchases. Appends to stringToReturn messages like "You can purchase your first choice\\n" for each affordable item. Must track running total of money spent. Only outputs items that CAN be purchased (no "cannot" messages). If logic doesn't track running total or outputs "cannot" messages, not complete.

            Stretch #3 (0.5): Multiple choice checking (CAN and first CANNOT). In "MARK: - Stretch #3": Similar to Stretch #2 but uses if...else statements to check each choice. Outputs all items that CAN be purchased, then ends with the FIRST item that CANNOT be purchased (e.g., "You can purchase your first choice\\nYou can purchase your second choice\\nYou cannot purchase your third choice"). Must track running total. Must show both CAN and CANNOT messages. If doesn't show first CANNOT or logic is wrong, not complete.

            Stretch #4 (0.5): Single if...else if chain for item count. In "MARK: - Stretch #4": Uses ONE if...else if...else structure (not multiple separate if statements). Checks in order: (1) Can purchase all 4 items → "You can purchase all four items", (2) Can purchase top 3 → "You can purchase your top three items", (3) Can purchase top 2 → "You can purchase your top two items", (4) Can purchase top 1 → "You can purchase your first choice", (5) Else → "Sorry, you cannot purchase the items in this order.". Each condition checks cumulative cost. If uses multiple separate if statements instead of one chain, or messages wrong, not complete.

            Stretch #5 (0.5): Item count with spending summary. In "MARK: - Stretch #5": Same structure as Stretch #4 (single if...else if chain) but ALSO calculates and displays total spent and remaining money. Example output: "You can purchase your top three items.\\nYou have spent $120 and have $20 remaining". Must show both the purchase message AND the spending summary with dollar amounts. If doesn't calculate spending or remaining, not complete.

            NOTE: Only the first 4 completed stretch goals count toward the +2 maximum bonus. Stretch #5 does not add points beyond 10.0 maximum but should still be tracked in stretchGoalsCompleted array.

            STUDENT CODE:
            %s

            RESPOND WITH ONLY VALID JSON (no extra text before or after):
            {
                "score": X.X,
                "maxScore": 10,
                "mvpComplete": true/false,
                "stretchGoalsCompleted": ["stretch1", "stretch2", "stretch3", "stretch4", "stretch5"],
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
            - MVP complete with stretches: score = 8.0 + (min(completed stretches, 4) * 0.5)
            - Maximum possible score: 10.0

            FEEDBACK STUDENT SUMMARY REQUIREMENTS:
            Use 2-3 complete sentences in "feedback.studentSummary" with clear, specific guidance (except when student earns 10/10). Reference which conditional structures are implemented correctly and which need work. If the student earns 10/10, provide a single celebratory sentence confirming everything is complete. Mention specific MARK sections that need attention.
            """;
}
