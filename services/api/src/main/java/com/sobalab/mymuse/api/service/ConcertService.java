package com.sobalab.mymuse.api.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.model.Concert;
import com.sobalab.mymuse.api.model.enums.Region;
import com.sobalab.mymuse.api.repository.ConcertRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConcertService {
    private final ConcertRepository concertRepository;

    // @Transactional(readOnly = true)
    // public List<ConcertDto> searchConcerts(String q, String region, String sort,
    // String cursor, Integer limit) {
    // int pageSize = (limit != null && limit > 0) ? limit : 20;
    // Region regionEnum = parseRegion(region);
    // Sort sorting = parseSort(sort);
    // PageRequest pageRequest = PageRequest.of(0, pageSize,
    // sorting.and(Sort.by(Sort.Direction.DESC, "id")));

    // List<Concert> concerts = concertRepository.searchConcerts(q, regionEnum,
    // pageRequest);
    // return concerts.stream().map(toDto).collect(Collectors.toList());
    // }

    @Transactional(readOnly = true)
    public List<ConcertDto> getTrendingFeed(String view, String month, String region, String sort, String cursor,
            Integer limit) {
        int pageSize = (limit != null && limit > 0) ? limit : 20;
        Region regionEnum = parseRegion(region);
        Sort sorting = parseSort(sort);
        PageRequest pageRequest = PageRequest.of(0, pageSize, sorting.and(Sort.by(Sort.Direction.DESC, "id")));

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (month != null && !month.isBlank()) {
            YearMonth ym = YearMonth.parse(month);
            startDate = ym.atDay(1).atStartOfDay();
            endDate = ym.plusMonths(1).atDay(1).atStartOfDay();
        }

        List<Concert> concerts = concertRepository.findTrendingFeed(regionEnum, startDate, endDate, pageRequest);
        return concerts.stream().map(toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConcertDto getConcert(String concertId) {
        UUID uuid = UUID.fromString(concertId);
        Concert concert = concertRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found: " + concertId));
        return toDto.apply(concert);
    }

    private Region parseRegion(String region) {
        if (region == null || region.isBlank()) {
            return null;
        }
        try {
            return Region.valueOf(region.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Sort parseSort(String sort) {
        if ("recently_announced".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        // default 'dday'
        return Sort.by(Sort.Direction.ASC, "date");
    }

    private final java.util.function.Function<Concert, ConcertDto> toDto = concert -> new ConcertDto(
            concert.getId().toString(),
            concert.getTitle(),
            concert.getDate() != null ? concert.getDate().atZone(ZoneId.systemDefault()).toOffsetDateTime() : null,
            concert.getVenue() != null ? concert.getVenue().getName() : null,
            concert.getDurationMinutes(),
            concert.getTicketingUrl(),
            concert.getVenue() != null && concert.getVenue().getRegion() != null
                    ? concert.getVenue().getRegion().name().toLowerCase()
                    : null);
}
