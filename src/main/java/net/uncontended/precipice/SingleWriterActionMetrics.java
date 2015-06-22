package net.uncontended.precipice;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Created by timbrooks on 11/10/14.
 */
public class SingleWriterActionMetrics {

    private final int totalSlots;
    private final AtomicReferenceArray<Slot> metrics;
    private final AtomicLong advanceSlotTimeInMillis;
    private final AtomicInteger slotNumber;


    public SingleWriterActionMetrics(int secondsToTrack) {
        this.totalSlots = secondsToTrack;
        this.metrics = new AtomicReferenceArray<>(secondsToTrack);
        for (int i = 0; i < secondsToTrack; ++i) {
            this.metrics.set(i, new Slot(i));
        }

        this.slotNumber = new AtomicInteger(0);
        this.advanceSlotTimeInMillis = new AtomicLong(System.currentTimeMillis() + 1000L);
    }

    public long getMetricForTimePeriod(int milliseconds, Metric metric) {
        int slotsBack = milliseconds / 1000;
        if (slotsBack > totalSlots) {
            throw new RuntimeException("That amount of time is not tracked.");
        } else if (slotsBack == totalSlots) {
            slotsBack--;
        }

        int slotsToAdvance = slotsToAdvance();
        if (slotsToAdvance > slotsBack) {
            return 0;
        }

        long count = 0;
        int currentSlot = slotNumber.get();
        for (int i = currentSlot - (slotsBack - slotsToAdvance); i <= currentSlot; ++i) {
            if (i < 0) {
                count = count + metrics.get(totalSlots + i).getMetric(metric).longValue();
            } else {
                count = count + metrics.get(i).getMetric(metric).longValue();
            }
        }

        return count;
    }

    public void reportActionResult(Metric metric) {
        int currentSlotNumber = slotNumber.get();
        int slotsToAdvance = slotsToAdvance();
        int slotNumber = advanceToCurrentSlot(currentSlotNumber, slotsToAdvance);

        metrics.get(slotNumber).incrementMetric(metric);
    }

    private int advanceToCurrentSlot(int currentSlotNumber, int slotsToAdvance) {
        if (slotsToAdvance != 0) {
            int newSlot = slotsToAdvance + currentSlotNumber;
            for (int i = currentSlotNumber + 1; i <= newSlot; ++i) {
                metrics.lazySet(i % totalSlots, new Slot(i));
            }
            int adjustedNewSlot = newSlot % totalSlots;
            this.slotNumber.lazySet(adjustedNewSlot);
            this.advanceSlotTimeInMillis.lazySet(advanceSlotTimeInMillis.get() + (1000 * slotsToAdvance));
            return adjustedNewSlot;
        }
        return currentSlotNumber;
    }

    private int slotsToAdvance() {
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp < advanceSlotTimeInMillis.get()) {
            return 0;
        }

        long advanceSlotTimeInMillis = this.advanceSlotTimeInMillis.get();
        return 1 + (int) ((currentTimestamp - advanceSlotTimeInMillis) / 1000);
    }
}
