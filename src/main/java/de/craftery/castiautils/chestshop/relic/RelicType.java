package de.craftery.castiautils.chestshop.relic;


import de.craftery.castiautils.CastiaUtils;
import lombok.Getter;

@Getter
public enum RelicType {
    MITHRIL(57519, 10000),
    LEGENDARY(57520, 2000),
    EPIC(57521, 200),
    RARE(57522, 0),
    COMMON(57523, 0);

    private final int charIntValue;
    private final float value;

    RelicType(int chatIntValue, float value) {
        this.charIntValue = chatIntValue;
        this.value = value;
    }

    public static RelicType of(int charIntValue) {
        for (RelicType relic : RelicType.values()) {
            if (relic.charIntValue == charIntValue) {
                return relic;
            }
        }
        CastiaUtils.LOGGER.error("Unrecognized RelicTypeIdentifier: " + charIntValue);
        return null;
    }
}
