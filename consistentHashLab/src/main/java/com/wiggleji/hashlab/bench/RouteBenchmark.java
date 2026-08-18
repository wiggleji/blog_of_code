package com.wiggleji.hashlab.bench;

import com.wiggleji.hashlab.HashRouter;
import com.wiggleji.hashlab.NodeId;
import com.wiggleji.hashlab.hash.Hashes;
import com.wiggleji.hashlab.hrw.RendezvousRouter;
import com.wiggleji.hashlab.jump.JumpRouter;
import com.wiggleji.hashlab.maglev.MaglevRouter;
import com.wiggleji.hashlab.ring.RingMode;
import com.wiggleji.hashlab.ring.RingRouter;
import com.wiggleji.hashlab.ring.TreeMapRingRouter;
import com.wiggleji.hashlab.slot.SlotTableRouter;
import com.wiggleji.hashlab.experiment.LabKt;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 실험 C - 조회 속도.
 *
 * 키 해시는 미리 계산해서 LongArray 로 넣어둔다. 우리가 재려는 건 "라우팅"이지 "문자열 해싱"이 아니다.
 * (문자열 해싱 비용은 네 알고리즘이 똑같이 내는 세금이라 비교에 아무 정보도 주지 않는다.)
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgs = {"-Xmx2g"})
public class RouteBenchmark {

    @Param({"10", "100", "1000"})
    public int nodes;

    @Param({"ring_v160", "ring_v1000", "ring_treemap_v160", "hrw", "jump", "maglev_65537", "maglev_655373", "slot_1024"})
    public String algorithm;

    private HashRouter router;
    private long[] keys;
    private int mask;
    private int cursor;

    @Setup(Level.Trial)
    public void setup() {
        List<NodeId> cluster = new ArrayList<>(LabKt.cluster(nodes));
        switch (algorithm) {
            case "ring_v160" -> router = new RingRouter(cluster, 160, RingMode.MURMUR3);
            case "ring_v1000" -> router = new RingRouter(cluster, 1000, RingMode.MURMUR3);
            case "ring_treemap_v160" -> router = new TreeMapRingRouter(cluster, 160);
            case "hrw" -> router = new RendezvousRouter(cluster);
            case "jump" -> router = new JumpRouter(cluster);
            case "maglev_65537" -> router = new MaglevRouter(cluster, 65537);
            case "maglev_655373" -> router = new MaglevRouter(cluster, 655373);
            case "slot_1024" -> router = new SlotTableRouter(cluster, 1024);
            default -> throw new IllegalArgumentException(algorithm);
        }
        int n = 1 << 16;
        mask = n - 1;
        keys = new long[n];
        for (int i = 0; i < n; i++) {
            keys[i] = Hashes.INSTANCE.murmur3_64("user:" + i, 0);
        }
    }

    @Benchmark
    public NodeId route() {
        return router.routeHashed(keys[(cursor++) & mask]);
    }
}
