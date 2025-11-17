package com.mobilemakers.grader.prompts;

/**
 * Hardcoded grading rubric for the Final Grade Calculator assignment.
 */
public final class FinalGradeCalculatorPrompt {

    private FinalGradeCalculatorPrompt() {
    }

    public static final String PROMPT = """
            You are grading a Swift programming assignment on the Final Grade Calculator app in SwiftUI.

            """ + PromptConstants.LENIENT_GRADING_PHILOSOPHY + """

            """ + PromptConstants.CRITICAL_GRADING_RULES + """

            GRADING CRITERIA:

            MVP (8 points total) — ALL FOUR required for mvpComplete=true:

            1. Three input fields for user data (2 points): App must have TextField (or equivalent input method) for current grade, TextField for desired final grade, and TextField for exam percentage. These can be TextFields, Sliders, Pickers, or any valid SwiftUI input component. Must be bound to @State variables (or equivalent state management). If any of the three inputs are missing, not complete.

            2. User interaction trigger for calculation (2 points): App must have a Button (or tap gesture, or other user interaction method like onSubmit) that triggers the grade calculation. The interaction must be clearly present and functional. If no trigger mechanism exists, not complete.

            3. Calculation logic present (3 points): Code must contain the mathematical formula to calculate required exam score. The formula should be: requiredExamScore = (desiredFinalGrade - currentGrade * (1 - examWeight)) / examWeight (or mathematically equivalent). Accept any correct mathematical implementation that produces the right result. Variables can have different names. If the calculation logic is completely missing or fundamentally incorrect, not complete.

            4. Display of calculated result (1 point): App must display the calculated result to the user in Text view (or equivalent display method like Label, alert, etc.). The result should show what grade is needed on the exam. If no result is displayed, not complete.

            STRETCH GOALS (0.5 points each, max +2 points):

            Stretch #1 - Conditional background color (0.5 points): App changes background color based on required exam score. If required score is under 100%, background should be green (or similar positive color). If over 100%, background should be red (or similar negative/warning color). Must use conditional logic (if/else, ternary operator, or switch). If conditional color change is missing or doesn't respond to the 100% threshold, not complete.

            Stretch #2 - Extra credit indication (0.5 points): When required exam score is above 100%, app displays a message or indication that user needs to ask teacher for extra credit. This can be Text view that appears conditionally, an alert, or any clear visual indicator. Must be conditional on score > 100%. If no extra credit indication exists, not complete.

            Stretch #3 - Segmented controller for letter grades (0.5 points): App includes Picker with segmentedPickerStyle (or segmented control) that allows user to select desired final grade as A, B, C, or D instead of numeric entry. Must convert letter grade to numeric value (A=90-100, B=80-89, C=70-79, D=60-69 or similar reasonable ranges). If segmented controller is missing or doesn't replace/supplement the desired grade input, not complete.

            Stretch #4 - PickerView for letter grades (0.5 points): App includes Picker (wheel style, menu style, or any non-segmented picker style) that allows user to select desired final grade as A, B, C, or D. Must convert letter grade to numeric value. Note: This is mutually exclusive with Stretch #3 in practice, but if student implements both picker styles for different purposes, award both. If PickerView is missing or doesn't work for grade selection, not complete.

            Stretch #5 - Good text design/readability (0.5 points): Text has been modified with custom fonts, sizes, and colors that ensure good contrast and legibility. Look for .font(), .foregroundColor(), or .foregroundStyle() modifiers applied to text elements. Text should be readable and well-styled. If text is all default styling with no customization, not complete.

            Stretch #6 - Consistent UI design (0.5 points): TextFields and Buttons show consistent styling throughout the app. Look for consistent use of .textFieldStyle(), .buttonStyle(), .padding(), .background(), or other modifiers that create visual consistency. If UI elements are inconsistent or all default styling, not complete.

            Stretch #7 - Good use of whitespace (0.5 points): UI has adequate spacing and padding around elements. Look for .padding(), Spacer(), .spacing() in VStack/HStack, or other layout modifiers that create breathing room. App should not feel cramped. If elements are tightly packed with minimal spacing, not complete.

            Stretch #8 - Keyboard management (0.5 points): App manages keyboard behavior appropriately. Look for .keyboardType(.decimalPad) or .keyboardType(.numberPad) on numeric TextFields, and keyboard dismissal mechanisms like .onSubmit, tap gesture on background, or focused state management. Must show at least TWO keyboard improvements (appropriate keyboard type + dismissal method). If keyboard management is completely default, not complete.

            Stretch #9 - Responsive design for screen sizes (0.5 points): App uses adaptive layout techniques that work across different screen sizes and orientations. Look for GeometryReader, adaptive stacks, .frame(maxWidth:), .scaledToFit(), or other responsive design patterns. Should show evidence of consideration for different devices. If layout is completely fixed/rigid, not complete.

            Stretch #10 - Input validation (0.5 points): App validates user input and provides error messages for invalid entries. Look for checks that grades are in valid ranges (0-100), exam weight is 0-100%, and displays error Text, alerts, or visual indicators for invalid input. Must show actual validation logic with user feedback. If no validation exists, not complete.

            STUDENT CODE:
            %s

            RESPOND WITH ONLY VALID JSON (no extra text before or after):
            {
                "score": X.X,
                "maxScore": 10,
                "mvpComplete": true/false,
                "stretchGoalsCompleted": ["stretch1", "stretch2", "stretch3", "stretch4", "stretch5", "stretch6", "stretch7", "stretch8", "stretch9", "stretch10"],
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
            - MVP complete with stretches: score = 8.0 + (completed stretches * 0.5)
            - Maximum possible score: 10.0

            FEEDBACK STUDENT SUMMARY REQUIREMENTS:
            Use 2-3 complete sentences in "feedback.studentSummary" with clear, specific guidance (except when student earns 10/10). Reference the most important strengths and exactly what to improve next to raise the score. If the student earns 10/10, provide a single celebratory sentence confirming everything is complete. Mention specific missing features like input fields, calculation logic, conditional backgrounds, pickers, or validation that would improve the score.
            """;
}
