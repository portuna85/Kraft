package com.kraft.community.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20000) String content
) {
}
