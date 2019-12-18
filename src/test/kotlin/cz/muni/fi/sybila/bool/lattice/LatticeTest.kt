package cz.muni.fi.sybila.bool.lattice

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LatticeTest {

    val vv = Lattice("--")
    val Iv = Lattice("1-")
    val Ov = Lattice("0-")
    val vI = Lattice("-1")
    val vO = Lattice("-0")
    val OO = Lattice("00")
    val II = Lattice("11")
    val OI = Lattice("01")
    val IO = Lattice("10")

    @Test
    fun intersectionTest() {
        assertEquals(OO, OO intersect vv)
        assertEquals(OO, OO intersect Ov)
        assertEquals(OO, OO intersect vO)
        assertEquals(null, OO intersect Iv)
        assertEquals(null, OO intersect vI)
        assertEquals(null, OO intersect IO)
        assertEquals(vv, vv intersect vv)
        assertEquals(II, vI intersect Iv)
        assertEquals(OI, Ov intersect vI)
    }

    @Test
    fun supersetTest() {
        assertTrue(OO supersetOf OO)
        assertTrue(vv supersetOf OO)
        assertTrue(vO supersetOf OO)
        assertTrue(Iv supersetOf IO)
        assertFalse(OO supersetOf vO)
        assertFalse(OO supersetOf IO)
        assertFalse(vI supersetOf vO)
        assertFalse(vI supersetOf vv)
    }

    @Test
    fun invertTest() {
        assertEquals(setOf(), vv.invert())
        assertEquals(setOf(Ov), Iv.invert())
        assertEquals(setOf(vI), vO.invert())
        assertEquals(setOf(Ov, vI), IO.invert())
    }

     @Test
     fun subtractTest() {
         assertEquals(setOf(vI), vv.subtract(vO))
         assertEquals(setOf(II), vI.subtract(Ov))
         assertEquals(setOf(OO), vO.subtract(IO))
         assertEquals(setOf(), vI.subtract(vI))
         assertEquals(setOf(), IO.subtract(vv))
         assertEquals(setOf(), Ov.subtract(vv))
         assertEquals(setOf(Ov, vI), vv.subtract(IO))
         assertEquals(setOf(Iv, vI), vv.subtract(OO))
     }

}