package at.slini204.usefulallays.model;

import java.util.Locale;

public enum AllayMode {
    FOLLOW,
    STAY,
    COLLECT_AROUND_OWNER,
    COLLECT_AROUND_HOME,
    PASSIVE;

    public AllayMode next() {
        AllayMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static AllayMode from(String value) {
        if (value == null || value.isBlank()) {
            return FOLLOW;
        }
        try {
            return AllayMode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return FOLLOW;
        }
    }
}
