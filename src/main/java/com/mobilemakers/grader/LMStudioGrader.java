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
 * Handles communication with LM Studio local API.
 */
public class LMStudioGrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(LMStudioGrader.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String DEFAULT_ENDPOINT = "http://localhost:1234/v1/chat/completions";

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private final String modelName;

    public LMStudioGrader() {
        this(Config.get("LM_STUDIO_ENDPOINT"), Config.get("LM_STUDIO_MODEL"));
    }

    public LMStudioGrader(String endpoint, String modelName) {
        this.endpoint = endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint;
        this.modelName = modelName == null || modelName.isBlank() ? "qwen3-4b-thinking-2507" : modelName;
        this.objectMapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofMinutes(10))     // Total call timeout: 10 minutes
                .connectTimeout(Duration.ofSeconds(30))  // Connection timeout: 30 seconds
                .readTimeout(Duration.ofMinutes(10))     // Read timeout: 10 minutes (critical for slow local models)
                .writeTimeout(Duration.ofSeconds(30))    // Write timeout: 30 seconds
                .build();
    }

    public GradingResult gradeSubmission(String studentKey, String prompt) throws IOException {
        LOGGER.info("Grading submission for {} using LM Studio ({}) - this may take 1-3 minutes for thinking models...", studentKey, modelName);

        // Note: LM Studio doesn't support response_format like OpenAI
        // We'll rely on the prompt to instruct JSON output
        Map<String, Object> payload = Map.of(
                "model", modelName,
                "messages", new Object[]{
                        Map.of("role", "system", "content", "You are a grading assistant for Swift assignments. You must respond ONLY with valid JSON. Do not include any explanatory text before or after the JSON."),
                        Map.of("role", "user", "content", prompt)
                },
                "temperature", 0
        );

        String body = objectMapper.writeValueAsString(payload);
        Request request = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(body, JSON))
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                if (response.code() == 404 || responseBody.contains("Connection refused")) {
                    throw new IOException("LM Studio server is not running at " + endpoint + ". Please start the server in LM Studio.");
                }
                throw new IOException("LM Studio API returned status " + response.code() + ": " + responseBody);
            }
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IOException("LM Studio response missing choices: " + responseBody);
            }

            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.path("message");
            JsonNode contentNode = message.path("content");
            String content = contentNode.isTextual() ? contentNode.asText() : contentNode.toString();

            GradingResult result = objectMapper.readValue(content, GradingResult.class);
            result.setRawResponse(responseBody);
            return result;
        } catch (java.net.ConnectException e) {
            throw new IOException("Cannot connect to LM Studio at " + endpoint + ". Please ensure the server is running and the model is loaded.", e);
        }
    }
}
