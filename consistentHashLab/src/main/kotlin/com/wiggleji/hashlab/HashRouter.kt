package com.wiggleji.hashlab

import com.wiggleji.hashlab.hash.Hashes

/** 노드 식별자. 실무의 "10.0.0.7:11211" 같은 문자열을 그대로 쓴다. */
data class NodeId(val value: String) : Comparable<NodeId> {
    override fun compareTo(other: NodeId) = value.compareTo(other.value)
    override fun toString() = value
}

fun node(v: String) = NodeId(v)

/**
 * 키 하나를 노드 하나로 보내는 것. 그게 전부다.
 *
 * [keyHash] 를 인터페이스에 노출한 이유: 벤치마크에서 키 해시 비용을 미리 빼두고
 * "라우팅 자체"만 재기 위해서다. ketama 호환 모드만 MD5 를 쓰므로 알고리즘마다 다를 수 있다.
 */
interface HashRouter {
    val nodes: List<NodeId>
    fun keyHash(key: String): Long = Hashes.murmur3_64(key)
    fun routeHashed(keyHash: Long): NodeId
    fun route(key: String): NodeId = routeHashed(keyHash(key))
}

/**
 * 노드가 동적으로 들고 나는 토폴로지.
 *
 * 여기서 이미 결론이 하나 나온다: **Jump 는 이 인터페이스를 구현할 수 없다.**
 * Jump 의 버킷은 0..n-1 이라 끝에서만 늘고 줄기 때문에, 3번 노드가 죽어도 3번을 뺄 수가 없다.
 * UnsupportedOperationException 으로 덮지 않고 타입을 갈라둔 이유다.
 * (JumpRouter 의 주석과 JumpRouterTest 참고)
 */
interface MutableTopology {
    fun add(node: NodeId)
    fun remove(node: NodeId)
}
