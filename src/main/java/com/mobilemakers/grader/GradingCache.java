package com.mobilemakers.grader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages a persistent cache of graded student assignments to avoid re-grading
 * submissions that have already been processed.
 *
 * Cache structure:
 * {
 *   "students": {
 *     "s513390": {
 *       "name": "Roman-Avdii Tyryk",
 *       "assignments": {
 *         "8017693698": {
 *           "assignment_name": "Constants Variables Datatypes",
 *           "last_graded_revision": 2,
 *           "grade": 10.0,
 *           "graded_timestamp": "2025-10-11T15:30:00Z"
 *         }
 *       }
 *     }
 *   },
 *   "cache_version": "1.0",
 *   "last_updated": "2025-10-11T15:30:00Z"
 * }
 */
public class GradingCache {

    private static final String CACHE_FILE = "grading-cache.json";
    private static final String CACHE_VERSION = "1.0";

    private final ObjectMapper mapper;
    private CacheData data;
    private final String cacheFilePath;

    public GradingCache(String resultsDirectory) {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.cacheFilePath = resultsDirectory + File.separator + CACHE_FILE;

        load();
    }

    /**
     * Load cache from disk. Creates new cache if file doesn't exist.
     */
    private void load() {
        File cacheFile = new File(cacheFilePath);

        if (cacheFile.exists()) {
            try {
                data = mapper.readValue(cacheFile, CacheData.class);
                System.out.println("✓ Loaded grading cache: " + data.students.size() + " students tracked");
            } catch (IOException e) {
                System.err.println("⚠ Failed to load cache, starting fresh: " + e.getMessage());
                data = new CacheData();
            }
        } else {
            System.out.println("ℹ No existing cache found, creating new cache");
            data = new CacheData();
        }
    }

    /**
     * Save cache to disk.
     */
    public void save() {
        data.last_updated = Instant.now().toString();

        try {
            File cacheFile = new File(cacheFilePath);
            cacheFile.getParentFile().mkdirs(); // Ensure directory exists
            mapper.writeValue(cacheFile, data);
            System.out.println("✓ Saved grading cache to " + cacheFilePath);
        } catch (IOException e) {
            System.err.println("✗ Failed to save cache: " + e.getMessage());
        }
    }

    /**
     * Check if a student's assignment revision has already been graded.
     *
     * @param schoolUid Student's school UID (e.g., "s513390")
     * @param assignmentId Assignment ID (e.g., "8017693698")
     * @param currentRevision The revision number we want to grade
     * @return true if this revision has already been graded
     */
    public boolean isAlreadyGraded(String schoolUid, String assignmentId, int currentRevision) {
        StudentData student = data.students.get(schoolUid);
        if (student == null) {
            return false;
        }

        AssignmentData assignment = student.assignments.get(assignmentId);
        if (assignment == null) {
            return false;
        }

        // Already graded if we've graded this revision or a higher one
        return assignment.last_graded_revision >= currentRevision;
    }

    /**
     * Get the last graded revision number for a student's assignment.
     *
     * @param schoolUid Student's school UID
     * @param assignmentId Assignment ID
     * @return Last graded revision number, or 0 if never graded
     */
    public int getLastGradedRevision(String schoolUid, String assignmentId) {
        StudentData student = data.students.get(schoolUid);
        if (student == null) {
            return 0;
        }

        AssignmentData assignment = student.assignments.get(assignmentId);
        if (assignment == null) {
            return 0;
        }

        return assignment.last_graded_revision;
    }

    /**
     * Update cache after successfully grading a student's assignment.
     *
     * @param schoolUid Student's school UID
     * @param studentName Student's full name
     * @param assignmentId Assignment ID
     * @param assignmentName Assignment name
     * @param revisionNumber Revision number that was graded
     * @param grade The grade assigned
     */
    public void updateGrade(String schoolUid, String studentName, String assignmentId,
                           String assignmentName, int revisionNumber, double grade) {
        // Get or create student entry
        StudentData student = data.students.computeIfAbsent(schoolUid, k -> new StudentData());
        student.name = studentName;

        // Get or create assignment entry
        AssignmentData assignment = student.assignments.computeIfAbsent(assignmentId, k -> new AssignmentData());
        assignment.assignment_name = assignmentName;
        assignment.last_graded_revision = revisionNumber;
        assignment.grade = grade;
        assignment.graded_timestamp = Instant.now().toString();

        System.out.println("  → Cache updated: " + schoolUid + " revision " + revisionNumber);
    }

    /**
     * Get summary statistics about the cache.
     */
    public String getSummary() {
        int totalStudents = data.students.size();
        int totalGrades = data.students.values().stream()
            .mapToInt(s -> s.assignments.size())
            .sum();

        return String.format("Cache: %d students, %d graded assignments", totalStudents, totalGrades);
    }

    // Inner classes for JSON structure

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CacheData {
        public Map<String, StudentData> students = new HashMap<>();
        public String cache_version = CACHE_VERSION;
        public String last_updated = Instant.now().toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class StudentData {
        public String name;
        public Map<String, AssignmentData> assignments = new HashMap<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AssignmentData {
        public String assignment_name;
        public int last_graded_revision;
        public double grade;
        public String graded_timestamp;
    }
}
