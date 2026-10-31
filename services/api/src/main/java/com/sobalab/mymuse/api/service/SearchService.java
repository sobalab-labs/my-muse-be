package com.sobalab.mymuse.api.service;

import org.springframework.stereotype.Service;

import com.sobalab.mymuse.api.controller.SearchController.SearchResponseDto;
import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.repository.ArtistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final ArtistRepository artistRepository;

    public SearchResponseDto search(String query, String cursor, Integer limit) {
        return new SearchResponseDto(new CursorResponse<>(), new CursorResponse<>());
    }
}
