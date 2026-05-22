package at.slini204.usefulallays.model;

public record LevelSettings(
        int filterSlots,
        double pickupRadius,
        int carryStacks,
        double teleportDistance
) {
}
