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

            // asi chyba v testoch, vysledok by mal by 0b10 nie 0b01
            // 01 -> 01 because 0 and 1 = 0, 0 xor 1 = 1
            assertEquals(setOf(
                    Transition(0b00, DirectionFormula.Atom.Proposition("x", Facet.NEGATIVE), tt),
                    Transition(0b11, DirectionFormula.Atom.Proposition("y", Facet.POSITIVE), tt)
            ), 0b01.successors(true).asSequence().toSet())

            // 10 -> 10 because 1 and 0 = 0, 1 xor 0 = 1
            assertEquals(setOf(
                    Transition(0b10, DirectionFormula.Atom.Loop, tt)
            ), 0b10.successors(true).asSequence().toSet())

            // 11 -> 10 because 1 and 1 = 1, 1 xor 1 = 0
            assertEquals(setOf(
                    Transition(0b01, DirectionFormula.Atom.Proposition("y", Facet.NEGATIVE), tt)
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
                    val x = s.getVarValue(0)
                    val y = s.getVarValue(1)
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

            // 01 -> 01 because 1 and 0 = 0, 1 or 0 = 1, 1 xor 0 = 1
            assertEquals(setOf(
                    Transition(0b11, DirectionFormula.Atom.Proposition("y", Facet.POSITIVE), tt),
                    Transition(0b00, DirectionFormula.Atom.Proposition("x", Facet.NEGATIVE), tt)
            ), 0b01.successors(true).asSequence().toSet())

            // 10 -> 10 because 0 and 1 = 0, 0 or 1 = 1, 0 xor 1 = 1
            assertEquals(setOf(
                    Transition(0b10, DirectionFormula.Atom.Loop, tt)
            ), 0b10.successors(true).asSequence().toSet())

            // 11 -!p-> 10, 11 -p-> 11 because 1 and 1 = 1, 1 or 1 = 1, 1 xor 1 = 0
            assertEquals(setOf(
                    Transition(0b01, DirectionFormula.Atom.Proposition("y", Facet.NEGATIVE), zero(0)),
                    Transition(0b11, DirectionFormula.Atom.Loop, one(0))
            ), 0b11.successors(true).asSequence().toSet())

        }
    }

    @Test
    fun noParameterMultipleVarsTest() {
        // A very simple boolean network with two variables, one parameters and following update functions:
        // x' = x and y
        // y' = x (p => or, !p => xor) y (or and xor differ by exactly one truth value

        val brokenModel = BooleanModel(0,
                BooleanModel.Variable("a") { s ->
                    val a = s.getVarValue(0)
                    val b = s.getVarValue(1)
                    val c = s.getVarValue(2)
                    val d = s.getVarValue(3)
                    when {
                        !a && b && !c && !d -> tt
                        a && !c && !d -> tt
                        else -> ff
                    }
                },
                BooleanModel.Variable("b") { s ->
                    val a = s.getVarValue(0)
                    val c = s.getVarValue(2)

                    if (a.not() && c) tt
                    else ff
                },

                BooleanModel.Variable("c") { s ->
                    val a = s.getVarValue(0)
                    val b = s.getVarValue(1)
                    val c = s.getVarValue(2)
                    val d = s.getVarValue(3)

                    if (a && !b && !c && d) tt
                    else ff
                },
                BooleanModel.Variable("d") { s ->
                    val a = s.getVarValue(0)
                    val d = s.getVarValue(3)
                    val e = s.getVarValue(4)

                    if (a && !d && !e) tt
                    else ff
                },

                BooleanModel.Variable("e") { s ->
                    val a = s.getVarValue(0)
                    val c = s.getVarValue(2)

                    if (a && !c) tt
                    else ff
                }

        )
        /*

            a :=
              case
              case
                (a = 0) & (b = 1) & (d = 0) & (e = 0) : 1;
                (a = 1) & (d = 0) & (e = 0) : 1;
                else : 0;one(0)
              esac;
            b :=
              caseone(0)
                (a = 0) & (c = 1) : 1;
                else : 0;
              esac;
            c :=
              case
                (a = 1) & (b = 0) & (c = 0) & (d = 1) : 1;
                else : 0;
              esac;
            d :=
              case
                (a = 1) & (d = 0) & (e = 0) : 1;
                else : 0;
              esac;
            e :=
              case
                (a = 1) & (c = 0) : 1;
                else : 0;
              esac;
            */

        BooleanStateSpaceGenerator(brokenModel).apply {

            // just e
            assertEquals(setOf(
                    Transition(0b00000, DirectionFormula.Atom.Proposition("e", Facet.NEGATIVE), tt)
            ), 0b10000.successors(true).asSequence().toSet())

            // just d
            assertEquals(setOf(
                    Transition(0b00000, DirectionFormula.Atom.Proposition("d", Facet.NEGATIVE), tt)
            ), 0b01000.successors(true).asSequence().toSet())

            // just c
            assertEquals(setOf(
                    Transition(0b00000, DirectionFormula.Atom.Proposition("c", Facet.NEGATIVE), tt),
                    Transition(0b00110, DirectionFormula.Atom.Proposition("b", Facet.POSITIVE), tt)
            ), 0b00100.successors(true).asSequence().toSet())

            // just b
            assertEquals(setOf(
                    Transition(0b00011, DirectionFormula.Atom.Proposition("a", Facet.POSITIVE), tt),
                    Transition(0b00000, DirectionFormula.Atom.Proposition("b", Facet.NEGATIVE), tt)
            ), 0b00010.successors(true).asSequence().toSet())

            // just a
            assertEquals(setOf(
                    Transition(0b10001, DirectionFormula.Atom.Proposition("e", Facet.POSITIVE), tt),
                    Transition(0b01001, DirectionFormula.Atom.Proposition("d", Facet.POSITIVE), tt)
            ), 0b00001.successors(true).asSequence().toSet())


            // rozsirit
        }

    }

    @Test
    fun `six parameters and two vars Test`() {

        /*
            x | y | x'       |   y'  |
            1 | 1 | p0       |   0   |
            1 | 0 | p1 or p2 |  !p3  |
            0 | 1 | 1        |   p4  |
            0 | 0 | p5       |   p5  |

         */
        // vracia mnozinu parametrov

        val model = BooleanModel(6,
                BooleanModel.Variable("x") { s ->
                    val x = s.getVarValue(0)
                    val y = s.getVarValue(1)

                    when {
                        x and y -> one(0)
                        x and !y -> one(1).or(one(2))
                        !x and y -> tt
                        !x and !y -> one(5)
                        else -> throw IllegalStateException("This shoudln't be possible.")
                    }
                },
                BooleanModel.Variable("y") { s ->
                    val x = s.getVarValue(0)
                    val y = s.getVarValue(1)

                    when {
                        x and y -> ff
                        x and !y -> zero(3)
                        !x and y -> one(4)
                        !x and !y -> one(5)
                        else -> throw IllegalStateException("This shoudln't be possible.")
                    }
                }
        )

        BooleanStateSpaceGenerator(model).apply {


            // 11 -!p0-> 10, 11 -> 01
            assertEquals(setOf(
                    Transition(0b10, DirectionFormula.Atom.Proposition("x", Facet.NEGATIVE), zero(0)),
                    Transition(0b01, DirectionFormula.Atom.Proposition("y", Facet.NEGATIVE), tt)),
                    0b11.successors(true).asSequence().toSet())
            
            // 01 -!(p1 or p2)-> 00, 01 -!p3-> 11
            assertEquals(setOf(
                    Transition(0b00, DirectionFormula.Atom.Proposition("x", Facet.NEGATIVE), zero(1) and zero(2)),
                    Transition(0b11, DirectionFormula.Atom.Proposition("y", Facet.POSITIVE), zero(3)),
                    Transition(0b01, DirectionFormula.Atom.Loop, (one(1) or one(2)) and one(3))),
                    0b01.successors(true).asSequence().toSet())



            // 10 -> 11, 10 -!p4>  00
            assertEquals(setOf(
                    Transition(0b11, DirectionFormula.Atom.Proposition("x", Facet.POSITIVE), tt),
                    Transition(0b00, DirectionFormula.Atom.Proposition("y", Facet.NEGATIVE), zero(4))),
                    0b10.successors(true).asSequence().toSet())



            // 00 -p5-> 10, 00 -p5-> 01, 00->!p-> 00
            assertEquals(setOf(
                    Transition(0b01, DirectionFormula.Atom.Proposition("x", Facet.POSITIVE), one(5)),
                    Transition(0b10, DirectionFormula.Atom.Proposition("y", Facet.POSITIVE), one(5)),
                    Transition(0b00, DirectionFormula.Atom.Loop, zero(5))
            ),
                    0b00.successors(true).asSequence().toSet())
        }
    }

}