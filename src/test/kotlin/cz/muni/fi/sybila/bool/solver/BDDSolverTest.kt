package cz.muni.fi.sybila.bool.solver

import org.junit.Test
import java.nio.ByteBuffer
import kotlin.test.assertEquals

class BDDSolverTest {

    @Test
    fun basicVarFunctionalityTest() {
        val solver = BDDSolver(1)
        solver.run {
            assertEquals("1", one(0).prettyPrint())
            assertEquals("0", zero(0).prettyPrint())
        }
    }


    @Test
    fun ttAndffTest() {
        val solver = BDDSolver(1)
        solver.run {
            assertEquals(1, tt.ref)
            assertEquals(0, ff.ref)
            assertEquals(1, tt.or(ff).ref)
            assertEquals(0, tt.and(ff).ref)
        }
    }


    @Test
    fun varCountTest() {
        val solver = BDDSolver(10)
        solver.run {
            assertEquals(10, vars.size)
            assertEquals(10, one(0).prettyPrint().length)
        }
    }

    @Test
    fun varTest() {
        val solver = BDDSolver(4)
        solver.run {
            assertEquals("01--\n1---", one(0).or(one(1)).prettyPrint())
            assertEquals("1111", one(0).and(one(1)).and(one(2)).and(one(3)).prettyPrint())
            assertEquals("0011", zero(0).and(zero(1)).and(one(2)).and(one(3)).prettyPrint())

        }
    }


    @Test
    fun coloringTest() {
        val solver = BDDSolver(4)
        var byteBuffer: ByteBuffer = ByteBuffer.allocate(0)
        solver.run {
            val a = one(0).or(zero(1))

            byteBuffer = ByteBuffer.allocate(a.byteSize())
            byteBuffer.putColors(a)
        }



        val recipientSolver = BDDSolver(4)
        recipientSolver.run {
            val b = byteBuffer.getColors()

            assertEquals("00--\n1---",b.prettyPrint())
        }
    }

}