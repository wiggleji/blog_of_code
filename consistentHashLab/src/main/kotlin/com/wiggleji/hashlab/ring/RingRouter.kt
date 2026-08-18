package com.wiggleji.hashlab.ring

import com.wiggleji.hashlab.HashRouter
import com.wiggleji.hashlab.MutableTopology
import com.wiggleji.hashlab.NodeId
import com.wiggleji.hashlab.hash.Hashes
import java.util.Arrays

enum class RingMode { MURMUR3, KETAMA }

/**
 * ① 링 + 가상 노드. 책이 가르치는 그것.
 *
 * 노드마다 [vnodes] 개의 점을 링에 뿌리고, 키 해시보다 크거나 같은 첫 점의 주인을 고른다.
 * 조회 O(log(N·V)), 메모리 O(N·V).
 *
 * [RingMode.KETAMA] 는 libketama 호환 모드다. MD5("<node>-<i>") 다이제스트 16바이트를
 * 4바이트씩 잘라 포인트 4개를 만들고 40회 반복 → 서버당 160 포인트.
 */
class RingRouter(
    seed: Collection<NodeId>,
    val vnodes: Int = 160,
    val mode: RingMode = RingMode.MURMUR3,
) : HashRouter, MutableTopology {

    private val members = sortedSetOf<NodeId>().apply { addAll(seed) }
    private var points = LongArray(0)
    private var owners = arrayOf<NodeId>()

    init {
        require(mode != RingMode.KETAMA || vnodes % 4 == 0) { "ketama 모드는 포인트 수가 4의 배수여야 한다" }
        rebuild()
    }

    override val nodes: List<NodeId> get() = members.toList()

    override fun keyHash(key: String): Long =
        if (mode == RingMode.KETAMA) Hashes.ketamaHash(key) else Hashes.murmur3_64(key)

    override fun routeHashed(keyHash: Long): NodeId {
        val i = Arrays.binarySearch(points, keyHash)
        val idx = if (i >= 0) i else -(i + 1)
        return owners[if (idx == points.size) 0 else idx] // 한 바퀴 돌아 처음으로
    }

    override fun add(node: NodeId) { if (members.add(node)) rebuild() }
    override fun remove(node: NodeId) { if (members.remove(node)) rebuild() }

    /** 노드 하나가 링에 뿌리는 점들. 여기가 두 모드의 유일한 차이. */
    private fun pointsOf(n: NodeId): LongArray = when (mode) {
        RingMode.MURMUR3 -> LongArray(vnodes) { Hashes.murmur3_64(n.value + "#" + it) }
        RingMode.KETAMA -> LongArray(vnodes).also { out ->
            for (i in 0 until vnodes / 4) {
                val d = Hashes.md5(n.value + "-" + i)
                for (q in 0 until 4) out[i * 4 + q] = Hashes.ketamaHash(d, q)
            }
        }
    }

    private fun rebuild() {
        val total = members.size * vnodes
        val tmp = ArrayList<Pair<Long, NodeId>>(total)
        for (n in members) for (p in pointsOf(n)) tmp += p to n
        // (해시, 노드이름) 으로 정렬 → 해시 충돌이 나도 결과가 결정적이다
        tmp.sortWith(compareBy({ it.first }, { it.second }))
        points = LongArray(total) { tmp[it].first }
        owners = Array(total) { tmp[it].second }
    }

    fun ringSize() = points.size
}
