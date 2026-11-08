package com.sobalab.mymuse.api.controller;

import java.util.UUID;

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

    @GetMapping("/{concertId}")
    public ConcertDto getConcert(@PathVariable UUID concertId) {
        return concertService.getConcert(concertId);
    }
}
