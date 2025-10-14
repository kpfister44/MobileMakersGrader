# Grading Cache System

## Overview

The grading cache system prevents re-grading student submissions that have already been processed, saving time and API costs. It tracks which revision of each assignment has been graded for each student.

## How It Works

### Revision Detection

The system scans each student's submission folder for revision folders named:
- `Revision 1 - On time`
- `Revision 2 - Late`
- `Revision 3 - On time`
- etc.

The **highest revision number** is automatically detected and compared against the cache.

### Grading Logic

```
For each student:
  1. Find highest revision number in their folder
  2. Check cache: what was the last graded revision?
  3. IF (current revision > last graded revision) OR (not in cache):
       → GRADE the current revision
       → UPDATE cache with revision number and grade
  4. ELSE:
       → SKIP (already graded)
```

### Cache File Structure

The cache is stored in `results/grading-cache.json`:

```json
{
  "students": {
    "s498776": {
      "name": "Krawec, Claire",
      "assignments": {
        "8017693698": {
          "assignment_name": "TextField Challenge",
          "last_graded_revision": 2,
          "grade": 10.0,
          "graded_timestamp": "2025-10-11T15:30:00Z"
        }
      }
    },
    "s513390": {
      "name": "Tyryk, Roman-Avdii",
      "assignments": {
        "8017693698": {
          "last_graded_revision": 1,
          "grade": 8.5,
          "graded_timestamp": "2025-10-11T15:28:00Z"
        }
      }
    }
  },
  "cache_version": "1.0",
  "last_updated": "2025-10-11T15:30:00Z"
}
```

## Key Features

### 1. **Automatic Cache Management**
- Cache is loaded at the start of each grading run
- Cache is saved after all students are processed
- No manual intervention required

### 2. **Revision-Based Tracking**
- Tracks by revision number, not timestamp
- Detects new revisions automatically
- Works with unlimited revision submissions (1, 2, 3, ..., 100+)

### 3. **Smart Skipping**
```
First run:
  - Student has "Revision 1" → Grade it → Cache: revision = 1

Second run (same submission):
  - Student still has "Revision 1"
  - Cache shows revision 1 already graded
  - → SKIP ✓

Third run (new revision):
  - Student now has "Revision 2"
  - Cache shows revision 1 graded
  - 2 > 1 → Grade Revision 2 → Update cache: revision = 2
```

### 4. **Multi-Assignment Support**
- Tracks multiple assignments per student
- Each assignment has its own revision counter
- Cache grows to ~150-240KB per year (40 students × 40 assignments)

## Console Output

### When Skipping Already-Graded Submissions:
```
⊘ Skipped Castro, Marianna - s486002 - Revision 1 already graded (last graded: revision 1)
⊘ Skipped Krawec, Claire - s498776 - Revision 2 already graded (last graded: revision 2)
```

### When Detecting New Revisions:
```
→ New submission detected for Krawec, Claire - s498776 - Revision 3 (previously graded: revision 2)
  → Cache updated: s498776 revision 3
```

### Summary at End:
```
═══════════════════════════════════════════
Grading Summary:
  Total students: 32
  Newly graded: 5
  Skipped (already graded): 27
  Cache: 32 students, 128 graded assignments
═══════════════════════════════════════════
```

## Benefits

### Time Savings
- **First run:** 40 students @ 30 sec each = 20 minutes
- **Second run (no changes):** All skipped = 10 seconds
- **With 5 new revisions:** Only grade 5 students = 2.5 minutes

### Cost Savings (OpenAI API)
- First run: $0.074 for 40 students
- Second run: $0.00 (all skipped)
- With 5 revisions: ~$0.009 (5 students only)

### Reliability
- Safe to run multiple times
- Idempotent (running twice = same result as once)
- Crash-resistant (cache persists between runs)

## Edge Cases Handled

### 1. No Revision Folders
If a student submits files directly without a revision folder:
- Treats it as "Revision 1"
- Works normally with cache

### 2. Mixed Folder Structure
Some students have revision folders, some don't:
- Each handled correctly based on their structure
- Cache tracks appropriately

### 3. Deleted Revisions
If a student deletes a revision folder:
- System only sees remaining folders
- Highest visible revision is used
- Won't re-grade if cache shows higher revision already graded

### 4. Cache File Corruption
If `grading-cache.json` gets corrupted:
- System starts fresh with new cache
- Logs warning message
- All students will be graded (safe fallback)

## File Locations

| File | Purpose | Location |
|------|---------|----------|
| `grading-cache.json` | Cache storage | `results/grading-cache.json` |
| `GradingCache.java` | Cache management | `src/main/java/com/mobilemakers/grader/GradingCache.java` |
| `SwiftFileReader.java` | Revision detection | `src/main/java/com/mobilemakers/grader/SwiftFileReader.java` |
| `GradeProcessor.java` | Main grading logic | `src/main/java/com/mobilemakers/grader/GradeProcessor.java` |

## Configuration

### Required Environment Variables

For cache to work with assignment tracking:
```bash
SCHOOLOGY_ASSIGNMENT_ID=8017693698  # Used as cache key
```

If not set, uses `"unknown_assignment"` as fallback (still works, but less organized).

### Disable Caching

To force re-grade all students (ignore cache):
1. Delete `results/grading-cache.json`
2. Run grading normally

The cache will be recreated from scratch.

## Testing the Cache

### Test Scenario 1: First Run
```bash
# Run grading
java -jar target/swift-grader-*.jar submissions results

Expected output:
✓ Loaded grading cache: 0 students tracked
# ... grades all students ...
✓ Saved grading cache to results/grading-cache.json
```

### Test Scenario 2: Re-run (No Changes)
```bash
# Run again without any changes
java -jar target/swift-grader-*.jar submissions results

Expected output:
✓ Loaded grading cache: 32 students tracked
⊘ Skipped Student 1 - Revision 1 already graded
⊘ Skipped Student 2 - Revision 1 already graded
# ... all students skipped ...
  Newly graded: 0
  Skipped (already graded): 32
```

### Test Scenario 3: New Revision
```bash
# Student submits new revision (create "Revision 2 - On time" folder)
java -jar target/swift-grader-*.jar submissions results

Expected output:
✓ Loaded grading cache: 32 students tracked
⊘ Skipped Student 1 - Revision 1 already graded
→ New submission detected for Student 2 - Revision 2 (previously graded: revision 1)
# ... grades Student 2 only ...
  → Cache updated: s123456 revision 2
  Newly graded: 1
  Skipped (already graded): 31
```

## Troubleshooting

### Problem: Students Being Skipped When They Shouldn't Be

**Solution:** Check the cache file to see what revision is recorded:
```bash
cat results/grading-cache.json | grep -A 5 "s123456"
```

### Problem: Cache Growing Too Large

**Solution:** Archive old cache files periodically:
```bash
mv results/grading-cache.json results/grading-cache-2024-fall.json
# Next run will create fresh cache
```

### Problem: Want to Re-grade Specific Student

**Solution:** Edit `grading-cache.json` and remove that student's entry, or set their `last_graded_revision` to 0.

## Future Enhancements

Potential improvements:
- [ ] `--force-regrade` command line flag
- [ ] `--regrade-student=s123456` option
- [ ] Cache cleanup (remove students who aren't in current submission folder)
- [ ] Cache statistics dashboard
- [ ] Multiple assignment tracking in single run

## Notes

1. **Cache is per-results-directory:** Each results folder has its own cache
2. **School UID is key:** Students matched by school UID (e.g., "s513390")
3. **Assignment ID is key:** Each assignment tracked separately
4. **Thread-safe:** Cache saved atomically at end of run
5. **JSON format:** Human-readable and editable if needed
