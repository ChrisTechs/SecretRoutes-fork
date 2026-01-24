package xyz.yourboykyle.secretroutes.routes.data;

public enum SecretCategory {
    CHEST,
    LEVER,
    BAT,
    ITEM,
    WITHER,
    SUPERBOOM,
    ENTRANCE,
    STONK,
    FAIRYSOUL,
    UNKNOWN;

    public static SecretCategory fromString(String str) {
        if (str == null) return UNKNOWN;
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            String s = str.toLowerCase();

            // Just do this for now
            if (s.contains("interact")) return CHEST;

            if (s.contains("chest")) return CHEST;
            if (s.contains("lever")) return LEVER;
            if (s.contains("bat")) return BAT;
            if (s.contains("item")) return ITEM;
            if (s.contains("wither")) return WITHER;

            return UNKNOWN;
        }
    }

    public boolean isRealSecret() {
        return this == CHEST || this == BAT || this == ITEM || this == WITHER;
    }
}