package de.craftery.castiautils.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefetchPeriod {
    OFF(Integer.MAX_VALUE),
    EVERY_MINUTE(60),
    EVERY_10_MINUTES(60*10),
    EVERY_HOUR(60*60);

    private final int seconds;
}
