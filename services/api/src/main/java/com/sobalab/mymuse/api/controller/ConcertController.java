package com.sobalab.mymuse.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.service.ConcertService;

@RestController
@RequestMapping("/concerts")
public class ConcertController {

    private final ConcertService concertService;

    public ConcertController(ConcertService concertService) {
        this.concertService = concertService;
    }

    // @GetMapping
    // public CursorResponse<ConcertDto> searchConcerts(
    // @RequestParam(required = false) String q,
    // @RequestParam(required = false) String region,
    // @RequestParam(required = false) String sort,
    // @RequestParam(required = false) String cursor,
    // @RequestParam(required = false) Integer limit) {
    // List<ConcertDto> items = concertService.searchConcerts(q, region, sort,
    // cursor, limit);
    // return new CursorResponse<>(items, null, false);
    // }

    @GetMapping("/{concertId}")
    public ConcertDto getConcert(@PathVariable String concertId) {
        return concertService.getConcert(concertId);
    }
}
