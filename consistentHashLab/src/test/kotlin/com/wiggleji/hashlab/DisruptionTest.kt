package com.wiggleji.hashlab

import com.wiggleji.hashlab.hash.Hashes
import com.wiggleji.hashlab.hrw.RendezvousRouter
import com.wiggleji.hashlab.maglev.MaglevRouter
import com.wiggleji.hashlab.ring.RingRouter
import com.wiggleji.hashlab.slot.SlotTableRouter
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * "안정 해시"라는 이름이 실제로 약속하는 성질을 문장으로 못 박는 테스트.
 * 테스트 이름이 곧 포스팅 소제목이다.
 */
class DisruptionTest {

    private val nodes = (0 until 20).map { NodeId("10.0.0.$it:11211") }
    private val victim = nodes[7]
    private val keys = (0 until 200_000).map { Hashes.murmur3_64("user:$it") }

    private fun disruption(router: HashRouter, mutate: (MutableTopology) -> Unit): Pair<Int, Int> {
        val before = keys.map { router.routeHashed(it) }
        mutate(router as MutableTopology)
        val after = keys.map { router.routeHashed(it) }
        var movedFromVictim = 0
        var movedFromOthers = 0
        for (i in keys.indices) {
            if (before[i] == after[i]) continue
            if (before[i] == victim) movedFromVictim++ else movedFromOthers++
        }
        return movedFromVictim to movedFromOthers
    }

    @Test
    @DisplayName("ring: removing a node moves only that node's keys")
    fun ringMovesOnlyVictimKeys() {
        val (fromVictim, fromOthers) = disruption(RingRouter(nodes, vnodes = 160)) { it.remove(victim) }
        assertEquals(0, fromOthers, "무관한 노드의 키가 움직이면 안 된다")
        assertTrue(fromVictim > 0)
    }

    @Test
    @DisplayName("rendezvous: removing a node moves only that node's keys")
    fun hrwMovesOnlyVictimKeys() {
        val (fromVictim, fromOthers) = disruption(RendezvousRouter(nodes)) { it.remove(victim) }
        assertEquals(0, fromOthers)
        assertTrue(fromVictim > 0)
    }

    @Test
    @DisplayName("slot table: removing a node moves only that node's slots")
    fun slotTableMovesOnlyVictimKeys() {
        val (fromVictim, fromOthers) = disruption(SlotTableRouter(nodes, slots = 1024)) { it.remove(victim) }
        assertEquals(0, fromOthers)
        assertTrue(fromVictim > 0)
    }

    @Test
    @DisplayName("maglev: removing a node ALSO moves keys that had nothing to do with it")
    fun maglevMovesInnocentKeys() {
        val (fromVictim, fromOthers) = disruption(MaglevRouter(nodes, m = 65537)) { it.remove(victim) }
        assertTrue(fromVictim > 0)
        assertTrue(fromOthers > 0, "Maglev 는 테이블을 다시 채우므로 초과 이동이 반드시 생긴다")
        println("maglev(M=65537, N=20): victim=$fromVictim innocent=$fromOthers")
    }

    @Test
    @DisplayName("maglev: a bigger table shrinks the collateral damage")
    fun biggerTableReducesExcess() {
        val small = disruption(MaglevRouter(nodes, m = 65537)) { it.remove(victim) }.second
        val big = disruption(MaglevRouter(nodes, m = 655373)) { it.remove(victim) }.second
        println("maglev innocent moves: M=65537 -> $small, M=655373 -> $big")
        assertTrue(big < small, "M 을 키우면 초과 이동이 줄어야 한다 ($big < $small)")
    }
}
