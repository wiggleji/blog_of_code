package com.wiggleji.hashlab.jump

import com.wiggleji.hashlab.HashRouter
import com.wiggleji.hashlab.NodeId
import com.wiggleji.hashlab.hash.Hashes

/**
 * ③ Jump Consistent Hash (Lamping & Veach, 2014).
 * 상태가 없다. 함수 하나가 전부다. 메모리 0, 조회 O(ln N), 분포는 거의 완벽.
 */
object JumpHash {

    /**
     * 원 논문의 7줄짜리 구현 그대로.
     *
     * 주의: `key ushr 33` 을 `shr` 로 쓰면 컴파일도 되고 테스트도 대충 통과하지만
     * 분포가 조용히 망가진다(음수 → 0번 버킷 편중). JumpHashTest 가 그걸 잡는다.
     */
    fun jump(key: Long, buckets: Int): Int {
        require(buckets > 0) { "buckets must be positive" }
        var k = key
        var b = -1L
        var j = 0L
        while (j < buckets) {
            b = j
            k = k * 2862933555777941757L + 1
            j = ((b + 1) * ((1L shl 31).toDouble() / ((k ushr 33) + 1).toDouble())).toLong()
        }
        return b.toInt()
    }
}

/**
 * Jump 를 라우터로 감싼 것. **[com.wiggleji.hashlab.MutableTopology] 를 구현하지 않는다.**
 *
 * 못 하는 게 아니라 "구현하면 거짓말"이라서 안 한다. 아래를 주석 해제하면 컴파일은 되지만
 * 의미가 성립하지 않는다:
 *
 * ```
 * override fun remove(n: NodeId) {
 *     // 3번 노드가 죽었다. buckets 를 4로 줄이면? 3번이 아니라 "마지막 버킷"이 사라진다.
 *     // 남은 0,1,2,4번 노드를 0..3 으로 다시 번호 매기면 그건 이미 최소 재배치가 아니다.
 *     throw UnsupportedOperationException()
 * }
 * ```
 *
 * 그래서 Jump 는 "노드가 죽는 로드밸런서"가 아니라 "샤드 개수가 정책적으로 바뀌는 저장소"용이다.
 * Guava 가 Hashing.consistentHash(long, int) 를 딱 이 시그니처로 내놓은 이유이기도 하다.
 */
class JumpRouter(override val nodes: List<NodeId>) : HashRouter {

    val buckets: Int get() = nodes.size

    override fun routeHashed(keyHash: Long): NodeId = nodes[JumpHash.jump(keyHash, buckets)]

    /** 끝에 한 대 붙이기. Jump 가 허용하는 유일한 확장. */
    fun grow(newNode: NodeId) = JumpRouter(nodes + newNode)

    /** 끝에서 한 대 떼기. Jump 가 허용하는 유일한 축소. */
    fun shrinkLast() = JumpRouter(nodes.dropLast(1))
}
