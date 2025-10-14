# Swift Assignment Auto-Grader

Java-based tool that grades introductory Swift submissions using the OpenAI API. Built to batch process ~40 student folders, score them against an MVP + stretch goal rubric, and emit CSV results for gradebook imports.

## Prerequisites
- Java 17+
- Maven 3.9+
- OpenAI API key (set `OPENAI_API_KEY` in environment or `.env` file)

## Setup
1. Clone/download this repository.
2. Install dependencies and build:
   ```bash
   mvn clean package
   ```
3. Add Swift submission folders inside `submissions/` (one folder per student). Include only `.swift` files.
4. Export your API key (**placeholder provided**):
   ```bash
   export OPENAI_API_KEY=YOUR_OPENAI_API_KEY_HERE
   ```

## Usage
```bash
java -jar target/swift-grader-0.1.0-SNAPSHOT.jar ./submissions ./results
```
- If no arguments are provided the defaults are `./submissions` and `./results`.
- CSV output is timestamped (e.g., `results/grades-20240205-153000.csv`).

## How It Works
- `SwiftFileReader` merges each student's `.swift` files into a single prompt payload, tagging filenames but not folder names.
- `AssignmentPrompt` injects the code into a fixed rubric text block.
- `OpenAIGrader` sends the prompt to OpenAI (`gpt-4o-mini` by default) expecting JSON formatted grading.
- `GradeProcessor` handles batching, logging, and CSV formatting.

## Testing Notes
- Include representative submissions in `submissions/` for dry runs.
- For development without hitting the API, stub `OpenAIGrader` or point to a mock server.

## Environment Variables
- `OPENAI_API_KEY`: required for live grading (placeholder currently).
- Optional: override the model via `OpenAIGrader` constructor if needed.

## Limitations & Next Steps
- Only supports the Constants/Variables/Data Types assignment.
- Assumes OpenAI returns the expected JSON structure; add guardrails or schema validation for production.
- Extend with additional assignments, web UI, or plagiarism detection as outlined in `AGENTS.md`.
