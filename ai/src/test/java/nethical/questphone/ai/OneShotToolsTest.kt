package nethical.questphone.ai

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class OneShotToolsTest {

    @Test
    fun padTokenIds_emptyInput_returnsEosFollowedByPadding() {
        val result = padTokenIds(intArrayOf(), maxLength = 64, padTokenId = 0L, eosTokenId = 1L)
        assertEquals(64, result.size)
        assertEquals(1L, result[0]) // EOS token at first position
        for (i in 1 until 64) {
            assertEquals("Expected padding at index $i", 0L, result[i])
        }
    }

    @Test
    fun padTokenIds_shortInputWithoutEos_appendsEosAndPads() {
        val input = intArrayOf(101, 202, 303)
        val result = padTokenIds(input, maxLength = 8, padTokenId = 0L, eosTokenId = 1L)
        assertEquals(8, result.size)
        assertArrayEquals(
            longArrayOf(101L, 202L, 303L, 1L, 0L, 0L, 0L, 0L),
            result
        )
    }

    @Test
    fun padTokenIds_inputAlreadyEndingWithEos_doesNotDuplicateEos() {
        val input = intArrayOf(101, 202, 1) // 1 is EOS
        val result = padTokenIds(input, maxLength = 6, padTokenId = 0L, eosTokenId = 1L)
        assertEquals(6, result.size)
        assertArrayEquals(
            longArrayOf(101L, 202L, 1L, 0L, 0L, 0L),
            result
        )
    }

    @Test
    fun padTokenIds_inputLengthEqualsMaxLengthMinusOne_placesEosAtLastIndex() {
        val input = IntArray(63) { it + 10 }
        val result = padTokenIds(input, maxLength = 64, padTokenId = 0L, eosTokenId = 1L)
        assertEquals(64, result.size)
        for (i in 0 until 63) {
            assertEquals((i + 10).toLong(), result[i])
        }
        assertEquals(1L, result[63])
    }

    @Test
    fun padTokenIds_inputLongerThanMaxLength_truncatesAndEndsWithEos() {
        val input = IntArray(100) { it + 5 }
        val result = padTokenIds(input, maxLength = 64, padTokenId = 0L, eosTokenId = 1L)
        assertEquals(64, result.size)
        for (i in 0 until 63) {
            assertEquals((i + 5).toLong(), result[i])
        }
        assertEquals(1L, result[63])
    }

    @Test
    fun padTokenIds_inputExactlyMaxLengthEndingWithEos_preservesAll() {
        val input = IntArray(64) { it + 2 }
        input[63] = 1 // Set last to EOS
        val result = padTokenIds(input, maxLength = 64, padTokenId = 0L, eosTokenId = 1L)
        assertEquals(64, result.size)
        for (i in 0 until 63) {
            assertEquals((i + 2).toLong(), result[i])
        }
        assertEquals(1L, result[63])
    }

    @Test
    fun padTokenIds_defaultParameters_matchGemmaSiglipSpec() {
        val input = intArrayOf(50, 60)
        val result = padTokenIds(input)
        assertEquals(64, result.size)
        assertEquals(50L, result[0])
        assertEquals(60L, result[1])
        assertEquals(1L, result[2]) // Default EOS = 1L
        assertEquals(0L, result[3]) // Default PAD = 0L
        assertEquals(0L, result[63])
    }

    @Test(expected = IllegalArgumentException::class)
    fun padTokenIds_zeroMaxLength_throwsIllegalArgumentException() {
        padTokenIds(intArrayOf(1, 2, 3), maxLength = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun padTokenIds_negativeMaxLength_throwsIllegalArgumentException() {
        padTokenIds(intArrayOf(1, 2, 3), maxLength = -5)
    }

    @Test
    fun padTokenIds_customPadAndEos_appliesCorrectly() {
        val input = intArrayOf(42)
        val result = padTokenIds(input, maxLength = 4, padTokenId = 999L, eosTokenId = 777L)
        assertArrayEquals(longArrayOf(42L, 777L, 999L, 999L), result)
    }

    @Test
    fun padTokenIds_singleEosOnlyInput_preservesWithoutDuplicate() {
        val input = intArrayOf(1)
        val result = padTokenIds(input, maxLength = 4, padTokenId = 0L, eosTokenId = 1L)
        assertArrayEquals(longArrayOf(1L, 0L, 0L, 0L), result)
    }
}
