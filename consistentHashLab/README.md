# 해시 라우팅 알고리즘 4파전

책은 "링"만 가르친다. 그런데 Envoy 설정에는 `ring_hash` 와 `maglev` 가 나란히 있고,
Guava 가 `consistentHash` 라는 이름으로 내놓은 유일한 API 는 링이 아니다.
넷을 같은 조건에 올려놓고 재본 실험실.

| | Ring / Ketama | Rendezvous (HRW) | Jump | Maglev |
|---|---|---|---|---|
| 조회 | O(log NV) | **O(N)** | O(ln N) | **O(1)** |
| 상태 메모리 | N·V 포인트 | 노드 시드뿐 | **없음** | 테이블 M |
| 임의 노드 제거 | ✅ | ✅ | **❌** | ✅ |
| 최소 재배치 | ✅ | ✅ (정의상) | 끝 버킷만 | **❌ 초과분 발생** |
| 가중치 | 포인트 수로 | 자연스럽게 | ❌ | 순열 반복으로 |

**성질 넷을 동시에 만족하는 알고리즘은 없다. 무엇을 포기했는지가 곧 그 알고리즘의 용도다.**

## 구조

```
src/main/kotlin/com/wiggleji/hashlab/
├── HashRouter.kt          # HashRouter + MutableTopology (여기서 이미 결론이 하나 나온다)
├── hash/Hashes.kt         # murmur3_64 / murmur3_32 / fmix64 / MD5(ketama)
├── ring/RingRouter.kt     # ① 정렬 LongArray + binarySearch, ketama 호환 모드 포함
├── ring/TreeMapRingRouter.kt  # 같은 링의 교과서 구현(TreeMap.ceilingEntry) — 속도 대조군
├── hrw/RendezvousRouter.kt    # ②
├── jump/JumpHash.kt           # ③ 7줄 + 라우터 래퍼 (MutableTopology 미구현)
├── maglev/MaglevRouter.kt     # ④
├── slot/SlotTableRouter.kt    # 에필로그: Jump + 슬롯 배정표 = Redis Cluster
└── experiment/                # 실험 A/B/D 하네스
src/main/java/.../bench/       # 실험 C (JMH)
```

## 실행

```bash
mvn -f consistentHashLab/pom.xml test        # 성질 테스트 23개
mvn -f consistentHashLab/pom.xml exec:java   # 실험 A/B/D → results/*.csv
```

| 문서 | 내용 |
|---|---|
| [FINDINGS.md](FINDINGS.md) | 가설 4개 채점, 실측 해석, 선택 가이드 |
| [SOURCES.md](SOURCES.md) | 오픈소스 대조 (Guava·libketama·Envoy·Cassandra·Redis 원문 인용) |
| [CLAUDE.md](CLAUDE.md) | 랩 규칙과 실행 방법 |
| [report.html](report.html) | 포스팅용 정리본 (아티팩트로 발행한 것과 동일) |

## 테스트가 곧 주장이다

```
DisruptionTest
  ✓ ring: removing a node moves only that node's keys
  ✓ rendezvous: removing a node moves only that node's keys
  ✓ slot table: removing a node moves only that node's slots
  ✓ maglev: removing a node ALSO moves keys that had nothing to do with it
  ✓ maglev: a bigger table shrinks the collateral damage
JumpHashTest
  ✓ Guava's Hashing.consistentHash IS Jump - our implementation agrees bit for bit
  ✓ growing N to N+1 moves keys ONLY into the new bucket
  ✓ unsigned shift matters: using shr instead of ushr collapses the distribution
  ✓ distribution is indistinguishable from a perfectly random assignment
  ✓ Jump cannot remove an arbitrary node - only the last bucket
```
