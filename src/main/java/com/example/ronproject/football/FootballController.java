package com.example.ronproject.football;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/football")
public class FootballController {

    private final FootballService footballService;

    public FootballController(FootballService footballService) {
        this.footballService = footballService;
    }

    @GetMapping("/leagues")
    List<FootballLeagueResponse> getLeagues() {
        return footballService.getLeagues();
    }

    @GetMapping("/matches")
    List<FootballMatchResponse> getTrackedMatches() {
        return footballService.getTrackedMatches();
    }
}
