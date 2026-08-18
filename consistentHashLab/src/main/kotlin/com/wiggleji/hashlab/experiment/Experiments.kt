package com.wiggleji.hashlab.experiment

import com.wiggleji.hashlab.HashRouter
import com.wiggleji.hashlab.MutableTopology
import com.wiggleji.hashlab.NodeId
import com.wiggleji.hashlab.jump.JumpRouter

private const val KEY_COUNT = 1_000_000
private val NODE_COUNTS = listOf(10, 100, 1000)
private val OUT = System.getProperty("results.dir", "results")

private fun registry(all: Collection<NodeId>): Map<NodeId, Int> =
    all.distinct().sorted().withIndex().associate { (i, n) -> n to i }

private fun <T> timedMs(block: () -> T): Pair<T, Double> {
    val t0 = System.nanoTime()
    val r = block()
    return r to (System.nanoTime() - t0) / 1e6
}

/** 실험 A — 분포 균일성. 노드마다 키가 몇 개씩 떨어지는가. */
private fun experimentA(keysets: Map<String, Array<String>>) {
    val csv = Csv(CSV_HEADER)
    for (n in NODE_COUNTS) {
        val nodes = cluster(n)
        val index = registry(nodes)
        val algos = buildList {
            listOf(1, 10, 100, 160, 1000).forEach { add(Algos.ring(it)) }
            add(Algos.ketama(160))
            add(Algos.hrw())
            add(Algos.jump())
            add(Algos.maglev(65537))
            add(Algos.maglev(655373))
            add(Algos.slots(1024))
        }
        for (algo in algos) {
            val router = algo.build(nodes)
            for ((ksName, keys) in keysets) {
                val hashes = Keys.hashed(keys, algo.hashKind)
                val s = spreadOf(assign(router, hashes, index), n)
                // 잡음 바닥: 완벽하게 무작위로 뿌려도 다항분포 때문에 이만큼은 흔들린다.
                // 이 값보다 낮은 CV 는 나올 수 없고, 이 값에 붙었으면 "완벽"이라는 뜻이다.
                val floor = 1.0 / kotlin.math.sqrt(KEY_COUNT.toDouble() / n)
                csv.row("A", algo.name, n, algo.param, ksName, "cv", s.cv)
                csv.row("A", algo.name, n, algo.param, ksName, "cv_noise_floor", floor)
                csv.row("A", algo.name, n, algo.param, ksName, "cv_over_floor", s.cv / floor)
                csv.row("A", algo.name, n, algo.param, ksName, "peak_over_mean", s.peakOverMean)
                csv.row("A", algo.name, n, algo.param, ksName, "min_over_mean", s.minOverMean)
            }
            println("  A: n=$n ${algo.name}(${algo.param}) done")
        }
    }
    csv.writeTo("$OUT/a_distribution.csv")
}

/** 실험 B — 재배치율. 노드 한 대가 빠지면 몇 %의 키가 이사하는가. */
private fun experimentB(keys: Array<String>) {
    val csv = Csv(CSV_HEADER)
    for (n in NODE_COUNTS) {
        val nodes = cluster(n)
        val extra = newcomer()
        val index = registry(nodes + extra)
        val victim = nodes[n / 2] // "하필 가운데 노드가 죽었다"
        val theoreticalRemove = 1.0 / n
        val theoreticalAdd = 1.0 / (n + 1)

        val algos = listOf(
            Algos.ring(160), Algos.ketama(160), Algos.hrw(),
            Algos.maglev(65537), Algos.maglev(655373), Algos.slots(1024),
        )
        for (algo in algos) {
            val hashes = Keys.hashed(keys, algo.hashKind)

            // 제거
            val r1 = algo.build(nodes)
            val before = assign(r1, hashes, index)
            (r1 as MutableTopology).remove(victim)
            val afterRemove = movedFraction(before, assign(r1, hashes, index))
            csv.row("B", algo.name, n, algo.param, "user", "moved_on_remove", afterRemove)
            csv.row("B", algo.name, n, algo.param, "user", "remove_over_theoretical", afterRemove / theoreticalRemove)

            // 추가
            val r2 = algo.build(nodes)
            val before2 = assign(r2, hashes, index)
            (r2 as MutableTopology).add(extra)
            val afterAdd = movedFraction(before2, assign(r2, hashes, index))
            csv.row("B", algo.name, n, algo.param, "user", "moved_on_add", afterAdd)
            csv.row("B", algo.name, n, algo.param, "user", "add_over_theoretical", afterAdd / theoreticalAdd)

            // 연쇄 장애: 3대가 차례로 죽는다
            val r3 = algo.build(nodes)
            val start = assign(r3, hashes, index)
            var cascade = 0.0
            for (k in 0 until 3) {
                (r3 as MutableTopology).remove(nodes[n / 2 + k])
                cascade = movedFraction(start, assign(r3, hashes, index))
            }
            csv.row("B", algo.name, n, algo.param, "user", "moved_on_cascade3", cascade)
            csv.row("B", algo.name, n, algo.param, "user", "cascade3_over_theoretical", cascade / (3.0 / n))
            println("  B: n=$n ${algo.name}(${algo.param}) remove=${"%.4f".format(afterRemove)}")
        }

        // Jump 는 MutableTopology 가 아니다. 할 수 있는 것과 없는 것을 따로 잰다.
        run {
            val hashes = Keys.hashed(keys, HashKind.MURMUR3)
            val full = JumpRouter(nodes.sorted())
            val before = assign(full, hashes, index)

            val shrunk = full.shrinkLast() // 허용된 유일한 축소
            val movedLast = movedFraction(before, assign(shrunk, hashes, index))
            csv.row("B", "jump", n, 0, "user", "moved_on_remove_last_bucket", movedLast)
            csv.row("B", "jump", n, 0, "user", "remove_last_over_theoretical", movedLast / theoreticalRemove)

            // 금지된 연산을 억지로 하면: 가운데 노드를 빼고 뒤를 당겨서 번호를 다시 매긴다
            val renumbered = JumpRouter(nodes.sorted().filter { it != victim })
            val movedMiddle = movedFraction(before, assign(renumbered, hashes, index))
            csv.row("B", "jump", n, 0, "user", "moved_on_remove_middle", movedMiddle)
            csv.row("B", "jump", n, 0, "user", "remove_middle_over_theoretical", movedMiddle / theoreticalRemove)

            val grown = full.grow(extra)
            val movedAdd = movedFraction(before, assign(grown, hashes, index))
            csv.row("B", "jump", n, 0, "user", "moved_on_add", movedAdd)
            csv.row("B", "jump", n, 0, "user", "add_over_theoretical", movedAdd / theoreticalAdd)
            println("  B: n=$n jump last=${"%.4f".format(movedLast)} middle=${"%.4f".format(movedMiddle)}")
        }
    }
    csv.writeTo("$OUT/b_rebalance.csv")
}

/** 실험 D — 구축 비용과 메모리. 조회가 빨라도 갱신이 비싸면 운영이 힘들다. */
private fun experimentD() {
    val csv = Csv(CSV_HEADER)
    for (n in NODE_COUNTS) {
        val nodes = cluster(n)
        val extra = newcomer()
        val algos = listOf(
            Algos.ring(160), Algos.ring(1000), Algos.ketama(160), Algos.hrw(),
            Algos.jump(), Algos.maglev(65537), Algos.maglev(655373), Algos.slots(1024),
        )
        for (algo in algos) {
            repeat(2) { algo.build(nodes) } // 워밍업
            val (router, buildMs) = timedMs { algo.build(nodes) }
            csv.row("D", algo.name, n, algo.param, "-", "build_ms", buildMs)

            if (router is MutableTopology) {
                val (_, addMs) = timedMs { router.add(extra) }
                csv.row("D", algo.name, n, algo.param, "-", "add_node_ms", addMs)
            }
            csv.row("D", algo.name, n, algo.param, "-", "retained_bytes", footprint(algo.build(nodes)).toDouble())
            println("  D: n=$n ${algo.name}(${algo.param}) build=${"%.2f".format(buildMs)}ms")
        }
    }
    csv.writeTo("$OUT/d_cost.csv")
}

/** JOL 로 잰 객체 그래프 크기. 실패하면 -1. */
private fun footprint(o: Any): Long = try {
    org.openjdk.jol.info.GraphLayout.parseInstance(o).totalSize()
} catch (t: Throwable) {
    -1L
}

fun main(args: Array<String>) {
    val only = args.firstOrNull()?.uppercase()
    println("== consistent hashing lab: keys=$KEY_COUNT nodes=$NODE_COUNTS out=$OUT ==")

    if (only == null || only == "A" || only == "B") {
        val user = Keys.userKeys(KEY_COUNT)
        val uuid = Keys.uuidKeys(KEY_COUNT)
        if (only == null || only == "A") {
            println("-- experiment A: distribution --")
            experimentA(mapOf("user" to user, "uuid" to uuid))
        }
        if (only == null || only == "B") {
            println("-- experiment B: rebalance --")
            experimentB(user)
        }
    }
    if (only == null || only == "D") {
        println("-- experiment D: build cost / memory --")
        experimentD()
    }
    println("== done ==")
}
