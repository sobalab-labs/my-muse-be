package com.sobalab.mymuse.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.model.enums.CountryCode;
import com.sobalab.mymuse.api.model.enums.FeedSort;
import com.sobalab.mymuse.api.model.enums.FeedView;
import com.sobalab.mymuse.api.service.FeedService;

@RestController
@RequestMapping("/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping("/trending")
    public CursorResponse<ConcertDto> getTrendingFeed(
            @RequestParam(required = false) FeedView view,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) CountryCode nation,
            @RequestParam(required = false) FeedSort sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        List<ConcertDto> items = feedService.getTrendingFeed(
                view, month, nation, sort, cursor, limit);
        return new CursorResponse<>(items, null, false);
    }
}
