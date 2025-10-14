# Swift Assignment Auto-Grader

AI-powered Java tool that automatically grades Swift programming assignments for high school intro classes. Processes ~40 student submissions in 15-40 minutes using OpenAI GPT-5 mini or local LM Studio models, with optional direct Schoology integration.

## Key Features

- **Dual AI Support**: Choose between OpenAI API (fast, paid) or local LM Studio models (free, private)
- **Batch Processing**: Automatically grades ~40 students with consistent scoring
- **Structured Grading**: MVP (7-8 points) + Stretch Goals (0.5 points each) = 10 max
- **Privacy-First**: No student identity data sent to AI (optional local-only processing)
- **Schoology Integration**: Optional automated grade/comment posting directly to LMS
- **Smart Caching**: Revision-based system avoids re-grading already-processed submissions
- **Flexible Submission Formats**: Supports nested folders, revision bundles, and `.zip` files
- **Student-Friendly Feedback**: Auto-generates 2-3 sentence summaries with actionable next steps

## Prerequisites

- **Java 17+**
- **Maven 3.9+**
- **OpenAI API key** (if using OpenAI) OR **LM Studio** (if using local models)

## Quick Start

### 1. Clone and Build
```bash
git clone <your-repo-url>
cd MobileMakersGrader
mvn clean package
```

### 2. Configure Environment
Create a `.env` file in the project root:

```bash
# Choose AI model
USE_LOCAL_MODEL=false  # false = OpenAI, true = LM Studio

# OpenAI Configuration (only needed if USE_LOCAL_MODEL=false)
OPENAI_API_KEY=your-openai-api-key-here

# LM Studio Configuration (optional - uses defaults if not set)
LM_STUDIO_ENDPOINT=http://localhost:1234/v1/chat/completions
LM_STUDIO_MODEL=qwen3-4b-2507
```

**Optional**: Enable Schoology integration (see `docs/` for setup details):
```bash
ENABLE_SCHOOLOGY_GRADES=true
ENABLE_SCHOOLOGY_COMMENTS=true
```

### 3. Add Student Submissions
Place student folders in `submissions/`:
```
submissions/
├── Student Name - id/
│   └── *.swift files (or revision folders/zips)
└── ...
```

### 4. Run the Grader
```bash
# Using Maven
java -jar target/swift-grader-0.1.0-SNAPSHOT.jar submissions results

# Or from IntelliJ
# Main class: com.mobilemakers.grader.Main
# Program arguments: submissions results
```

### 5. Review Results
- **CSV files**: `results/grades-YYYYMMDD-HHMMSS.csv` (detailed feedback)
- **Schoology CSV**: `results/schoology-grades-YYYYMMDD-HHMMSS.csv` (for manual import)
- **Console output**: Real-time grading progress

## Grading Scale

- **MVP Complete**: 7-8 points (varies by assignment)
- **Stretch Goals**: 0.5 points each
- **Maximum Score**: 10/10

Scoring is consistent across runs using structured prompts with explicit grading rules.

## Available Assignment Prompts

Switch between assignments by updating the import in `AssignmentPrompt.java`:

- **ConstantsVariablesDatatypesPrompt** - Variables, constants, data types
- **ButtonsPrompt** - Button interactions and state management
- **TextFieldsChallengePrompt** - TextField input and validation
- **OperatorsChallengePrompt** - Arithmetic and comparison operators
- **MultiplyPrompt** - Multiplication app with operators

See `docs/ADDING_ASSIGNMENT_PROMPTS.md` for creating new prompts.

## AI Model Options

### OpenAI API (GPT-5 mini) - RECOMMENDED
- **Cost**: ~$0.074 per 40-student batch
- **Speed**: ~20-60 seconds per submission
- **Quality**: Excellent consistency with reasoning capabilities

### LM Studio (Qwen 3 4B Local)
- **Cost**: $0 (runs locally)
- **Speed**: ~30-60 seconds per submission
- **Quality**: Good but less consistent than OpenAI
- **Privacy**: Code never leaves your computer

Download LM Studio from [lmstudio.ai](https://lmstudio.ai/) and load the **Qwen3 4B Instruct 2507** model (Q4_K_M GGUF format).

## Schoology Integration (Optional)

Automatically post grades and feedback directly to Schoology, eliminating manual CSV uploads:

- **Grade Posting**: Numeric scores appear in gradebook (~0.5 sec per student)
- **Comment Posting**: Feedback summaries appear in dropbox (~1-2 sec per student)

See setup instructions in `docs/` folder (requires session cookie and CSRF tokens from browser DevTools).

## Project Structure

```
MobileMakersGrader/
├── src/main/java/com/mobilemakers/grader/
│   ├── Main.java                    # Entry point
│   ├── GradeProcessor.java          # Main processing loop
│   ├── SwiftFileReader.java         # Extracts code from submissions
│   ├── OpenAIGrader.java            # OpenAI API client
│   ├── LMStudioGrader.java          # LM Studio API client
│   ├── GradingCache.java            # Revision-based caching
│   ├── SchoologyCommentUpdater.java # Schoology integration
│   └── prompts/                     # Assignment rubrics
├── submissions/                      # Student code (ignored by git)
├── results/                          # Grading output (ignored by git)
├── docs/                            # Detailed documentation
└── .env                             # Configuration (ignored by git)
```

## How It Works

1. **SwiftFileReader** extracts `.swift` files from most recent revision per student
2. **AssignmentPrompt** injects code into structured rubric (MVP + stretch goals)
3. **OpenAIGrader/LMStudioGrader** sends prompt to AI, receives JSON response
4. **GradeProcessor** formats feedback, writes CSVs, optionally posts to Schoology
5. **GradingCache** tracks processed revisions to avoid re-grading

**Privacy**: Only code content is sent to AI (no student names/IDs). Use LM Studio for complete privacy.

## Performance

| Model | Time per Student | Total Time (40 students) | Cost per Batch |
|-------|-----------------|-------------------------|----------------|
| OpenAI GPT-5 mini | 20-60 sec | 15-40 min | ~$0.074 |
| LM Studio (Qwen 3 4B) | 30-60 sec | 20-40 min | $0 |

## Documentation

- **Data Flow**: `docs/data-flow.md` - Step-by-step execution flow and API patterns
- **Caching System**: `docs/caching-system.md` - How revision tracking works
- **Adding Prompts**: `docs/ADDING_ASSIGNMENT_PROMPTS.md` - Create new assignments
- **Schoology API**: `docs/schoology-*.md` - Integration endpoint details

## Future Enhancements

- Automatic prompt selection based on assignment folder name
- Support for additional languages (Kotlin, JavaScript)
- Improved error handling for malformed AI responses
- Batch retry logic for API failures
- Plagiarism detection between submissions

## License

Built for single-instructor use at Mobile Makers Academy. Not designed for scale or general distribution.
