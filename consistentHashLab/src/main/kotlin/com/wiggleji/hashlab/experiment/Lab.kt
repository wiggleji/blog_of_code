package com.wiggleji.hashlab.experiment

import com.wiggleji.hashlab.HashRouter
import com.wiggleji.hashlab.NodeId
import com.wiggleji.hashlab.hash.Hashes
import com.wiggleji.hashlab.hrw.RendezvousRouter
import com.wiggleji.hashlab.jump.JumpRouter
import com.wiggleji.hashlab.maglev.MaglevRouter
import com.wiggleji.hashlab.ring.RingMode
import com.wiggleji.hashlab.ring.RingRouter
import com.wiggleji.hashlab.slot.SlotTableRouter
import java.util.Random
import java.util.UUID
import kotlin.math.sqrt

/** 노드 이름은 실무처럼 생기게. 이름이 바뀌면 모든 매핑이 바뀌므로 고정해 둔다. */
fun cluster(n: Int): List<NodeId> = (0 until n).map { NodeId("10.0.%d.%d:11211".format(it / 250, it % 250 + 1)) }

/** 클러스터에 새로 들어오는 노드. 기존 이름과 절대 겹치지 않게. */
fun newcomer(tag: String = "0") = NodeId("10.9.9.$tag:11211")

enum class HashKind { MURMUR3, KETAMA }

object Keys {
    fun userKeys(n: Int): Array<String> = Array(n) { "user:$it" }

    fun uuidKeys(n: Int): Array<String> {
        val rnd = Random(42)
        return Array(n) { UUID(rnd.nextLong(), rnd.nextLong()).toString() }
    }

    fun hashed(keys: Array<String>, kind: HashKind): LongArray = when (kind) {
        HashKind.MURMUR3 -> LongArray(keys.size) { Hashes.murmur3_64(keys[it]) }
        HashKind.KETAMA -> LongArray(keys.size) { Hashes.ketamaHash(keys[it]) }
    }
}

/** 실험에 올릴 알고리즘 한 종류. param 은 V(가상노드) / M(테이블) / S(슬롯). */
data class Algo(
    val name: String,
    val param: Int,
    val hashKind: HashKind = HashKind.MURMUR3,
    val build: (List<NodeId>) -> HashRouter,
)

object Algos {
    fun ring(v: Int) = Algo("ring", v) { RingRouter(it, vnodes = v) }
    fun ketama(v: Int = 160) = Algo("ring_ketama", v, HashKind.KETAMA) { RingRouter(it, vnodes = v, mode = RingMode.KETAMA) }
    fun hrw() = Algo("hrw", 0) { RendezvousRouter(it) }
    fun jump() = Algo("jump", 0) { JumpRouter(it.sorted()) }
    fun maglev(m: Int) = Algo("maglev", m) { MaglevRouter(it, m = m) }
    fun slots(s: Int = 1024) = Algo("slot_table", s) { SlotTableRouter(it.sorted(), slots = s) }
}

/** 각 키가 어느 노드로 갔는지 인덱스 배열로. 재배치율은 이 배열 두 개의 차이다. */
fun assign(router: HashRouter, hashes: LongArray, index: Map<NodeId, Int>): IntArray {
    val out = IntArray(hashes.size)
    for (i in hashes.indices) out[i] = index.getValue(router.routeHashed(hashes[i]))
    return out
}

data class Spread(val cv: Double, val peakOverMean: Double, val minOverMean: Double)

fun spreadOf(assignment: IntArray, nodeCount: Int): Spread {
    val counts = LongArray(nodeCount)
    for (a in assignment) counts[a]++
    val mean = assignment.size.toDouble() / nodeCount
    var sq = 0.0
    var peak = Long.MIN_VALUE
    var low = Long.MAX_VALUE
    for (c in counts) {
        val d = c - mean
        sq += d * d
        if (c > peak) peak = c
        if (c < low) low = c
    }
    val sd = sqrt(sq / nodeCount)
    return Spread(sd / mean, peak / mean, low / mean)
}

fun movedFraction(before: IntArray, after: IntArray): Double {
    var moved = 0
    for (i in before.indices) if (before[i] != after[i]) moved++
    return moved.toDouble() / before.size
}

class Csv(private val header: String) {
    private val rows = StringBuilder().append(header).append('\n')
    fun row(vararg cells: Any) {
        rows.append(cells.joinToString(",") {
            when (it) {
                is Double -> if (it.isNaN()) "NaN" else String.format("%.6f", it)
                else -> it.toString()
            }
        }).append('\n')
    }
    fun writeTo(path: String) {
        val f = java.io.File(path)
        f.parentFile?.mkdirs()
        f.writeText(rows.toString())
        println("  wrote $path")
    }
}

const val CSV_HEADER = "experiment,algorithm,nodes,param,keyset,metric,value"
