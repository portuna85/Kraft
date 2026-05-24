package com.kraft.lotto.feature.recommend.application;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

class ConstraintAwareLottoNumberGenerator implements LottoNumberGenerator {

    private static final int MAX_NUMBER = 45;
    private static final int SIZE = 6;

    private final int birthdayThreshold;
    private final int longRunThreshold;
    private final int decadeThreshold;
    private final int initialPickMaxAttempts;
    private final int fixupMaxAttempts;

    @SuppressFBWarnings(
            value = "CT_CONSTRUCTOR_THROW",
            justification = "Fail-fast constructor validation for immutable generator configuration."
    )
    ConstraintAwareLottoNumberGenerator(int birthdayThreshold, int longRunThreshold, int decadeThreshold) {
        this(birthdayThreshold, longRunThreshold, decadeThreshold, 10_000, 1_000);
    }

    @SuppressFBWarnings(
            value = "CT_CONSTRUCTOR_THROW",
            justification = "Fail-fast constructor validation for immutable generator configuration."
    )
    ConstraintAwareLottoNumberGenerator(int birthdayThreshold,
                                        int longRunThreshold,
                                        int decadeThreshold,
                                        int initialPickMaxAttempts,
                                        int fixupMaxAttempts) {
        if (birthdayThreshold < 1 || birthdayThreshold > MAX_NUMBER - 1) {
            throw new IllegalArgumentException("birthdayThreshold must be between 1 and 44");
        }
        if (longRunThreshold < 2 || longRunThreshold > SIZE) {
            throw new IllegalArgumentException("longRunThreshold must be between 2 and 6");
        }
        if (decadeThreshold < 3 || decadeThreshold > SIZE) {
            throw new IllegalArgumentException("decadeThreshold must be between 3 and 6");
        }
        if (initialPickMaxAttempts <= 0) {
            throw new IllegalArgumentException("initialPickMaxAttempts must be positive");
        }
        if (fixupMaxAttempts <= 0) {
            throw new IllegalArgumentException("fixupMaxAttempts must be positive");
        }
        this.birthdayThreshold = birthdayThreshold;
        this.longRunThreshold = longRunThreshold;
        this.decadeThreshold = decadeThreshold;
        this.initialPickMaxAttempts = initialPickMaxAttempts;
        this.fixupMaxAttempts = fixupMaxAttempts;
    }

    @Override
    public LottoCombination generate() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Set<Integer> selected = new TreeSet<>();
        int birthdayAboveCount = 0;
        int[] decadeBuckets = new int[5];
        int initialPickAttempts = 0;

        while (selected.size() < SIZE) {
            if (++initialPickAttempts > initialPickMaxAttempts) {
                throw new RecommendGenerationTimeoutException(
                        "initial pick exceeded max attempts",
                        RecommendGenerationTimeoutException.FailureReason.INITIAL_PICK_TIMEOUT
                );
            }
            int n = rng.nextInt(MAX_NUMBER) + 1;
            int bucket = bucketIndex(n);
            if (shouldSkipInitialPick(selected, birthdayAboveCount, decadeBuckets, n, bucket)) {
                continue;
            }

            selected.add(n);
            decadeBuckets[bucket]++;
            if (n > birthdayThreshold) {
                birthdayAboveCount++;
            }
        }

        List<Integer> numbers = new ArrayList<>(selected);
        int fixupAttempts = 0;
        while (hasLongRun(numbers, longRunThreshold)) {
            if (++fixupAttempts > fixupMaxAttempts) {
                throw new RecommendGenerationTimeoutException(
                        "fixup exceeded max attempts",
                        RecommendGenerationTimeoutException.FailureReason.FIXUP_TIMEOUT
                );
            }
            List<Integer> longRunIndices = findLongRunIndices(numbers, longRunThreshold);
            int replaceIndex;
            if (!longRunIndices.isEmpty()) {
                replaceIndex = longRunIndices.get(rng.nextInt(longRunIndices.size()));
            } else {
                replaceIndex = rng.nextInt(SIZE);
            }
            int old = numbers.get(replaceIndex);
            int newNumber = rng.nextInt(MAX_NUMBER) + 1;
            int oldBucket = bucketIndex(old);
            int newBucket = bucketIndex(newNumber);
            if (shouldSkipFixup(numbers, decadeBuckets, newNumber, newBucket)) {
                continue;
            }
            numbers.set(replaceIndex, newNumber);
            decadeBuckets[oldBucket]--;
            decadeBuckets[newBucket]++;
            numbers.sort(Integer::compareTo);
        }
        return new LottoCombination(numbers);
    }

    private static List<Integer> findLongRunIndices(List<Integer> numbers, int threshold) {
        if (threshold <= 1 || numbers.size() < threshold) {
            return Collections.emptyList();
        }
        List<Integer> indices = new ArrayList<>();
        int runStart = 0;
        int runLength = 1;
        for (int i = 1; i < numbers.size(); i++) {
            if (numbers.get(i) - numbers.get(i - 1) == 1) {
                runLength++;
                if (runLength >= threshold) {
                    for (int idx = runStart; idx <= i; idx++) {
                        if (!indices.contains(idx)) {
                            indices.add(idx);
                        }
                    }
                }
            } else {
                runStart = i;
                runLength = 1;
            }
        }
        return indices;
    }

    private static int bucketIndex(int n) {
        if (n <= 9) {
            return 0;
        }
        if (n <= 19) {
            return 1;
        }
        if (n <= 29) {
            return 2;
        }
        if (n <= 39) {
            return 3;
        }
        return 4;
    }

    private boolean shouldSkipInitialPick(Set<Integer> selected,
                                          int birthdayAboveCount,
                                          int[] decadeBuckets,
                                          int n,
                                          int bucket) {
        if (selected.contains(n)) {
            return true;
        }
        if (birthdayAboveCount == 0 && selected.size() == SIZE - 1 && n <= birthdayThreshold) {
            return true;
        }
        return exceedsDecadeThreshold(decadeBuckets, bucket);
    }

    private boolean shouldSkipFixup(List<Integer> numbers, int[] decadeBuckets, int newNumber, int newBucket) {
        if (numbers.contains(newNumber)) {
            return true;
        }
        return exceedsDecadeThreshold(decadeBuckets, newBucket);
    }

    private boolean exceedsDecadeThreshold(int[] decadeBuckets, int bucket) {
        return decadeBuckets[bucket] >= Math.max(1, decadeThreshold - 1);
    }

    private static boolean hasLongRun(List<Integer> numbers, int threshold) {
        int run = 1;
        for (int i = 1; i < numbers.size(); i++) {
            if (numbers.get(i) - numbers.get(i - 1) == 1) {
                run++;
                if (run >= threshold) {
                    return true;
                }
            } else {
                run = 1;
            }
        }
        return false;
    }
}
