package com.mobilemakers.grader.prompts;

/**
 * Hardcoded grading rubric for the Property Wrappers Challenge assignment.
 */
public final class PropertyWrappersChallengePrompt {

    private PropertyWrappersChallengePrompt() {
    }

    public static final String PROMPT = """
            You are grading a Swift programming assignment on Property Wrappers (@State, @Binding, @StateObject, @EnvironmentObject, @Published) in SwiftUI.

            """ + PromptConstants.LENIENT_GRADING_PHILOSOPHY + """

            """ + PromptConstants.CRITICAL_GRADING_RULES + """
            6. Look for MARK comments to identify correct code sections, but accept functional implementations even if MARK sections are missing or misplaced
            7. Variable names should match specifications (word1, word2, number, contact), but accept reasonable variations if functionality is correct
            8. Property wrapper usage is key - @State, @Binding, @StateObject, @EnvironmentObject, @Published must be present for credit

            GRADING CRITERIA:

            MVP (8 points total) — ALL FOUR required for mvpComplete=true:

            1. Three @State variables created in ContentView (2 points): In ContentView.swift in "MARK: MVP - Part I" section (or anywhere in ContentView), must have three @State variables named word1, word2, and number (or similar names). word1 and word2 should be String type, number should be Int type. Variables should be initialized to empty strings ("") and 0 appropriately. If any of the three @State variables are missing, wrong types, or not initialized, not complete.

            2. Three variables with property wrappers in MVPView (2 points): In MVPView.swift in "MARK: MVP - Part II" section (or anywhere in MVPView), must have three variables named word1Reference, word2Reference, and numberReference (or similar names). word1Reference must use @State property wrapper, word2Reference must use @Binding property wrapper, and numberReference must use @Binding property wrapper. All three must specify their data types (String, String, Int) and should NOT be initialized. If property wrappers are incorrect (@State vs @Binding), variables are missing, or data types are wrong, not complete.

            3. MVPView called with variables passed (2 points): In ContentView.swift in "MARK: MVP - Part III" section (or in navigation/sheet presentation), must create a call to MVPView and pass all three variables from Part I (word1, word2, number) to MVPView. This typically looks like MVPView(word1Reference: $word1, word2Reference: $word2, numberReference: $number) with $ syntax for bindings. If MVPView is not instantiated, variables are not passed, or binding syntax ($) is missing, not complete.

            4. Property wrapper behavior demonstrates understanding (2 points): Code should demonstrate understanding that @State creates independent copy while @Binding creates two-way connection. Based on the user story: when values are changed in MVPView and user returns to ContentView, word1 (using @State in MVPView) should NOT reflect changes, but word2 and number (using @Binding) should reflect changes. Look for evidence that student understands this difference through correct property wrapper usage. Navigation between views should work (NavigationLink, sheet, or similar). If property wrappers are fundamentally misused or navigation is completely broken, not complete.

            STRETCH GOALS (1.0 points each, max +2 points):

            Stretch #1 - ObservableObject and EnvironmentObject pattern (1.0 points): Must implement complete Contact class pattern with @Published properties and @EnvironmentObject sharing. In Contact.swift "MARK: Stretch #1 - Part I": must have Contact class that adopts ObservableObject protocol with three @Published String properties (name, address, phone or similar names). In MyApp.swift "MARK: Stretch #1 - Part II": must create @StateObject named contact with Contact instance. In MyApp.swift "MARK: Stretch #1 - Part III": must add .environmentObject modifier to ContentView passing the contact object. In ContentView.swift "MARK: Stretch #1 - Part IV", Stretch1Name.swift "MARK: Stretch #1 - Part V", Stretch1Address.swift "MARK: Stretch #1 - Part VI", and Stretch1Phone.swift "MARK: Stretch #1 - Part VII": must create @EnvironmentObject of Contact class named contact. Must show understanding of @StateObject (creation), @EnvironmentObject (consumption), @Published (change notification), and ObservableObject protocol. If Contact class is missing, doesn't adopt ObservableObject, @Published is missing, @StateObject is missing, @EnvironmentObject is missing in any required file, or .environmentObject modifier is not applied, not complete.

            Stretch #2 - Custom property wrapper (1.0 points): In BetweenOneAndTen.swift "MARK: Stretch #2 - Part I": must create a custom property wrapper named BetweenOneAndTen (or similar name) that constrains integer values to be between 1 and 10. Property wrapper should have @propertyWrapper attribute, a wrappedValue property with getter/setter that enforces the 1-10 constraint (using min/max, clamping, or conditional logic). In "MARK: Stretch #2 - Part II": must apply the @BetweenOneAndTen property wrapper to a property in the BoundedNumber class. When user enters number outside 1-10 range, the property wrapper should automatically constrain it to valid range. If custom property wrapper is missing, doesn't use @propertyWrapper attribute, doesn't constrain values to 1-10 range, or is not applied to BoundedNumber class property, not complete.

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
            Use 2-3 complete sentences in "feedback.studentSummary" with clear, specific guidance (except when student earns 10/10). Reference the most important strengths and exactly what to improve next to raise the score. If the student earns 10/10, provide a single celebratory sentence confirming everything is complete. Mention specific missing elements like @State variables, @Binding usage, property wrapper syntax, Contact class with @Published properties, @EnvironmentObject setup, or custom property wrapper implementation that would improve the score.
            """;
}
