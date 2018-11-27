package cz.muni.fi.sybila.bool

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BooleanStateEncoderTest {

    @Test
    fun getVarValueTest() {
        BooleanStateEncoder.make(3).apply {

            // All true
            assertTrue { 0b111.getVarValue(0) }
            assertTrue { 0b111.getVarValue(1) }
            assertTrue { 0b111.getVarValue(2) }

            // All false
            assertFalse { 0b000.getVarValue(0) }
            assertFalse { 0b000.getVarValue(1) }
            assertFalse { 0b000.getVarValue(2) }

            // Mixed
            assertTrue { 0b101.getVarValue(0) }
            assertFalse { 0b101.getVarValue(1) }
            assertTrue { 0b101.getVarValue(2) }

            assertTrue { 0b011.getVarValue(0) }
            assertTrue { 0b011.getVarValue(1) }
            assertFalse { 0b011.getVarValue(2) }

            // Invalid
            assertFails { 0b0.getVarValue(-1) }
            assertFails { 0x11000.getVarValue(3) }

        }
    }

    @Test
    fun setVarValueTest() {
        BooleanStateEncoder.make(3).apply {

            // Set to true
            assertEquals(0b001, 0b000.setVarValue(0, true))
            assertEquals(0b010, 0b000.setVarValue(1, true))
            assertEquals(0b100, 0b000.setVarValue(2, true))
            assertEquals(0b011, 0b010.setVarValue(0, true))
            assertEquals(0b011, 0b011.setVarValue(0, true))
            assertEquals(0b111, 0b011.setVarValue(2, true))

            // Set to false
            assertEquals(0b000, 0b001.setVarValue(0, false))
            assertEquals(0b000, 0b010.setVarValue(1, false))
            assertEquals(0b000, 0b100.setVarValue(2, false))
            assertEquals(0b010, 0b011.setVarValue(0, false))
            assertEquals(0b010, 0b010.setVarValue(0, false))
            assertEquals(0b101, 0b111.setVarValue(1, false))

            // Invalid
            assertFails { 0b0.setVarValue(-1, true) }
            assertFails { 0b1011.setVarValue(3, true) }

        }
    }

    @Test
    fun decodeStateTest() {
        BooleanStateEncoder.make(5).apply {

            // make new array
            assertEquals(listOf(true, false, true, true, false), 0b01101.decodeState().toList())

            // cached array
            val dest = BooleanArray(5)
            0b01101.decodeState(dest)
            assertEquals(listOf(true, false, true, true, false), dest.toList())

            // invalid array
            assertFails { 0b01101.decodeState(BooleanArray(3)) }
            assertFails { 0b01101.decodeState(BooleanArray(10)) }

        }
    }

    @Test
    fun encodeStateTest() {
        BooleanStateEncoder.make(5).apply {

            // valid state
            assertEquals(0b01101, booleanArrayOf(true, false, true, true, false).encodeState())

            // invalid state
            assertFails { booleanArrayOf(true, false, true).encodeState() }
            assertFails { booleanArrayOf(true, true, false, true, false, true).encodeState() }

        }
    }

}