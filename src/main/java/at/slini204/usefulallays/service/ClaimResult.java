package at.slini204.usefulallays.service;

public record ClaimResult(Status status, int limit) {

    public enum Status {
        CLAIMED,
        DISABLED,
        ALREADY_OWNED_BY_YOU,
        ALREADY_OWNED_BY_OTHER,
        LIMIT_REACHED
    }
}
