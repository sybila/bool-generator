package cz.muni.fi.sybila.bool

import com.github.sybila.checker.Transition
import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.huctl.Facet
import org.junit.Test
import kotlin.test.assertEquals

class BooleanStateSpaceGeneratorTest {

    @Test
    fun noParametersTwoVarTest() {
        // A very simple boolean network with two variables, no parameters and following update functions:
        // x' = x and y
        // y' = x xor y
        val model = BooleanModel(0,
                BooleanModel.Variable("x") { s ->
                    if (s.getVarValue(0) && s.getVarValue(1)) tt else ff
                },
                BooleanModel.Variable("y") { s ->
                    if (s.getVarValue(0) xor s.getVarValue(1)) tt else ff
                }
        )

        BooleanStateSpaceGenerator(model).apply {

            // 00 -> 00 because 0 and 0 = 0, 0 xor 0 = 0
            assertEquals(setOf(
                Transition(0b00, DirectionFormula.Atom.Loop, tt)
            ), 0b00.successors(true).asSequence().toSet())

            // 01 -> 01 because 0 and 0 = 0, 0 xor 1 = 1
            assertEquals(setOf(
                    Transition(0b01, DirectionFormula.Atom.Loop, tt)
            ), 0b01.successors(true).asSequence().toSet())

            // 10 -> 00, 10 -> 11 because 1 and 0 = 0, 1 xor 0 = 1
            assertEquals(setOf(
                    Transition(0b00, DirectionFormula.Atom.Proposition("x", Facet.NEGATIVE), tt),
                            Transition(0b11, DirectionFormula.Atom.Proposition("y", Facet.POSITIVE), tt)
            ), 0b10.successors(true).asSequence().toSet())

            // 11 -> 10 because 1 and 1 = 1, 1 xor 1 = 0
            assertEquals(setOf(
                    Transition(0b10, DirectionFormula.Atom.Proposition("y", Facet.NEGATIVE), tt)
            ), 0b11.successors(true).asSequence().toSet())

        }
    }

    @Test
    fun oneParamTwoVarTest() {
        // A very simple boolean network with two variables, one parameters and following update functions:
        // x' = x and y
        // y' = x (p => or, !p => xor) y (or and xor differ by exactly one truth value

        val model = BooleanModel(1,
                BooleanModel.Variable("x") { s ->
                    if (s.getVarValue(0) && s.getVarValue(1)) tt else ff
                },
                BooleanModel.Variable("y") { s ->
                    val x = s.getVarValue(0); val y = s.getVarValue(1)
                    if (x && y) {
                        one(0)  // update is one if parameter is one
                    } else {
                        // otherwise run either function (or/xor)
                        val update = x or y
                        if (update) tt else ff
                    }
                }
        )

        BooleanStateSpaceGenerator(model).apply {

            // 00 -> 00 because 0 and 0 = 0, 0 or 0 = 0, 0 xor 0 = 0
            assertEquals(setOf(
                    Transition(0b00, DirectionFormula.Atom.Loop, tt)
            ), 0b00.successors(true).asSequence().toSet())

            // 01 -> 01 because 0 and 1 = 0, 0 or 1 = 1, 0 xor 1 = 1
            assertEquals(setOf(
                    Transition(0b01, DirectionFormula.Atom.Loop, tt)
            ), 0b01.successors(true).asSequence().toSet())

            // 10 -> 00, 10 -> 11 because 1 and 0 = 0, 1 or 0 = 1, 1 xor 0 = 1
            assertEquals(setOf(
                    Transition(0b00, DirectionFormula.Atom.Proposition("x", Facet.NEGATIVE), tt),
                    Transition(0b11, DirectionFormula.Atom.Proposition("y", Facet.POSITIVE), tt)
            ), 0b10.successors(true).asSequence().toSet())

            // 11 -!p-> 10, 11 -p-> 11 because 1 and 1 = 1, 1 or 1 = 1, 1 xor 1 = 0
            assertEquals(setOf(
                    Transition(0b10, DirectionFormula.Atom.Proposition("y", Facet.NEGATIVE), zero(0)),
                    Transition(0b11, DirectionFormula.Atom.Loop, one(0))
            ), 0b11.successors(true).asSequence().toSet())

        }
    }

}