package com.example.ronproject.football;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.ronproject.config.FootballApiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FootballService {

    private static final Logger log = LoggerFactory.getLogger(FootballService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient footballRestClient;
    private final FootballApiProperties properties;

    public FootballService(RestClient footballRestClient, FootballApiProperties properties) {
        this.footballRestClient = footballRestClient;
        this.properties = properties;
    }

    public List<FootballLeagueResponse> getLeagues() {
        JsonNode body = getResponseBody("/v4/competitions");
        JsonNode competitions = body.path("competitions");
        if (!competitions.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(competitions.spliterator(), false)
                .map(this::toLeagueResponse)
                .toList();
    }

    public List<FootballMatchResponse> getTrackedMatches() {
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusMonths(1);
        LocalDate monthAhead = today.plusMonths(1);

        return properties.getCompetitionCodes().stream()
                .flatMap(code -> fetchMatches(code, monthAgo, monthAhead))
                .filter(this::isTrackedMatch)
                .toList();
    }

    private Stream<FootballMatchResponse> fetchMatches(String competitionCode, LocalDate from, LocalDate to) {
        String path = "/v4/competitions/" + competitionCode
                + "/matches?dateFrom=" + from + "&dateTo=" + to;
        JsonNode body = getResponseBody(path);
        JsonNode matches = body.path("matches");
        if (!matches.isArray()) {
            return Stream.empty();
        }
        return java.util.stream.StreamSupport.stream(matches.spliterator(), false)
                .map(this::toMatchResponse);
    }

    private FootballLeagueResponse toLeagueResponse(JsonNode item) {
        return new FootballLeagueResponse(
                intValue(item.path("id")),
                textValue(item.path("name")),
                textValue(item.path("area").path("name")),
                textValue(item.path("type")),
                textValue(item.path("emblem"))
        );
    }

    private FootballMatchResponse toMatchResponse(JsonNode item) {
        JsonNode competition = item.path("competition");
        JsonNode homeTeam = item.path("homeTeam");
        JsonNode awayTeam = item.path("awayTeam");
        JsonNode score = item.path("score").path("fullTime");
        JsonNode season = item.path("season");

        return new FootballMatchResponse(
                textValue(item.path("id")),
                textValue(competition.path("name")),
                textValue(homeTeam.path("name")),
                textValue(awayTeam.path("name")),
                textValue(homeTeam.path("crest")),
                textValue(awayTeam.path("crest")),
                textValue(item.path("status")),
                intValue(score.path("home")),
                intValue(score.path("away")),
                instantValue(item.path("utcDate")),
                textValue(item.path("matchday")) != null
                        ? "Matchday " + textValue(item.path("matchday"))
                        : null
        );
    }

    private boolean isTrackedMatch(FootballMatchResponse match) {
        if (properties.getAllowedTeams().isEmpty()) {
            return true;
        }
        return properties.getAllowedTeams().stream()
                .anyMatch(team -> equalsIgnoreCase(team, match.homeTeam())
                        || equalsIgnoreCase(team, match.awayTeam()));
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.toLowerCase(Locale.ROOT).equals(right.toLowerCase(Locale.ROOT));
    }

    private JsonNode getResponseBody(String path) {
        try {
            String body = footballRestClient.get()
                    .uri(path)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return OBJECT_MAPPER.createObjectNode();
            }
            return OBJECT_MAPPER.readTree(body);
        } catch (Exception ex) {
            log.warn("Football API call failed for path {}: {}", path, ex.getMessage());
            return OBJECT_MAPPER.createObjectNode();
        }
    }

    private String textValue(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private Integer intValue(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asInt();
    }

    private Instant instantValue(JsonNode node) {
        String value = textValue(node);
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }
}
