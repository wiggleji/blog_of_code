package com.wiggleji.hashlab.bench;

import com.wiggleji.hashlab.hash.Hashes;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * 기준선. 라우팅 비용을 읽을 때 "키 해싱 한 번"이 몇 ns 인지 알고 있어야 한다.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgs = {"-Xmx2g"})
public class HashBenchmark {

    private String[] keys;
    private int mask;
    private int cursor;

    @Setup(Level.Trial)
    public void setup() {
        int n = 1 << 16;
        mask = n - 1;
        keys = new String[n];
        for (int i = 0; i < n; i++) keys[i] = "user:" + i;
    }

    @Benchmark
    public long murmur3_64() {
        return Hashes.INSTANCE.murmur3_64(keys[(cursor++) & mask], 0);
    }

    @Benchmark
    public long ketamaMd5() {
        return Hashes.INSTANCE.ketamaHash(keys[(cursor++) & mask]);
    }
}
