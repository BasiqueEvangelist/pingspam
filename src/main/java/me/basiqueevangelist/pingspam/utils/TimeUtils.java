package me.basiqueevangelist.pingspam.utils;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.time.Duration;
import java.time.Instant;

public final class TimeUtils {
    private TimeUtils() {

    }

    public static MutableText formatTime(Instant time) {
        var duration = Duration.between(time, Instant.now());

        StringBuilder b = new StringBuilder();
        if (duration.toDaysPart() > 0)
            b.append(duration.toDaysPart()).append("d");
        if (duration.toHoursPart() > 0)
            b.append(duration.toHoursPart()).append("h");
        if (duration.toMinutesPart() > 0)
            b.append(duration.toMinutesPart()).append("m");
        if (duration.toSecondsPart() > 0)
            b.append(duration.toSecondsPart()).append("s");

        return Text.literal(b.toString());
    }
}
