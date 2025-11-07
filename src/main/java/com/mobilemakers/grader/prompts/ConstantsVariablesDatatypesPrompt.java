package com.mobilemakers.grader.prompts;

/**
 * Hardcoded grading rubric for the Buttons challenge (Constants/Variables/Data Types).
 */
public final class ConstantsVariablesDatatypesPrompt {

    private ConstantsVariablesDatatypesPrompt() {
    }

    public static final String PROMPT = """
            You are grading a Swift programming assignment on Constants, Variables, and Data Types.

            """ + PromptConstants.LENIENT_GRADING_PHILOSOPHY + """

            """ + PromptConstants.CRITICAL_GRADING_RULES + """

            GRADING CRITERIA:

            MVP (8 points total) - ALL FOUR requirements must be present for mvpComplete=true:

            1. Creates @State variable showMVP of type Bool, initialized to false (2 points)
               - Variable name variations are acceptable (showMVP, ShowMVP, show_mvp)

            2. Creates String constant firstName with student's name (2 points)
               - Must be: let firstName = "ActualName" (not empty, not a placeholder)
               - Must use 'let' (constant), not 'var' (variable)
               - Must contain an actual name string value

            3. Code would compile and run without errors (2 points)
               - Check for missing braces, semicolons, incomplete statements
               - Check for syntax errors that would prevent compilation
               - If major syntax errors exist, mark compileIssues as "major"

            4. Follows basic Swift naming conventions (2 points)
               - Variables and constants start with lowercase letter (camelCase)
               - Example: showMVP (correct) vs ShowMVP (incorrect)
               - Deduct points for consistent naming violations

            STRETCH GOALS (0.5 points each, max 2 additional points total):

            Stretch #1 (0.5 points): Creates lastName, titleName, and greeting constants with proper string concatenation
            - REQUIRED: All three constants must be present and uncommented:
              * let lastName = "..."
              * let titleName = "..."
              * let greeting = "..." (must combine titleName, firstName, lastName using concatenation or interpolation)
            - Example: let greeting = "\\(titleName) \\(firstName) \\(lastName)"
            - If ANY of these three are missing, commented out, or declared as @State variables instead of constants, DO NOT mark stretch1 as complete

            Stretch #2 (0.5 points): Creates four name constants and two team constants with comma separation
            - REQUIRED: All six constants must be present and uncommented:
              * nameOne, nameTwo, nameThree, nameFour (4 name constants)
              * teamOne, teamTwo (2 team constants that combine names with commas)
            - Example: let teamOne = "\\(nameOne), \\(nameTwo)"
            - If ANY of these six constants are missing, commented out, or not properly combined, DO NOT mark stretch2 as complete

            Stretch #3 (0.5 points): Creates Int constants for scores and combines with names
            - REQUIRED:
              * Int constants for scores (e.g., let scoreOne: Int = 100, let scoreTwo: Int = 95)
              * String combinations using scores and names (can use & or + or interpolation)
            - Example: let teamHighScoreOne = nameOne + " & " + nameTwo + " " + String(scoreOne)
            - If score constants are missing, commented out, or not combined with names, DO NOT mark stretch3 as complete

            Stretch #4 (0.5 points): Clean, readable code with good variable names
            - ONLY award if code is well-organized and follows Swift conventions throughout
            - Code should have meaningful variable names and consistent formatting

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
