package com.mobilemakers.grader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilemakers.grader.model.GradingResult;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Handles communication with the OpenAI API.
 */
public class OpenAIGrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIGrader.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String DEFAULT_MODEL = "gpt-5-mini";

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAIGrader() {
        this(Config.get("OPENAI_API_KEY"), DEFAULT_MODEL);
    }

    public OpenAIGrader(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model;
        this.objectMapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofMinutes(10))
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofMinutes(10))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
    }

    public GradingResult gradeSubmission(String studentKey, String prompt) throws IOException {
        ensureApiKeyPresent();
        LOGGER.info("Grading submission for {}", studentKey);

        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", new Object[]{
                        Map.of("role", "system", "content", "You are a grading assistant for Swift assignments."),
                        Map.of("role", "user", "content", prompt)
                },
                "response_format", Map.of("type", "json_object")
        );

        String body = objectMapper.writeValueAsString(payload);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(RequestBody.create(body, JSON))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI API returned status " + response.code() + ": " + responseBody);
            }
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IOException("OpenAI response missing choices: " + responseBody);
            }

            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.path("message");
            JsonNode contentNode = message.path("content");
            String content = contentNode.isTextual() ? contentNode.asText() : contentNode.toString();

            GradingResult result = objectMapper.readValue(content, GradingResult.class);
            result.setRawResponse(responseBody);
            return result;
        }
    }

    private void ensureApiKeyPresent() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured.");
        }
    }
}
