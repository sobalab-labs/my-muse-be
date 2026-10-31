package com.sobalab.mymuse.api.controller;

import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.service.ConcertService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feed")
public class FeedController {

    private final ConcertService concertService;

    public FeedController(ConcertService concertService) {
        this.concertService = concertService;
    }

    @GetMapping("/trending")
    public CursorResponse<ConcertDto> getTrendingFeed(
            @RequestParam(required = false) String view,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        List<ConcertDto> items = concertService.getTrendingFeed(view, month, region, sort, cursor, limit);
        return new CursorResponse<>(items, null, false);
    }
}
