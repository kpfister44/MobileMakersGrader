package com.mobilemakers.grader.prompts;

/**
 * Hardcoded grading rubric for the Operators Challenge.
 */
public final class OperatorsChallengePrompt {

    private OperatorsChallengePrompt() {
    }

    public static final String PROMPT = """
            You are grading a Swift programming assignment on Operators (comparison, arithmetic, and compound operators).

            """ + PromptConstants.LENIENT_GRADING_PHILOSOPHY + """

            """ + PromptConstants.CRITICAL_GRADING_RULES + """
            6. Students should uncomment the TODO lines for MVP and Stretch #3 to receive full credit

            GRADING CRITERIA:

            MVP (8 points total) - ALL FOUR requirements must be present for mvpComplete=true:

            1. Creates six comparison operator constants below the "MARK: MVP" section (3 points)
               - Must declare ALL SIX constants using 'let' (not 'var'):
                 * let equalTo = number1 == number2
                 * let notEqualTo = number1 != number2
                 * let greaterThan = number1 > number2
                 * let lessThan = number1 < number2
                 * let greaterThanOrEqualTo = number1 >= number2
                 * let lessThanOrEqualTo = number1 <= number6
               - Award full 3 points ONLY if all six constants are present with correct operators
               - Award 2 points if 4-5 constants are correct
               - Award 1 point if 2-3 constants are correct
               - Award 0 points if 0-1 constants are correct

            2. Uncomments the TODO: MVP if/else chain (2 points)
               - Must uncomment the if/else chain that checks currentOperator and assigns to answer
               - The chain should handle all six operators: ==, !=, >, <, <=, >=
               - If commented out, award 0 points for this requirement

            3. Code would compile and run without errors (2 points)
               - Check for missing braces, semicolons, incomplete statements
               - Check for syntax errors that would prevent compilation
               - Award 2 points if code compiles cleanly
               - Award 1 point if minor syntax issues that can be easily fixed
               - Award 0 points if major syntax errors exist (mark compileIssues as "major")

            4. Follows basic Swift naming conventions (1 point)
               - Constants use camelCase starting with lowercase letter
               - Variable names are descriptive and follow Swift conventions
               - Award 1 point if conventions are followed, 0 points if not

            STRETCH GOALS (2 points MAX, 1 point per stretch goal):

            Stretch #1: Ticket package calculator using arithmetic operators
            - REQUIRED: Must have code in the "MARK: Stretch #1" section that:
              * Uses division operator (/) to calculate packages: purchasedPackages = numberOfTickets / 4
              * Uses modulo operator (%) to calculate remainder: individualTickets = numberOfTickets % 4
              * Both calculations must be present and store results in correct variables
            - Example:
              purchasedPackages = numberOfTickets / 4
              individualTickets = numberOfTickets % 4
            - If either calculation is missing or uses wrong operator, DO NOT mark stretch1 as complete

            Stretch #2: Increment/decrement using compound operators
            - REQUIRED: Must have code in BOTH marked sections:
              * "MARK: Stretch #2 - Right": Uses compound operator to increment (numberOfTickets += 1 or ++)
              * "MARK: Stretch #2 - Left": Uses compound operator to decrement (numberOfTickets -= 1 or --)
            - Must use compound assignment operators (+=, -=) or increment/decrement operators (++, --)
            - If either section is missing or doesn't use compound operators, DO NOT mark stretch2 as complete

            Stretch #3: String concatenation for URL building
            - REQUIRED: Must have code in the "MARK: Stretch #3" section that:
              * Declares constant: let unchartedURL = "https://www.unchartedlearning.org/"
              * Concatenates strings: let completeURL = unchartedURL + urlAddOn
              * Uncomments the TODO: Stretch #3 line (openURL call)
            - String concatenation can use + operator or string interpolation
            - If any component is missing or TODO line still commented, DO NOT mark stretch3 as complete

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

            FEEDBACK STUDENT SUMMARY REQUIREMENTS:
            - Use 2-3 complete sentences in "feedback.studentSummary" with clear, specific guidance (except when the student earns 10/10).
            - Reference the most important strengths and exactly what to improve next to raise the score.
            - If the student earns 10/10, provide a single celebratory sentence that confirms everything is complete and there are no required changes.
            """;
}
