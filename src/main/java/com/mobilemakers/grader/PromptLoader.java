package com.mobilemakers.grader;

import java.lang.reflect.Field;

/**
 * Dynamically loads assignment prompts using Java Reflection.
 * Allows runtime selection of prompt classes without hardcoded imports.
 */
public class PromptLoader {

    private static final String PROMPTS_PACKAGE = "com.mobilemakers.grader.prompts";
    private static final String PROMPT_FIELD_NAME = "PROMPT";

    /**
     * Loads a prompt string from a prompt class by name.
     *
     * @param promptClassName The simple class name (e.g., "MultiplyPrompt")
     * @return The prompt string from the PROMPT constant
     * @throws ClassNotFoundException if prompt class doesn't exist
     * @throws IllegalStateException if PROMPT field is missing or inaccessible
     */
    public static String loadPrompt(String promptClassName) throws ClassNotFoundException {
        if (promptClassName == null || promptClassName.isBlank()) {
            throw new IllegalArgumentException("Prompt class name cannot be null or blank");
        }

        // Construct fully qualified class name
        String fullyQualifiedName = PROMPTS_PACKAGE + "." + promptClassName;

        try {
            // Load the class using reflection
            Class<?> promptClass = Class.forName(fullyQualifiedName);

            // Get the PROMPT field
            Field promptField = promptClass.getDeclaredField(PROMPT_FIELD_NAME);

            // Ensure the field is accessible (should be public static final)
            promptField.setAccessible(true);

            // Get the value of the PROMPT field
            Object promptValue = promptField.get(null); // null because it's a static field

            if (promptValue == null) {
                throw new IllegalStateException(
                    String.format("PROMPT field in %s is null", promptClassName)
                );
            }

            if (!(promptValue instanceof String)) {
                throw new IllegalStateException(
                    String.format("PROMPT field in %s is not a String (found: %s)",
                                promptClassName, promptValue.getClass().getName())
                );
            }

            String promptText = (String) promptValue;

            if (promptText.isBlank()) {
                throw new IllegalStateException(
                    String.format("PROMPT field in %s is blank", promptClassName)
                );
            }

            return promptText;

        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(
                String.format("Prompt class %s does not have a PROMPT field", promptClassName),
                e
            );
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                String.format("Cannot access PROMPT field in %s", promptClassName),
                e
            );
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException(
                String.format("Prompt class not found: %s. Make sure the class exists in package %s",
                            promptClassName, PROMPTS_PACKAGE),
                e
            );
        }
    }

    /**
     * Validates that a prompt class exists and has a valid PROMPT field.
     *
     * @param promptClassName The simple class name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPromptClass(String promptClassName) {
        try {
            loadPrompt(promptClassName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
