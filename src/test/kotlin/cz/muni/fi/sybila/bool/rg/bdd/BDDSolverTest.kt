package cz.muni.fi.sybila.bool.rg.bdd

import org.junit.Test
import kotlin.test.assertEquals

class BDDSolverTest {
/*
    @Test
    fun andTest() {
        val solver = BDDSolver()
        solver.run {
            val a = variable(0)
            val b = variable(1)
            val and1 = a and b
            // We expect two decision nodes:
            // Root will decide based on B, true -> go to A, false, 0
            // then if A true, go to 1, else go to zero
            assertEquals(listOf(-1,-1,0,1,0,0,4,1), and1.toList())
            val and2 = b and a
            assertEquals(listOf(-1,-1,0,1,0,0,4,1), and2.toList())
            val and3 = a and a
            assertEquals(a.toList(), and3.toList())
            val and4 = a and one
            assertEquals(a.toList(), and4.toList())
            val and5 = one and a
            assertEquals(a.toList(), and5.toList())
            val and6 = a and zero
            assertEquals(zero.toList(), and6.toList())
            val and7 = zero and a
            assertEquals(zero.toList(), and7.toList())
        }
    }*/

}