package com.mobilemakers.grader;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Downloads and organizes student submissions from Schoology.
 * Transforms Schoology's ZIP structure into SwiftFileReader-compatible format.
 */
public class SchoologySubmissionDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchoologySubmissionDownloader.class);

    private final OkHttpClient client;
    private final String baseUrl;
    private final String sessionCookie;
    private final String csrfKey;
    private final String csrfToken;
    private final SubmissionCache cache;

    public SchoologySubmissionDownloader(String baseUrl, String sessionCookie,
                                          String csrfKey, String csrfToken) {
        this(baseUrl, sessionCookie, csrfKey, csrfToken, null);
    }

    public SchoologySubmissionDownloader(String baseUrl, String sessionCookie,
                                          String csrfKey, String csrfToken,
                                          SubmissionCache cache) {
        this.baseUrl = baseUrl;
        this.sessionCookie = sessionCookie;
        this.csrfKey = csrfKey;
        this.csrfToken = csrfToken;
        this.cache = cache;
        this.client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofMinutes(5))
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofMinutes(5))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Downloads all submissions for an assignment as a ZIP file.
     *
     * @param assignmentId Schoology assignment ID
     * @param assignmentName Human-readable assignment name (for file naming)
     * @return Path to downloaded ZIP file
     * @throws IOException if download fails
     */
    public Path downloadSubmissions(String assignmentId, String assignmentName) throws IOException {
        String url = baseUrl + "/assignment/" + assignmentId + "/dropbox/download_all";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Cookie", sessionCookie)
                .header("x-csrf-key", csrfKey)
                .header("x-csrf-token", csrfToken)
                .header("accept", "*/*")
                .header("referer", baseUrl + "/assignment/" + assignmentId + "/info")
                .build();

        LOGGER.info("Downloading submissions for assignment: {} (ID: {})", assignmentName, assignmentId);
        LOGGER.debug("Request URL: {}", url);

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorMsg = String.format("Failed to download submissions. HTTP %d: %s",
                        response.code(), response.message());
                LOGGER.error(errorMsg);

                if (response.code() == 401) {
                    throw new IOException("Authentication failed. Session cookie may be expired. " +
                            "Please refresh .schoology-cookie file.");
                } else if (response.code() == 403) {
                    throw new IOException("Access forbidden. CSRF tokens may be invalid. " +
                            "Please extract fresh tokens from browser DevTools.");
                } else if (response.code() == 404) {
                    throw new IOException("Assignment not found (ID: " + assignmentId + "). " +
                            "Verify assignment ID is correct.");
                }

                throw new IOException(errorMsg);
            }

            // Check Content-Type header
            String contentType = response.header("Content-Type", "");
            if (!contentType.contains("zip") && !contentType.contains("octet-stream")) {
                LOGGER.warn("Unexpected Content-Type: {}. Expected application/zip.", contentType);
            }

            // Save ZIP file to temporary location
            String sanitizedName = assignmentName.replaceAll("\\s+", "_")
                    .replaceAll("[^a-zA-Z0-9_-]", "");
            Path tempZip = Files.createTempFile(sanitizedName + "_", ".zip");

            // Download file
            byte[] zipBytes = response.body().bytes();
            Files.write(tempZip, zipBytes);

            long fileSizeKB = zipBytes.length / 1024;
            LOGGER.info("✓ Downloaded {} KB to {}", fileSizeKB, tempZip.getFileName());

            // Get Last-Modified header for caching
            String lastModified = response.header("Last-Modified");
            if (lastModified != null) {
                LOGGER.debug("Last-Modified: {}", lastModified);
            }

            return tempZip;
        }
    }

    /**
     * Extracts and organizes submissions into SwiftFileReader-compatible structure.
     * Transforms:
     *   "LastName, FirstName - school_uid/Revision N/files"
     * Into:
     *   "school_uid/files" (most recent revision only)
     *
     * @param zipFile Path to downloaded ZIP file
     * @param assignmentName Assignment name (used for folder name)
     * @return Path to organized submissions directory
     * @throws IOException if extraction fails
     */
    public Path extractAndOrganizeSubmissions(Path zipFile, String assignmentName) throws IOException {
        LOGGER.info("Extracting and organizing submissions for: {}", assignmentName);

        // Create temp extraction directory
        Path tempExtractDir = Files.createTempDirectory("schoology_extract_");

        try {
            // Step 1: Extract ZIP file
            extractZipFile(zipFile, tempExtractDir);

            // Step 2: Find the root folder in the ZIP (Schoology wraps everything in one folder)
            Path rootFolder = findRootFolder(tempExtractDir);

            // Step 3: Create target submissions directory
            String sanitizedName = assignmentName.replaceAll("\\s+", "_")
                    .replaceAll("[^a-zA-Z0-9_-]", "");
            Path submissionsDir = Path.of("submissions", sanitizedName);
            Files.createDirectories(submissionsDir);

            // Step 4: Process each student folder
            try (Stream<Path> studentFolders = Files.list(rootFolder)) {
                studentFolders.forEach(studentFolder -> {
                    try {
                        processStudentFolder(studentFolder, submissionsDir);
                    } catch (IOException e) {
                        LOGGER.error("Failed to process student folder: {}", studentFolder.getFileName(), e);
                    }
                });
            }

            LOGGER.info("✓ Organized submissions into: {}", submissionsDir);
            return submissionsDir;

        } finally {
            // Clean up temp directory
            deleteRecursively(tempExtractDir);
            LOGGER.debug("Cleaned up temp directory: {}", tempExtractDir);
        }
    }

    /**
     * Extracts a ZIP file to the specified directory.
     */
    private void extractZipFile(Path zipFilePath, Path destDir) throws IOException {
        LOGGER.debug("Extracting ZIP: {} to {}", zipFilePath.getFileName(), destDir);

        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = destDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream in = zipFile.getInputStream(entry);
                         FileOutputStream out = new FileOutputStream(entryPath.toFile())) {
                        in.transferTo(out);
                    }
                }
            }
        }
    }

    /**
     * Finds the root folder containing student submissions.
     * Schoology ZIPs typically have a single top-level folder.
     */
    private Path findRootFolder(Path extractDir) throws IOException {
        try (Stream<Path> contents = Files.list(extractDir)) {
            List<Path> items = contents.collect(Collectors.toList());

            // If there's only one folder, that's the root
            if (items.size() == 1 && Files.isDirectory(items.get(0))) {
                return items.get(0);
            }

            // Otherwise, the extract dir is the root
            return extractDir;
        }
    }

    /**
     * Processes a single student's folder:
     * 1. Extracts school_uid from folder name
     * 2. Flattens revision folders (keeps most recent)
     * 3. Moves files to target directory
     */
    private void processStudentFolder(Path studentFolder, Path submissionsDir) throws IOException {
        if (!Files.isDirectory(studentFolder)) {
            LOGGER.debug("Skipping non-directory: {}", studentFolder.getFileName());
            return;
        }

        String folderName = studentFolder.getFileName().toString();

        // Extract school_uid from "LastName, FirstName - school_uid"
        String schoolUid = extractSchoolUid(folderName);
        if (schoolUid == null) {
            LOGGER.warn("Could not extract school_uid from folder: {}", folderName);
            return;
        }

        LOGGER.debug("Processing student: {} -> {}", folderName, schoolUid);

        // Create target student directory
        Path targetStudentDir = submissionsDir.resolve(schoolUid);
        Files.createDirectories(targetStudentDir);

        // Find and process revisions
        flattenRevisions(studentFolder, targetStudentDir);
    }

    /**
     * Extracts school_uid from Schoology folder name format.
     * Format: "LastName, FirstName - school_uid"
     * Example: "Castro, Marianna - s486002" -> "s486002"
     */
    private String extractSchoolUid(String folderName) {
        int lastDashIndex = folderName.lastIndexOf(" - ");
        if (lastDashIndex == -1) {
            return null;
        }

        String uid = folderName.substring(lastDashIndex + 3).trim();

        // Validate it looks like a school UID (starts with 's' followed by digits)
        if (uid.matches("s\\d+")) {
            return uid;
        }

        return null;
    }

    /**
     * Flattens revision folders and keeps only the most recent submission.
     * Schoology format: "Revision N - Status/" folders
     * Keeps: Most recent by last modified time
     */
    private void flattenRevisions(Path studentFolder, Path targetDir) throws IOException {
        // Find all revision folders
        List<Path> revisionFolders = new ArrayList<>();

        try (Stream<Path> contents = Files.list(studentFolder)) {
            revisionFolders = contents
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("Revision "))
                    .sorted(Comparator.comparing(this::getLastModifiedTime).reversed())
                    .collect(Collectors.toList());
        }

        if (revisionFolders.isEmpty()) {
            LOGGER.warn("No revision folders found for student: {}", studentFolder.getFileName());
            return;
        }

        // Get most recent revision
        Path mostRecentRevision = revisionFolders.get(0);
        LOGGER.debug("Selected revision: {} (out of {} total)",
                mostRecentRevision.getFileName(), revisionFolders.size());

        // Copy all files from most recent revision to target directory
        try (Stream<Path> files = Files.list(mostRecentRevision)) {
            files.forEach(file -> {
                try {
                    Path targetFile = targetDir.resolve(file.getFileName());
                    if (Files.isDirectory(file)) {
                        copyDirectoryRecursively(file, targetFile);
                    } else {
                        Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to copy file: {}", file.getFileName(), e);
                }
            });
        }
    }

    /**
     * Gets the last modified time of a path, handling errors gracefully.
     */
    private FileTime getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            LOGGER.warn("Could not get modified time for: {}", path);
            return FileTime.fromMillis(0);
        }
    }

    /**
     * Recursively copies a directory.
     */
    private void copyDirectoryRecursively(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    private void deleteRecursively(Path path) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Failed to delete directory: {}", path, e);
        }
    }

    /**
     * Downloads and extracts submissions with caching support.
     * Checks cache first - only downloads if server has updates.
     *
     * @param assignmentId Schoology assignment ID
     * @param assignmentName Human-readable assignment name
     * @return Path to submissions directory (may be cached or freshly downloaded)
     * @throws IOException if download or extraction fails
     */
    public Path downloadAndExtractIfNeeded(String assignmentId, String assignmentName) throws IOException {
        // If no cache provided, always download
        if (cache == null) {
            LOGGER.debug("No cache configured, downloading submissions");
            Path zipFile = downloadSubmissions(assignmentId, assignmentName);
            return extractAndOrganizeSubmissions(zipFile, assignmentName);
        }

        // Check server's Last-Modified header
        String serverLastModified = getServerLastModified(assignmentId);

        // Check if download is needed
        if (!cache.needsDownload(assignmentId, serverLastModified)) {
            // Cache hit - return cached path
            String cachedPath = cache.getCachedPath(assignmentId);
            if (cachedPath != null && Files.exists(Path.of(cachedPath))) {
                LOGGER.info("⊘ Using cached submissions from: {}", cachedPath);
                return Path.of(cachedPath);
            } else {
                LOGGER.warn("Cache entry exists but path not found, re-downloading");
            }
        }

        // Cache miss - download and extract
        LOGGER.info("→ Downloading fresh submissions for: {}", assignmentName);
        Path zipFile = downloadSubmissions(assignmentId, assignmentName);
        Path submissionsDir = extractAndOrganizeSubmissions(zipFile, assignmentName);

        // Update cache
        cache.updateDownload(assignmentId, assignmentName, serverLastModified,
                submissionsDir.toString());
        cache.save();

        return submissionsDir;
    }

    /**
     * Checks the server's Last-Modified header for an assignment without downloading.
     * Uses a HEAD request for efficiency.
     *
     * @param assignmentId Assignment ID
     * @return Last-Modified header value, or null if not available
     */
    private String getServerLastModified(String assignmentId) {
        String url = baseUrl + "/assignment/" + assignmentId + "/dropbox/download_all";

        Request request = new Request.Builder()
                .url(url)
                .head() // HEAD request - no body downloaded
                .header("Cookie", sessionCookie)
                .header("x-csrf-key", csrfKey)
                .header("x-csrf-token", csrfToken)
                .header("accept", "*/*")
                .header("referer", baseUrl + "/assignment/" + assignmentId + "/info")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String lastModified = response.header("Last-Modified");
                LOGGER.debug("Server Last-Modified for assignment {}: {}", assignmentId, lastModified);
                return lastModified;
            } else {
                LOGGER.warn("HEAD request failed for assignment {} (HTTP {}), will download anyway",
                        assignmentId, response.code());
                return null;
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to check Last-Modified header: {}", e.getMessage());
            return null;
        }
    }
}
