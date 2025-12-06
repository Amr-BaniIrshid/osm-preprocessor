package osm.model;

/**
 * Canonical codes for turn restriction types we care about.
 */
public enum TurnRestrictionType {
    UNKNOWN(0),
    NO_LEFT_TURN(1),
    NO_RIGHT_TURN(2),
    NO_U_TURN(3),
    NO_STRAIGHT_ON(4),
    ONLY_RIGHT_TURN(5),
    ONLY_LEFT_TURN(6),
    ONLY_STRAIGHT_ON(7);

    private final int code;

    TurnRestrictionType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * Parse an OSM restriction value into a TurnRestrictionType.
     */
    public static TurnRestrictionType fromOsmValue(String value) {
        if (value == null || value.isEmpty()) {
            return UNKNOWN;
        }

        String v = value.trim().toLowerCase();

        switch (v) {
            case "no_left_turn":
                return NO_LEFT_TURN;
            case "no_right_turn":
                return NO_RIGHT_TURN;
            case "no_u_turn":
                return NO_U_TURN;
            case "no_straight_on":
                return NO_STRAIGHT_ON;
            case "only_right_turn":
                return ONLY_RIGHT_TURN;
            case "only_left_turn":
                return ONLY_LEFT_TURN;
            case "only_straight_on":
                return ONLY_STRAIGHT_ON;
            default:
                return UNKNOWN;
        }
    }
}