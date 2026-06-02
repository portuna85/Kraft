package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.web.dto.NumberFrequencyDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record FrequencyViewModel(int number, long count, double percent, int rank) {

    public static List<FrequencyViewModel> from(List<NumberFrequencyDto> frequencies) {
        long max = Math.max(1L, frequencies.stream().mapToLong(NumberFrequencyDto::count).max().orElse(1L));
        List<NumberFrequencyDto> byCount = frequencies.stream()
                .sorted(Comparator.comparingLong(NumberFrequencyDto::count).reversed())
                .toList();
        Map<Integer, Integer> rankByNumber = new HashMap<>(byCount.size() * 2);
        for (int i = 0; i < byCount.size(); i++) {
            rankByNumber.put(byCount.get(i).number(), i + 1);
        }
        List<FrequencyViewModel> result = new ArrayList<>(frequencies.size());
        for (NumberFrequencyDto f : frequencies) {
            double percent = f.count() * 100.0 / max;
            int rank = rankByNumber.getOrDefault(f.number(), frequencies.size());
            result.add(new FrequencyViewModel(f.number(), f.count(), percent, rank));
        }
        return result;
    }
}
