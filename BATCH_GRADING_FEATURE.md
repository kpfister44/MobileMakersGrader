# Batch Grading Feature

## Overview

The Batch Grading feature enables automatic grading of multiple assignments in a single run. The system automatically downloads student submissions from Schoology, processes them with assignment-specific prompts, and posts results back to Schoology.

**Status**: Planned (API Discovery Complete)

---

## Goals

1. **Multi-Assignment Grading**: Grade multiple assignments sequentially in one execution
2. **Automatic Submission Download**: Fetch student submissions directly from Schoology API
3. **Smart Caching**: Only download/grade new or updated submissions
4. **Error Resilience**: Skip failed assignments and continue processing
5. **Zero Manual Intervention**: Run once, all assignments graded and posted to Schoology

---

## User Workflow

### Current (Single Assignment)
1. Manually download submissions from Schoology
2. Manually place in `submissions/` folder
3. Manually update `AssignmentPrompt.java` import
4. Run grader
5. Results posted to Schoology

### New (Batch Grading)
1. Configure assignments in `.env` file
2. Run grader
3. **Done!** All assignments graded and posted automatically

---

## Configuration

### Environment Variables (`.env`)

```bash
# ========================================
# Batch Grading Configuration
# ========================================

# Assignment 1: Constants, Variables, Datatypes
ASSIGNMENT_1_ID=8017693525
ASSIGNMENT_1_NAME=Constants Variables Datatypes
ASSIGNMENT_1_PROMPT=ConstantsVariablesDatatypesPrompt

# Assignment 2: Operators Challenge
ASSIGNMENT_2_ID=8017693608
ASSIGNMENT_2_NAME=Operators
ASSIGNMENT_2_PROMPT=OperatorsChallengePrompt

# Assignment 3: TextFields Challenge
ASSIGNMENT_3_ID=8017693698
ASSIGNMENT_3_NAME=TextFields
ASSIGNMENT_3_PROMPT=TextFieldsChallengePrompt

# Assignment 4: Unit App Multiply
ASSIGNMENT_4_ID=8017693799
ASSIGNMENT_4_NAME=Multiply
ASSIGNMENT_4_PROMPT=MultiplyPrompt

# Commented out assignments are skipped
# ASSIGNMENT_5_ID=...
# ASSIGNMENT_5_NAME=...
# ASSIGNMENT_5_PROMPT=...
```

### Configuration Rules

1. **Assignment numbering**: Must be sequential (1, 2, 3, 4...)
2. **Required fields**: All three fields (ID, NAME, PROMPT) must be present
3. **Skipping assignments**: Comment out entire assignment block (all 3 lines)
4. **Assignment names**: Will be sanitized for folder names (spaces → underscores)
5. **Prompt classes**: Must exist in `src/main/java/com/mobilemakers/grader/prompts/`

---

## Architecture

### New Components

#### 1. `AssignmentConfig.java`
**Purpose**: Parse and validate assignment configuration from `.env`

```java
public class AssignmentConfig {
    private final String id;
    private final String name;
    private final String promptClassName;

    // Loads all ASSIGNMENT_*_ID from .env
    public static List<AssignmentConfig> loadFromEnvironment()

    // Sanitizes assignment name for folder paths
    public String getSanitizedName() // "Functions Challenge" → "Functions_Challenge"
}
```

#### 2. `PromptLoader.java`
**Purpose**: Dynamically load prompt classes using Java Reflection

```java
public class PromptLoader {
    // Uses reflection to load prompt by class name
    public static String loadPrompt(String promptClassName) throws ClassNotFoundException

    // Example: loadPrompt("MultiplyPrompt")
    //   → Class.forName("com.mobilemakers.grader.prompts.MultiplyPrompt")
    //   → Field.get("PROMPT")
}
```

#### 3. `SchoologySubmissionDownloader.java`
**Purpose**: Download and extract student submissions from Schoology

**API Endpoints Used**:
- `GET /assignment/{assignmentId}/dropbox/download_all` - Bulk download ZIP

**Key Methods**:
```java
// Download all submissions as ZIP file
public Path downloadSubmissions(String assignmentId, String assignmentName)

// Extract ZIP to submissions folder
public void extractAndOrganizeSubmissions(Path zipFile, String assignmentName)

// Rename folders: "LastName, FirstName - school_uid" → "school_uid"
private void renameStudentFolders(Path assignmentDir)

// Select most recent revision by timestamp
private void flattenRevisions(Path studentDir)
```

**Folder Structure Transformation**:

**Before (Schoology ZIP format)**:
```
submissions/Functions_Challenge/
├── Castro, Marianna - s486002/
│   ├── Revision 1 - On time/
│   │   └── FunctionsChallenge.zip
│   └── Revision 2 - Late/
│       └── FunctionsChallengeV2.zip
└── Lesniak, Nicholas - s498783/
    └── Revision 1 - On time/
        └── FunctionsChallenge.zip
```

**After (SwiftFileReader compatible)**:
```
submissions/Functions_Challenge/
├── s486002/
│   └── FunctionsChallengeV2.zip  (most recent revision)
└── s498783/
    └── FunctionsChallenge.zip
```

#### 4. `SubmissionCache.java`
**Purpose**: Track downloaded submissions to avoid redundant downloads

**Cache Format** (`results/submission-cache.json`):
```json
{
  "s486002_8017693525": {
    "assignmentId": "8017693525",
    "assignmentName": "Constants Variables Datatypes",
    "schoolUid": "s486002",
    "lastDownloaded": "2025-10-20T14:30:00Z",
    "revisionCount": 1
  }
}
```

**Behavior**:
- Skip download if cache timestamp matches server `Last-Modified` header
- Manual override: Delete cache entry to force re-download

#### 5. `BatchGrader.java`
**Purpose**: Orchestrate batch grading across multiple assignments

**Processing Flow**:
```java
public void gradeAllAssignments() {
    List<AssignmentConfig> assignments = AssignmentConfig.loadFromEnvironment();

    for (AssignmentConfig assignment : assignments) {
        try {
            // 1. Download submissions (if needed)
            Path zipFile = downloadSubmissions(assignment);

            // 2. Extract and organize folders
            Path submissionsDir = organizeSubmissions(zipFile, assignment);

            // 3. Load prompt dynamically
            String promptText = PromptLoader.loadPrompt(assignment.getPromptClassName());

            // 4. Create assignment-specific GradeProcessor
            AssignmentPrompt assignmentPrompt = new AssignmentPrompt(promptText);
            GradeProcessor processor = new GradeProcessor(
                swiftFileReader,
                assignmentPrompt,
                openAIGrader,
                lmStudioGrader,
                assignment.getId(),        // ← Assignment ID for cache keys
                assignment.getName()       // ← Assignment name for results folder
            );

            // 5. Grade all students
            Path resultsDir = Path.of("results", assignment.getSanitizedName() + "-" + timestamp());
            processor.gradeAll(submissionsDir, resultsDir);

            // Cache automatically prevents redundant AI calls!
            // Only new/updated submissions are graded.

            log.info("✓ Completed: " + assignment.getName());

        } catch (Exception e) {
            log.error("✗ Skipped: " + assignment.getName() + " - " + e.getMessage());
            // Continue with next assignment
        }
    }

    printSummary();
}
```

### Modified Components

#### `AssignmentPrompt.java`
**Before**: Hardcoded import
```java
import static com.mobilemakers.grader.prompts.MultiplyPrompt.PROMPT;

public String buildPrompt(String swiftCode) {
    return PROMPT.replace("%s", swiftCode);
}
```

**After**: Dynamic prompt injection
```java
private final String promptText;

public AssignmentPrompt(String promptText) {
    this.promptText = promptText;
}

public String buildPrompt(String swiftCode) {
    return promptText.replace("%s", swiftCode);
}
```

#### `GradeProcessor.java`
**Critical changes for batch mode**:
- Constructor accepts `assignmentId` and `assignmentName` parameters
- Uses passed `assignmentId` instead of reading from `.env` (was: `Config.get("SCHOOLOGY_ASSIGNMENT_ID")`)
- Results saved to `results/{AssignmentName}-{timestamp}/`
- **Existing `GradingCache` automatically prevents duplicate AI calls** (no changes needed!)

**Before (single assignment)**:
```java
// Line 158: Read from global .env
String assignmentId = Config.get("SCHOOLOGY_ASSIGNMENT_ID");
```

**After (batch mode)**:
```java
private final String assignmentId;

public GradeProcessor(..., String assignmentId, String assignmentName) {
    this.assignmentId = assignmentId;
    this.assignmentName = assignmentName;
    // ...
}

// Now uses this.assignmentId everywhere (line 158, 232, etc.)
```

**Why this matters**: Cache key is `{school_uid}_{assignmentId}`, so each assignment tracks revisions independently

#### `Main.java`
**Before**: Single assignment mode
```java
public static void main(String[] args) {
    GradeProcessor processor = new GradeProcessor(...);
    processor.gradeAll(Path.of("submissions"), Path.of("results"));
}
```

**After**: Batch mode support
```java
public static void main(String[] args) {
    boolean batchMode = Config.getBoolean("ENABLE_BATCH_GRADING");

    if (batchMode) {
        BatchGrader batchGrader = new BatchGrader();
        batchGrader.gradeAllAssignments();
    } else {
        // Original single-assignment mode (backward compatible)
        GradeProcessor processor = new GradeProcessor(...);
        processor.gradeAll(Path.of("submissions"), Path.of("results"));
    }
}
```

---

## Cost Protection: AI Call Caching

### How It Works

**The existing `GradingCache.java` already supports multiple assignments!** No changes needed to prevent redundant AI calls.

**Cache Structure** (`results/grading-cache.json`):
```json
{
  "students": {
    "s486002": {
      "name": "Castro, Marianna",
      "assignments": {
        "8017693525": {
          "assignment_name": "Constants Variables Datatypes",
          "last_graded_revision": 1,
          "grade": 10.0,
          "graded_timestamp": "2025-10-20T14:30:00Z"
        },
        "8017693608": {
          "assignment_name": "Operators",
          "last_graded_revision": 2,
          "grade": 9.5,
          "graded_timestamp": "2025-10-20T15:45:00Z"
        }
      }
    },
    "s498783": {
      "name": "Lesniak, Nicholas",
      "assignments": {
        "8017693525": {
          "assignment_name": "Constants Variables Datatypes",
          "last_graded_revision": 1,
          "grade": 8.0,
          "graded_timestamp": "2025-10-20T14:32:00Z"
        }
      }
    }
  },
  "cache_version": "1.0",
  "last_updated": "2025-10-20T15:45:00Z"
}
```

**Cache Key**: `{school_uid}_{assignmentId}`
- Example: `s486002_8017693525` (Castro's Constants assignment)
- Example: `s486002_8017693608` (Castro's Operators assignment)

### AI Call Decision Flow

**For each student submission**:

```java
// 1. Get highest revision from downloaded files
int highestRevision = swiftFileReader.findHighestRevision(studentDir);

// 2. Check cache for last graded revision
int lastGradedRevision = gradingCache.getLastGradedRevision(schoolUid, assignmentId);

// 3. Decision
if (highestRevision <= lastGradedRevision) {
    // ✓ SKIP AI CALL - Already graded this or higher revision
    log.info("⊘ Skipped {} - Revision {} already graded", studentKey, highestRevision);
    continue;
} else {
    // → CALL AI - New revision detected
    log.info("→ New submission detected for {} - Revision {}", studentKey, highestRevision);
    GradingResult result = openAIGrader.gradeSubmission(studentKey, prompt);

    // Update cache
    gradingCache.updateGrade(schoolUid, studentName, assignmentId, assignmentName,
                             highestRevision, result.score());
}
```

### Example: Batch Grading Run

**Scenario**: Grade 4 assignments with 18 students each

**Run 1 (Fresh cache)**:
```
Processing: Constants Variables Datatypes
  → Grading 18 students...
  ✓ Completed: 18/18 graded (18 AI calls)

Processing: Operators
  → Grading 18 students...
  ✓ Completed: 18/18 graded (18 AI calls)

Processing: TextFields
  → Grading 18 students...
  ✓ Completed: 18/18 graded (18 AI calls)

Processing: Multiply
  → Grading 18 students...
  ✓ Completed: 18/18 graded (18 AI calls)

Total AI calls: 72
Total cost: ~$0.296 (72 × $0.0041)
```

**Run 2 (Same day, no new submissions)**:
```
Processing: Constants Variables Datatypes
  ⊘ Skipped all 18 students - Revision 1 already graded
  ✓ Completed: 0/18 graded (0 AI calls)

Processing: Operators
  ⊘ Skipped all 18 students - Revision 1 already graded
  ✓ Completed: 0/18 graded (0 AI calls)

Processing: TextFields
  ⊘ Skipped all 18 students - Revision 1 already graded
  ✓ Completed: 0/18 graded (0 AI calls)

Processing: Multiply
  ⊘ Skipped all 18 students - Revision 1 already graded
  ✓ Completed: 0/18 graded (0 AI calls)

Total AI calls: 0
Total cost: $0.00
```

**Run 3 (3 students resubmitted Operators)**:
```
Processing: Constants Variables Datatypes
  ⊘ Skipped all 18 students
  ✓ Completed: 0/18 graded (0 AI calls)

Processing: Operators
  ⊘ Skipped 15 students - Revision 1 already graded
  → New submission detected for Castro, Marianna - Revision 2
  → New submission detected for Lesniak, Nicholas - Revision 2
  → New submission detected for Patel, Krishiv - Revision 2
  ✓ Completed: 3/18 graded (3 AI calls)

Processing: TextFields
  ⊘ Skipped all 18 students
  ✓ Completed: 0/18 graded (0 AI calls)

Processing: Multiply
  ⊘ Skipped all 18 students
  ✓ Completed: 0/18 graded (0 AI calls)

Total AI calls: 3
Total cost: ~$0.012 (3 × $0.0041)
```

### Cost Protection Guarantees

✅ **No duplicate AI calls**: Same revision never graded twice
✅ **Cross-assignment isolation**: Grading Assignment 1 doesn't affect Assignment 2 cache
✅ **Revision tracking**: Only grade new revisions (revision 2 after revision 1 was graded)
✅ **Persistent cache**: Survives program restarts (saved to `results/grading-cache.json`)
✅ **Manual override**: Delete cache entry to force re-grade

### Cache Management

**View cache contents**:
```bash
cat results/grading-cache.json | jq
```

**Clear cache for specific student**:
```bash
# Edit results/grading-cache.json
# Remove student's entry under "students" → "{school_uid}"
```

**Clear cache for specific assignment**:
```bash
# Edit results/grading-cache.json
# Remove assignment entry under student → "assignments" → "{assignmentId}"
```

**Clear entire cache** (force re-grade everything):
```bash
rm results/grading-cache.json
```

---

## Folder Structure

```
MobileMakersGrader/
├── submissions/
│   ├── Constants_Variables_Datatypes/
│   │   ├── s486002/
│   │   │   └── *.swift files
│   │   └── s498783/
│   │       └── *.swift files
│   ├── Operators/
│   │   └── ...
│   └── Multiply/
│       └── ...
├── results/
│   ├── Constants_Variables_Datatypes-20251020-143000/
│   │   ├── grades.csv
│   │   └── schoology-grades.csv
│   ├── Operators-20251020-144500/
│   │   └── ...
│   ├── submission-cache.json (NEW)
│   └── grading-cache.json (existing)
└── .env
```

---

## Error Handling

### Assignment-Level Errors
If any step fails for an assignment:
1. Log detailed error message
2. Print: `✗ Skipped: {AssignmentName} - {ErrorMessage}`
3. **Continue** processing remaining assignments
4. Final summary shows: Completed vs. Skipped

**Example Output**:
```
Processing: Constants Variables Datatypes
  → Downloading submissions...
  → Extracting submissions...
  → Loading prompt: ConstantsVariablesDatatypesPrompt
  → Grading 18 students...
  ✓ Completed: Constants Variables Datatypes (18/18 graded)

Processing: Operators
  ✗ Skipped: Operators - Failed to download: 403 Forbidden

Processing: Multiply
  → Downloading submissions...
  ✓ Completed: Multiply (18/18 graded)

═══════════════════════════════════════════
Batch Grading Summary:
  Total assignments: 3
  Successfully graded: 2
  Skipped (errors): 1
  Total students graded: 36
═══════════════════════════════════════════
```

### Student-Level Errors
- Handled by existing `GradeProcessor` error handling
- Students with errors get `0/10` grade with error message in CSV
- Grading continues for remaining students

---

## Implementation Phases

### Phase 1: Configuration & Dynamic Prompts (4-5 hours)
- [ ] `AssignmentConfig.java` - Parse `.env` assignments
- [ ] `PromptLoader.java` - Dynamic prompt loading via reflection
- [ ] Update `AssignmentPrompt.java` - Accept prompt string in constructor
- [ ] Add `ENABLE_BATCH_GRADING` flag to `.env`
- [ ] Unit tests for config parsing

### Phase 2: Submission Downloader (8-10 hours)
- [ ] `SchoologySubmissionDownloader.java` - Download ZIP endpoint
- [ ] Extract ZIP file to `submissions/{AssignmentName}/`
- [ ] Rename folders: `"LastName, FirstName - school_uid"` → `"school_uid"`
- [ ] Flatten revisions: Keep only most recent by timestamp
- [ ] Handle nested ZIP files (student submissions)
- [ ] Error handling for network failures

### Phase 3: Submission Caching (4-5 hours)
- [ ] `SubmissionCache.java` - Track download timestamps
- [ ] Cache persistence: `results/submission-cache.json`
- [ ] HTTP `Last-Modified` header checking
- [ ] Skip download if cache is fresh
- [ ] Manual cache invalidation support

### Phase 4: Batch Orchestration (3-4 hours)
- [ ] `BatchGrader.java` - Main batch processing loop
- [ ] Update `Main.java` - Batch mode vs. single mode
- [ ] **CRITICAL**: Update `GradeProcessor` - Accept `assignmentId` and `assignmentName` parameters
  - Replace `Config.get("SCHOOLOGY_ASSIGNMENT_ID")` with `this.assignmentId`
  - Ensures cache keys use correct assignment ID
  - **This enables cost protection across multiple assignments**
- [ ] Results folder per assignment
- [ ] Summary report generation

### Phase 5: Testing & Documentation (4-6 hours)
- [ ] Integration testing with real Schoology data
- [ ] Test error scenarios (network failure, missing prompts, etc.)
- [ ] Update `CLAUDE.md` with batch grading instructions
- [ ] Update `README.md` with new workflow
- [ ] Create troubleshooting guide

**Total Estimated Time**: 23-30 hours

---

## Testing Plan

### Unit Tests
- [ ] `AssignmentConfig` - Parse valid/invalid .env configs
- [ ] `PromptLoader` - Load existing/missing prompt classes
- [ ] `SubmissionCache` - Cache hit/miss scenarios

### Integration Tests
- [ ] Download submissions for 1 assignment
- [ ] Extract and rename folders correctly
- [ ] Grade single assignment end-to-end
- [ ] Grade multiple assignments in batch
- [ ] Verify Schoology posting works across assignments

### Error Scenario Tests
- [ ] Network failure during download
- [ ] Invalid assignment ID
- [ ] Missing prompt class
- [ ] Malformed ZIP file
- [ ] Expired Schoology session

---

## Backward Compatibility

### Single Assignment Mode (Current Behavior)
```bash
# .env (no batch config)
OPENAI_API_KEY=...
ENABLE_SCHOOLOGY_GRADES=true
# ... existing config only
```

**Behavior**: Works exactly as before
- Manual submission upload to `submissions/`
- Manual prompt selection in `AssignmentPrompt.java`
- Single grading run

### Batch Mode (New Behavior)
```bash
# .env (with batch config)
ENABLE_BATCH_GRADING=true
ASSIGNMENT_1_ID=...
ASSIGNMENT_1_NAME=...
ASSIGNMENT_1_PROMPT=...
```

**Behavior**: New batch workflow activated

---

## Future Enhancements

### Phase 6+ (Post-Launch)
- [ ] **Parallel grading**: Grade multiple assignments concurrently
- [ ] **Incremental grading**: Only grade new students since last run
- [ ] **Web UI**: Configure assignments via web interface
- [ ] **Assignment detection**: Auto-detect assignments from Schoology
- [ ] **Dry run mode**: Preview what would be graded without API calls
- [ ] **Notifications**: Email/Slack notifications on completion
- [ ] **Analytics**: Track grading metrics across assignments

---

## Security Considerations

1. **Schoology Session Expiration**:
   - Sessions expire after ~24 hours
   - Error handling will detect 401/403 and prompt for new credentials

2. **API Rate Limiting**:
   - Batch downloads are infrequent (once per assignment)
   - No rate limit concerns identified

3. **File System Security**:
   - Downloaded ZIP files deleted after extraction
   - Student folders readable only by instructor

4. **Credential Storage**:
   - `.env` and `.schoology-cookie` remain in `.gitignore`
   - No new credential storage needed

---

## Dependencies

### New Maven Dependencies
None required - uses existing dependencies:
- `okhttp3` - HTTP client for downloads
- `jackson` - JSON parsing for cache
- `dotenv-java` - Environment variable loading

### System Requirements
- **Disk space**: ~50-100 MB per assignment (temporary ZIP storage)
- **Network**: Stable connection for bulk downloads (8-10 MB per assignment)

---

## Success Metrics

- [x] API discovery complete
- [ ] Configuration system working
- [ ] Download and extraction working
- [ ] Dynamic prompt loading working
- [ ] Batch orchestration working
- [ ] End-to-end test: Grade 4 assignments in one run
- [ ] Documentation complete
- [ ] User successfully grades full semester in single run

---

## Questions & Decisions

### Resolved
- [x] **Folder naming**: Rename to `school_uid` only (simpler than updating SwiftFileReader)
- [x] **Revision selection**: Most recent timestamp (ignore late/on-time status)
- [x] **Assignment name sanitization**: Replace spaces with underscores
- [x] **Error handling**: Skip failed assignments, continue processing (Option A)

### Open Questions
None - ready to implement!

---

## References

- **API Documentation**: `docs/schoology-submission-api.md`
- **Existing Features**:
  - Grading Cache: `docs/caching-system.md`
  - Grade Posting: See `SchoologyCommentUpdater.java:379-495`
  - Comment Posting: See `SchoologyCommentUpdater.java:143-314`
