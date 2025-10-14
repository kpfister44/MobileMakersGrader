package com.mobilemakers.grader.prompts;

/**
 * Hardcoded grading rubric for the Multiply Challenge.
 */
public final class MultiplyPrompt {

    private MultiplyPrompt() {
    }

    public static final String PROMPT = """
          You are grading a Swift programming assignment on building a Multiply app in SwiftUI.

          CRITICAL GRADING RULES (with leniency for equivalent code):

          1. ONLY count stretch goals as complete if the code contains the actual working implementation.
          2. IGNORE all commented-out code — it does NOT count toward any points.
          3. IGNORE Package.swift files — only grade actual Swift source code (.swift files).
          4. Be CONSISTENT: if you mark a stretch goal as complete in stretchGoalsCompleted, do NOT mention it as missing in feedback.
          5. If any MVP requirement is missing, mvpComplete MUST be false.
          6. Equivalence & Evidence Rule (important): If behavior is clearly implemented via different-but-valid SwiftUI code, award credit. Prefer functional behavior over exact modifier names.
          7. Benefit of the doubt: If code compiles and a requirement's behavior is present (even if implemented with different variable names or extra modifiers), award full credit for that requirement.

          GRADING CRITERIA:

          MVP (7 points total) — ALL THREE must be present for mvpComplete=true:

          1. Two TextFields for numeric input (3 pts). The app must have two TextFields that accept numeric input. Accept any approach that allows users to enter numbers (text bindings with conversion, numeric bindings, appropriate keyboard types, etc.).

          2. Display the multiplication result (3 pts). The app must calculate and display the product of the two numbers entered in the TextFields. The result can be displayed in a Text view, Label, or any visible UI element. The multiplication calculation must be present and functional.

          3. Code compiles and runs + basic Swift naming conventions (1 pt). Check for missing braces/parentheses or syntax errors that would prevent compilation. Variables should follow camelCase naming (lowercase first letter). TextField bindings should use correct syntax with $ where required. If major syntax errors exist, mark compileIssues = "major". Otherwise "none" or "minor".

          STRETCH GOALS (0.5 points each, maximum total score is 10/10):

          FUNCTIONALITY STRETCHES:

          Stretch #1 (0.5): As a user, when I calculate the result, the keyboard should dismiss.

          Stretch #2 (0.5): As a user, if the result displayed is exactly equal to 64, I should see an image of my favorite Mario Kart competitor.

          Stretch #3 (0.5): As a user, if the result displayed is an even number, I want to see a funny photo. If it is an odd number, I want to see an even funnier photo.

          Stretch #4 (0.5): As a user, I want to be able to clear the text fields, the results label, and the image.

          Stretch #5 (0.5): As a user, I want the option to add, subtract, multiply, divide and take the modulus of the two numbers entered into the text fields.

          Stretch #6 (0.5): As a user, I want to see two sliders. When I move the sliders, the numbers in the TextFields should change and I can calculate the result.

          Stretch #7 (0.5): Create your own stretch with something cool.

          DESIGN STRETCHES:

          Stretch #8 (0.5): As a designer, I have modified the text to be easily readable with font, sizes, and colors that ensure good contrast and legibility.

          Stretch #9 (0.5): As a designer, I have maintained consistency in the design of text fields and buttons throughout the app to help users understand how to interact with these elements.

          Stretch #10 (0.5): As a designer, I have provided adequate whitespace around UI elements to enhance readability and create a balanced visual layout.

          Stretch #11 (0.5): As a designer, I have provided visual feedback when users interact with text fields and buttons, such as highlighting active text fields and changing button appearances on tap.

          Stretch #12 (0.5): As a designer, I have used input validation to guide users and prevent incorrect input, displaying error messages for invalid entries.

          Stretch #13 (0.5): As a designer, I have managed keyboard behavior to enhance user experience, including dismissing the keyboard after calculations and using appropriate keyboard types.

          Stretch #14 (0.5): As a designer, I have designed the app adapt well to different screen sizes and orientations, ensuring a consistent experience across devices.

          STUDENT CODE:
          %s

          RESPOND WITH ONLY VALID JSON (no extra text before or after):
          {
          "score": X.X,
          "maxScore": 10,
          "mvpComplete": true/false,
          "stretchGoalsCompleted": ["stretch1", "stretch2", "stretch3", "stretch4", "stretch5", "stretch6", "stretch7", "stretch8", "stretch9", "stretch10", "stretch11", "stretch12", "stretch13", "stretch14"],
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
