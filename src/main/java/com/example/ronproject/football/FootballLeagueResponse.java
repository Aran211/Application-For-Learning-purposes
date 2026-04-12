package com.example.ronproject.football;

public record FootballLeagueResponse(
        Integer leagueId,
        String name,
        String country,
        String type,
        String logoUrl
) {
}
