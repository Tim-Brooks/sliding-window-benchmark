package net.uncontended;

import net.uncontended.codahale.SlidingTimeWindowReservoir;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class CodaHaleBenchmark {

    private final SlidingTimeWindowReservoir window;

    public CodaHaleBenchmark() {
        this.window = new SlidingTimeWindowReservoir(60, TimeUnit.SECONDS);
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void testIncrement() {
        window.update(1);
    }
}
