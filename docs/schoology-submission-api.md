# Schoology Submission Download API

## Overview

Documentation for Schoology's reverse-engineered submission download API endpoints. These endpoints enable programmatic downloading of student assignment submissions for batch grading.

**Discovery Date**: October 20, 2025
**Status**: Working (as of October 2025)
**Authentication**: Session cookie + CSRF tokens (same as grade/comment posting)

---

## Endpoints

### 1. Get Student Enrollment List

**Purpose**: Retrieve list of students enrolled in a course with UID mappings.

**Endpoint**:
```
GET /iapi/enrollment/member_enrollments/course/{courseId}
```

**Parameters**:
- `courseId` (path) - Course identifier (e.g., `7921261672`)

**Headers**:
```http
Cookie: [session cookie from .schoology-cookie file]
x-csrf-key: [from .env]
x-csrf-token: [from .env]
accept: */*
referer: https://schoology.d214.org/course/{courseId}/grades
```

**Response** (200 OK):
```json
{
  "response_code": 200,
  "body": {
    "3394039558": {
      "id": "3394039558",
      "uid": "115532718",
      "school_uid": "s486002",
      "name_first": "Marianna",
      "name_last": "Castro",
      "name": "Marianna <b>Castro</b>",
      "type": "2",
      "status": "1",
      "realm": "course",
      "realm_id": "7921261672"
    },
    "3394039559": {
      "id": "3394039559",
      "uid": "124771090",
      "school_uid": "s498783",
      "name_first": "Nicholas",
      "name_last": "Lesniak",
      ...
    },
    ...
  }
}
```

**Key Fields**:
- `uid` - Internal Schoology user ID (used for dropbox URLs)
- `school_uid` - Student's unique school identifier (e.g., `s486002`)
- `name_first`, `name_last` - Student names
- `type` - User role (`"2"` = student)

**Note**: This endpoint is already used in `SchoologyCommentUpdater.java:68-128`

---

### 2. Bulk Download All Submissions

**Purpose**: Download all student submissions for an assignment as a single ZIP file.

**Endpoint**:
```
GET /assignment/{assignmentId}/dropbox/download_all
```

**Parameters**:
- `assignmentId` (path) - Assignment identifier (e.g., `8048841803`)

**Headers**:
```http
Cookie: [session cookie from .schoology-cookie file]
x-csrf-key: [from .env]
x-csrf-token: [from .env]
accept: */*
referer: https://schoology.d214.org/assignment/{assignmentId}/info
```

**Response** (200 OK):
```http
Content-Type: application/zip
Content-Disposition: attachment; filename="Challenge_Functions.zip"
Content-Length: 8475902
Last-Modified: Mon, 20 Oct 2025 14:11:13 GMT
Etag: "68f64301-8154fe"
```

**Body**: Binary ZIP file

---

### 3. ZIP File Structure

The downloaded ZIP file contains student submissions organized as follows:

```
Challenge_Functions.zip/
├── Aleman, Juaquin - s519779/
│   └── Revision 1 - On time/
│       └── FunctionsChallenge.zip
├── Castro, Marianna - s486002/
│   ├── Revision 1 - On time/
│   │   └── FunctionsChallenge.zip
│   └── Revision 2 - Late/
│       └── FunctionsChallengeV2.zip
├── Lesniak, Nicholas - s498783/
│   └── Revision 1 - On time/
│       └── FunctionsChallenge.zip
└── ...
```

**Folder Naming Convention**:
```
{LastName}, {FirstName} - {school_uid}/
  └── Revision {N} - {Status}/
      └── [submitted files]
```

**Revision Status Values**:
- `On time` - Submitted before due date
- `Late` - Submitted after due date

**File Contents**:
- Student-submitted files (`.zip`, `.swift`, etc.)
- May include nested ZIP archives
- Preserves original file names

---

### 4. View Individual Submission (Metadata)

**Purpose**: Get submission details and file information for a single student.

**Endpoint**:
```
GET /assignment/{assignmentId}/dropbox/view/{studentUid}?ajax&revision=0&item=0&from_gb=1
```

**Parameters**:
- `assignmentId` (path) - Assignment identifier
- `studentUid` (path) - Student's internal UID (from enrollment API)
- `revision` (query) - Revision number (`0` = latest)
- `item` (query) - Item index (`0` = first item)
- `from_gb` (query) - Source flag (`1` = from gradebook)

**Headers**:
```http
Cookie: [session cookie]
x-csrf-key: [from .env]
x-csrf-token: [from .env]
x-requested-with: XMLHttpRequest
accept: application/json, text/javascript, */*; q=0.01
referer: https://schoology.d214.org/assignment/{assignmentId}/dropbox/view/{prevStudentUid}?from_gb=1
```

**Response** (200 OK):
```json
{
  "popups": { ... },
  "ajax_redirects": [],
  "output": "<div>...</div>",
  "messages": ""
}
```

**Response HTML (in `output` field)** contains:
- Submission timestamp: `Thursday, October 16, 2025 at 9:53 am`
- Submission status: `<span class="submission-status ontime">On time</span>`
- File information:
  ```html
  <select name="dropbox-viewer-item-select">
    <option value="5313223778">FunctionsChallenge.zip</option>
  </select>
  ```
- Download link:
  ```html
  <a href="https://schoology.d214.org/submission/5313223778/source">
    FunctionsChallenge.zip
  </a>
  ```

**Note**: This endpoint returns HTML wrapped in JSON. File metadata must be parsed from HTML.

---

### 5. Download Individual File (Alternative)

**Purpose**: Download a specific submission file by its ID.

**Endpoint**:
```
GET /submission/{fileId}/source
```

**Parameters**:
- `fileId` (path) - Submission file identifier (from dropbox view response)

**Headers**:
```http
Cookie: [session cookie]
```

**Response** (200 OK):
```http
Content-Type: application/zip (or appropriate MIME type)
Content-Disposition: attachment; filename="FunctionsChallenge.zip"
Content-Length: 706560
```

**Body**: Binary file content

**Note**: This endpoint is useful for downloading individual files but is **not recommended** for batch operations. Use bulk download instead.

---

## Authentication

All endpoints require the same authentication as grade/comment posting:

### Session Cookie
**Location**: `.schoology-cookie` file in project root

**Format**: Full cookie string
```
has_js=1; apt.uid=AP-IBYB1G3SIPA6-2-1755022324706-31284494.0.2.6513d47d-6c07-47df-a93c-60f85439fa54; SESSff8a9a578b33909b0e365eb21af2201f=fa9b408331098bda806f7876c0763079; ...
```

**Expiration**: ~24 hours

### CSRF Tokens
**Location**: `.env` file

**Variables**:
```bash
SCHOOLOGY_CSRF_KEY=uoFZ_-36ixEm4YK_qKI1K2hle9ZJVLBPpzG_jJ47ld0
SCHOOLOGY_CSRF_TOKEN=51e31d2d7725b36dd5f0dee51ef16c10
```

**How to Extract**:
1. Open Schoology in browser (logged in)
2. Open DevTools → Network tab
3. Navigate to assignment dropbox
4. Find any API request (e.g., enrollment call)
5. Copy `x-csrf-key` and `x-csrf-token` headers

**Important**: Use CSRF tokens from **grade posting** requests, not comment posting (different tokens).

---

## Usage Examples

### Example 1: Bulk Download Submissions

**Java (using OkHttp)**:
```java
String assignmentId = "8048841803";
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

try (Response response = client.newCall(request).execute()) {
    if (response.isSuccessful()) {
        // Save ZIP file
        byte[] zipBytes = response.body().bytes();
        Files.write(Path.of("submissions.zip"), zipBytes);
    }
}
```

### Example 2: Check Download Freshness (Caching)

**Use HTTP `Last-Modified` header**:
```java
// GET request with If-Modified-Since header
Request request = new Request.Builder()
    .url(url)
    .header("If-Modified-Since", cachedTimestamp)
    .build();

Response response = client.newCall(request).execute();
if (response.code() == 304) {
    // Not modified - use cached version
} else if (response.code() == 200) {
    // New data - download and update cache
    String lastModified = response.header("Last-Modified");
}
```

### Example 3: Extract and Organize Submissions

**Folder transformation**:
```java
// 1. Extract ZIP
ZipFile zipFile = new ZipFile("Challenge_Functions.zip");
zipFile.extractAll("submissions/temp/");

// 2. Rename folders
Path tempDir = Path.of("submissions/temp/Challenge_Functions");
Files.list(tempDir).forEach(studentFolder -> {
    String folderName = studentFolder.getFileName().toString();
    // "Castro, Marianna - s486002" → "s486002"
    String schoolUid = folderName.substring(folderName.lastIndexOf("- ") + 2);

    Path targetFolder = Path.of("submissions/Functions_Challenge/" + schoolUid);
    Files.move(studentFolder, targetFolder);
});

// 3. Flatten revisions (keep most recent)
Files.list(Path.of("submissions/Functions_Challenge/")).forEach(studentDir -> {
    // Find all "Revision N - Status" folders
    List<Path> revisions = Files.list(studentDir)
        .filter(Files::isDirectory)
        .sorted(Comparator.comparing(this::getLastModifiedTime).reversed())
        .collect(Collectors.toList());

    // Move files from most recent revision to student root
    Path mostRecent = revisions.get(0);
    Files.list(mostRecent).forEach(file -> {
        Files.move(file, studentDir.resolve(file.getFileName()));
    });

    // Delete revision folders
    revisions.forEach(this::deleteRecursively);
});
```

---

## Error Handling

### HTTP 401 Unauthorized
**Cause**: Session cookie expired or invalid

**Solution**:
1. Refresh `.schoology-cookie` file
2. Extract new cookie from browser DevTools
3. Restart application

### HTTP 403 Forbidden
**Cause**: Invalid CSRF tokens or insufficient permissions

**Solution**:
1. Verify CSRF tokens are from **grade posting** request (not comment posting)
2. Extract fresh tokens from browser DevTools
3. Ensure user has instructor/admin role for course

### HTTP 404 Not Found
**Cause**: Invalid assignment ID or assignment has no submissions

**Solution**:
1. Verify `assignmentId` is correct
2. Check that assignment exists in Schoology
3. Verify at least one student has submitted

### HTTP 200 but Empty ZIP
**Cause**: No students have submitted yet

**Solution**:
- Check assignment submission count in Schoology
- Verify assignment due date hasn't passed without submissions
- Skip grading for this assignment

### Malformed ZIP File
**Cause**: Incomplete download or corrupted file

**Solution**:
1. Retry download
2. Verify `Content-Length` matches downloaded bytes
3. Check network stability

---

## Performance Characteristics

### Bulk Download
- **File size**: ~500 KB - 10 MB per assignment (depends on submission count)
- **Download time**: 5-30 seconds (depends on network speed)
- **Rate limiting**: No limits observed (tested up to 10 concurrent downloads)

### Extraction
- **Time**: ~1-3 seconds per assignment
- **Disk I/O**: Minimal (temporary files cleaned up after extraction)

### Caching Strategy
- **Cache key**: `{assignmentId}_{Last-Modified header}`
- **Invalidation**: When server's `Last-Modified` changes
- **Storage**: `results/submission-cache.json`

---

## Security Considerations

### Session Management
- **Expiration**: Sessions expire after ~24 hours
- **Renewal**: Manual re-authentication required
- **Storage**: Cookie stored in `.schoology-cookie` (gitignored)

### Data Privacy
- **Student names**: Included in folder names (sanitized in final structure)
- **Submission content**: Downloaded to local disk (not sent externally)
- **API calls**: All traffic over HTTPS

### File System
- **Permissions**: Downloaded files inherit system user permissions
- **Cleanup**: Temporary ZIP files can be deleted after extraction
- **Isolation**: Each assignment stored in separate folder

---

## Limitations

### Known Constraints
1. **No official API**: Uses reverse-engineered endpoints (subject to change)
2. **Session expiration**: Requires manual token refresh every 24 hours
3. **No batch submission metadata**: Must parse HTML for submission details
4. **ZIP nesting**: Student submissions may contain nested ZIPs (requires recursive extraction)

### Unsupported Features
- **Selective download**: Cannot filter by student (all-or-nothing)
- **Delta sync**: No API to fetch only new/updated submissions
- **Submission history**: Can only download latest revisions via bulk endpoint

---

## Testing

### Manual Testing Checklist
- [ ] Download submissions for assignment with 0 submissions (empty ZIP)
- [ ] Download submissions for assignment with 1-5 students
- [ ] Download submissions for assignment with 20+ students
- [ ] Verify ZIP extraction preserves nested ZIPs
- [ ] Verify folder renaming works with special characters in names
- [ ] Test with expired session cookie (expect 401)
- [ ] Test with invalid CSRF tokens (expect 403)
- [ ] Test with invalid assignment ID (expect 404)

### Automated Testing
```java
@Test
public void testBulkDownload() {
    SchoologySubmissionDownloader downloader = new SchoologySubmissionDownloader(...);
    Path zipFile = downloader.downloadSubmissions("8048841803", "Test Assignment");

    assertTrue(Files.exists(zipFile));
    assertTrue(Files.size(zipFile) > 0);
}

@Test
public void testZipExtraction() {
    Path zipFile = Path.of("test-data/sample-submissions.zip");
    downloader.extractAndOrganizeSubmissions(zipFile, "Test Assignment");

    Path submissionsDir = Path.of("submissions/Test_Assignment");
    assertTrue(Files.exists(submissionsDir));

    // Verify folder structure: school_uid folders exist
    assertTrue(Files.exists(submissionsDir.resolve("s486002")));
    assertTrue(Files.exists(submissionsDir.resolve("s498783")));
}
```

---

## Troubleshooting

### Issue: ZIP file downloads but is corrupted

**Symptoms**: Cannot extract ZIP, error messages about invalid format

**Diagnosis**:
```bash
# Check if file is actually a ZIP
file Challenge_Functions.zip
# Should output: "Zip archive data"

# Check file size matches Content-Length
ls -lh Challenge_Functions.zip
```

**Solution**:
- Ensure complete download (check Content-Length header)
- Retry download with fresh session
- Check disk space availability

### Issue: Folder names contain unexpected characters

**Symptoms**: Special characters in student names cause file system errors

**Example**: `O'Brien, Patrick - s123456` → invalid path on Windows

**Solution**:
```java
// Sanitize folder name before creating
String sanitized = folderName
    .replaceAll("[<>:\"/\\\\|?*]", "_")  // Replace invalid chars
    .replaceAll("\\s+", "_");              // Replace whitespace
```

### Issue: Multiple revisions - which to keep?

**Symptoms**: Student has 3 revisions, unclear which to grade

**Current behavior**: Keep most recent by timestamp (ignore late/on-time status)

**Verification**:
```java
// Get file modification time (when revision was uploaded)
FileTime timestamp = Files.getLastModifiedTime(revisionFolder);
```

---

## References

### Related Documentation
- **Batch Grading Feature**: `../BATCH_GRADING_FEATURE.md`
- **Grading Cache System**: `caching-system.md`
- **Schoology Grade Posting**: See `SchoologyCommentUpdater.java:379-495`
- **Schoology Comment Posting**: See `SchoologyCommentUpdater.java:143-314`

### External Resources
- **Schoology API Docs** (official REST API): https://developers.schoology.com/api-documentation
  - Note: Official API does NOT include submission download endpoints
- **OkHttp Documentation**: https://square.github.io/okhttp/
- **ZIP File Handling (Java)**: https://www.baeldung.com/java-compress-and-uncompress

### Discovery Session
- **Date**: October 20, 2025
- **Method**: Browser DevTools inspection of Schoology web interface
- **Assignment tested**: Challenge: Functions (ID: `8048841803`)
- **Course tested**: COMP PRG MOB APP (ID: `7921261672`)
