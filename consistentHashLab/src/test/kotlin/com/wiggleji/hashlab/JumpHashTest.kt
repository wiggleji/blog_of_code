package com.wiggleji.hashlab

import com.google.common.hash.Hashing
import com.wiggleji.hashlab.hash.Hashes
import com.wiggleji.hashlab.jump.JumpHash
import com.wiggleji.hashlab.jump.JumpRouter
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Random
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JumpHashTest {

    @Test
    @DisplayName("Guava's Hashing.consistentHash IS Jump - our implementation agrees bit for bit")
    fun agreesWithGuava() {
        val rnd = Random(1)
        repeat(20_000) {
            val key = rnd.nextLong()
            val buckets = 1 + rnd.nextInt(2000)
            assertEquals(
                Hashing.consistentHash(key, buckets),
                JumpHash.jump(key, buckets),
                "key=$key buckets=$buckets",
            )
        }
    }

    @Test
    @DisplayName("growing N to N+1 moves keys ONLY into the new bucket")
    fun growthIsMonotone() {
        val rnd = Random(2)
        repeat(50_000) {
            val key = rnd.nextLong()
            val n = 1 + rnd.nextInt(200)
            val a = JumpHash.jump(key, n)
            val b = JumpHash.jump(key, n + 1)
            assertTrue(b == a || b == n, "key moved sideways: $a -> $b (n=$n)")
        }
    }

    @Test
    @DisplayName("unsigned shift matters: using shr instead of ushr collapses the distribution")
    fun unsignedShiftMatters() {
        // 부호 있는 시프트로 잘못 구현한 버전
        fun broken(key: Long, buckets: Int): Int {
            var k = key; var b = -1L; var j = 0L
            while (j < buckets) {
                b = j
                k = k * 2862933555777941757L + 1
                j = ((b + 1) * ((1L shl 31).toDouble() / ((k shr 33) + 1).toDouble())).toLong()
            }
            return b.toInt()
        }
        val n = 16
        val good = IntArray(n); val bad = IntArray(n)
        val rnd = Random(3)
        repeat(200_000) {
            val k = rnd.nextLong()
            good[JumpHash.jump(k, n)]++
            bad[broken(k, n).coerceIn(0, n - 1)]++
        }
        val mean = 200_000.0 / n
        val goodPeak = good.max() / mean
        val badPeak = bad.max() / mean
        assertTrue(goodPeak < 1.05, "정상 구현의 peak/mean=$goodPeak")
        assertTrue(badPeak > 1.5, "잘못된 구현은 눈에 띄게 쏠려야 한다. peak/mean=$badPeak")
    }

    @Test
    @DisplayName("distribution is indistinguishable from a perfectly random assignment")
    fun distributionHitsTheNoiseFloor() {
        // 1M 키를 1000개 버킷에 뿌리면 버킷당 평균 1000개다.
        // 완벽하게 무작위로 뿌려도 표준편차는 sqrt(1000)≈31.6 → cv≈3.16% 가 나온다.
        // 이건 알고리즘의 결함이 아니라 표본 잡음의 바닥(noise floor)이다.
        // "Jump 는 분포가 완벽하다"의 정확한 뜻: 이 바닥보다 나빠지지 않는다.
        for (n in listOf(10, 100, 1000)) {
            val counts = IntArray(n)
            for (i in 0 until 1_000_000) counts[JumpHash.jump(Hashes.murmur3_64("user:$i"), n)]++
            val mean = 1_000_000.0 / n
            val floor = 1.0 / sqrt(mean) // 다항분포의 이론 CV
            val cv = sqrt(counts.sumOf { (it - mean) * (it - mean) } / n) / mean
            // 버킷이 적으면 CV 추정치 자체가 흔들린다(상대 표준편차 ≈ 1/sqrt(2(n-1))). 3시그마 허용.
            val tolerance = 1.0 + 3.0 / sqrt(2.0 * (n - 1))
            println("jump N=$n -> cv=%.5f (noise floor=%.5f, ratio=%.2f, tol=%.2f)".format(cv, floor, cv / floor, tolerance))
            assertTrue(cv < floor * tolerance, "N=$n: cv=$cv 가 잡음 바닥 $floor 을 유의미하게 넘었다")
            assertEquals(1_000_000, counts.sum())
        }
    }

    @Test
    @DisplayName("Jump cannot remove an arbitrary node - only the last bucket")
    fun cannotRemoveArbitraryNode() {
        val nodes = (0 until 10).map { NodeId("node-$it") }
        val router = JumpRouter(nodes)
        val keys = (0 until 100_000).map { Hashes.murmur3_64("user:$it") }
        val before = keys.map { router.routeHashed(it) }

        // 허용된 축소: 마지막 버킷을 뗀다 → 그 노드의 키만 움직인다
        val shrunk = router.shrinkLast()
        val movedByShrink = keys.indices.count { before[it] != shrunk.routeHashed(keys[it]) }
        assertTrue(movedByShrink.toDouble() / keys.size < 0.11, "1/10 근처여야 한다: $movedByShrink")

        // 금지된 축소: 가운데(5번) 노드가 죽어서 뒤를 당겨 번호를 다시 매겼다
        val renumbered = JumpRouter(nodes.filterIndexed { i, _ -> i != 5 })
        val movedByRenumber = keys.indices.count { before[it] != renumbered.routeHashed(keys[it]) }
        assertTrue(
            movedByRenumber.toDouble() / keys.size > 0.4,
            "번호를 다시 매기면 재앙이 나야 한다. moved=${movedByRenumber.toDouble() / keys.size}",
        )
    }
}
