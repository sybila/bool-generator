package cz.muni.fi.sybila.bool.rg.bdd

import org.junit.Test
import kotlin.test.assertEquals

class BDDSolverTest {

    @Test
    fun andTest() {
        val solver = BDDWorker(2)
        solver.run {
            val a = variable(0)
            val b = variable(1)
            val and1 = solver.and(a, b)
            // We expect two decision nodes:
            // Root will decide based on B, true -> go to A, false, 0
            // then if A true, go to 1, else go to zero
            assertEquals(listOf(-1,-1,0,1,0,0,4,1), and1.toList())
            val and2 = solver.and(b, a)
            assertEquals(listOf(-1,-1,0,1,0,0,4,1), and2.toList())
            val and3 = solver.and(a, a)
            assertEquals(a.toList(), and3.toList())
            val and4 = solver.and(a, one)
            assertEquals(a.toList(), and4.toList())
            val and5 = solver.and(one, a)
            assertEquals(a.toList(), and5.toList())
            val and6 = solver.and(a, zero)
            assertEquals(zero.toList(), and6.toList())
            val and7 = solver.and(zero, a)
            assertEquals(zero.toList(), and7.toList())
        }
    }

}