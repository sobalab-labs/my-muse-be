package com.sobalab.mymuse.api.controller;

import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.service.ConcertService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/concerts")
public class ConcertController {

    private final ConcertService concertService;

    public ConcertController(ConcertService concertService) {
        this.concertService = concertService;
    }

    @GetMapping
    public List<ConcertDto> listConcerts(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String artistId,
            @RequestParam(required = false) String region) {
        return concertService.listConcerts(scope, artistId, region);
    }

    @GetMapping("/{concertId}")
    public ConcertDto getConcert(@PathVariable String concertId) {
        return concertService.getConcert(concertId);
    }
}
