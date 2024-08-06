package me.basiqueevangelist.pechkin.data;

import me.basiqueevangelist.pechkin.Pechkin;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class LeakyBucket {
    private Instant debtExpiryTime;

    public LeakyBucket() {
        this(Instant.now());
    }

    public LeakyBucket(Instant debtExpiryTime) {
        this.debtExpiryTime = debtExpiryTime;
    }

    public void addTime(int cost) {
        Instant now = Instant.now();
        if (debtExpiryTime.isBefore(now))
            debtExpiryTime = now.plus(cost, ChronoUnit.SECONDS);
        else
            debtExpiryTime = debtExpiryTime.plus(cost, ChronoUnit.SECONDS);
    }

    public boolean hasEnoughFor(int cost) {
        return Pechkin.CONFIG.getConfig().maxTimeDebt - Duration.between(Instant.now(), debtExpiryTime).get(ChronoUnit.SECONDS) > cost;
    }

    public boolean isFull() {
        return debtExpiryTime.isBefore(Instant.now());
    }

    public void fromTag(NbtCompound tag) {
        if (tag.contains("LeakyBucketFillTime", NbtElement.LONG_TYPE)) {
            debtExpiryTime = Instant.ofEpochMilli(tag.getLong("LeakyBucketFillTime"));
        }
    }

    public void toTag(NbtCompound tag) {
        if (debtExpiryTime.isAfter(Instant.now())) {
            tag.putLong("LeakyBucketFillTime", debtExpiryTime.toEpochMilli());
        }
    }
}
