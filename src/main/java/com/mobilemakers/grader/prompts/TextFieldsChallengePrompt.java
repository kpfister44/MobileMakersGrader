package com.mobilemakers.grader.prompts;

/**
 * Hardcoded grading rubric for the TextFields Challenge.
 */
public final class TextFieldsChallengePrompt {

    private TextFieldsChallengePrompt() {
    }

    public static final String PROMPT = """
          You are grading a Swift programming assignment on TextFields in SwiftUI.

          """ + PromptConstants.LENIENT_GRADING_PHILOSOPHY + """

          """ + PromptConstants.CRITICAL_GRADING_RULES + """

          GRADING CRITERIA:

          MVP (8 points total) — ALL FOUR must be present for mvpComplete=true:

          1. Rounded border name TextField (2 pts). A TextField with placeholder text “Enter your name” that has a visually rounded border appearance. Accept any approach that makes it look rounded (for example, .textFieldStyle(.roundedBorder) or .padding + .background(Color(.systemGray6)) + .cornerRadius(...) or .overlay(RoundedRectangle(...))). The placeholder text should read “Enter your name” (case-insensitive; ignore surrounding whitespace).
          2. Hello greeting (3 pts). When the user enters their name, the UI displays “Hello <name>” somewhere visible. This can be conditional (for example, empty string shows nothing or space).
          3. Compiles and runs (1 pt). Check for missing braces/parentheses or syntax errors that would prevent compilation. If major syntax errors exist, mark compileIssues = "major". Otherwise "none" or "minor".
          4. Basic Swift naming and proper TextField binding (1 pt). Variables in camelCase (lowercase first letter). TextField binding syntax is correct with $ where required. Code is readable and follows SwiftUI conventions. Do not be strict about exact variable names.

          STRETCH GOALS (0.5 points each, max +2 points): Only count as complete if the required items in their respective MARK sections are present.

          Stretch #1 (0.5): Two number-pad TextFields in an HStack. In the "MARK: Stretch #1" section: HStack with two TextFields bound to $number1 and $number2 (either value: with numeric bindings or text: with converters is acceptable). Both have a rounded appearance (either .textFieldStyle(.roundedBorder) or equivalent). Both set .keyboardType(.numberPad). The equation line is present (not commented). If HStack missing, bindings wrong, no numberPad, or equation line commented, not complete.

          Stretch #2 (0.5): Currency TextField with specific dimensions. In "MARK: Stretch #2": TextField bound to $billAmount (or a clearly equivalent approach where user input is parsed into a numeric and displayed as currency). Has approximately 300×50pt frame (small variance allowed). Currency formatting (for example, .currency(code: "USD") or manual NumberFormatter producing USD display). Centered text alignment. The formatted amount line is present (not commented). If any required behavior is missing, not complete.

          Stretch #3 (0.5): Email and password with button and alert. In "MARK: Stretch #3": Email TextField with "Enter your email" and either .textInputAutocapitalization(.never) or .autocapitalization(.none). SecureField for password with "Enter your password". Both visually rounded. Button and alert lines present (not commented). Missing any of these, not complete.

          Stretch #4 (0.5): Two TextFields with focus-triggered alerts. In "MARK: Stretch #4": Two TextFields, "Field #1" and "Field #2", bound to $field1Text and $field2Text. Tapping/focusing each sets field1Complain = true or field2Complain = true. .alert modifiers show the complaint messages. Visually rounded. If bindings/tap triggers/alerts missing, not complete.

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
          Use 2-3 complete sentences in "feedback.studentSummary" with clear, specific guidance (except when the student earns 10/10). Reference the most important strengths and exactly what to improve next to raise the score. If the student earns 10/10, provide a single celebratory sentence confirming everything is complete and there are no required changes.
            """;
}
