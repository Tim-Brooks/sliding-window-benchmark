/*
 * Copyright 2014 Timothy Brooks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.uncontended.precipice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class CircularBuffer {
    private final AtomicReferenceArray<Slot> metrics;
    private final int totalSlots;
    private final int mask;
    private final long startTime;
    private final int millisecondsPerSlot;

    public CircularBuffer(int slotsToTrack, long resolution, TimeUnit slotUnit) {

        long millisecondsPerSlot = TimeUnit.MILLISECONDS.convert(resolution, slotUnit);
        if (millisecondsPerSlot < 0) {
            throw new IllegalArgumentException(String.format("Too low of resolution. %s milliseconds per slot is the " +
                    "lowest valid resolution", Integer.MAX_VALUE));
        } else if (100 > millisecondsPerSlot) {
            throw new IllegalArgumentException(String.format("Too low of resolution: [%s milliseconds]. 100 " +
                    "milliseconds is the minimum resolution.", millisecondsPerSlot));
        }


        this.millisecondsPerSlot = (int) millisecondsPerSlot;
        this.startTime = System.currentTimeMillis();
        this.totalSlots = slotsToTrack;

        int arraySlot = nextPositivePowerOfTwo(slotsToTrack);
        this.mask = arraySlot - 1;
        this.metrics = new AtomicReferenceArray<>(arraySlot);

        for (int i = 0; i < arraySlot; ++i) {
            metrics.set(i, new Slot(i));
        }
    }

    public void incrementMetricCount(Metric metric) {
        long currentTime = System.currentTimeMillis();
        int absoluteSlot = currentAbsoluteSlot(currentTime);
        int relativeSlot = absoluteSlot & mask;
        Slot slot = metrics.get(relativeSlot);

        if (slot.getAbsoluteSlot() == absoluteSlot) {
            slot.incrementMetric(metric);
        } else {
            for (; ; ) {
                slot = metrics.get(relativeSlot);
                if (slot.getAbsoluteSlot() == absoluteSlot) {
                    slot.incrementMetric(metric);
                    break;
                } else {
                    Slot newSlot = new Slot(absoluteSlot);
                    if (metrics.compareAndSet(relativeSlot, slot, newSlot)) {
                        newSlot.incrementMetric(metric);
                        break;
                    }
                }
            }
        }
    }

    public long getMetricCountForTimePeriod(Metric metric, long timePeriod, TimeUnit timeUnit) {
        int slots = convertToSlots(timePeriod, timeUnit);
        long currentTime = System.currentTimeMillis();

        int absoluteSlot = currentAbsoluteSlot(currentTime);
        int startSlot = 1 + absoluteSlot - slots;
        int adjustedStartSlot = startSlot >= 0 ? startSlot : 0;

        long count = 0;
        for (int i = adjustedStartSlot; i <= absoluteSlot; ++i) {
            int relativeSlot = i & mask;
            Slot slot = metrics.get(relativeSlot);
            if (slot.getAbsoluteSlot() == i) {
                count = count + slot.getMetric(metric).longValue();
            }
        }

        return count;
    }

    private int currentAbsoluteSlot(long currentTime) {
        return ((int) (currentTime - startTime)) / millisecondsPerSlot;
    }

    private int convertToSlots(long timePeriod, TimeUnit timeUnit) {
        long longSlots = TimeUnit.MILLISECONDS.convert(timePeriod, timeUnit) / millisecondsPerSlot;

        if (longSlots > totalSlots) {
            String message = String.format("Slots greater than slots tracked: [Tracked: %s, Argument: %s]",
                    totalSlots, longSlots);
            throw new IllegalArgumentException(message);
        } else if (longSlots <= 0) {
            String message = String.format("Slots must be greater than 0. [Argument: %s]", longSlots);
            throw new IllegalArgumentException(message);
        }
        return (int) longSlots;
    }

    private int nextPositivePowerOfTwo(int slotsToTrack) {
        return 1 << (32 - Integer.numberOfLeadingZeros(slotsToTrack - 1));
    }
}
