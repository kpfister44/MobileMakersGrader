package com.mobilemakers.grader.prompts;

/**
 * Hardcoded grading rubric for the Classes Challenge assignment.
 */
public final class ClassesChallengePrompt {

    private ClassesChallengePrompt() {
    }

    public static final String PROMPT = """
            You are grading a Swift programming assignment on Classes, Properties, and Initializers in Swift.

            """ + PromptConstants.LENIENT_GRADING_PHILOSOPHY + """

            """ + PromptConstants.CRITICAL_GRADING_RULES + """
            6. Look for MARK comments to identify correct code sections, but accept functional implementations even if MARK sections are missing or misplaced
            7. Class names should be "Student" but accept variations if the class structure is correct
            8. Property and method names should match specifications, but accept reasonable variations if functionality is correct

            GRADING CRITERIA:

            MVP (8 points total) — ALL FOUR required for mvpComplete=true:

            1. Student class with firstName and lastName properties (2 points): In Student.swift file (or any .swift file), must have a class named "Student" (or similar class name) with two String properties called firstName and lastName (or similar property names like first, last, etc.). Properties should be directly initialized with values (hardcoded string literals or default values). If class is missing, properties are missing, or properties are not String type, not complete.

            2. Properties initialized with student's name (2 points): The firstName and lastName properties must be given actual name values (not empty strings, not placeholder text like "firstName" or "John Doe"). Must be initialized directly in the property declaration (e.g., var firstName = "Kyle" or var firstName: String = "Kyle"). If properties are not initialized with real name values, not complete.

            3. Student object created in ContentView (2 points): In ContentView.swift (or main view file), in "MARK: MVP - Part II" section (or anywhere in the view), must create an instance of the Student class (e.g., let student = Student() or var myStudent = Student()). If no Student object is instantiated, not complete.

            4. Properties accessed and assigned to variables (2 points): Must access the firstName property from the Student object and assign it to mvpFirstName variable (or display it somehow). Must also access lastName property and assign to mvpLastName variable (or display it). Look for object.firstName and object.lastName syntax (or equivalent property access). If properties are not accessed from the object, not complete.

            STRETCH GOALS (0.5 points each, max +2 points):

            Stretch #1 - Default initializer with additional properties (0.5 points): Student class must have idNumber and favoriteColor properties (both String type). Must have a default initializer (init() method) that initializes these two properties with appropriate values. In ContentView "MARK: Stretch #1 - Part II" section, must create a Student object, access all four properties (firstName, lastName, idNumber, favoriteColor) and assign them to stretch1 variables (stretch1FirstName, stretch1LastName, stretch1IdNumber, stretch1FavoriteColor) or display them. If default initializer is missing, properties are missing, or properties are not accessed, not complete.

            Stretch #2 - Argument initializer (0.5 points): Student class must have an argument initializer that receives parameters for all four properties (firstName, lastName, idNumber, favoriteColor) and initializes them. In ContentView "MARK: Stretch #2 - Part II" section, must create a Student object using this argument initializer, passing in the TextField values (firstNameTextField, lastNameTextField, idNumberTextField, favoriteColorTextField or equivalent input values). Must access all four properties from the created object and assign to stretch2 variables or display them. If argument initializer is missing, not called with parameters, or properties not accessed, not complete.

            Stretch #3 - Instance method (0.5 points): Student class must have a function named sayHello (or similar greeting method) that returns a String. The returned string should be formatted like "Hello [firstName] [lastName], your id number is [idNumber]" (or similar personalized greeting that includes name and id). In ContentView "MARK: Stretch #3 - Part II" section, must create a Student object and call the sayHello function, storing the result in message variable or displaying it. If sayHello method is missing, doesn't return a String, doesn't include name/id in the output, or is not called, not complete.

            Stretch #4 - Custom stretch goal (0.5 points): Student has implemented additional creative functionality beyond the required stretches. This could include: computed properties, additional methods, class inheritance, multiple different Student objects with different data, custom string interpolation, or other object-oriented programming concepts. Must show creativity and understanding of classes beyond basic requirements. If no custom functionality exists, not complete.

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

            SCORING FORMULA:
            - MVP incomplete: score = (completed MVP requirements / 4) * 8
            - MVP complete, no stretches: score = 8.0
            - MVP complete with stretches: score = 8.0 + (completed stretches * 0.5)
            - Maximum possible score: 10.0

            FEEDBACK STUDENT SUMMARY REQUIREMENTS:
            Use 2-3 complete sentences in "feedback.studentSummary" with clear, specific guidance (except when student earns 10/10). Reference the most important strengths and exactly what to improve next to raise the score. If the student earns 10/10, provide a single celebratory sentence confirming everything is complete. Mention specific missing elements like Student class, properties (firstName, lastName, idNumber, favoriteColor), initializers (default vs argument), the sayHello method, or object instantiation that would improve the score.
            """;
}
