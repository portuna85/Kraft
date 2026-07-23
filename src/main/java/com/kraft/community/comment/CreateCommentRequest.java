package com.kraft.community.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank @Size(max = 1000) String content,
        Long parentId
) {
}
