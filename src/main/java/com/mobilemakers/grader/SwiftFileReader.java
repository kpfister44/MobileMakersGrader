package com.mobilemakers.grader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Loads `.swift` files for each student directory and merges them into a single submission string.
 */
public class SwiftFileReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwiftFileReader.class);
    private static final Pattern REVISION_PATTERN = Pattern.compile("Revision\\s+(\\d+)\\s+-\\s+(On time|Late)", Pattern.CASE_INSENSITIVE);

    public Map<String, String> readStudentSubmissions(Path submissionsRoot) throws IOException {
        if (!Files.exists(submissionsRoot) || !Files.isDirectory(submissionsRoot)) {
            throw new IOException("Submissions path does not exist or is not a directory: " + submissionsRoot);
        }

        Map<String, String> submissions = new LinkedHashMap<>();
        try (Stream<Path> directories = Files.list(submissionsRoot).filter(Files::isDirectory)) {
            for (Path studentDir : directories.collect(Collectors.toList())) {
                String studentKey = studentDir.getFileName().toString();
                String mergedCode = readLatestSubmission(studentDir);
                if (mergedCode.isBlank()) {
                    LOGGER.warn("No Swift files found for student directory: {}", studentKey);
                }
                submissions.put(studentKey, mergedCode);
            }
        }
        return submissions;
    }

    private String readLatestSubmission(Path studentDir) throws IOException {
        List<Path> candidates = listSubmissionCandidates(studentDir);
        for (Path candidate : candidates) {
            String merged = mergeCandidateContent(candidate);
            if (!merged.isBlank()) {
                return merged;
            }
        }

        // Fallback to pre-existing behavior in case no explicit revision folders exist.
        return mergeDirectorySwiftFiles(studentDir, studentDir);
    }

    private List<Path> listSubmissionCandidates(Path studentDir) throws IOException {
        List<Path> candidates = new ArrayList<>();
        try (Stream<Path> stream = Files.list(studentDir)) {
            candidates = stream
                    .filter(path -> Files.isDirectory(path) || isZip(path))
                    .sorted(Comparator.comparing(this::lastModified).reversed())
                    .collect(Collectors.toList());
        }
        return candidates;
    }

    private String mergeCandidateContent(Path candidate) throws IOException {
        StringBuilder builder = new StringBuilder();
        if (Files.isDirectory(candidate)) {
            mergeDirectorySwiftFiles(candidate, candidate, builder);
        } else if (isZip(candidate)) {
            mergeZipSwiftFiles(candidate, builder);
        }
        return builder.toString().trim();
    }

    private String mergeDirectorySwiftFiles(Path root, Path baseForLabel) throws IOException {
        StringBuilder builder = new StringBuilder();
        mergeDirectorySwiftFiles(root, baseForLabel, builder);
        return builder.toString().trim();
    }

    private void mergeDirectorySwiftFiles(Path directory, Path baseForLabel, StringBuilder builder) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            for (Path child : stream.sorted().collect(Collectors.toList())) {
                if (Files.isDirectory(child)) {
                    mergeDirectorySwiftFiles(child, baseForLabel, builder);
                } else if (isSwift(child)) {
                    appendFile(builder, baseForLabel.relativize(child).toString(), Files.readString(child, StandardCharsets.UTF_8));
                } else if (isZip(child)) {
                    mergeZipSwiftFiles(child, builder);
                }
            }
        }
    }

    private void mergeZipSwiftFiles(Path zipPath, StringBuilder builder) throws IOException {
        try (InputStream fileStream = Files.newInputStream(zipPath); ZipInputStream zipInput = new ZipInputStream(fileStream)) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                try {
                    if (entry.isDirectory() || !entry.getName().endsWith(".swift")) {
                        continue;
                    }
                    String label = extractEntryLabel(entry.getName());
                    String content = readZipEntry(zipInput);
                    appendFile(builder, label, content);
                } finally {
                    zipInput.closeEntry();
                }
            }
        }
    }

    private String readZipEntry(InputStream zipInput) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = zipInput.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private void appendFile(StringBuilder builder, String label, String content) {
        builder.append("// File: ").append(label).append(System.lineSeparator());
        builder.append(content);
        builder.append(System.lineSeparator()).append(System.lineSeparator());
    }

    private String extractEntryLabel(String entryName) {
        String normalized = entryName.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < normalized.length() - 1) {
            return normalized.substring(lastSlash + 1);
        }
        return normalized;
    }

    private boolean isSwift(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".swift");
    }

    private boolean isZip(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".zip");
    }

    private FileTime lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException ex) {
            LOGGER.warn("Failed to read last modified time for {}", path, ex);
            return FileTime.fromMillis(0);
        }
    }

    /**
     * Find the highest revision number in a student's directory.
     * Looks for folders named "Revision X - On time" or "Revision X - Late"
     *
     * @param studentDir The student's submission directory
     * @return Highest revision number found, or 1 if no revision folders exist
     */
    public int findHighestRevision(Path studentDir) throws IOException {
        int maxRevision = 0;

        try (Stream<Path> stream = Files.list(studentDir)) {
            List<Path> paths = stream.filter(Files::isDirectory).collect(Collectors.toList());

            for (Path path : paths) {
                String folderName = path.getFileName().toString();
                int revision = parseRevisionNumber(folderName);
                if (revision > maxRevision) {
                    maxRevision = revision;
                }
            }
        }

        // If no revision folders found, assume revision 1 (direct submission)
        return maxRevision > 0 ? maxRevision : 1;
    }

    /**
     * Parse revision number from folder name.
     * Examples:
     *   "Revision 1 - On time" -> 1
     *   "Revision 2 - Late" -> 2
     *   "SomeOtherFolder" -> 0
     *
     * @param folderName The folder name to parse
     * @return Revision number, or 0 if not a revision folder
     */
    private int parseRevisionNumber(String folderName) {
        Matcher matcher = REVISION_PATTERN.matcher(folderName);
        if (matcher.matches()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                LOGGER.warn("Failed to parse revision number from: {}", folderName);
            }
        }
        return 0;
    }

    /**
     * Read Swift files from a specific revision folder.
     *
     * @param studentDir The student's submission directory
     * @param revisionNumber The revision number to read (e.g., 1, 2, 3)
     * @return Merged Swift code, or empty string if revision not found
     */
    public String readSpecificRevision(Path studentDir, int revisionNumber) throws IOException {
        // Look for folder matching "Revision {revisionNumber} - ..."
        try (Stream<Path> stream = Files.list(studentDir)) {
            List<Path> paths = stream.filter(Files::isDirectory).collect(Collectors.toList());

            for (Path path : paths) {
                String folderName = path.getFileName().toString();
                if (parseRevisionNumber(folderName) == revisionNumber) {
                    LOGGER.debug("Reading revision {} from: {}", revisionNumber, folderName);
                    return mergeDirectorySwiftFiles(path, path);
                }
            }
        }

        LOGGER.warn("Revision {} not found in {}", revisionNumber, studentDir);
        return "";
    }
}
