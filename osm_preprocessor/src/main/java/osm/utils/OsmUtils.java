package osm.utils;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import osm.config.CarFilterConfig;

import java.util.Collection;
import java.util.Optional;

/**
 * Utility methods for parsing OSM tags and determining way properties.
 */
public class OsmUtils {

    /**
     * Get a tag value from a collection of tags.
     */
    public static Optional<String> getTag(Collection<Tag> tags, String key) {
        return tags.stream()
                .filter(tag -> tag.getKey().equals(key))
                .map(Tag::getValue)
                .findFirst();
    }

    /**
     * Decide whether a way is usable for car routing.
     *
     * We check:
     *   - highway type is in allowed set
     *   - access/motor_vehicle tags don't explicitly block cars
     */
    public static boolean isCarWay(Collection<Tag> tags, CarFilterConfig cfg) {
        Optional<String> highway = getTag(tags, "highway");
        if (highway.isEmpty()) {
            return false;
        }

        if (!cfg.isAllowedHighway(highway.get())) {
            return false;
        }

        Optional<String> access = getTag(tags, "access");
        if (access.isPresent() && cfg.isBlockedAccess(access.get())) {
            return false;
        }

        Optional<String> motorVehicle = getTag(tags, "motor_vehicle");
        if (motorVehicle.isPresent() && cfg.isBlockedAccess(motorVehicle.get())) {
            return false;
        }

        return true;
    }

    /**
     * Parse maxspeed=* into an integer km/h.
     *
     * Handles simple numeric values like '50', '80', and a basic 'mph' case.
     * Falls back to a highway-based default or global fallback.
     */
    public static int parseMaxspeedKph(Collection<Tag> tags, CarFilterConfig cfg) {
        Optional<String> maxspeed = getTag(tags, "maxspeed");

        if (maxspeed.isPresent()) {
            String raw = maxspeed.get().trim().toLowerCase();

            // Common junk values that are not numeric
            if (raw.equals("signals") || raw.equals("variable") || raw.equals("none")) {
                // Fall through to default
            } else {
                StringBuilder num = new StringBuilder();
                for (char ch : raw.toCharArray()) {
                    if (Character.isDigit(ch)) {
                        num.append(ch);
                    } else {
                        break;
                    }
                }

                if (num.length() > 0) {
                    int speed = Integer.parseInt(num.toString());

                    // Very naive mph detection
                    if (raw.contains("mph")) {
                        speed = (int) Math.round(speed * 1.60934);
                    }

                    return Math.max(1, Math.min(speed, 255));
                }
            }
        }

        // Fallback to default based on highway type
        Optional<String> highway = getTag(tags, "highway");
        if (highway.isPresent()) {
            return cfg.getDefaultSpeed(highway.get());
        }

        return cfg.getFallbackSpeedKph();
    }

    /**
     * Determine oneway-ness.
     *
     * Returns:
     *   1 -> forward only
     *  -1 -> backward only (oneway=-1)
     *   0 -> bidirectional
     */
    public static int isOneway(Collection<Tag> tags) {
        Optional<String> oneway = getTag(tags, "oneway");

        if (oneway.isEmpty()) {
            return 0;
        }

        String raw = oneway.get().trim().toLowerCase();

        if (raw.equals("yes") || raw.equals("true") || raw.equals("1")) {
            return 1;
        }

        if (raw.equals("-1")) {
            return -1;
        }

        return 0;
    }
}