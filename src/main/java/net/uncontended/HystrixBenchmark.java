package net.uncontended;

import net.uncontended.hystrix.HystrixRollingNumber;
import net.uncontended.hystrix.HystrixRollingNumberEvent;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class HystrixBenchmark {

    private HystrixRollingNumber rollingNumber;

    @Setup
    public void prepare() {
        this.rollingNumber = new HystrixRollingNumber(5 * 1000, 5);
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void testIncrement() {
        rollingNumber.increment(HystrixRollingNumberEvent.SUCCESS);
    }
}
