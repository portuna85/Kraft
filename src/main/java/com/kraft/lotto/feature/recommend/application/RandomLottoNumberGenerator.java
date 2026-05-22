package com.kraft.lotto.feature.recommend.application;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"},
        justification = "Injected Random supports deterministic tests and constructor validates non-null contract")
public class RandomLottoNumberGenerator implements LottoNumberGenerator {

    private final Random random;

    public RandomLottoNumberGenerator(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = random;
    }

    @Override
    public LottoCombination generate() {
        Set<Integer> picked = new LinkedHashSet<>(LottoCombination.SIZE);
        while (picked.size() < LottoCombination.SIZE) {
            picked.add(random.nextInt(LottoCombination.MAX_NUMBER) + LottoCombination.MIN_NUMBER);
        }
        return new LottoCombination(new ArrayList<>(picked));
    }
}
