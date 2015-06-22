package net.uncontended;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Created by timbrooks on 6/20/15.
 */
public class EntryPoint {

    public static void main(String[] args) {
        Options opt = new OptionsBuilder()
                .include(HystrixBenchmark.class.getSimpleName())
                .threads(6)
                .forks(1)
                .build();

        try {
            new Runner(opt).run();
        } catch (RunnerException e) {
            e.printStackTrace();
        }
    }

}
