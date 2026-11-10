package com.sobalab.mymuse.api.dto;

import java.util.List;

public record CursorResponse<T>(
    List<T> items,
    String nextCursor,
    boolean hasNext
) {}
