package com.example.ronproject.football;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.ronproject.config.FootballApiProperties;

class FootballServiceTests {

    private static final String BASE_URL = "https://api.football-data.org";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;
    private FootballApiProperties properties;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        properties = new FootballApiProperties();
    }

    private FootballService createService() {
        return new FootballService(restClientBuilder.baseUrl(BASE_URL).build(), properties);
    }

    private String matchesUrl(String code) {
        LocalDate today = LocalDate.now();
        return BASE_URL + "/v4/competitions/" + code + "/matches?dateFrom="
                + today.minusMonths(1) + "&dateTo=" + today.plusMonths(1);
    }

    // ───── Leagues ─────

    @Test
    void getLeaguesMapsCompetitionPayload() {
        server.expect(requestTo(BASE_URL + "/v4/competitions"))
                .andRespond(withSuccess("""
                        {
                          "competitions": [
                            {
                              "id": 2021,
                              "name": "Premier League",
                              "area": { "name": "England" },
                              "type": "LEAGUE",
                              "emblem": "https://cdn.example.com/premier-league.png"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<FootballLeagueResponse> leagues = createService().getLeagues();

        assertThat(leagues).containsExactly(new FootballLeagueResponse(
                2021, "Premier League", "England", "LEAGUE",
                "https://cdn.example.com/premier-league.png"
        ));
    }

    @Test
    void getLeaguesReturnsEmptyListWhenCompetitionsArrayIsEmpty() {
        server.expect(requestTo(BASE_URL + "/v4/competitions"))
                .andRespond(withSuccess("""
                        { "competitions": [] }
                        """, MediaType.APPLICATION_JSON));

        assertThat(createService().getLeagues()).isEmpty();
    }

    // ───── Matches — filtering ─────

    @Test
    void getTrackedMatchesFiltersMatchesByAllowedTeams() {
        properties.setCompetitionCodes(List.of("PL"));
        properties.setAllowedTeams(List.of("Arsenal FC"));

        server.expect(requestTo(matchesUrl("PL")))
                .andRespond(withSuccess("""
                        {
                          "matches": [
                            {
                              "id": 1,
                              "utcDate": "2026-04-09T18:00:00Z",
                              "status": "FINISHED",
                              "matchday": 32,
                              "competition": { "name": "Premier League" },
                              "homeTeam": { "name": "Arsenal FC", "crest": "https://cdn.example.com/arsenal.png" },
                              "awayTeam": { "name": "Chelsea FC", "crest": "https://cdn.example.com/chelsea.png" },
                              "score": { "fullTime": { "home": 2, "away": 1 } }
                            },
                            {
                              "id": 2,
                              "utcDate": "2026-04-09T20:00:00Z",
                              "status": "FINISHED",
                              "matchday": 32,
                              "competition": { "name": "Premier League" },
                              "homeTeam": { "name": "Brighton & Hove Albion FC", "crest": "https://cdn.example.com/brighton.png" },
                              "awayTeam": { "name": "Wolverhampton Wanderers FC", "crest": "https://cdn.example.com/wolves.png" },
                              "score": { "fullTime": { "home": 0, "away": 0 } }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<FootballMatchResponse> matches = createService().getTrackedMatches();

        assertThat(matches).containsExactly(new FootballMatchResponse(
                "1", "Premier League", "Arsenal FC", "Chelsea FC",
                "https://cdn.example.com/arsenal.png", "https://cdn.example.com/chelsea.png",
                "FINISHED", 2, 1, Instant.parse("2026-04-09T18:00:00Z"), "Matchday 32"
        ));
    }

    @Test
    void getTrackedMatchesReturnsAllMatchesWhenAllowedTeamsIsEmpty() {
        properties.setCompetitionCodes(List.of("PL"));
        properties.setAllowedTeams(List.of());

        server.expect(requestTo(matchesUrl("PL")))
                .andRespond(withSuccess("""
                        {
                          "matches": [
                            {
                              "id": 1, "utcDate": "2026-04-09T18:00:00Z", "status": "SCHEDULED", "matchday": 33,
                              "competition": { "name": "Premier League" },
                              "homeTeam": { "name": "Brighton & Hove Albion FC", "crest": null },
                              "awayTeam": { "name": "Wolverhampton Wanderers FC", "crest": null },
                              "score": { "fullTime": { "home": null, "away": null } }
                            },
                            {
                              "id": 2, "utcDate": "2026-04-10T15:00:00Z", "status": "SCHEDULED", "matchday": 33,
                              "competition": { "name": "Premier League" },
                              "homeTeam": { "name": "Fulham FC", "crest": null },
                              "awayTeam": { "name": "Brentford FC", "crest": null },
                              "score": { "fullTime": { "home": null, "away": null } }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(createService().getTrackedMatches()).hasSize(2);
    }

    @Test
    void getTrackedMatchesFilterIsCaseInsensitive() {
        properties.setCompetitionCodes(List.of("PL"));
        properties.setAllowedTeams(List.of("arsenal fc"));

        server.expect(requestTo(matchesUrl("PL")))
                .andRespond(withSuccess("""
                        {
                          "matches": [
                            {
                              "id": 1, "utcDate": "2026-04-09T18:00:00Z", "status": "SCHEDULED", "matchday": 33,
                              "competition": { "name": "Premier League" },
                              "homeTeam": { "name": "Arsenal FC", "crest": null },
                              "awayTeam": { "name": "Chelsea FC", "crest": null },
                              "score": { "fullTime": { "home": null, "away": null } }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(createService().getTrackedMatches()).hasSize(1);
    }

    @Test
    void getTrackedMatchesCombinesMultipleCompetitions() {
        properties.setCompetitionCodes(List.of("PL", "CL"));
        properties.setAllowedTeams(List.of());

        server.expect(requestTo(matchesUrl("PL")))
                .andRespond(withSuccess("""
                        {
                          "matches": [
                            {
                              "id": 1, "utcDate": "2026-04-09T18:00:00Z", "status": "SCHEDULED", "matchday": 33,
                              "competition": { "name": "Premier League" },
                              "homeTeam": { "name": "Arsenal FC", "crest": null },
                              "awayTeam": { "name": "Chelsea FC", "crest": null },
                              "score": { "fullTime": { "home": null, "away": null } }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(matchesUrl("CL")))
                .andRespond(withSuccess("""
                        {
                          "matches": [
                            {
                              "id": 100, "utcDate": "2026-04-10T20:00:00Z", "status": "SCHEDULED", "matchday": 8,
                              "competition": { "name": "UEFA Champions League" },
                              "homeTeam": { "name": "Real Madrid CF", "crest": null },
                              "awayTeam": { "name": "FC Bayern München", "crest": null },
                              "score": { "fullTime": { "home": null, "away": null } }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<FootballMatchResponse> matches = createService().getTrackedMatches();

        assertThat(matches).hasSize(2);
        assertThat(matches).extracting(FootballMatchResponse::leagueName)
                .containsExactly("Premier League", "UEFA Champions League");
    }

    // ───── JSON edge cases ─────

    @Test
    void getTrackedMatchesHandlesNullScoreGracefully() {
        properties.setCompetitionCodes(List.of("PL"));
        properties.setAllowedTeams(List.of());

        server.expect(requestTo(matchesUrl("PL")))
                .andRespond(withSuccess("""
                        {
                          "matches": [
                            {
                              "id": 1, "utcDate": "2026-04-09T18:00:00Z", "status": "SCHEDULED", "matchday": 33,
                              "competition": { "name": "Premier League" },
                              "homeTeam": { "name": "Arsenal FC", "crest": null },
                              "awayTeam": { "name": "Chelsea FC", "crest": null },
                              "score": { "fullTime": { "home": null, "away": null } }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<FootballMatchResponse> matches = createService().getTrackedMatches();

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().homeGoals()).isNull();
        assertThat(matches.getFirst().awayGoals()).isNull();
    }

    @Test
    void getTrackedMatchesHandlesMissingFieldsGracefully() {
        properties.setCompetitionCodes(List.of("PL"));
        properties.setAllowedTeams(List.of());

        server.expect(requestTo(matchesUrl("PL")))
                .andRespond(withSuccess("""
                        {
                          "matches": [
                            {
                              "id": 1,
                              "status": "SCHEDULED"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<FootballMatchResponse> matches = createService().getTrackedMatches();

        assertThat(matches).hasSize(1);
        FootballMatchResponse match = matches.getFirst();
        assertThat(match.homeTeam()).isNull();
        assertThat(match.awayTeam()).isNull();
        assertThat(match.leagueName()).isNull();
        assertThat(match.date()).isNull();
        assertThat(match.homeGoals()).isNull();
    }

    @Test
    void getTrackedMatchesReturnsEmptyListWhenMatchesArrayIsEmpty() {
        properties.setCompetitionCodes(List.of("PL"));

        server.expect(requestTo(matchesUrl("PL")))
                .andRespond(withSuccess("""
                        { "matches": [] }
                        """, MediaType.APPLICATION_JSON));

        assertThat(createService().getTrackedMatches()).isEmpty();
    }

    // ───── API failure resilience ─────

    @Test
    void getTrackedMatchesReturnsEmptyListWhenProviderCallFails() {
        properties.setCompetitionCodes(List.of("PL"));

        server.expect(requestTo(matchesUrl("PL")))
                .andRespond(request -> {
                    throw new IllegalStateException("provider unavailable");
                });

        assertThat(createService().getTrackedMatches()).isEmpty();
    }

    @Test
    void getTrackedMatchesReturnsEmptyListWhenResponseBodyIsEmpty() {
        properties.setCompetitionCodes(List.of("PL"));

        server.expect(requestTo(matchesUrl("PL")))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThat(createService().getTrackedMatches()).isEmpty();
    }

    @Test
    void getTrackedMatchesReturnsEmptyListWhenResponseIsInvalidJson() {
        properties.setCompetitionCodes(List.of("PL"));

        server.expect(requestTo(matchesUrl("PL")))
                .andRespond(withSuccess("<html>Service Unavailable</html>", MediaType.TEXT_HTML));

        assertThat(createService().getTrackedMatches()).isEmpty();
    }

    @Test
    void getLeaguesReturnsEmptyListWhenProviderCallFails() {
        server.expect(requestTo(BASE_URL + "/v4/competitions"))
                .andRespond(request -> {
                    throw new IllegalStateException("provider unavailable");
                });

        assertThat(createService().getLeagues()).isEmpty();
    }
}
