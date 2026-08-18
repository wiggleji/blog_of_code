package com.wiggleji.hashlab.hash

import java.security.MessageDigest

/**
 * 네 알고리즘이 공유하는 해시 원시함수.
 *
 * 공정 비교의 전제: Ring(murmur 모드) / HRW / Jump / Maglev 는 모두 [murmur3_64] 로 키를 해시한다.
 * ketama 호환 모드만 MD5 를 쓴다(원본 libketama 가 그렇기 때문).
 */
object Hashes {

    private const val C1 = -0x783c846eeebdac2bL // 0x87c37b91114253d5
    private const val C2 = 0x4cf5ad432745937fL

    /** MurmurHash3 x64_128 의 앞쪽 64비트. Guava 의 murmur3_128().asLong() 과 값이 같아야 한다. */
    fun murmur3_64(data: ByteArray, seed: Int = 0): Long {
        var h1 = seed.toLong() and 0xffffffffL
        var h2 = seed.toLong() and 0xffffffffL
        val nblocks = data.size / 16

        for (i in 0 until nblocks) {
            var k1 = readLongLE(data, i * 16)
            var k2 = readLongLE(data, i * 16 + 8)
            k1 *= C1; k1 = java.lang.Long.rotateLeft(k1, 31); k1 *= C2; h1 = h1 xor k1
            h1 = java.lang.Long.rotateLeft(h1, 27); h1 += h2; h1 = h1 * 5 + 0x52dce729L
            k2 *= C2; k2 = java.lang.Long.rotateLeft(k2, 33); k2 *= C1; h2 = h2 xor k2
            h2 = java.lang.Long.rotateLeft(h2, 31); h2 += h1; h2 = h2 * 5 + 0x38495ab5L
        }

        val tail = nblocks * 16
        val rem = data.size and 15
        var k1 = 0L
        var k2 = 0L
        if (rem > 8) {
            for (i in rem - 1 downTo 8) k2 = k2 or ((data[tail + i].toLong() and 0xff) shl (8 * (i - 8)))
            k2 *= C2; k2 = java.lang.Long.rotateLeft(k2, 33); k2 *= C1; h2 = h2 xor k2
        }
        if (rem > 0) {
            for (i in minOf(rem, 8) - 1 downTo 0) k1 = k1 or ((data[tail + i].toLong() and 0xff) shl (8 * i))
            k1 *= C1; k1 = java.lang.Long.rotateLeft(k1, 31); k1 *= C2; h1 = h1 xor k1
        }

        h1 = h1 xor data.size.toLong(); h2 = h2 xor data.size.toLong()
        h1 += h2; h2 += h1
        h1 = fmix64(h1); h2 = fmix64(h2)
        h1 += h2
        return h1
    }

    fun murmur3_64(s: String, seed: Int = 0): Long = murmur3_64(s.toByteArray(Charsets.UTF_8), seed)

    /** MurmurHash3 x86_32. Guava 의 murmur3_32_fixed().asInt() 과 값이 같아야 한다. */
    fun murmur3_32(data: ByteArray, seed: Int = 0): Int {
        val c1 = -0x3361d2af // 0xcc9e2d51
        val c2 = 0x1b873593
        var h1 = seed
        val nblocks = data.size / 4
        for (i in 0 until nblocks) {
            var k1 = readIntLE(data, i * 4)
            k1 *= c1; k1 = Integer.rotateLeft(k1, 15); k1 *= c2
            h1 = h1 xor k1
            h1 = Integer.rotateLeft(h1, 13); h1 = h1 * 5 + -0x19ab949c // 0xe6546b64
        }
        var k1 = 0
        val tail = nblocks * 4
        for (i in (data.size and 3) - 1 downTo 0) k1 = k1 or ((data[tail + i].toInt() and 0xff) shl (8 * i))
        if ((data.size and 3) > 0) { k1 *= c1; k1 = Integer.rotateLeft(k1, 15); k1 *= c2; h1 = h1 xor k1 }
        h1 = h1 xor data.size
        return fmix32(h1)
    }

    fun murmur3_32(s: String, seed: Int = 0): Int = murmur3_32(s.toByteArray(Charsets.UTF_8), seed)

    /** murmur3 의 finalizer. 두 개의 64비트 값을 "섞는" 용도로 HRW 가 쓴다. */
    fun fmix64(x: Long): Long {
        var k = x
        k = k xor (k ushr 33)
        k *= 0xff51afd7ed558ccdUL.toLong()
        k = k xor (k ushr 33)
        k *= 0xc4ceb9fe1a85ec53UL.toLong()
        k = k xor (k ushr 33)
        return k
    }

    fun fmix32(x: Int): Int {
        var h = x
        h = h xor (h ushr 16)
        h *= -0x7a143595 // 0x85ebca6b
        h = h xor (h ushr 13)
        h *= -0x3d4d51cb // 0xc2b2ae35
        h = h xor (h ushr 16)
        return h
    }

    /** libketama 의 해시: MD5 다이제스트의 앞 4바이트를 리틀엔디언 unsigned 32비트로 읽는다. */
    fun ketamaHash(s: String): Long = ketamaHash(md5(s), 0)

    /** libketama 의 포인트 추출: 16바이트 다이제스트를 4바이트씩 잘라 포인트 4개를 만든다. */
    fun ketamaHash(digest: ByteArray, quarter: Int): Long {
        val o = quarter * 4
        return ((digest[o + 3].toLong() and 0xff) shl 24) or
            ((digest[o + 2].toLong() and 0xff) shl 16) or
            ((digest[o + 1].toLong() and 0xff) shl 8) or
            (digest[o].toLong() and 0xff)
    }

    fun md5(s: String): ByteArray =
        MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.UTF_8))

    private fun readLongLE(b: ByteArray, o: Int): Long {
        var v = 0L
        for (i in 7 downTo 0) v = (v shl 8) or (b[o + i].toLong() and 0xff)
        return v
    }

    private fun readIntLE(b: ByteArray, o: Int): Int {
        var v = 0
        for (i in 3 downTo 0) v = (v shl 8) or (b[o + i].toInt() and 0xff)
        return v
    }
}
