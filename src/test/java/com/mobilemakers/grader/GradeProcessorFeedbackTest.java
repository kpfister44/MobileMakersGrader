package com.mobilemakers.grader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilemakers.grader.model.GradingResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradeProcessorFeedbackTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void usesStudentSummaryWhenPresent() throws Exception {
        GradingResult result = parseResult("""
                {
                  \"score\": 9.5,
                  \"maxScore\": 10,
                  \"mvpComplete\": true,
                  \"stretchGoalsCompleted\": [\"stretch1\"],
                  \"feedback\": {
                    \"studentSummary\": \"Great progress on the MVP layout. Add the console logging to lock in the remaining points.\",
                    \"strengths\": [\"MVP layout is correct\"],
                    \"improvements\": [\"Add console logging\"],
                    \"syntaxErrors\": []
                  },
                  \"compileIssues\": \"none\"
                }
                """);

        String feedback = invokeJoinFeedback(result);
        assertEquals(
                "Great progress on the MVP layout. Add the console logging to lock in the remaining points.",
                feedback);
    }

    @Test
    void fallsBackToStructuredSentencesWhenSummaryMissing() throws Exception {
        GradingResult result = parseResult("""
                {
                  \"score\": 6.0,
                  \"maxScore\": 10,
                  \"mvpComplete\": false,
                  \"stretchGoalsCompleted\": [],
                  \"feedback\": {
                    \"strengths\": [\"Button layout matches spec\"],
                    \"improvements\": [\"Add console logs\", \"Implement the counter stretch goal\"],
                    \"syntaxErrors\": [\"Close the VStack brace\"]
                  },
                  \"compileIssues\": \"minor\"
                }
                """);

        String feedback = invokeJoinFeedback(result);
        assertEquals(
                "You nailed: Button layout matches spec. To raise your score next time, focus on: Add console logs; Implement the counter stretch goal. Fix syntax issues such as: Close the VStack brace.",
                feedback);
    }

    @Test
    void extractsUniqueIdFromFolderName() throws Exception {
        Method method = GradeProcessor.class.getDeclaredMethod("extractUniqueUserId", String.class);
        method.setAccessible(true);
        String id = (String) method.invoke(null, "Doe, Jane - s123456");
        assertEquals("s123456", id);
    }

    @Test
    void fallsBackToTrimmedNameWhenNoHyphen() throws Exception {
        Method method = GradeProcessor.class.getDeclaredMethod("extractUniqueUserId", String.class);
        method.setAccessible(true);
        String id = (String) method.invoke(null, "studentFolder");
        assertEquals("studentFolder", id);
    }

    @Test
    void formatsSchoologyScore() throws Exception {
        Method method = GradeProcessor.class.getDeclaredMethod("formatSchoologyScore", double.class);
        method.setAccessible(true);
        assertEquals("10", method.invoke(null, 10.0));
        assertEquals("8.5", method.invoke(null, 8.5));
    }

    private static GradingResult parseResult(String json) throws Exception {
        GradingResult gradingResult = MAPPER.readValue(json, GradingResult.class);
        gradingResult.setRawResponse(json);
        return gradingResult;
    }

    private static String invokeJoinFeedback(GradingResult result) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> gradeRecordClass = null;
        for (Class<?> nested : GradeProcessor.class.getDeclaredClasses()) {
            if ("GradeRecord".equals(nested.getSimpleName())) {
                gradeRecordClass = nested;
                break;
            }
        }
        if (gradeRecordClass == null) {
            throw new IllegalStateException("Unable to locate GradeRecord nested class");
        }
        Method joinFeedback = gradeRecordClass.getDeclaredMethod("joinFeedback", GradingResult.class);
        joinFeedback.setAccessible(true);
        return (String) joinFeedback.invoke(null, result);
    }
}
