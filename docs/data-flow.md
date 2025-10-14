# Data Flow Documentation

## Overview

This document traces how data flows through the Swift Assignment Auto-Grader from program start to completion. Understanding this flow is critical for optimizing performance and debugging issues.

## High-Level Flow

```
[Start]
  → Load Configuration
  → Initialize Components
  → Load Grading Cache
  → Fetch Schoology Mappings (optional)
  → FOR EACH Student:
      → Check Cache (skip if already graded)
      → Read Swift Files
      → Call AI API (OpenAI or LM Studio) ← SLOWEST STEP
      → Update Cache
      → Post to Schoology (optional)
  → Save Cache
  → Write CSV Files
[End]
```

## Detailed Data Flow

### 1. Program Initialization (`Main.java`)

**Entry Point:** `Main.main(String[] args)` (Line 12)

```
Input: Command-line arguments
  - args[0]: submissions directory (default: "submissions")
  - args[1]: results directory (default: "results")

Flow:
  1. Parse arguments (Lines 18-19)
  2. Create component instances:
     - SwiftFileReader (Line 21)
     - AssignmentPrompt (Line 22)
     - OpenAIGrader (Line 23)
     - LMStudioGrader (Line 24)
     - GradeProcessor (Lines 26-28)
  3. Call processor.gradeAll() (Line 31)

Output: Initialized GradeProcessor with all dependencies
```

---

### 2. Configuration Loading (`Config.java`)

**Triggered:** During GradeProcessor initialization (Line 59, 74-75)

```
Flow:
  1. Load .env file from project root
  2. Read environment variables:
     - USE_LOCAL_MODEL (boolean) - which AI to use
     - OPENAI_API_KEY (if OpenAI selected)
     - LM_STUDIO_ENDPOINT, LM_STUDIO_MODEL (if local selected)
     - ENABLE_SCHOOLOGY_COMMENTS (boolean)
     - ENABLE_SCHOOLOGY_GRADES (boolean)
     - SCHOOLOGY_* credentials (if Schoology enabled)

Output: Environment configuration accessible via Config.get(key)
```

---

### 3. Grading Cache Initialization (`GradeProcessor.gradeAll()`)

**File:** `GradeProcessor.java` Line 155

```
Flow:
  1. Create GradingCache instance with results directory path
  2. GradingCache.load() reads results/grading-cache.json
  3. Parse JSON into in-memory data structure:
     {
       "students": {
         "s513390": {
           "name": "Student Name",
           "assignments": {
             "8017693698": {
               "last_graded_revision": 2,
               "grade": 10.0,
               "graded_timestamp": "2025-10-11T15:30:00Z"
             }
           }
         }
       }
     }

Output: In-memory cache of previously graded submissions
Cache Purpose: Avoid re-grading already-processed submissions
```

**Key Methods:**
- `getLastGradedRevision(schoolUid, assignmentId)` → Returns revision number or 0
- `updateGrade(...)` → Updates cache after successful grading
- `save()` → Persists cache to disk at end of run

---

### 4. Student Submissions Reading (`SwiftFileReader.readStudentSubmissions()`)

**File:** `GradeProcessor.java` Line 163

```
Input: Path to submissions directory (e.g., "submissions/")

Flow:
  1. List all subdirectories under submissions/ (each = one student)
  2. For each student directory:
     a. Extract student key from folder name (e.g., "Smith, John - s123456")
     b. Call readLatestSubmission(studentDir):
        i.   Find all revision folders/zips
        ii.  Sort by last-modified timestamp (most recent first)
        iii. For most recent revision:
             - If directory: recursively merge all .swift files
             - If .zip: extract and merge all .swift files
        iv.  Tag each file with sanitized "// File: filename.swift" comment
        v.   Concatenate all code into single string
     c. Store in Map<String, String>: studentKey → swiftCode

Output: Map of student keys to their Swift code
  Example: {"Smith, John - s123456" → "// File: ContentView.swift\nimport SwiftUI...", ...}

Performance: ~5-10 seconds for 40 students with typical submissions
```

**Key Detail:** This reads **ALL** Swift file contents for **ALL** students upfront. We need the folder structure to check revision numbers against the cache, so reading everything upfront is acceptable.

---

### 5. Schoology Student Mapping (Optional)

**File:** `GradeProcessor.java` Lines 168-175
**Triggered:** If `ENABLE_SCHOOLOGY_COMMENTS` or `ENABLE_SCHOOLOGY_GRADES` is true

```
Flow:
  1. SchoologyCommentUpdater.fetchStudentMappings()
  2. HTTP GET to: {baseUrl}/iapi/enrollment/member_enrollments/course/{courseId}
  3. Parse JSON response to build mapping:
     school_uid → uid
     Example: "s513390" → "314159265"
  4. Cache mapping in memory (studentUidCache)

Output: Map<String, String> for translating school UIDs to Schoology UIDs
Purpose: Convert student folder names (s513390) to Schoology's internal IDs

API Call Count: 1 (fast, <1 second)
Performance Impact: Negligible
Note: Student UIDs don't change all year, but API call is cheap so no optimization needed
```

---

### 6. Main Grading Loop (`GradeProcessor.gradeAll()`)

**File:** `GradeProcessor.java` Lines 181-268

This is the heart of the system. For each student submission:

#### Step 6.1: Extract Student Identifiers

```
Line 182: String studentKey = entry.getKey()
  Example: "Krawec, Claire - s498776"

Line 183: String schoolUid = extractUniqueUserId(studentKey)
  Example: "s498776"

Line 184: Path studentDir = submissionsPath.resolve(studentKey)
  Example: "submissions/Krawec, Claire - s498776"
```

#### Step 6.2: Cache Check (CRITICAL OPTIMIZATION)

```
Lines 187-204:

1. Call fileReader.findHighestRevision(studentDir)
   → Lists directories in student folder
   → Finds "Revision 3 - On time", "Revision 2 - Late", etc.
   → Returns highest number (e.g., 3)
   → If no revision folders: returns 1 (direct submission)

2. Call gradingCache.getLastGradedRevision(schoolUid, assignmentId)
   → Looks up cache: what revision did we last grade?
   → Returns revision number or 0 if never graded

3. DECISION POINT:
   if (highestRevision <= lastGradedRevision) {
       LOG: "⊘ Skipped {student} - Revision {N} already graded"
       skippedCount++
       continue  // ← SKIP TO NEXT STUDENT (NO AI CALL)
   }

4. If revision is NEW:
   LOG: "→ New submission detected - Revision {N} (previously: {M})"
   → Continue to grading
```

**Performance Impact:** This is the PRIMARY optimization that prevents unnecessary OpenAI API calls. On re-runs with no new submissions, all 40 students are skipped here.

#### Step 6.3: Empty Code Check

```
Lines 206-212:

String swiftCode = entry.getValue()

if (swiftCode.isBlank()) {
    LOG: "Skipping {student} because no Swift code was extracted"
    records.add(GradeRecord.failed(...))
    continue  // ← SKIP (NO AI CALL)
}
```

#### Step 6.4: Build AI Prompt

```
Line 213: String prompt = assignmentPrompt.buildPrompt(swiftCode)

Flow:
  1. AssignmentPrompt loads active rubric (e.g., ConstantsVariablesDatatypesPrompt.PROMPT)
  2. Concatenates:
     - Grading instructions (MVP criteria, stretch goals, scoring rules)
     - Student's Swift code
  3. Returns complete prompt string

Output: Full prompt ready for AI model
Size: ~2-5KB (depends on rubric + student code length)
```

#### Step 6.5: AI Grading (SLOWEST STEP)

```
Lines 217-223:

GradingResult result;
if (useLocalModel) {
    result = lmStudioGrader.gradeSubmission(studentKey, prompt);
} else {
    result = openAIGrader.gradeSubmission(studentKey, prompt);
}
```

##### OpenAI Path (`OpenAIGrader.gradeSubmission()`)

```
Flow:
  1. Build HTTP POST request:
     URL: https://api.openai.com/v1/chat/completions
     Headers:
       - Authorization: Bearer {OPENAI_API_KEY}
       - Content-Type: application/json
     Body:
       {
         "model": "gpt-5-mini",
         "messages": [
           {"role": "system", "content": "You are a grading assistant..."},
           {"role": "user", "content": "{prompt with code}"}
         ],
         "response_format": {"type": "json_object"}
       }

  2. Send request (timeout: 10 minutes)

  3. Receive JSON response:
     {
       "score": 10.0,
       "mvpComplete": true,
       "stretchGoalsCompleted": ["stretch1", "stretch2"],
       "feedback": {
         "studentSummary": "Great work! All requirements met...",
         "strengths": ["Good naming", "Clean code"],
         "improvements": ["Add comments"],
         "syntaxErrors": []
       },
       "compileIssues": "none"
     }

  4. Parse JSON into GradingResult object

Performance: 20-60 seconds per student
Cost: ~$0.002 per student (~$0.074 for 40 students)
```

##### LM Studio Path (`LMStudioGrader.gradeSubmission()`)

```
Flow:
  1. Build HTTP POST request:
     URL: http://localhost:1234/v1/chat/completions
     (OpenAI-compatible local API)

  2. Similar structure to OpenAI, but:
     - No API key required
     - No response_format parameter (not supported)
     - Relies on prompt instructions for JSON output

  3. Parse response (same JSON structure)

Performance: 30-60 seconds per student (slower than OpenAI)
Cost: $0 (runs locally)
```

#### Step 6.6: Process Results

```
Lines 224-226:

records.add(GradeRecord.from(studentKey, result))
schoologyRecords.add(SchoologyRecord.from(studentKey, result))
logResult(studentKey, result)

Output: Results stored in memory for CSV export later
Log Example: "Krawec, Claire - s498776 -> Score: 10.0 / 10.0 | MVP: true | Stretch Goals: [stretch1, stretch2] | Compile: none"
```

#### Step 6.7: Update Cache

```
Lines 228-237:

gradingCache.updateGrade(
    schoolUid,          // "s498776"
    studentKey,         // "Krawec, Claire - s498776"
    assignmentId,       // "8017693698"
    assignmentName,     // "Constants Challenge"
    currentRevision,    // 3
    result.score()      // 10.0
)

Output: In-memory cache updated with new grading record
Log: "  → Cache updated: s498776 revision 3"

Purpose: Next run will skip this submission (until Revision 4 appears)
```

#### Step 6.8: Post to Schoology (Optional)

##### Grade Posting (Lines 240-247)

```
If ENABLE_SCHOOLOGY_GRADES is true:

Flow:
  1. schoologyCommentUpdater.postGrade(schoolUid, result.score())
  2. HTTP PUT to: {baseUrl}/iapi/grades/grader_grade_data/{courseId}/{gradingPeriodId}
  3. JSON payload:
     {
       "grades": {
         "{studentUid}": {
           "{assignmentId}": {
             "grade": "10.0",
             "exception": 0,
             "comment": null,
             "updateSequence": 0
           }
         }
       },
       "sequence": 1
     }
  4. Receive response: {"response_code": 200}

Performance: ~0.5 seconds per student
Log: "✓ Successfully posted grade for s498776 - Schoology confirmed"
```

##### Comment Posting (Lines 249-263)

```
If ENABLE_SCHOOLOGY_COMMENTS is true:

Flow:
  1. Extract feedbackComment = result.feedback().studentSummary()
     Example: "Great work! All requirements met. Consider adding more comments."

  2. schoologyCommentUpdater.postComment(schoolUid, feedbackComment)

  3. Two-step process:
     a. HTTP GET to: {baseUrl}/assignment/{assignmentId}/dropbox/view/{studentUid}
        → Fetches HTML page
        → Extracts CSRF tokens: form_token, form_build_id, sid

     b. HTTP POST to same URL with:
        → Comment text
        → Extracted tokens
        → drupal_ajax=1 flag (returns JSON instead of HTML)

  4. Receive JSON response: {"status": true}

Performance: ~1-2 seconds per student
Log: "✓ Successfully posted comment for s498776 - Schoology confirmed status:true"
```

**Error Handling:** If Schoology posting fails, the grading continues and CSV files still work. Errors are logged but not fatal.

---

### 7. Cache Persistence

**File:** `GradeProcessor.java` Line 272

```
Flow:
  1. gradingCache.save()
  2. Update cache metadata:
     - last_updated: current timestamp
  3. Write JSON to: results/grading-cache.json
  4. Pretty-print format (indented)

Output: Persistent cache file on disk
Purpose: Next run will load this cache and skip already-graded submissions
```

---

### 8. Summary Logging

**File:** `GradeProcessor.java` Lines 273-279

```
Log Output:
═══════════════════════════════════════════
Grading Summary:
  Total students: 40
  Newly graded: 5
  Skipped (already graded): 35
  Cache: 40 students, 160 graded assignments
═══════════════════════════════════════════
```

---

### 9. CSV Export

#### Detailed Grades CSV (`writeCsv()`)

**File:** `GradeProcessor.java` Lines 299-310

```
Output File: results/grades-20251012-143022.csv

Headers:
  FolderName, Score, MVP, Stretch1, Stretch2, Stretch3, CompileIssues, Feedback

Row Example:
  "Krawec, Claire - s498776", 10.0/10.0, true, true, true, false, none, "Great work! All requirements met. Consider adding more comments."

Purpose: Full grading details with feedback for instructor review
```

#### Schoology Import CSV (`writeSchoologyCsv()`)

**File:** `GradeProcessor.java` Lines 436-447

```
Output File: results/schoology-grades-20251012-143022.csv

Headers:
  Unique User ID, {Assignment Name}

Row Example:
  s498776, 10.0

Purpose: Direct import into Schoology gradebook (if not using API posting)
Format: Minimal two-column format required by Schoology LMS
```

---

## Performance Characteristics

### Timing Breakdown (40 students, first run)

| Step | Time | Notes |
|------|------|-------|
| Configuration loading | <0.1s | One-time at startup |
| Cache loading | <0.1s | Read single JSON file |
| Read all Swift files | 5-10s | Depends on file sizes |
| Schoology mapping fetch | <1s | Single API call |
| **AI grading (40 students)** | **13-40 min** | **BOTTLENECK** |
| ↳ OpenAI (GPT-5 mini) | 20-60s/student | ~13-40 min total |
| ↳ LM Studio (Qwen 3 4B) | 30-60s/student | ~20-40 min total |
| Cache updates | <0.1s | In-memory operations |
| Schoology grade posting (40) | ~20s | 0.5s × 40 students |
| Schoology comment posting (40) | ~40-80s | 1-2s × 40 students |
| Cache save | <0.1s | Write single JSON file |
| CSV export | <0.1s | Write two small files |

**Total (OpenAI, no Schoology):** ~15-40 minutes
**Total (OpenAI, with Schoology):** ~16-42 minutes
**Total (LM Studio):** ~20-40 minutes

### Timing Breakdown (40 students, all cached)

| Step | Time | Notes |
|------|------|-------|
| Configuration loading | <0.1s | |
| Cache loading | <0.1s | |
| Read all Swift files | 5-10s | Still reads to check revisions |
| Cache checks (40 students) | <0.1s | All skipped |
| No AI calls | **0s** | **All students skipped** |
| CSV export (empty) | <0.1s | No new grades to export |

**Total:** ~5-10 seconds

### Memory Usage

| Component | Memory |
|-----------|--------|
| Swift code (40 students) | ~2-5 MB |
| Grading cache (full year) | ~150-240 KB |
| AI responses | ~1-2 MB |
| CSV buffers | <1 MB |
| **Total peak usage** | **~5-10 MB** |

Very lightweight - no memory concerns.

---

## Critical Decision Points

### 1. Skip Student? (Cache Check)

**Location:** `GradeProcessor.java` Lines 192-196

```
Decision Logic:
  IF (current revision <= last graded revision):
    → SKIP (no AI call, no Schoology posting)
  ELSE:
    → CONTINUE (grade this student)

Impact: This is the PRIMARY performance optimization
  - First run: 0 students skipped
  - Second run (no changes): 40 students skipped
  - Typical run: 35-38 students skipped
```

### 2. Skip Student? (Empty Code)

**Location:** `GradeProcessor.java` Lines 207-211

```
Decision Logic:
  IF (swiftCode.isBlank()):
    → SKIP (no AI call)
    → Record failure: "No Swift files located"
  ELSE:
    → CONTINUE

Impact: Rare (only if student submits empty folder)
```

### 3. Which AI Model?

**Location:** `GradeProcessor.java` Lines 219-222

```
Decision Logic:
  IF (USE_LOCAL_MODEL == true):
    → Use LM Studio (free, slower, private)
  ELSE:
    → Use OpenAI API (paid, faster, cloud)

Impact:
  - OpenAI: ~20-60s per student, $0.002/student
  - LM Studio: ~30-60s per student, $0.00/student
```

### 4. Post to Schoology?

**Location:** `GradeProcessor.java` Lines 240, 250

```
Decision Logic:
  IF (ENABLE_SCHOOLOGY_GRADES == true):
    → Post grade via API

  IF (ENABLE_SCHOOLOGY_COMMENTS == true):
    → Post comment via API

Impact: Adds ~1.5-2.5 seconds per student when enabled
```

---

## Data Transformations

### Student Folder Name → School UID

```
Input:  "Krawec, Claire - s498776"
Method: extractUniqueUserId() - takes substring after last dash
Output: "s498776"
```

### School UID → Schoology UID

```
Input:  "s498776"
Method: SchoologyCommentUpdater.studentUidCache lookup
Output: "314159265" (Schoology's internal ID)
```

### Swift Files → Merged Code String

```
Input:  Multiple .swift files in revision folder
        - ContentView.swift
        - MyModel.swift
        - Helper.swift

Process:
  1. Read each file
  2. Add "// File: {filename}" header
  3. Concatenate all files

Output: Single string:
  "// File: ContentView.swift\nimport SwiftUI\n...\n\n// File: MyModel.swift\nstruct MyModel {\n..."
```

### AI JSON Response → GradingResult Object

```
Input: JSON string from OpenAI/LM Studio
  {
    "score": 10.0,
    "mvpComplete": true,
    "stretchGoalsCompleted": ["stretch1", "stretch2"],
    "feedback": {...},
    "compileIssues": "none"
  }

Process: Parse JSON, validate fields

Output: GradingResult record object (immutable)
  - score: 10.0
  - maxScore: 10.0
  - mvpComplete: true
  - stretchGoalsCompleted: List.of("stretch1", "stretch2")
  - feedback: Feedback record
  - compileIssues: "none"
```

### GradingResult → CSV Row

```
Input: GradingResult object

Process: Format fields as CSV-safe strings
  - Sanitize: replace quotes with single quotes
  - Sanitize: replace newlines with spaces
  - Format score: "10.0/10.0"
  - Extract stretch goal booleans

Output: CSV row string:
  "Krawec, Claire - s498776,10.0/10.0,true,true,true,false,none,\"Great work! All requirements met.\""
```

---

## Error Handling Strategy

### Non-Fatal Errors (Continue Processing)

```
1. Schoology grade posting fails
   → Log warning
   → Continue to next student
   → CSV export still works

2. Schoology comment posting fails
   → Log warning
   → Continue to next student
   → CSV export still works

3. Revision check fails for one student
   → Log warning
   → Assume revision 1
   → Attempt to grade anyway

4. Cache save fails
   → Log error
   → Next run will re-grade everything (safe fallback)
```

### Fatal Errors (Stop Program)

```
1. Submissions directory doesn't exist
   → IOException thrown
   → Program exits

2. OpenAI API key missing (when OpenAI selected)
   → IllegalStateException thrown
   → Program exits with error message

3. AI API call fails with non-transient error
   → Exception logged
   → Student marked as "failed" in CSV
   → Continue to next student
```

---

## File System Interactions

### Reads

| File/Directory | When | Purpose |
|----------------|------|---------|
| `.env` | Startup | Load configuration |
| `.schoology-cookie` | Startup (if enabled) | Read session cookie |
| `results/grading-cache.json` | Before grading loop | Load cached grades |
| `submissions/{student}/` | For each student | List revision folders |
| `submissions/{student}/**/*.swift` | For students needing grading | Read Swift code |

### Writes

| File | When | Purpose |
|------|------|---------|
| `results/grading-cache.json` | End of run | Persist cache updates |
| `results/grades-{timestamp}.csv` | End of run | Export detailed grades |
| `results/schoology-grades-{timestamp}.csv` | End of run | Export Schoology import file |
| `dropbox-page-debug.html` | If token extraction fails | Debug Schoology issues |

---

## Network API Calls

### Schoology APIs (Optional)

| API | Count | When | Purpose |
|-----|-------|------|---------|
| GET `/iapi/enrollment/member_enrollments/course/{courseId}` | 1 | Start of run | Fetch student UID mappings |
| PUT `/iapi/grades/grader_grade_data/{courseId}/{gradingPeriodId}` | N | Per newly-graded student | Post grade to gradebook |
| GET `/assignment/{assignmentId}/dropbox/view/{studentUid}` | N | Per newly-graded student (if comments enabled) | Extract CSRF tokens |
| POST `/assignment/{assignmentId}/dropbox/view/{studentUid}` | N | Per newly-graded student (if comments enabled) | Post feedback comment |

### AI APIs (Required)

| API | Count | When | Purpose |
|-----|-------|------|---------|
| **OpenAI:** POST `https://api.openai.com/v1/chat/completions` | N | Per newly-graded student | Get AI grading |
| **LM Studio:** POST `http://localhost:1234/v1/chat/completions` | N | Per newly-graded student | Get AI grading (local) |

**N = Number of students needing grading** (not cached)
- First run: N = 40
- Typical run: N = 0-5
- Worst case: N = 40 (if cache deleted)

---

## Summary: Where Time is Spent

**First Run (40 students):**
```
5% - File reading, cache loading, initialization
93% - AI API calls (20-60 seconds × 40 students)
2% - Schoology posting (if enabled)
<1% - Everything else
```

**Re-run (35 cached, 5 new):**
```
10% - File reading (still checks all folders for revisions)
88% - AI API calls (20-60 seconds × 5 students)
2% - Schoology posting (if enabled)
<1% - Everything else
```

**Re-run (all cached):**
```
100% - File reading (~5-10 seconds)
0% - AI API calls (all skipped)
0% - Schoology posting (no new grades)
```

**The AI API call is the bottleneck, but it's unavoidable.** The caching system ensures we only call the AI when truly necessary, which is the key optimization.
