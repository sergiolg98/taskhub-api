package com.taskhub.task.domain.model;

/** What the task area knows about a user. It is its own copy of the contract: it never imports the auth area. */
public record UserSummary(Long id, String name, String role) {
}
