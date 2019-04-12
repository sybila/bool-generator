package cz.muni.fi.sybila.bool.rg

import org.junit.Test
import kotlin.test.assertEquals

/**
 * We consider two simple networks:
 *
 * One single specie network with auto-regulation and one two specie network A, B with
 * A -> B, B -> B and A -> A.
 *
 */
class BooleanParamEncoderTest {

    private val network1 = BooleanNetwork(
            species = listOf("A"),
            regulations = listOf(
                    BooleanNetwork.Regulation(0, 0, true, null)
            )
    )

    private val network2 = BooleanNetwork(
            species = listOf("A", "B"),
            regulations = listOf(
                    BooleanNetwork.Regulation(0, 0, true, null),
                    BooleanNetwork.Regulation(1, 1, true, null),
                    BooleanNetwork.Regulation(0, 1, true, null)
            )
    )

    @Test
    fun parameterCountTest1() {
        val enc = BooleanParamEncoder(network1)
        assertEquals(2, enc.parameterCount)
    }

    @Test
    fun parameterCountTest2() {
        val enc = BooleanParamEncoder(network2)
        assertEquals(6, enc.parameterCount)
    }

    @Test
    fun transitionParameterTest1() {
        val enc = BooleanParamEncoder(network1)
        assertEquals(0, enc.transitionParameter(0, 0))
        assertEquals(1, enc.transitionParameter(1, 0))
    }

    @Test
    fun transitionParameterTest2() {
        val enc = BooleanParamEncoder(network2)
        // 0bBA
        assertEquals(0, enc.transitionParameter(0b00, 0))
        assertEquals(0, enc.transitionParameter(0b10, 0))
        assertEquals(1, enc.transitionParameter(0b01, 0))
        assertEquals(1, enc.transitionParameter(0b11, 0))
        assertEquals(2, enc.transitionParameter(0b00, 1))
        assertEquals(3, enc.transitionParameter(0b01, 1))
        assertEquals(4, enc.transitionParameter(0b10, 1))
        assertEquals(5, enc.transitionParameter(0b11, 1))
    }

    @Test
    fun regulationPairsTest1() {
        val enc = BooleanParamEncoder(network1)
        assertEquals(setOf(0 to 1), enc.regulationPairs(0, 0).toSet())
    }

    @Test
    fun regulationPairsTest2() {
        val enc = BooleanParamEncoder(network2)
        assertEquals(setOf(0 to 1), enc.regulationPairs(0, 0).toSet())
        assertEquals(setOf(2 to 4, 3 to 5), enc.regulationPairs(1, 1).toSet())
        assertEquals(setOf(2 to 3, 4 to 5), enc.regulationPairs(0, 1).toSet())
    }

}