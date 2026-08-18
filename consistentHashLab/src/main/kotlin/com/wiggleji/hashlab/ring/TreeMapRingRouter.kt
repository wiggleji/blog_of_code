package com.wiggleji.hashlab.ring

import com.wiggleji.hashlab.HashRouter
import com.wiggleji.hashlab.MutableTopology
import com.wiggleji.hashlab.NodeId
import com.wiggleji.hashlab.hash.Hashes
import java.util.TreeMap

/**
 * 링의 "교과서 구현". 자료구조만 다르고 매핑 결과는 [RingRouter] 와 같아야 한다.
 * 정렬된 LongArray + binarySearch 대비 얼마나 느린지 재보려고 남겨둔 대조군.
 */
class TreeMapRingRouter(
    seed: Collection<NodeId>,
    val vnodes: Int = 160,
) : HashRouter, MutableTopology {

    private val members = sortedSetOf<NodeId>().apply { addAll(seed) }
    private val ring = TreeMap<Long, NodeId>()

    init { rebuild() }

    override val nodes: List<NodeId> get() = members.toList()

    override fun routeHashed(keyHash: Long): NodeId =
        (ring.ceilingEntry(keyHash) ?: ring.firstEntry()).value

    override fun add(node: NodeId) { if (members.add(node)) rebuild() }
    override fun remove(node: NodeId) { if (members.remove(node)) rebuild() }

    private fun rebuild() {
        ring.clear()
        // 충돌 시 이름이 작은 노드가 이긴다 → RingRouter 의 정렬 규칙과 일치
        for (n in members.sortedDescending()) {
            for (i in 0 until vnodes) ring[Hashes.murmur3_64(n.value + "#" + i)] = n
        }
    }
}
