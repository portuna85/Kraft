package com.kraft.lotto.web;

import org.springframework.stereotype.Component;

@Component("lottoBallHelper")
public class LottoBallHelper {

    public int colorGroup(int num) {
        return (num - 1) / 10 + 1;
    }
}
