package com.mobilemakers.grader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GradingResult {

    @JsonProperty("score")
    private double score;

    @JsonProperty("maxScore")
    private double maxScore;

    @JsonProperty("mvpComplete")
    private boolean mvpComplete;

    @JsonProperty("stretchGoalsCompleted")
    private List<String> stretchGoalsCompleted;

    @JsonProperty("feedback")
    private GradingFeedback feedback;

    @JsonProperty("compileIssues")
    private String compileIssues;

    private String rawResponse;

    public double score() {
        return score;
    }

    public double maxScore() {
        return maxScore;
    }

    public boolean mvpComplete() {
        return mvpComplete;
    }

    public List<String> stretchGoalsCompleted() {
        return stretchGoalsCompleted == null ? Collections.emptyList() : stretchGoalsCompleted;
    }

    public GradingFeedback feedback() {
        return feedback == null ? new GradingFeedback() : feedback;
    }

    public String compileIssues() {
        return compileIssues == null ? "unknown" : compileIssues;
    }

    public String rawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
}
