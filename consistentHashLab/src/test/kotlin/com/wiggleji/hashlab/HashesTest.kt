package com.wiggleji.hashlab

import com.google.common.hash.Hashing
import com.wiggleji.hashlab.hash.Hashes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 실험 결과를 믿으려면 해시부터 맞아야 한다.
 * "우리가 짠 murmur3" 가 진짜 murmur3 인지 Guava 로 대조한다.
 */
class HashesTest {

    private fun samples(): List<String> {
        val rnd = Random(7)
        return buildList {
            add(""); add("a"); add("ab"); add("abc"); add("hello world")
            add("10.0.0.1:11211#0")
            repeat(500) {
                val len = rnd.nextInt(64)
                add((0 until len).map { ('a' + rnd.nextInt(26)) }.joinToString(""))
            }
        }
    }

    @Test
    @DisplayName("our murmur3_64 equals Guava murmur3_128().asLong()")
    fun murmur64MatchesGuava() {
        for (s in samples()) {
            val expected = Hashing.murmur3_128(0).hashString(s, UTF_8).asLong()
            assertEquals(expected, Hashes.murmur3_64(s), "mismatch for '$s'")
        }
    }

    @Test
    @DisplayName("our murmur3_32 equals Guava murmur3_32_fixed().asInt()")
    fun murmur32MatchesGuava() {
        for (s in samples()) {
            val expected = Hashing.murmur3_32_fixed(0).hashString(s, UTF_8).asInt()
            assertEquals(expected, Hashes.murmur3_32(s), "mismatch for '$s'")
        }
    }

    @Test
    @DisplayName("ketama key hash is the little-endian first 4 bytes of MD5")
    fun ketamaHashIsMd5Prefix() {
        val d = Hashes.md5("somekey")
        val expected = ((d[3].toLong() and 0xff) shl 24) or ((d[2].toLong() and 0xff) shl 16) or
            ((d[1].toLong() and 0xff) shl 8) or (d[0].toLong() and 0xff)
        assertEquals(expected, Hashes.ketamaHash("somekey"))
        assertTrue(Hashes.ketamaHash("somekey") in 0..0xffffffffL, "ketama 해시는 unsigned 32비트다")
    }

    @Test
    @DisplayName("fmix64 is a bijection-ish mixer: distinct inputs give distinct outputs")
    fun fmixSpreads() {
        val seen = HashSet<Long>()
        for (i in 0 until 100_000) seen.add(Hashes.fmix64(i.toLong()))
        assertEquals(100_000, seen.size)
    }
}
