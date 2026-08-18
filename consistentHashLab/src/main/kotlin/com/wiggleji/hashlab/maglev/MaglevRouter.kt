package com.wiggleji.hashlab.maglev

import com.wiggleji.hashlab.HashRouter
import com.wiggleji.hashlab.MutableTopology
import com.wiggleji.hashlab.NodeId
import com.wiggleji.hashlab.hash.Hashes

/**
 * ④ Maglev Hashing (Google, 2016).
 *
 * 크기 [m] 인 룩업 테이블을 미리 채워두고, 조회는 배열 인덱싱 한 번 — O(1).
 * 각 백엔드는 (offset, skip) 으로 정의된 순열을 갖고, 라운드로빈으로 돌면서
 * 자기 순열의 다음 후보 슬롯이 비어 있으면 차지한다.
 *
 * 포기한 것은 "최소 재배치"다. 노드가 하나 바뀌면 테이블을 처음부터 다시 채우기 때문에
 * 죽은 노드의 키 말고도 일부가 덤으로 움직인다. [m] 을 키우면 그 초과분이 줄어든다.
 * 대신 임의 노드 제거 + O(1) 조회 + 좋은 분포를 동시에 갖는 유일한 후보다.
 *
 * @param m 테이블 크기. **소수여야 한다** (skip 과 서로소가 되어 순열이 성립하도록).
 */
class MaglevRouter(
    seed: Collection<NodeId>,
    val m: Int = 65537,
) : HashRouter, MutableTopology {

    private val members = sortedSetOf<NodeId>().apply { addAll(seed) }
    private var names = arrayOf<NodeId>()
    private var table = IntArray(0)

    init {
        require(isPrime(m)) { "테이블 크기 m 은 소수여야 한다: $m" }
        rebuild()
    }

    override val nodes: List<NodeId> get() = members.toList()

    override fun routeHashed(keyHash: Long): NodeId {
        val i = ((keyHash % m) + m) % m
        return names[table[i.toInt()]]
    }

    override fun add(node: NodeId) { if (members.add(node)) rebuild() }
    override fun remove(node: NodeId) { if (members.remove(node)) rebuild() }

    /** 논문의 populate(). 빈 슬롯이 없어질 때까지 백엔드들이 번갈아 자기 순열을 소비한다. */
    private fun rebuild() {
        names = members.toTypedArray()
        val n = names.size
        if (n == 0) { table = IntArray(0); return }

        val offset = IntArray(n)
        val skip = IntArray(n)
        for (i in 0 until n) {
            val h1 = Hashes.murmur3_64(names[i].value, seed = 0xc0ffee)
            val h2 = Hashes.murmur3_64(names[i].value, seed = 0xdecaf)
            offset[i] = (((h1 % m) + m) % m).toInt()
            skip[i] = ((((h2 % (m - 1)) + (m - 1)) % (m - 1)) + 1).toInt()
        }

        val entry = IntArray(m) { -1 }
        val next = IntArray(n)
        var filled = 0
        while (true) {
            for (i in 0 until n) {
                var c = permutation(offset[i], skip[i], next[i])
                while (entry[c] >= 0) { next[i]++; c = permutation(offset[i], skip[i], next[i]) }
                entry[c] = i
                next[i]++
                filled++
                if (filled == m) { table = entry; return }
            }
        }
    }

    private fun permutation(offset: Int, skip: Int, j: Int): Int =
        ((offset.toLong() + skip.toLong() * j) % m).toInt()

    fun tableSize() = m

    /** 백엔드별 슬롯 수. 논문의 주장대로 편차가 1을 넘지 않아야 한다. */
    fun slotCounts(): Map<NodeId, Int> {
        val c = IntArray(names.size)
        for (t in table) c[t]++
        return names.indices.associate { names[it] to c[it] }
    }

    private fun isPrime(x: Int): Boolean {
        if (x < 2) return false
        if (x % 2 == 0) return x == 2
        var d = 3
        while (d.toLong() * d <= x) { if (x % d == 0) return false; d += 2 }
        return true
    }
}
