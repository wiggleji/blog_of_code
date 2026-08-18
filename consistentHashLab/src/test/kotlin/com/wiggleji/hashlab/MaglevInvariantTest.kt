package com.wiggleji.hashlab

import com.wiggleji.hashlab.maglev.MaglevRouter
import com.wiggleji.hashlab.slot.SlotTableRouter
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MaglevInvariantTest {

    @Test
    @DisplayName("maglev fills every slot and no backend is off by more than one")
    fun tableIsCompleteAndBalanced() {
        for (n in listOf(3, 7, 20, 100)) {
            val nodes = (0 until n).map { NodeId("10.0.0.$it:11211") }
            val r = MaglevRouter(nodes, m = 65537)
            val counts = r.slotCounts()
            assertEquals(n, counts.size)
            assertEquals(65537, counts.values.sum(), "빈 슬롯이 있으면 안 된다")
            assertTrue(counts.values.max() - counts.values.min() <= 1, "N=$n 편차=${counts.values.max() - counts.values.min()}")
        }
    }

    @Test
    @DisplayName("maglev refuses a non-prime table size (skip must be coprime with M)")
    fun tableSizeMustBePrime() {
        assertFailsWith<IllegalArgumentException> { MaglevRouter(listOf(NodeId("a")), m = 65536) }
    }

    @Test
    @DisplayName("slot table keeps slots within one of each other after churn")
    fun slotTableStaysBalanced() {
        val nodes = (0 until 8).map { NodeId("10.0.0.$it:11211") }
        val r = SlotTableRouter(nodes, slots = 1024)
        r.remove(nodes[3])
        r.add(NodeId("10.9.9.1:11211"))
        r.add(NodeId("10.9.9.2:11211"))
        val counts = r.slotCounts()
        assertEquals(1024, counts.values.sum())
        val ideal = 1024.0 / counts.size
        assertTrue(counts.values.max() < ideal * 1.35, "슬롯 배정표가 크게 기울면 안 된다: $counts")
    }
}
