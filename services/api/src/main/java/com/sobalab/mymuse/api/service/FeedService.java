package com.sobalab.mymuse.api.service;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sobalab.mymuse.api.dto.ConcertDto;
import com.sobalab.mymuse.api.dto.CursorResponse;
import com.sobalab.mymuse.api.model.enums.CountryCode;
import com.sobalab.mymuse.api.model.enums.FeedSort;
import com.sobalab.mymuse.api.model.enums.FeedView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedService {
    private final ConcertService concertService;

    /**
     * Applies feed defaults and range rules, then delegates a typed query to the concert
     * service.
     */
    public CursorResponse<ConcertDto> getTrendingFeed(FeedView view, String month, CountryCode nation, FeedSort sort, String cursor,
            Integer limit) {
        int pageSize = limit == null ? 20 : limit;
        if (pageSize < 1 || pageSize > 100) {
            throw badRequest("Limit must be between 1 and 100");
        }
        if (view == FeedView.CALENDAR && (month == null || month.isBlank())) {
            throw badRequest("Calendar view requires a month (yyyy-MM)");
        }
        FeedSort sorting = sort == null ? FeedSort.DDAY : sort;

        UUID cursorId = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                cursorId = UUID.fromString(cursor);
                if (!cursorId.toString().equalsIgnoreCase(cursor)) {
                    throw badRequest("Cursor must be a canonical concert UUID");
                }
            } catch (IllegalArgumentException e) {
                throw badRequest("Cursor must be a concert UUID");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now;
        LocalDateTime endDate = null;
        if (month != null && !month.isBlank()) {
            try {
                YearMonth ym = YearMonth.parse(month.strip());
                LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
                startDate = monthStart.isAfter(now) ? monthStart : now;
                endDate = ym.plusMonths(1).atDay(1).atStartOfDay();
            } catch (DateTimeException e) {
                throw badRequest("Month must be a valid yyyy-MM value");
            }
        }

        if (endDate != null && !startDate.isBefore(endDate)) {
            return new CursorResponse<>(List.of(), null, false);
        }
        List<ConcertDto> rows = concertService.getTrendingFeed(nation, startDate, endDate, sorting, cursorId,
                pageSize + 1);
        boolean hasNext = rows.size() > pageSize;
        List<ConcertDto> items = hasNext ? rows.subList(0, pageSize) : rows;
        String nextCursor = hasNext ? items.get(items.size() - 1).id() : null;
        return new CursorResponse<>(items, nextCursor, hasNext);
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
