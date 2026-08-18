package com.wiggleji.hashlab.hrw

import com.wiggleji.hashlab.HashRouter
import com.wiggleji.hashlab.MutableTopology
import com.wiggleji.hashlab.NodeId
import com.wiggleji.hashlab.hash.Hashes

/**
 * ② Rendezvous / HRW (Highest Random Weight).
 *
 * 키 하나에 대해 모든 노드의 가중치를 계산해서 최댓값을 고른다. 링도 가상 노드도 없다.
 * 조회 O(N), 상태 메모리 O(N) (노드 시드 배열이 전부).
 *
 * 최소 재배치를 *정의상* 만족한다: 노드 X 가 빠져도 나머지 노드들의 가중치 순위는 그대로라
 * X 가 1등이던 키만 2등에게 넘어간다.
 *
 * 가중치 결합은 문자열 접합(hash(key + node))이 아니라 정수 믹싱으로 한다.
 * 문자열 접합은 느리고("user:12"+"node-3" 마다 배열 할당) 접두 충돌도 생긴다.
 */
class RendezvousRouter(seed: Collection<NodeId>) : HashRouter, MutableTopology {

    private val members = sortedSetOf<NodeId>().apply { addAll(seed) }
    private var names = arrayOf<NodeId>()
    private var seeds = LongArray(0)

    init { rebuild() }

    override val nodes: List<NodeId> get() = members.toList()

    override fun routeHashed(keyHash: Long): NodeId {
        var best = 0
        var bestWeight = Long.MIN_VALUE
        for (i in seeds.indices) {
            val w = Hashes.fmix64(keyHash xor seeds[i])
            if (w > bestWeight) { bestWeight = w; best = i }
        }
        return names[best]
    }

    override fun add(node: NodeId) { if (members.add(node)) rebuild() }
    override fun remove(node: NodeId) { if (members.remove(node)) rebuild() }

    private fun rebuild() {
        names = members.toTypedArray() // 이름 정렬 순서 → 동점 시 결과가 결정적
        seeds = LongArray(names.size) { Hashes.murmur3_64(names[it].value, seed = 0x5bd1) }
    }
}
