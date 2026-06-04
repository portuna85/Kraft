package com.kraft.lotto.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@Controller
@RequiredArgsConstructor
public class FrequencyController {

    private static final String FREQUENCY_FRAGMENT = "fragments/frequency-card :: frequency-card";

    private final FrequencyModelSupport frequencyModelSupport;

    @GetMapping("/frequency")
    public String frequencyPage(
            @RequestParam(defaultValue = "0") @Min(0) @Max(200) int period,
            Model model
    ) {
        frequencyModelSupport.addFrequencyModel(period, model);
        return "frequency";
    }

    @GetMapping("/fragments/frequency")
    public String frequency(
            @RequestParam(defaultValue = "0") @Min(0) @Max(200) int period,
            Model model
    ) {
        frequencyModelSupport.addFrequencyModel(period, model);
        return FREQUENCY_FRAGMENT;
    }
}
