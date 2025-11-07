package com.mobilemakers.grader.prompts;

/**
 * Hardcoded grading rubric for the Functions Challenge.
 */
public final class FunctionsChallengePrompt {

    private FunctionsChallengePrompt() {
    }

    public static final String PROMPT = """
            You are grading a Swift programming assignment on Functions in Swift.

            """ + PromptConstants.LENIENT_GRADING_PHILOSOPHY + """

            """ + PromptConstants.CRITICAL_GRADING_RULES + """
            6. Function names, parameter names, and return types should match specifications, but accept reasonable variations if functionality is correct
            7. MARK sections should contain the appropriate code - functions in Part I sections, calls in Part II sections

            GRADING CRITERIA:

            MVP (8 points total) — ALL FOUR required for mvpComplete=true:

            1. displayMVP function defined (3 points). In "MARK: MVP - Part I" section: function named displayMVP that is void (no return type) and takes no parameters. Must match exact signature: func displayMVP().

            2. Function sets output variable (2 points). Inside displayMVP function: assigns the string "MVP Completed" to the output variable.

            3. Function is called correctly (2 points). In "MARK: MVP - Part II" section (within case 0 of switch statement): contains call to displayMVP() (not commented out).

            4. Code compiles and follows Swift conventions (1 points). No major syntax errors (missing braces, incorrect function syntax). 

            STRETCH GOALS (0.5 points each, max +2 points, only first 4 completed count):

            Stretch #1 (0.5): returnAString function. In "MARK: Stretch #1 - Part I": function named returnAString, no parameters, returns String type. Returns "Stretch #1 Complete" (exact text). In "MARK: Stretch #1 - Part II" (case 1): assigns result of returnAString() call to output variable (not commented). If function signature wrong, return value wrong, or call missing/commented, not complete.

            Stretch #2 (0.5): createSentence function with parameter. In "MARK: Stretch #2 - Part I": function named createSentence with String parameter named favoriteClass, returns String. Returns sentence "I love [favoriteClass] class!" using the parameter. In "MARK: Stretch #2 - Part II" (case 2): calls createSentence with a String argument and assigns to output (not commented). If parameter name wrong, return format wrong, or call missing, not complete.

            Stretch #3 (0.5): createFruitLovingSentence function with labeled parameters. In "MARK: - Stretch #3": function named createFruitLovingSentence that takes two String parameters with appropriate labels (must match the uncommented call: createFruitLovingSentence("apples", And: "bananas")). Returns sentence "I love [fruit1] and [fruit2]" using both parameters. The TODO comment line must be uncommented for credit. If function signature doesn't match call, or line still commented, not complete.

            Stretch #4 (0.5): countTheCharacters function. In "MARK: - Stretch #4": function named countTheCharacters that takes String parameter and returns Int representing character count. Must actually count characters in string (e.g., using .count). The TODO comment lines must be uncommented. If function missing, wrong return type, or lines commented, not complete.

            Stretch #5 (0.5): findVowelsConsonants function with tuple return. In "MARK: - Stretch #5": function named findVowelsConsonants that takes String parameter and returns tuple with two Int values (vowel count, consonant count). Must actually count vowels and consonants (a, e, i, o, u are vowels, case-insensitive). The TODO comment lines must be uncommented. If tuple structure wrong, counting logic missing, or lines commented, not complete.

            Stretch #6 (0.5): findVowelsConsonantsPunctuation function. In "MARK: - Stretch #6": function named findVowelsConsonantsPunctuation (note spelling) that takes String parameter and returns tuple with three Int values (vowels, consonants, punctuation/spaces). Must count all three categories. The TODO comment lines must be uncommented. If tuple has wrong number of elements, counting incomplete, or lines commented, not complete.

            NOTE: Only the first 4 completed stretch goals count toward the +2 maximum bonus. Stretch #5 and #6 do not add points beyond the 10.0 maximum but should still be tracked in stretchGoalsCompleted array.

            STUDENT CODE:
            %s

            RESPOND WITH ONLY VALID JSON (no extra text before or after):
            {
                "score": X.X,
                "maxScore": 10,
                "mvpComplete": true/false,
                "stretchGoalsCompleted": ["stretch1", "stretch2", "stretch3", "stretch4", "stretch5", "stretch6"],
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
            Use 2-3 complete sentences in "feedback.studentSummary" with clear, specific guidance (except when student earns 10/10). Reference the most important strengths and exactly what to improve next to raise the score. If the student earns 10/10, provide a single celebratory sentence confirming everything is complete. Mention specific function names and requirements that are missing or incomplete.
            """;
}
