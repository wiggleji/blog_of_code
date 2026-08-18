package com.wiggleji.hashlab

import com.wiggleji.hashlab.hash.Hashes
import com.wiggleji.hashlab.hrw.RendezvousRouter
import com.wiggleji.hashlab.jump.JumpRouter
import com.wiggleji.hashlab.maglev.MaglevRouter
import com.wiggleji.hashlab.ring.RingMode
import com.wiggleji.hashlab.ring.RingRouter
import com.wiggleji.hashlab.ring.TreeMapRingRouter
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * 라우터는 프로세스마다 따로 만들어진다. 같은 멤버십이면 같은 답이 나와야 캐시가 캐시 노릇을 한다.
 */
class DeterminismTest {

    private val nodes = (0 until 12).map { NodeId("10.0.0.$it:11211") }
    private val keys = (0 until 50_000).map { Hashes.murmur3_64("user:$it") }
    private val ketamaKeys = (0 until 50_000).map { Hashes.ketamaHash("user:$it") }

    private fun routeAll(r: HashRouter, ks: List<Long> = keys) = ks.map { r.routeHashed(it) }

    @Test
    @DisplayName("same membership, fresh instance, identical mapping")
    fun freshInstanceSameMapping() {
        assertEquals(routeAll(RingRouter(nodes)), routeAll(RingRouter(nodes.shuffled())))
        assertEquals(routeAll(RendezvousRouter(nodes)), routeAll(RendezvousRouter(nodes.shuffled())))
        assertEquals(routeAll(MaglevRouter(nodes)), routeAll(MaglevRouter(nodes.shuffled())))
        assertEquals(routeAll(JumpRouter(nodes.sorted())), routeAll(JumpRouter(nodes.shuffled().sorted())))
        assertEquals(
            routeAll(RingRouter(nodes, mode = RingMode.KETAMA), ketamaKeys),
            routeAll(RingRouter(nodes.shuffled(), mode = RingMode.KETAMA), ketamaKeys),
        )
    }

    @Test
    @DisplayName("mutating a router equals rebuilding it from the resulting membership")
    fun mutationEqualsRebuild() {
        val extra = NodeId("10.9.9.9:11211")
        listOf<Pair<HashRouter, HashRouter>>(
            RingRouter(nodes).also { it.add(extra) } to RingRouter(nodes + extra),
            RendezvousRouter(nodes).also { it.add(extra) } to RendezvousRouter(nodes + extra),
            MaglevRouter(nodes).also { it.add(extra) } to MaglevRouter(nodes + extra),
            RingRouter(nodes).also { it.remove(nodes[3]) } to RingRouter(nodes - nodes[3]),
            RendezvousRouter(nodes).also { it.remove(nodes[3]) } to RendezvousRouter(nodes - nodes[3]),
            MaglevRouter(nodes).also { it.remove(nodes[3]) } to MaglevRouter(nodes - nodes[3]),
        ).forEach { (mutated, rebuilt) -> assertEquals(routeAll(rebuilt), routeAll(mutated)) }
    }

    @Test
    @DisplayName("sorted array + binarySearch gives the same ring as the textbook TreeMap")
    fun arrayRingEqualsTreeMapRing() {
        assertEquals(routeAll(TreeMapRingRouter(nodes, vnodes = 160)), routeAll(RingRouter(nodes, vnodes = 160)))
    }
}
