package com.wiggleji.hashlab.slot

import com.wiggleji.hashlab.HashRouter
import com.wiggleji.hashlab.MutableTopology
import com.wiggleji.hashlab.NodeId
import com.wiggleji.hashlab.jump.JumpHash

/**
 * Jump 의 한계를 우회하는 2단 매핑 — 그리고 이게 사실상 Redis Cluster 다.
 *
 *   key ──(Jump, 고정)──▶ 논리 슬롯 0..S-1 ──(가변 배정표)──▶ 물리 노드
 *
 * 1단은 상태 없는 순수 함수라 절대 안 변한다(S 를 안 바꾸는 한).
 * 2단은 그냥 배열이라 "3번 노드 빼기"가 자유롭고, 옮길 슬롯을 우리가 직접 고르므로 최소 재배치다.
 * 대신 슬롯 단위(1/S)로만 균형을 맞출 수 있고, 배정표를 누군가 관리해야 한다.
 *
 * Redis Cluster 는 1단이 CRC16(key) mod 16384 이고 2단이 클러스터 버스로 공유되는 슬롯 맵이다.
 * 즉 "안정 해시를 안 쓰는 시스템"이 아니라 "다른 층위에서 푸는 시스템"이다.
 */
class SlotTableRouter(
    seed: List<NodeId>,
    val slots: Int = 1024,
) : HashRouter, MutableTopology {

    private val members = ArrayList(seed.sorted())
    private val owner = IntArray(slots) // 슬롯 → members 인덱스

    init {
        require(seed.isNotEmpty())
        for (s in 0 until slots) owner[s] = s * members.size / slots // 연속 구간 배정
    }

    override val nodes: List<NodeId> get() = members.toList()

    fun slotOf(keyHash: Long): Int = JumpHash.jump(keyHash, slots)

    override fun routeHashed(keyHash: Long): NodeId = members[owner[slotOf(keyHash)]]

    /** 새 노드가 들어오면 과부하 노드에서 슬롯을 딱 필요한 만큼만 뺏어온다. */
    override fun add(node: NodeId) {
        if (members.contains(node)) return
        members.add(node)
        val newIdx = members.size - 1
        val target = slots / members.size
        var moved = 0
        val counts = IntArray(members.size)
        for (o in owner) counts[o]++
        for (s in 0 until slots) {
            if (moved >= target) break
            val from = owner[s]
            if (counts[from] > target) { owner[s] = newIdx; counts[from]--; moved++ }
        }
    }

    /** 노드가 빠지면 그 노드의 슬롯만 생존 노드에 라운드로빈으로 나눠준다. 다른 슬롯은 안 움직인다. */
    override fun remove(node: NodeId) {
        val idx = members.indexOf(node)
        if (idx < 0) return
        require(members.size > 1) { "마지막 노드는 뺄 수 없다" }
        var rr = 0
        val survivors = members.indices.filter { it != idx }
        for (s in 0 until slots) {
            if (owner[s] == idx) owner[s] = survivors[rr++ % survivors.size]
        }
        // members 에서 제거하면서 뒤쪽 인덱스를 당겨준다
        members.removeAt(idx)
        for (s in 0 until slots) if (owner[s] > idx) owner[s]--
    }

    fun slotCounts(): Map<NodeId, Int> {
        val c = IntArray(members.size)
        for (o in owner) c[o]++
        return members.indices.associate { members[it] to c[it] }
    }
}
