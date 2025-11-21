# Swift Assignment Auto-Grader

An AI-powered Java application that automates grading for high school Swift programming assignments, processing ~40 student submissions in 15-40 minutes with consistent, structured feedback and seamless Schoology LMS integration.

---

## Overview

Grading programming assignments shouldn't require manually reading through 40+ student projects, copying feedback into Schoology, and maintaining grade spreadsheets. This application transforms the grading workflow into a fully automated process using AI-powered code evaluation.

**What it does:**
- **Grades** Swift programming assignments using OpenAI GPT-5 mini or local LM Studio models
- **Downloads** student submissions automatically from Schoology LMS
- **Evaluates** code against structured rubrics (MVP requirements + stretch goals)
- **Posts** grades and feedback directly to Schoology gradebook and student dropboxes
- **Caches** grading results to avoid re-processing unchanged submissions
- **Generates** student-friendly feedback summaries with actionable next steps

**Why I built this:**

As a high school computer programming instructor, I needed a way to provide consistent, detailed feedback on Swift assignments for ~40 students per class without spending hours manually grading. This project demonstrates practical application of AI for education technology while showcasing end-to-end software engineering: API integration (OpenAI, Schoology), data processing, caching strategies, and production deployment patterns.

**Key Achievement:** Reduced grading time from 4-6 hours to 15-40 minutes per assignment while improving feedback consistency and detail.

---

## Features & Technical Highlights

### 1. Dual AI Model Support

Choose between cloud-based AI (fast, high-quality) or local models (free, private):

**OpenAI GPT-5 mini (Recommended):**
- High-quality reasoning for consistent grading
- 20-60 seconds per student
- ~$0.074 per 40-student batch
- Extended reasoning tokens for complex rubric evaluation

**LM Studio (Local Models):**
- Zero API costs
- Complete privacy (code never leaves your computer)
- 30-60 seconds per student
- Recommended model: Qwen 3 4B Instruct (Q4_K_M GGUF)

**Technical highlights:**
- Abstracted grader interface allows seamless model switching
- Structured JSON response parsing with Pydantic-style validation
- 10-minute timeout for reasoning models
- Graceful fallback on API failures

---

### 2. Batch Assignment Processing

Grade multiple assignments in a single run with zero manual intervention:

**Configuration-driven workflow:**
```bash
# Configure once in .env
ASSIGNMENT_1_ID=8048841803
ASSIGNMENT_1_NAME=Functions
ASSIGNMENT_1_PROMPT=FunctionsChallengePrompt

ASSIGNMENT_2_ID=8048841871
ASSIGNMENT_2_NAME=Conditionals
ASSIGNMENT_2_PROMPT=ConditionalsChallengePrompt

# Run once
java -jar swift-grader.jar

# Done! All assignments graded and posted to Schoology
```

**Technical highlights:**
- Dynamic prompt loading using Java Reflection (no hardcoded imports)
- Sequential assignment processing with error isolation
- Dedicated results folder per assignment
- Comprehensive batch summary reports
- Continue-on-error strategy (failed assignments don't block others)

---

### 3. Smart Two-Level Caching System

Minimize API costs and processing time with intelligent caching:

**Submission Cache:**
- Tracks Schoology download timestamps using HTTP `Last-Modified` headers
- Only downloads when submissions change
- Cache hit: instant (reuses existing files)
- Cache miss: downloads and extracts new submissions

**Grading Cache:**
- Revision-based tracking per student per assignment
- Skips already-graded work automatically
- Example: Revision 3 triggers grading only if Revision 2 was previously graded
- Persistent JSON cache (`results/grading-cache.json`)

**Cost Protection:**
- First run: 40 AI calls (~$0.074)
- Re-run with 5 new revisions: 5 AI calls (~$0.009)
- Re-run with no changes: 0 AI calls ($0.00)

**Technical highlights:**
- Cache keys: `{school_uid}_{assignmentId}_{revision}`
- Thread-safe atomic writes
- Corruption-resistant fallback (starts fresh if corrupted)
- Manual invalidation support

---

### 4. Seamless LMS Integration

Automatically post grades and feedback directly to the Learning Management System:

**Grade Posting:**
- Numeric scores appear instantly in gradebook
- Single API request per student (~0.5 seconds)
- Direct integration with LMS grading system

**Comment Posting:**
- Feedback summaries appear in student assignment submissions
- Automated authentication and session management
- ~1-2 seconds per student

**Technical highlights:**
- Session-based authentication with automatic token refresh
- CSRF token management for secure API requests
- Student identifier mapping via enrollment data
- Graceful degradation (CSV export always works if API fails)
- Robust error handling and retry logic

---

### 5. Structured Grading with Consistent Rubrics

AI evaluates assignments using detailed, explicit rubrics:

**Grading Scale:**
- **MVP Requirements:** 8 points (4 criteria × 2 points each)
- **Stretch Goals:** 2 points (0.5 points each for 4 goals)
- **Maximum Score:** 10/10

**Rubric Design:**
- Explicit scoring formulas in prompts
- Examples for each requirement
- Ignore commented-out code
- Consistency rules (e.g., "if marked complete, don't list as missing")
- JSON response format enforcement

**Example Prompt Structure:**
```
CRITICAL GRADING RULES:
1. ONLY count stretch goals with working implementations
2. IGNORE commented-out code
3. Be CONSISTENT in marking

MVP (8 points total) - ALL FOUR required:
1. @State variable showMVP: Bool (2 points)
2. String constant firstName with name (2 points)
3. Code compiles without errors (2 points)
4. Follows Swift naming conventions (2 points)

STRETCH GOALS (0.5 points each, max 2 points):
Stretch #1: Creates lastName, titleName, greeting
  - REQUIRED: All three constants present
  - If ANY missing, DO NOT mark complete
...

RESPOND IN JSON FORMAT:
{
  "score": X.X,
  "mvpComplete": true/false,
  "stretchGoalsCompleted": ["stretch1", "stretch2"],
  "feedback": {
    "studentSummary": "2-3 sentence summary...",
    "strengths": [...],
    "improvements": [...],
    "syntaxErrors": [...]
  }
}
```

**Technical highlights:**
- Dynamic prompt loading by class name
- Student-friendly summaries auto-formatted from AI responses
- Detailed CSV export with full feedback
- Compile issue detection (none/minor/major)

---

## Tech Stack

### Backend / Core Application
- **Java 17** - Modern Java with records, pattern matching, and text blocks
- **Maven 3.9+** - Dependency management and build automation
- **OkHttp 4.12** - HTTP client for API calls (OpenAI, Schoology)
- **Jackson 2.15** - JSON parsing and serialization
- **dotenv-java 3.0** - Environment variable management from `.env` files

### AI Integration
- **OpenAI API** - GPT-5 mini model with extended reasoning
- **LM Studio** - Local model hosting (OpenAI-compatible API)
- **JSON Schema Validation** - Structured response parsing

### Data Processing
- **SwiftFileReader** - Recursive file extraction from nested folders/ZIPs
- **GradingCache** - Revision tracking with JSON persistence
- **SubmissionCache** - HTTP-based download optimization

### External Integrations
- **LMS Integration** - Custom API client for:
  - Enrollment data (student identifier mappings)
  - Automated grade posting to gradebook
  - Feedback comment posting to student submissions

---

## Architecture

### System Design

```
┌────────────────────┐
│   BatchGrader      │  Main orchestrator (batch mode)
│   (Configuration)  │
└─────────┬──────────┘
          │
          │ For each assignment:
          │
          ▼
┌────────────────────────────────────────────────────┐
│           GradeProcessor (Per-Assignment)          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │ SwiftFile    │  │ Assignment   │  │ Grading  │ │
│  │ Reader       │  │ Prompt       │  │ Cache    │ │
│  └──────────────┘  └──────────────┘  └──────────┘ │
└─────────┬──────────────────────────────────────────┘
          │
          │ For each student:
          │
          ▼
┌─────────────────────────────────────────┐
│    Grader (OpenAI or LM Studio)         │
│  ┌─────────────┐    ┌────────────────┐ │
│  │ HTTP Client │───►│ AI API         │ │
│  │ (OkHttp)    │◄───│ (JSON Response)│ │
│  └─────────────┘    └────────────────┘ │
└─────────┬───────────────────────────────┘
          │
          │ Results
          │
          ▼
┌─────────────────────────────────────────┐
│    Output Layer                         │
│  ┌──────────┐  ┌──────────────────────┐ │
│  │ CSV      │  │ Schoology API        │ │
│  │ Export   │  │ (Grades + Comments)  │ │
│  └──────────┘  └──────────────────────┘ │
└─────────────────────────────────────────┘
```

### Data Flow

**Batch Grading Workflow:**

1. **Load Configuration** - Parse `.env` for assignment IDs, names, and prompt class names
2. **Download Submissions** - Schoology API bulk download with cache check
3. **Extract & Organize** - ZIP extraction, folder renaming (`"LastName, FirstName - s123456"` → `"s123456"`)
4. **For Each Assignment:**
   - Load prompt dynamically using Java Reflection
   - Create `GradeProcessor` instance
   - Grade all students (with cache protection)
   - Post results to Schoology
   - Save CSV backups
5. **Generate Summary** - Report completed vs. skipped assignments, total time

**Per-Student Grading Flow:**

1. **Cache Check** - Compare revision number vs. last graded revision
   - If `currentRevision <= lastGradedRevision`: **Skip** (already graded)
   - Else: **Continue** to grading
2. **Read Swift Code** - Merge all `.swift` files with `// File:` markers
3. **Build Prompt** - Inject code into assignment-specific rubric
4. **AI Grading** - Send to OpenAI/LM Studio, receive JSON response
5. **Parse Results** - Extract score, feedback, stretch goals completed
6. **Update Cache** - Record revision number and grade
7. **Post to Schoology** - Grade (PUT) and comment (POST) if enabled
8. **Export to CSV** - Append to detailed and Schoology-import CSVs

---

## Project Structure

```
MobileMakersGrader/
├── src/main/java/com/mobilemakers/grader/
│   ├── BatchGrader.java                      # Main orchestrator for batch mode
│   ├── AssignmentConfig.java                 # .env configuration parser
│   ├── PromptLoader.java                     # Dynamic prompt loading via Reflection
│   ├── SchoologySubmissionDownloader.java    # Schoology submission downloads
│   ├── SubmissionCache.java                  # Download caching with Last-Modified
│   ├── GradeProcessor.java                   # Per-assignment grading loop
│   ├── GradingCache.java                     # Revision-based result caching
│   ├── SwiftFileReader.java                  # Swift code extraction from folders/ZIPs
│   ├── AssignmentPrompt.java                 # Prompt injection with dynamic text
│   ├── OpenAIGrader.java                     # OpenAI API client
│   ├── LMStudioGrader.java                   # LM Studio local model client
│   ├── SchoologyCommentUpdater.java          # Schoology grade/comment posting
│   ├── Config.java                           # Environment variable management
│   ├── Main.java                             # Application entry point
│   └── prompts/                              # Assignment-specific rubrics
│       ├── FunctionsChallengePrompt.java
│       ├── ConditionalsChallengePrompt.java
│       ├── MultiplyPrompt.java
│       ├── ConstantsVariablesDatatypesPrompt.java
│       ├── OperatorsChallengePrompt.java
│       ├── TextFieldsChallengePrompt.java
│       ├── ButtonsPrompt.java
│       ├── ClassesChallengePrompt.java
│       ├── EventsChallengePrompt.java
│       ├── PropertyWrappersChallengePrompt.java
│       └── FinalGradeCalculatorPrompt.java
│
├── submissions/                               # Student code (auto-downloaded)
│   ├── Functions/
│   │   ├── s486002/                          # School UID only (privacy)
│   │   │   └── *.swift files
│   │   └── s498783/
│   ├── Conditionals/
│   └── Multiply/
│
├── results/
│   ├── Functions-YYYYMMDD-HHMMSS/            # Dedicated folder per assignment
│   │   ├── grades-YYYYMMDD-HHMMSS.csv        # Detailed feedback export
│   │   └── schoology-grades-YYYYMMDD-HHMMSS.csv  # Schoology import format
│   ├── Conditionals-YYYYMMDD-HHMMSS/
│   ├── grading-cache.json                    # Revision tracking cache
│   └── submission-cache.json                 # Download timestamp cache
│
├── docs/
│   ├── data-flow.md                          # Detailed execution flow documentation
│   ├── caching-system.md                     # Cache architecture and behavior
│   ├── BATCH_GRADING_FEATURE.md              # Batch grading feature specification
│   ├── schoology-submission-api.md           # Schoology download API details
│   ├── schoology-dropbox-endpoint.md         # Schoology comment posting API
│   └── schoology-enrollment-endpoint.md      # Schoology enrollment API
│
├── .env                                       # Configuration (gitignored)
├── .schoology-cookie                          # Session authentication (gitignored)
├── pom.xml                                    # Maven dependencies
└── README.md                                  # This file
```

---

## Quick Start

### Prerequisites

- **Java 17+** (JDK with modern Java features)
- **Maven 3.9+** (for dependency management and building)
- **OpenAI API key** (if using OpenAI) **OR** **LM Studio** (if using local models)
- **Optional:** Schoology instructor account with session cookie access

### Setup

**1. Clone the repository:**
```bash
git clone https://github.com/yourusername/MobileMakersGrader.git
cd MobileMakersGrader
```

**2. Build the project:**
```bash
mvn clean package
```

**3. Configure environment variables:**

Create a `.env` file in the project root:

```bash
# ========================================
# AI Model Configuration
# ========================================

# Choose which AI model to use
USE_LOCAL_MODEL=false  # false = OpenAI, true = LM Studio

# OpenAI Configuration (only needed if USE_LOCAL_MODEL=false)
OPENAI_API_KEY=your-openai-api-key-here

# LM Studio Configuration (optional - uses defaults if not set)
LM_STUDIO_ENDPOINT=http://localhost:1234/v1/chat/completions
LM_STUDIO_MODEL=qwen3-4b-2507

# ========================================
# Batch Assignment Configuration
# ========================================

# Assignment 1: Functions Challenge
ASSIGNMENT_1_ID=8048841803
ASSIGNMENT_1_NAME=Functions
ASSIGNMENT_1_PROMPT=FunctionsChallengePrompt

# Assignment 2: Conditionals Challenge
ASSIGNMENT_2_ID=8048841871
ASSIGNMENT_2_NAME=Conditionals
ASSIGNMENT_2_PROMPT=ConditionalsChallengePrompt

# Assignment 3: Unit App Multiply
ASSIGNMENT_3_ID=8017693799
ASSIGNMENT_3_NAME=Multiply
ASSIGNMENT_3_PROMPT=MultiplyPrompt

# ========================================
# Schoology Integration (Optional)
# ========================================

ENABLE_SCHOOLOGY_GRADES=true
ENABLE_SCHOOLOGY_COMMENTS=true

SCHOOLOGY_BASE_URL=https://schoology.d214.org
SCHOOLOGY_COURSE_ID=7921261672
SCHOOLOGY_GRADING_PERIOD_ID=1126303
SCHOOLOGY_SECTION_ID=7921330259

# Extract these from browser DevTools (see docs/)
SCHOOLOGY_CSRF_KEY=your-csrf-key-here
SCHOOLOGY_CSRF_TOKEN=your-csrf-token-here
```

Create `.schoology-cookie` file (if using LMS integration):
```
[Session authentication cookie - contact for setup details]
```

**4. Run the grader:**

```bash
# Batch mode (grades all configured assignments)
java -jar target/swift-grader-0.1.0-SNAPSHOT.jar

# Or run from IntelliJ IDEA:
# Main class: com.mobilemakers.grader.Main
# Working directory: project root
```

**5. Review results:**

- **Console output:** Real-time grading progress and summary
- **CSV files:** `results/{AssignmentName}-YYYYMMDD-HHMMSS/grades-*.csv`
- **LMS:** Grades and comments automatically posted (if enabled)

---

## Configuration

### Environment Variables

All configuration is managed through the `.env` file (automatically loaded at startup):

**Required:**
- `USE_LOCAL_MODEL` - AI model selection (`true` = LM Studio, `false` = OpenAI)
- `OPENAI_API_KEY` - Your OpenAI API key (if using OpenAI)

**Batch Grading:**
- `ASSIGNMENT_N_ID` - Schoology assignment ID
- `ASSIGNMENT_N_NAME` - Assignment name (for folder naming)
- `ASSIGNMENT_N_PROMPT` - Java class name of prompt (in `prompts/` package)

**LMS Integration (Optional):**
- `ENABLE_SCHOOLOGY_GRADES` - Auto-post numeric grades (`true`/`false`)
- `ENABLE_SCHOOLOGY_COMMENTS` - Auto-post feedback comments (`true`/`false`)
- `SCHOOLOGY_BASE_URL` - Your LMS instance URL
- `SCHOOLOGY_COURSE_ID` - Course identifier
- `SCHOOLOGY_GRADING_PERIOD_ID` - Grading period for grade posting
- `SCHOOLOGY_SECTION_ID` - Course section identifier
- `SCHOOLOGY_CSRF_KEY` - CSRF token for authentication
- `SCHOOLOGY_CSRF_TOKEN` - CSRF token for authentication

**LM Studio (Optional):**
- `LM_STUDIO_ENDPOINT` - Local API URL (default: `http://localhost:1234/v1/chat/completions`)
- `LM_STUDIO_MODEL` - Model identifier (default: `qwen3-4b-2507`)

### Adding New Assignments

**1. Create a new prompt class** in `src/main/java/com/mobilemakers/grader/prompts/`:

```java
package com.mobilemakers.grader.prompts;

public class LoopsChallengePrompt {
    public static final String PROMPT = """
        CRITICAL GRADING RULES:
        1. ONLY count stretch goals with working implementations
        2. IGNORE commented-out code
        3. Be CONSISTENT in marking

        MVP (8 points total) - ALL FOUR required:
        1. for-in loop iterates 1 through 10 (2 points)
        2. Prints each number to console (2 points)
        3. Code compiles without errors (2 points)
        4. Follows Swift naming conventions (2 points)

        STRETCH GOALS (0.5 points each, max 2 points):
        ...

        RESPOND IN JSON FORMAT:
        {
          "score": X.X,
          "mvpComplete": true/false,
          ...
        }
        """;
}
```

**2. Add to `.env` configuration:**

```bash
ASSIGNMENT_4_ID=1234567890
ASSIGNMENT_4_NAME=Loops
ASSIGNMENT_4_PROMPT=LoopsChallengePrompt
```

**3. Run the grader** - the new assignment will be processed automatically!

---

## How It Works

### Grading Pipeline

**1. Submission Download (LMS Integration):**
- Bulk download API call for all student submissions
- Downloads ZIP file with all assignment submissions
- Extracts to `submissions/{AssignmentName}/`
- Renames folders for privacy (removes student names, keeps identifiers only)
- Flattens revisions: keeps most recent by timestamp

**2. Code Extraction:**
- Scans student folder for `.swift` files
- Handles nested folders and ZIP archives
- Merges all files with `// File: filename.swift` markers
- Outputs single code string per student

**3. Prompt Construction:**
- Loads assignment-specific prompt class dynamically
- Injects student code into rubric template
- Generates complete prompt string (~2-5KB)

**4. AI Grading:**
- Sends HTTP POST to OpenAI or LM Studio
- Uses `response_format: json_object` for structured output (OpenAI)
- 10-minute timeout for extended reasoning
- Parses JSON response into `GradingResult` object

**5. Result Processing:**
- Extracts score, MVP status, stretch goals completed
- Generates student-friendly 2-3 sentence summary
- Updates grading cache with revision number
- Posts to LMS (if enabled)
- Appends to CSV files

**6. Cache Management:**
- Saves grading cache to `results/grading-cache.json`
- Saves submission cache to `results/submission-cache.json`
- Next run loads caches and skips unchanged work

**Note:** LMS integration uses private API endpoints. Detailed implementation documentation available upon request for portfolio review.

---

## Performance

### Timing Breakdown

**First Run (40 students, all new):**

| Step | Time | Notes |
|------|------|-------|
| Configuration loading | <0.1s | One-time at startup |
| Download submissions | 5-30s | Depends on file size |
| Extract and organize | 1-3s | Per assignment |
| Read all Swift files | 5-10s | Depends on file sizes |
| **AI grading (40 students)** | **13-40 min** | **BOTTLENECK** |
| ↳ OpenAI (GPT-5 mini) | 20-60s/student | ~13-40 min total |
| ↳ LM Studio (Qwen 3 4B) | 30-60s/student | ~20-40 min total |
| Cache updates | <0.1s | In-memory operations |
| LMS posting (40) | ~60-100s | ~1.5-2.5s per student |
| Cache persistence | <0.1s | Write JSON files |
| CSV export | <0.1s | Write two small files |

**Total (OpenAI with LMS integration):** ~15-42 minutes
**Total (LM Studio):** ~20-40 minutes

**Re-run (all cached, no new submissions):**

| Step | Time | Notes |
|------|------|-------|
| Configuration loading | <0.1s | |
| Cache loading | <0.1s | Read JSON files |
| Download check (HTTP HEAD) | <1s | Cache hit |
| Read Swift files | 5-10s | Still checks for new revisions |
| Cache checks (40 students) | <0.1s | All skipped |
| **AI grading** | **0s** | **All students skipped** |
| CSV export (empty) | <0.1s | No new grades |

**Total:** ~5-10 seconds

### Cost Analysis

**OpenAI GPT-5 mini pricing:**
- ~$0.002 per student (includes reasoning tokens)
- ~$0.074 per 40-student batch (first run)
- ~$0.009 per 5 new revisions
- ~$0.00 for cached re-runs

**LM Studio:**
- $0.00 (runs locally)
- Requires ~8GB RAM for model

---

## Documentation

Comprehensive technical documentation is available in the `docs/` directory:

- **[Data Flow](docs/data-flow.md)** - Step-by-step execution flow with performance characteristics
- **[Caching System](docs/caching-system.md)** - Revision-based cache architecture and behavior
- **[Batch Grading Feature](docs/BATCH_GRADING_FEATURE.md)** - Multi-assignment processing specification

**Note:** Detailed LMS integration documentation available upon request for portfolio review.

---

## Future Enhancements

### Planned Features
- [ ] Web interface for configuring assignments and viewing results
- [ ] Support for additional programming languages (Kotlin, JavaScript, Python)
- [ ] Plagiarism detection between student submissions
- [ ] Historical grade analytics and trend reporting
- [ ] Automatic prompt selection based on assignment folder name

### Technical Improvements
- [ ] PostgreSQL migration for better scalability
- [ ] Parallel assignment processing (multi-threaded grading)
- [ ] Improved error handling for malformed AI responses
- [ ] Batch retry logic for API failures
- [ ] API authentication and rate limiting for potential web deployment
- [ ] CI/CD pipeline with GitHub Actions

---

## Key Concepts

### Privacy-First Design

**No student identity data sent to AI:**
- Only Swift code content is transmitted
- Student names and IDs stripped from folder names before processing
- For complete privacy: use LM Studio (code never leaves your computer)

### Revision-Based Caching

**How it works:**
- Each student submission is in a folder: `"Student Name - s123456"`
- Schoology creates revision folders: `"Revision 1 - On time"`, `"Revision 2 - Late"`
- Grader tracks highest revision number per student per assignment
- Cache key: `{school_uid}_{assignmentId}_{revision}`
- Only grades new revisions (Revision 3 after Revision 2 was processed)

### Grading Scale Consistency

**Scoring formula:**
- MVP = 8 points (4 requirements × 2 points each)
- Stretch goals = 2 points total:
  - If 4 stretch goals: 0.5 points each
  - If 3 stretch goals: 0.67 points each
  - If 2 stretch goals: 1.0 point each
- Maximum: 10.0 points

**Enforced through prompts:**
- Explicit scoring formulas
- Examples for each requirement
- Consistency rules to prevent contradictions

---

## License

This project was built for single-instructor use at a high school programming course. It is **not designed for scale** or general distribution. Feel free to use as a reference for your own projects.

---

## Contact

**Kyle Pfister**

- **GitHub:** [@kpfister44](https://github.com/kpfister44)
- **LinkedIn:** https://www.linkedin.com/in/kyle-pfister-510753286/
- **Email:** kpfister44@gmail.com

**Have questions or want to discuss AI in education?** Feel free to reach out!

---

**Built with Java, OpenAI GPT-5 mini, and a passion for making programming education more effective.**
