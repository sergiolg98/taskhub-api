package com.taskhub.task.domain.model;

/** Who is asking, from the point of view of tasks: an id and whether they are an administrator. */
public record Requester(Long userId, boolean admin) {
}
