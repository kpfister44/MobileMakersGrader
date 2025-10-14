package com.mobilemakers.grader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GradingFeedback {

    @JsonProperty("strengths")
    private List<String> strengths;

    @JsonProperty("improvements")
    private List<String> improvements;

    @JsonProperty("syntaxErrors")
    private List<String> syntaxErrors;

    @JsonProperty("studentSummary")
    private String studentSummary;

    public List<String> strengths() {
        return strengths == null ? Collections.emptyList() : strengths;
    }

    public List<String> improvements() {
        return improvements == null ? Collections.emptyList() : improvements;
    }

    public List<String> syntaxErrors() {
        return syntaxErrors == null ? Collections.emptyList() : syntaxErrors;
    }

    public String studentSummary() {
        return studentSummary == null ? "" : studentSummary;
    }
}
