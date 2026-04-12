package com.example.ronproject.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.football")
public class FootballApiProperties {

    private String apiBaseUrl;
    private String apiToken;
    private List<String> competitionCodes = new ArrayList<>();
    private List<String> allowedTeams = new ArrayList<>();

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public List<String> getCompetitionCodes() {
        return competitionCodes;
    }

    public void setCompetitionCodes(List<String> competitionCodes) {
        this.competitionCodes = competitionCodes;
    }

    public List<String> getAllowedTeams() {
        return allowedTeams;
    }

    public void setAllowedTeams(List<String> allowedTeams) {
        this.allowedTeams = allowedTeams;
    }
}
