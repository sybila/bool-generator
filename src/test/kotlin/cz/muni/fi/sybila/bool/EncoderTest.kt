package cz.muni.fi.sybila.bool

import org.junit.Test
import kotlin.test.assertEquals

class EncoderTest {

    private val levels = intArrayOf(1, 5, 3, 2)

    private val state1 = intArrayOf(0, 0, 0, 0)
    private val state2 = intArrayOf(1, 5, 3, 2)
    private val state3 = intArrayOf(1, 3, 1, 0)
    private val state4 = intArrayOf(0, 1, 3, 1)

    // multipliers: 1, 2, 12, 48
    // state count = 144

    private val s1 = 0*1 + 0*2 + 0*12 + 0*48
    private val s2 = 1*1 + 5*2 + 3*12 + 2*48
    private val s3 = 1*1 + 3*2 + 1*12 + 0*48
    private val s4 = 0*1 + 1*2 + 3*12 + 1*48

    @Test
    fun encode() {

        val encoder = BooleanModel.Encoder(levels)

        assertEquals(s1, encoder.encodeState(state1))
        assertEquals(s2, encoder.encodeState(state2))
        assertEquals(s3, encoder.encodeState(state3))
        assertEquals(s4, encoder.encodeState(state4))

    }

}