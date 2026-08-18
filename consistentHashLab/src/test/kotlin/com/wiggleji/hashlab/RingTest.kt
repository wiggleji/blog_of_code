package com.wiggleji.hashlab

import com.wiggleji.hashlab.hash.Hashes
import com.wiggleji.hashlab.ring.RingMode
import com.wiggleji.hashlab.ring.RingRouter
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RingTest {

    private val nodes = (0 until 20).map { NodeId("10.0.0.$it:11211") }

    @Test
    @DisplayName("more virtual nodes, flatter distribution - the 1/sqrt(V) curve")
    fun virtualNodesFlattenTheRing() {
        val keys = (0 until 200_000).map { Hashes.murmur3_64("user:$it") }
        var previous = Double.MAX_VALUE
        for (v in listOf(1, 10, 100, 1000)) {
            val r = RingRouter(nodes, vnodes = v)
            val counts = HashMap<NodeId, Int>()
            for (k in keys) counts.merge(r.routeHashed(k), 1, Int::plus)
            val mean = keys.size.toDouble() / nodes.size
            val cv = kotlin.math.sqrt(nodes.map { (counts[it] ?: 0) - mean }.sumOf { it * it } / nodes.size) / mean
            println("V=$v -> cv=%.4f (1/sqrt(V)=%.4f)".format(cv, 1.0 / kotlin.math.sqrt(v.toDouble())))
            assertTrue(cv < previous, "V 를 늘렸는데 분포가 나빠졌다 (V=$v)")
            previous = cv
        }
    }

    @Test
    @DisplayName("ketama mode puts 160 points per server, four per MD5 digest")
    fun ketamaLayout() {
        val r = RingRouter(nodes, vnodes = 160, mode = RingMode.KETAMA)
        assertEquals(nodes.size * 160, r.ringSize())
        // libketama 의 포인트는 unsigned 32비트 범위 안에 있다
        val d = Hashes.md5("10.0.0.1:11211-0")
        for (q in 0 until 4) assertTrue(Hashes.ketamaHash(d, q) in 0..0xffffffffL)
    }

    @Test
    @DisplayName("a key lands on the first point clockwise, wrapping past the largest one")
    fun wrapsAround() {
        val r = RingRouter(nodes, vnodes = 8)
        // 링 최댓값보다 큰 해시는 반드시 첫 점으로 감싸 돌아간다
        assertEquals(r.routeHashed(Long.MIN_VALUE), r.routeHashed(Long.MAX_VALUE))
    }
}
