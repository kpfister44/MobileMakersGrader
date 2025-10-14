package com.mobilemakers.grader;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Centralized configuration loader for environment variables.
 * Loads variables from .env file with fallback to system environment variables.
 */
public class Config {
    private static final Dotenv dotenv;

    static {
        // Load .env file, but don't fail if it doesn't exist
        // This allows the app to still work with system environment variables
        dotenv = Dotenv.configure()
                .ignoreIfMissing()  // Don't throw exception if .env is missing
                .load();
    }

    /**
     * Get an environment variable value.
     * First checks .env file, then falls back to system environment variables.
     *
     * @param key The environment variable name
     * @return The value, or null if not found
     */
    public static String get(String key) {
        // Try .env file first
        String value = dotenv.get(key);

        // Fall back to system environment variable if not found in .env
        if (value == null) {
            value = System.getenv(key);
        }

        return value;
    }

    /**
     * Get an environment variable value with a default fallback.
     *
     * @param key The environment variable name
     * @param defaultValue The default value if not found
     * @return The value, or defaultValue if not found
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get a boolean environment variable.
     * Treats "true" (case-insensitive) as true, everything else as false.
     *
     * @param key The environment variable name
     * @return true if value is "true", false otherwise
     */
    public static boolean getBoolean(String key) {
        String value = get(key);
        return "true".equalsIgnoreCase(value);
    }

    /**
     * Get a boolean environment variable with a default value.
     *
     * @param key The environment variable name
     * @param defaultValue The default value if not found
     * @return The boolean value
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value);
    }
}
