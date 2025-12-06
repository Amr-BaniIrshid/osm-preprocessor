package osm.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration controlling what we keep from OSM for car routing.
 */
public class CarFilterConfig {
    private final Set<String> allowedHighways;
    private final Set<String> blockedAccessValues;
    private final Map<String, Integer> defaultSpeedsKph;
    private final int fallbackSpeedKph;

    public CarFilterConfig(
            Set<String> allowedHighways,
            Set<String> blockedAccessValues,
            Map<String, Integer> defaultSpeedsKph,
            int fallbackSpeedKph) {
        this.allowedHighways = Set.copyOf(allowedHighways);
        this.blockedAccessValues = Set.copyOf(blockedAccessValues);
        this.defaultSpeedsKph = Map.copyOf(defaultSpeedsKph);
        this.fallbackSpeedKph = fallbackSpeedKph;
    }

    /**
     * Reasonable defaults for a first production-style car-only filter.
     *
     * You can tune these values later per country or legal requirements.
     */
    public static CarFilterConfig defaultConfig() {
        Set<String> allowedHighways = Set.of(
                "motorway", "motorway_link",
                "trunk", "trunk_link",
                "primary", "primary_link",
                "secondary", "secondary_link",
                "tertiary", "tertiary_link",
                "residential", "living_street",
                "service", "unclassified"
        );

        Set<String> blockedAccessValues = Set.of("no", "private");

        Map<String, Integer> defaultSpeeds = new HashMap<>();
        defaultSpeeds.put("motorway", 110);
        defaultSpeeds.put("motorway_link", 70);
        defaultSpeeds.put("trunk", 90);
        defaultSpeeds.put("trunk_link", 70);
        defaultSpeeds.put("primary", 80);
        defaultSpeeds.put("primary_link", 60);
        defaultSpeeds.put("secondary", 70);
        defaultSpeeds.put("secondary_link", 60);
        defaultSpeeds.put("tertiary", 60);
        defaultSpeeds.put("tertiary_link", 50);
        defaultSpeeds.put("residential", 50);
        defaultSpeeds.put("living_street", 30);
        defaultSpeeds.put("service", 30);
        defaultSpeeds.put("unclassified", 50);

        return new CarFilterConfig(
                allowedHighways,
                blockedAccessValues,
                defaultSpeeds,
                50
        );
    }

    public boolean isAllowedHighway(String highway) {
        return allowedHighways.contains(highway);
    }

    public boolean isBlockedAccess(String value) {
        return blockedAccessValues.contains(value);
    }

    public int getDefaultSpeed(String highway) {
        return defaultSpeedsKph.getOrDefault(highway, fallbackSpeedKph);
    }

    public int getFallbackSpeedKph() {
        return fallbackSpeedKph;
    }
}
