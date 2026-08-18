# 목적

해시 라우팅 알고리즘 4종(Ring/Ketama, Rendezvous HRW, Jump, Maglev)을 동일 조건에서
비교 측정하는 실험용 랩. 블로그 포스팅의 근거 데이터를 만드는 게 목표다.

# 원칙

- 프로덕션 코드가 아니라 "증명용 코드". 짧고 읽히는 걸 최우선으로.
- 각 알고리즘 구현은 30줄을 넘기지 않게. 최적화보다 논문/원본 구현과의 대응이 우선.
- 라이브러리로 대체하지 말 것. Guava 는 우리 구현의 **검증용**으로만 쓴다
  (`Hashing.consistentHash` ↔ 우리 Jump, `Hashing.murmur3_*` ↔ 우리 Murmur3).
- 네 구현 모두 동일한 키 해시(murmur3_64)를 쓸 것. ketama 호환 모드만 예외(MD5).
- 테스트 이름은 알고리즘의 성질을 문장으로 말할 것.
  `"removing a node moves only that node's keys"` 같은 이름이 그대로 포스팅 소제목이 된다.
- 측정 결과는 `results/*.csv` 에 long format 으로:
  `experiment,algorithm,nodes,param,keyset,metric,value`
- 수치 해석 시 **잡음 바닥(noise floor)** 을 먼저 계산할 것. 키 K개를 N개 노드에 완벽히
  무작위로 뿌려도 CV 는 `1/sqrt(K/N)` 만큼 흔들린다. 이보다 낮은 값은 나올 수 없다.

# 스택

Kotlin 2.2.21 / JDK 21 / Maven / JUnit5 / JMH 1.37 / JOL

# 실행

```bash
mvn -f consistentHashLab/pom.xml test                      # 성질 테스트 23개
mvn -f consistentHashLab/pom.xml exec:java                 # 실험 A/B/D → results/*.csv
mvn -f consistentHashLab/pom.xml exec:java -Dexec.args=A   # 실험 하나만

# 실험 C (JMH)
mvn -f consistentHashLab/pom.xml dependency:build-classpath -Dmdep.outputFile=target/cp.txt
cd consistentHashLab && java -cp "target/classes:$(cat target/cp.txt)" org.openjdk.jmh.Main \
  -rf csv -rff results/c_speed.csv
```

# 주의

- `JumpRouter` 는 `MutableTopology` 를 **일부러** 구현하지 않는다. 고쳐서 구현하지 말 것.
  그게 이 실험의 결론 중 하나다.
- Jump 의 `k ushr 33` 을 `shr` 로 바꾸면 조용히 분포가 망가진다. `JumpHashTest` 가 잡는다.
- Maglev 의 테이블 크기 M 은 소수여야 한다. skip 과 서로소가 되어야 순열이 성립한다.
