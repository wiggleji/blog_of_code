# 오픈소스 대조 체크리스트

기억으로 쓰지 않고 **직접 소스를 열어서** 확인한 것만 적는다. (확인일: 2026-08-18)

## ✅ Guava `Hashing.consistentHash(long, int)` 는 정말 Jump 인가

[guava/src/com/google/common/hash/Hashing.java](https://github.com/google/guava/blob/master/guava/src/com/google/common/hash/Hashing.java)

```java
public static int consistentHash(long input, int buckets) {
  checkArgument(buckets > 0, "buckets must be positive: %s", buckets);
  long generatorState = input;
  int candidate = 0;
  int generated;

  // Jump from bucket to bucket until we go out of range
  while (true) {
    generatorState = LinearCongruentialGenerator.nextState(generatorState);
    generated = (int) ((candidate + 1) / LinearCongruentialGenerator.toDouble(generatorState));
    if (generated >= 0 && generated < buckets) {
      candidate = generated;
    } else {
      return candidate;
    }
  }
}
```

논문의 루프와 형태는 다르지만(논문은 `j < buckets` 로 while 을 돌고, Guava 는 범위를 벗어나면
탈출한다) 같은 알고리즘이다. `JumpHashTest.agreesWithGuava` 가 랜덤 키 20,000개 × 임의 버킷 수에
대해 **비트 단위로 동일함**을 확인한다.

> 시사점: Guava 가 "consistent hash" 라는 이름으로 내놓은 유일한 API 는 링이 아니라 Jump 다.
> 그리고 그 시그니처에는 노드 목록이 없다 — `(long, int)`. 즉 노드를 뺄 방법이 애초에 없다.

## ✅ libketama — 서버당 160 포인트, MD5 4분할

[RJ/ketama libketama/ketama.c](https://github.com/RJ/ketama/blob/master/libketama/ketama.c)

```c
sprintf( ss, "%s-%d", slist[i].addr, k );
ketama_md5_digest( ss, digest );

/* Use successive 4-bytes from hash as numbers
 * for the points on the circle: */
int h;
for( h = 0; h < 4; h++ )
{
    continuum[cont].point = ( digest[3+h*4] << 24 )
                          | ( digest[2+h*4] << 16 )
                          | ( digest[1+h*4] <<  8 )
                          |   digest[h*4];
    ...
}
```

- 서버 이름 포맷: `"%s-%d"` (`addr` 은 보통 `1.2.3.4:11211`) — **`-` 구분자**
- 포인트 수: `/* 40 hashes, 4 numbers per hash = 160 points per server */`
  (정확히는 `ks = floorf(pct * 40.0 * numservers)`. 메모리가 균등하면 pct=1/N 이라 40회)
- 키 해시 `ketama_hashi`: MD5 다이제스트의 앞 4바이트를 리틀엔디언 unsigned 32비트로

우리 `RingRouter(mode = KETAMA)` 는 이 규칙을 그대로 옮겼다. (`RingTest.ketamaLayout`)

## ✅ Envoy — `ring_hash` 와 `maglev` 를 둘 다 준다

[api/envoy/config/cluster/v3/cluster.proto](https://github.com/envoyproxy/envoy/blob/main/api/envoy/config/cluster/v3/cluster.proto)

```protobuf
// Minimum hash ring size. The larger the ring is (that is, the more hashes there are for each
// provided host) the better the request distribution will reflect the desired weights. Defaults
// to 1024 entries, and limited to 8M entries.
google.protobuf.UInt64Value minimum_ring_size = 1 [(validate.rules).uint64 = {lte: 8388608}];

// The hash function used to hash hosts onto the ketama ring. The value defaults to XX_HASH.
HashFunction hash_function = 3 [(validate.rules).enum = {defined_only: true}];

message MaglevLbConfig {
  // The table size for Maglev hashing. Maglev aims for "minimal disruption" rather than an
  // absolute guarantee. Minimal disruption means that when the set of upstream hosts change, a
  // connection will likely be sent to the same upstream as it was before.
  // Increasing the table size reduces the amount of disruption.
  // The table size must be prime number limited to 5000011. If it is not specified,
  // the default is 65537.
  google.protobuf.UInt64Value table_size = 1 [(validate.rules).uint64 = {lte: 5000011}];
}
```

- `ring_hash`: 기본 링 크기 1024, 최대 8M, 기본 해시 XX_HASH. 주석에서 링을 **"ketama ring"** 이라 부른다.
- `maglev`: 기본 테이블 65537(소수), 최대 5000011(소수).
- **Envoy 문서가 직접 인정한다**: *"Maglev aims for minimal disruption rather than an absolute
  guarantee... Increasing the table size reduces the amount of disruption."*
  → 우리 실험 B 가 잰 게 정확히 이 문장의 정량값이다.

## ✅ Cassandra — Murmur3Partitioner + num_tokens

[conf/cassandra.yaml (trunk)](https://github.com/apache/cassandra/blob/trunk/conf/cassandra.yaml)

```yaml
num_tokens: 16
partitioner: org.apache.cassandra.dht.Murmur3Partitioner
```

주석: *"If you leave this unspecified, Cassandra will use the default of 1 token for legacy
compatibility"* — 즉 설정 파일의 값은 16이고, 미지정 시 1이다.
(3.x 시절의 기본값 256 → 4.0에서 16으로 내려왔다. 가상 노드가 많을수록 분포는 좋아지지만
복구/스트리밍 비용과 장애 시 상관관계가 나빠진다는 트레이드오프.)

## ✅ Redis Cluster — 16384 슬롯, CRC16, 해시 태그

[src/cluster.h](https://github.com/redis/redis/blob/unstable/src/cluster.h)

```c
#define CLUSTER_SLOT_MASK_BITS 14 /* Number of bits used for slot id. */
#define CLUSTER_SLOTS (1<<CLUSTER_SLOT_MASK_BITS) /* Total number of slots in cluster mode, which is 16384. */

/* We have 16384 hash slots. The hash slot of a given key is obtained
 * as the least significant 14 bits of the crc16 of the key.
 *
 * However, if the key contains the {...} pattern, only the part between
 * { and } is hashed. ... */
static inline unsigned int keyHashSlot(const char *key, int keylen) {
    ...
    if (likely(s == keylen)) return crc16(key,keylen) & 0x3FFF;
```

`& 0x3FFF` 가 곧 `mod 16384`. 안정 해시 링이 아니라 **고정 슬롯 + 배정표**다.
우리 `SlotTableRouter` 가 이 구조를 Jump 로 흉내 낸 것이다.

## ☐ 아직 안 본 것

- HAProxy `hash-type consistent`
- nginx `upstream ... hash $key consistent`
