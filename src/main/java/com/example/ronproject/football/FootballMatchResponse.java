package com.example.ronproject.football;

import java.time.Instant;
import java.util.List;

public record FootballMatchResponse(
        String fixtureId,
        String leagueName,
        String homeTeam,
        String awayTeam,
        String homeLogoUrl,
        String awayLogoUrl,
        String status,
        Integer homeGoals,
        Integer awayGoals,
        Instant date,
        String round
) {
}
