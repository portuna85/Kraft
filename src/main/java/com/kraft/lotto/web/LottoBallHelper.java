package com.kraft.lotto.web;

import org.springframework.stereotype.Component;

@Component("lottoBallHelper")
public class LottoBallHelper {

    // Ball color groups follow the official KRX scheme: 1–10, 11–20, 21–30, 31–40, 41–45.
    // This differs from SingleDecadeRule's "tens-digit" buckets (1–9, 10–19, …), which is intentional.
    public int colorGroup(int num) {
        return (num - 1) / 10 + 1;
    }
}
