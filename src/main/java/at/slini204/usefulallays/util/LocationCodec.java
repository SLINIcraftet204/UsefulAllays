package at.slini204.usefulallays.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

public final class LocationCodec {

    private LocationCodec() {
    }

    public static String encode(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }

        return String.join(";",
                location.getWorld().getName(),
                Double.toString(location.getX()),
                Double.toString(location.getY()),
                Double.toString(location.getZ()),
                Float.toString(location.getYaw()),
                Float.toString(location.getPitch())
        );
    }

    public static Optional<Location> decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String[] parts = raw.split(";");
        if (parts.length < 4) {
            return Optional.empty();
        }

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return Optional.empty();
        }

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length >= 5 ? Float.parseFloat(parts[4]) : 0.0F;
            float pitch = parts.length >= 6 ? Float.parseFloat(parts[5]) : 0.0F;
            return Optional.of(new Location(world, x, y, z, yaw, pitch));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static String readable(Location location) {
        if (location == null || location.getWorld() == null) {
            return "-";
        }
        return location.getWorld().getName()
                + " "
                + location.getBlockX()
                + ", "
                + location.getBlockY()
                + ", "
                + location.getBlockZ();
    }
}
