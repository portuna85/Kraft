package com.kraft.community.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20000) String content,
        @NotNull Long expectedVersion
) {
}
