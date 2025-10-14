# Adding Assignment Prompt Files

Each Swift assignment keeps its own rubric as a Java constant in `src/main/java/com/mobilemakers/grader/prompts/`.
Examples available today:
- `ConstantsVariablesDatatypesPrompt` (state/constant basics assignment)
- `ButtonsPrompt` (Buttons Challenge)

1. **Create the prompt file**
   - Copy whichever existing prompt is closest (e.g., `ConstantsVariablesDatatypesPrompt.java` or `ButtonsPrompt.java`) to `YourAssignmentNamePrompt.java`.
   - Update the `PROMPT` string with the new grading instructions (keep the 8/2 scoring and JSON schema).

2. **Activate the prompt**
   - Open `src/main/java/com/mobilemakers/grader/AssignmentPrompt.java`.
   - Change the static import to the new prompt class, e.g.:
     ```java
     import static com.mobilemakers.grader.prompts.YourAssignmentNamePrompt.PROMPT;
     ```

3. **Run the grader**
   - Build and execute as usual. `AssignmentPrompt.buildPrompt(...)` will inject student code into the selected rubric.

_Note:_ The guard in `AssignmentPrompt` will throw if the selected prompt constant is blank, helping catch missing files before grading.
