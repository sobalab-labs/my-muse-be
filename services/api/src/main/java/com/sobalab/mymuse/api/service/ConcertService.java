package com.sobalab.mymuse.api.service;

import com.sobalab.mymuse.api.dto.ConcertDto;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;

@Service
public class ConcertService {
    public List<ConcertDto> listConcerts(String scope, String artistId, String region) {
        return Collections.emptyList();
    }

    public ConcertDto getConcert(String concertId) {
        return null;
    }
}
