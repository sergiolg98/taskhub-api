package com.taskhub.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// ownerId viaja en el body solo en la versión base: con JWT (clase 1) saldrá del token.
public record CreateTaskRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @NotNull Long ownerId) {
}
