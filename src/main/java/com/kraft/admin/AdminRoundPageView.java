package com.kraft.admin;

import java.util.List;

public record AdminRoundPageView(
        List<AdminRoundView> items,
        int page,
        long totalElements,
        int totalPages
) {}
