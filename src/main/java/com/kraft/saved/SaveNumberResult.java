package com.kraft.saved;

public record SaveNumberResult(
        SavedNumberResponse savedNumber,
        boolean created
) {
}
