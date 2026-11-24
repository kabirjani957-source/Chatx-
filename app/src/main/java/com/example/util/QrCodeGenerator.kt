package com.example.util

/**
 * Pure-Kotlin ISO/IEC 18004 QR Code Matrix Generator (Byte mode, ECC Level L, Versions 1..6).
 * Generates real, scannable QR matrices with Reed-Solomon error correction and format BCH bits.
 */
object QrCodeGenerator {

    private data class VersionSpec(
        val version: Int,
        val size: Int,
        val totalCodewords: Int,
        val dataCodewords: Int,
        val eccCodewords: Int,
        val alignmentPos: Int?
    )

    // Single-block ECC-L specs for versions 1..6 (supports up to 134 bytes)
    private val SPECS = listOf(
        VersionSpec(1, 21, 26, 19, 7, null),
        VersionSpec(2, 25, 44, 34, 10, 18),
        VersionSpec(3, 29, 70, 55, 15, 22),
        VersionSpec(4, 33, 100, 80, 20, 26),
        VersionSpec(5, 37, 134, 108, 26, 30),
        VersionSpec(6, 41, 172, 136, 36, 34)
    )

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if (x >= 256) x = x xor 0x11D
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun rsGeneratorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            val factor = GF_EXP[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], factor)
            }
            poly = next
        }
        return poly
    }

    private fun computeEcc(data: IntArray, eccCount: Int): IntArray {
        val gen = rsGeneratorPoly(eccCount)
        val buffer = IntArray(data.size + eccCount)
        System.arraycopy(data, 0, buffer, 0, data.size)
        for (i in data.indices) {
            val coef = buffer[i]
            if (coef != 0) {
                for (j in 1 until gen.size) {
                    buffer[i + j] = buffer[i + j] xor gfMul(gen[j], coef)
                }
            }
        }
        return buffer.copyOfRange(data.size, buffer.size)
    }

    fun encode(text: String): Array<BooleanArray> {
        val rawBytes = text.toByteArray(Charsets.UTF_8)
        val spec = SPECS.firstOrNull { rawBytes.size + 2 <= it.dataCodewords }
            ?: SPECS.last()
        val payload = if (rawBytes.size + 2 > spec.dataCodewords) {
            rawBytes.copyOf(spec.dataCodewords - 2)
        } else {
            rawBytes
        }

        // Build bit stream: Mode 0100 (Byte), 8-bit length, payload bytes, terminator, padding
        val bits = ArrayList<Int>(spec.dataCodewords * 8)
        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }
        appendBits(0b0100, 4)
        appendBits(payload.size, 8)
        for (b in payload) appendBits(b.toInt() and 0xFF, 8)

        val maxBits = spec.dataCodewords * 8
        val termLen = minOf(4, maxBits - bits.size)
        repeat(termLen) { bits.add(0) }
        while (bits.size % 8 != 0) bits.add(0)

        val dataCodewords = IntArray(spec.dataCodewords)
        var idx = 0
        while (idx < bits.size / 8) {
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or bits[idx * 8 + b]
            }
            dataCodewords[idx] = v
            idx++
        }
        var padIdx = 0
        while (idx < spec.dataCodewords) {
            dataCodewords[idx++] = if (padIdx % 2 == 0) 0xEC else 0x11
            padIdx++
        }

        val ecc = computeEcc(dataCodewords, spec.eccCodewords)
        val allCodewords = IntArray(spec.totalCodewords)
        System.arraycopy(dataCodewords, 0, allCodewords, 0, dataCodewords.size)
        System.arraycopy(ecc, 0, allCodewords, dataCodewords.size, ecc.size)

        val n = spec.size
        val modules = Array(n) { BooleanArray(n) }
        val isFunction = Array(n) { BooleanArray(n) }

        fun setFunc(r: Int, c: Int, black: Boolean) {
            if (r in 0 until n && c in 0 until n) {
                modules[r][c] = black
                isFunction[r][c] = true
            }
        }

        fun drawFinder(top: Int, left: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val rr = top + r
                    val cc = left + c
                    if (rr !in 0 until n || cc !in 0 until n) continue
                    val black = (r in 0..6 && c in 0..6) &&
                        (r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4))
                    setFunc(rr, cc, black)
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, n - 7)
        drawFinder(n - 7, 0)

        // Timing patterns
        for (i in 8 until n - 8) {
            val black = (i % 2 == 0)
            setFunc(6, i, black)
            setFunc(i, 6, black)
        }

        // Alignment pattern if version >= 2
        spec.alignmentPos?.let { pos ->
            for (dr in -2..2) {
                for (dc in -2..2) {
                    val black = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc)) != 1
                    setFunc(pos + dr, pos + dc, black)
                }
            }
        }

        // Reserve format info areas & dark module
        for (i in 0..8) {
            if (i != 6) {
                setFunc(8, i, false)
                setFunc(i, 8, false)
            }
        }
        for (i in 0..7) {
            setFunc(8, n - 1 - i, false)
            setFunc(n - 1 - i, 8, false)
        }
        setFunc(n - 8, 8, true)

        // Zig-zag data placement with mask pattern 0: (r + c) % 2 == 0
        var bitPtr = 0
        val totalBits = allCodewords.size * 8
        var right = n - 1
        var upward = true
        while (right >= 1) {
            if (right == 6) right--
            for (step in 0 until n) {
                val r = if (upward) n - 1 - step else step
                for (dx in 0..1) {
                    val c = right - dx
                    if (!isFunction[r][c]) {
                        val bit = if (bitPtr < totalBits) {
                            (allCodewords[bitPtr ushr 3] ushr (7 - (bitPtr and 7))) and 1
                        } else 0
                        bitPtr++
                        val masked = if ((r + c) % 2 == 0) bit xor 1 else bit
                        modules[r][c] = masked == 1
                    }
                }
            }
            right -= 2
            upward = !upward
        }

        // Format info for ECC Level L (01) and Mask 0 (000) -> 15-bit BCH = 0x77C4
        val formatBits = 0x77C4
        for (i in 0..5) setFunc(8, i, ((formatBits ushr (14 - i)) and 1) == 1)
        setFunc(8, 7, ((formatBits ushr 8) and 1) == 1)
        setFunc(8, 8, ((formatBits ushr 7) and 1) == 1)
        setFunc(7, 8, ((formatBits ushr 6) and 1) == 1)
        for (i in 0..5) setFunc(5 - i, 8, ((formatBits ushr (5 - i)) and 1) == 1)

        for (i in 0..6) setFunc(n - 1 - i, 8, ((formatBits ushr (14 - i)) and 1) == 1)
        for (i in 7..14) setFunc(8, n - 15 + i, ((formatBits ushr (14 - i)) and 1) == 1)

        return modules
    }
}
