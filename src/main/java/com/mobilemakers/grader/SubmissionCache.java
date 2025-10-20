package com.mobilemakers.grader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages a persistent cache of downloaded submissions to avoid redundant downloads.
 * Tracks Last-Modified headers from Schoology to detect when assignments have new submissions.
 *
 * Cache structure:
 * {
 *   "assignments": {
 *     "8017693525": {
 *       "assignmentName": "Constants Variables Datatypes",
 *       "lastModified": "Mon, 20 Oct 2025 14:11:13 GMT",
 *       "lastDownloaded": "2025-10-20T14:30:00Z",
 *       "downloadedPath": "submissions/Constants_Variables_Datatypes"
 *     }
 *   },
 *   "cache_version": "1.0",
 *   "last_updated": "2025-10-20T14:30:00Z"
 * }
 */
public class SubmissionCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionCache.class);
    private static final String CACHE_FILE = "submission-cache.json";
    private static final String CACHE_VERSION = "1.0";

    private final ObjectMapper mapper;
    private CacheData data;
    private final String cacheFilePath;

    public SubmissionCache(String resultsDirectory) {
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
                LOGGER.info("✓ Loaded submission cache: {} assignments tracked", data.assignments.size());
            } catch (IOException e) {
                LOGGER.warn("⚠ Failed to load submission cache, starting fresh: {}", e.getMessage());
                data = new CacheData();
            }
        } else {
            LOGGER.info("ℹ No existing submission cache found, creating new cache");
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
            LOGGER.info("✓ Saved submission cache to {}", cacheFilePath);
        } catch (IOException e) {
            LOGGER.error("✗ Failed to save submission cache: {}", e.getMessage());
        }
    }

    /**
     * Check if submissions for an assignment need to be downloaded.
     *
     * @param assignmentId Assignment ID
     * @param serverLastModified Last-Modified header from server (can be null)
     * @return true if download is needed, false if cache is fresh
     */
    public boolean needsDownload(String assignmentId, String serverLastModified) {
        AssignmentCacheEntry entry = data.assignments.get(assignmentId);

        // No cache entry - need to download
        if (entry == null) {
            LOGGER.debug("No cache entry for assignment {}, download needed", assignmentId);
            return true;
        }

        // Server didn't provide Last-Modified header - safer to re-download
        if (serverLastModified == null || serverLastModified.isBlank()) {
            LOGGER.debug("Server provided no Last-Modified header, re-downloading to be safe");
            return true;
        }

        // Compare Last-Modified headers
        boolean isFresh = serverLastModified.equals(entry.lastModified);

        if (isFresh) {
            LOGGER.info("✓ Cache hit for assignment {} - submissions unchanged since {}",
                    assignmentId, entry.lastDownloaded);
        } else {
            LOGGER.info("→ Cache miss for assignment {} - server has updates (server: {}, cached: {})",
                    assignmentId, serverLastModified, entry.lastModified);
        }

        return !isFresh;
    }

    /**
     * Update cache after successfully downloading an assignment's submissions.
     *
     * @param assignmentId Assignment ID
     * @param assignmentName Assignment name
     * @param serverLastModified Last-Modified header from server
     * @param downloadedPath Path where submissions were extracted
     */
    public void updateDownload(String assignmentId, String assignmentName,
                               String serverLastModified, String downloadedPath) {
        AssignmentCacheEntry entry = data.assignments.computeIfAbsent(
                assignmentId, k -> new AssignmentCacheEntry());

        entry.assignmentName = assignmentName;
        entry.lastModified = serverLastModified;
        entry.lastDownloaded = Instant.now().toString();
        entry.downloadedPath = downloadedPath;

        LOGGER.debug("Updated cache for assignment {}: Last-Modified={}", assignmentId, serverLastModified);
    }

    /**
     * Get the path where an assignment's submissions were last downloaded.
     *
     * @param assignmentId Assignment ID
     * @return Path to submissions directory, or null if not cached
     */
    public String getCachedPath(String assignmentId) {
        AssignmentCacheEntry entry = data.assignments.get(assignmentId);
        return entry != null ? entry.downloadedPath : null;
    }

    /**
     * Check if an assignment has been downloaded before.
     *
     * @param assignmentId Assignment ID
     * @return true if assignment exists in cache
     */
    public boolean hasDownloaded(String assignmentId) {
        return data.assignments.containsKey(assignmentId);
    }

    /**
     * Remove an assignment from the cache (force re-download on next run).
     *
     * @param assignmentId Assignment ID to invalidate
     */
    public void invalidate(String assignmentId) {
        AssignmentCacheEntry removed = data.assignments.remove(assignmentId);
        if (removed != null) {
            LOGGER.info("Invalidated cache for assignment: {}", assignmentId);
            save();
        } else {
            LOGGER.warn("Attempted to invalidate non-existent cache entry: {}", assignmentId);
        }
    }

    /**
     * Clear entire cache (force re-download of all assignments).
     */
    public void clearAll() {
        int count = data.assignments.size();
        data.assignments.clear();
        LOGGER.info("Cleared entire submission cache ({} assignments)", count);
        save();
    }

    /**
     * Get summary statistics about the cache.
     */
    public String getSummary() {
        return String.format("Submission cache: %d assignments tracked", data.assignments.size());
    }

    // Inner classes for JSON structure

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CacheData {
        public Map<String, AssignmentCacheEntry> assignments = new HashMap<>();
        public String cache_version = CACHE_VERSION;
        public String last_updated = Instant.now().toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AssignmentCacheEntry {
        public String assignmentName;
        public String lastModified;      // Last-Modified header from server
        public String lastDownloaded;    // ISO-8601 timestamp when we downloaded
        public String downloadedPath;    // Where submissions were extracted
    }
}
