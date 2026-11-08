package com.sobalab.mymuse.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.model.Concert;
import com.sobalab.mymuse.api.model.enums.CountryCode;
import com.sobalab.mymuse.api.model.enums.FeedSort;
import com.sobalab.mymuse.api.repository.ConcertRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConcertService {
    private final ConcertRepository concertRepository;

    /**
     * Queries a normalized feed range and maps entities while the read transaction
     * is open.
     */
    @Transactional(readOnly = true)
    public List<ConcertDto> getTrendingFeed(CountryCode nationEnum, LocalDateTime startDate, LocalDateTime endDate,
            FeedSort sort, UUID cursorId, int pageSize) {
        Sort sorting = sort == FeedSort.RECENTLY_ANNOUNCED
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(Sort.Direction.ASC, "date");
        Limit limit = Limit.of(pageSize);
        sorting = sorting.and(Sort.by(Sort.Direction.DESC, "id"));
        List<Concert> concerts;
        if (cursorId == null) {
            concerts = concertRepository.findTrendingFeed(nationEnum, startDate, endDate, sorting, limit);
        } else {
            Concert anchor = concertRepository.findById(cursorId)
                    .orElseThrow(() -> badRequest("Cursor concert no longer exists; restart pagination"));
            if (sort == FeedSort.RECENTLY_ANNOUNCED) {
                concerts = concertRepository.findTrendingFeedAfterAnnouncement(nationEnum, startDate, endDate,
                        anchor.getCreatedAt(), cursorId, limit);
            } else {
                concerts = concertRepository.findTrendingFeedAfterDate(nationEnum, startDate, endDate,
                        anchor.getDate(), cursorId, limit);
            }
        }
        return concerts.stream().map(ConcertDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConcertDto getConcert(UUID concertId) {
        if (concertId == null) {
            throw badRequest("Concert ID is required");
        }
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concert not found: " + concertId));
        return ConcertDto.from(concert);
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
