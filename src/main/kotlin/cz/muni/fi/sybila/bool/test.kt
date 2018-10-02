package cz.muni.fi.sybila.bool

import com.github.sybila.checker.SequentialChecker
import com.github.sybila.huctl.*

fun main(args: Array<String>) {
    /*
            a:1 <- b:1
            a:2 <- b:1&c
            b:2 <- a:2|!c
            b:1 <- a:2&c
            c <- c&a:2
     */
    val iA = 0
    val iB = 1
    val iC = 2
    val model1 = BooleanModel(
            BooleanModel.Variable("a", maxLevel = 2, levelFunctions = listOf(
                    atLeast(iB, 1), atLeast(iB, 1) and isValue(iC, 1)
            )),
            BooleanModel.Variable("b", maxLevel = 2, levelFunctions = listOf(
                    atLeast(iA, 2) and isValue(iC, 1), atLeast(iA, 2) or !isValue(iC, 1)
            )),
            BooleanModel.Variable("c", maxLevel = 1, levelFunctions = listOf(
                    isValue(iC, 1) and atLeast(iA, 2)
            ))
    )

    var xy = True
    var xz = True
    var yz = True
    var sx = True
    var sy = True

    val parser = BooleanModelParser()
    var model2 = parser.readString("""
        sx <- sx
        sy <- sy
        x <- sx
        y <- x&sy
        z <- x&y
    """.trimIndent())
    println("Parsed model: $model2")

    // println("======= Run Model 1 =======")
    // runExperiments(model1)
    println("======= Run Model 2 =======")
    runExperiments(model2)
}

fun runExperiments(model: BooleanModel) = BooleanFragment(model).run {
            /*val aGreaterOne = "a".asVariable() ge 1.0.asConstant()
            val states = aGreaterOne.eval()*/
            val zero = 0.0.asConstant()
            val one = 1.0.asConstant()
            val allZero = ("x".asVariable() eq zero) and ("y".asVariable() eq zero) and ("z".asVariable() eq zero)

            println("Verify EF(x = 0 & y = 0 & z = 0)")
            val reachZero = EF(allZero)
            SequentialChecker(this).use { checker ->
                val states = checker.verify(reachZero)
                for ((s, _) in states.entries()) {
                    println(s.prettyPrint(model))
                }
                println(states.sizeHint)
            }

            println("Verify delay_on: AU((z = 0),(y = 1))")
            val delay_on = (("sx".asVariable() eq one) and ("sy".asVariable() eq one) and ("x".asVariable() eq zero) and ("y".asVariable() eq zero) and ("z".asVariable() eq zero) and ("z".asVariable() eq zero)AU("y".asVariable() eq one))
            SequentialChecker(this).use { checker ->
                val states = checker.verify(delay_on)
                for ((s, _) in states.entries()) {
                    println(s.prettyPrint(model))
                }
                println(states.sizeHint)
            }

            println("Verify delay_on1: AU((z = 0),(y = 1))")
            val delay_on1 = (("z".asVariable() eq zero)AU("y".asVariable() eq one))
            SequentialChecker(this).use { checker ->
                val states = checker.verify(delay_on1)
                for ((s, _) in states.entries()) {
                    println(s.prettyPrint(model))
                }
                println(states.sizeHint)
            }

            println("Verify no_delay_off AF(~(S(z=1,y=1,sx=0,sy=1) & EX(S(y=0))) | EX(S(y=0,z=0)))")
            val no_delay_off = AF(not( ("z".asVariable() eq one) and ("y".asVariable() eq one) and ("sx".asVariable() eq zero) and ("sy".asVariable() eq one)) and EX("y".asVariable() eq zero)) or EX(("y".asVariable() eq zero) and ("z".asVariable() eq zero))
            SequentialChecker(this).use { checker ->
                val states = checker.verify(no_delay_off)
                for ((s, _) in states.entries()) {
                    println(s.prettyPrint(model))
                }
                println(states.sizeHint)
            }

            println("Verify delay_off: AU((z = 1),(y = 0))")
            val delay_off = ("z".asVariable() eq one)AU("y".asVariable() eq zero)
            SequentialChecker(this).use { checker ->
                val states = checker.verify(delay_off)
                for ((s, _) in states.entries()) {
                    println(s.prettyPrint(model))
                }
                println(states.sizeHint)
            }


            println("Verify no_delay_on AF(~(S(z=0,y=0,sx=1,sy=1) & EX(S(y=1))) | EX(S(y=1,z=1)))")
            val no_delay_on = AF(not( ("z".asVariable() eq zero) and ("y".asVariable() eq zero) and ("sx".asVariable() eq one) and ("sy".asVariable() eq one)) and EX("y".asVariable() eq one)) or EX(("y".asVariable() eq one) and ("z".asVariable() eq one))
            SequentialChecker(this).use { checker ->
                val states = checker.verify(no_delay_on)
                for ((s, _) in states.entries()) {
                    println(s.prettyPrint(model))
                }
                println(states.sizeHint)
            }

            println("Reachability for sx=1, sy=1, x=0, y=1, z=1")
            val init = intArrayOf(1, 1, 0, 1, 1)
            var recompute = setOf<Int>(model.encoder.encodeState(init))
            var discovered = recompute
            while (recompute.isNotEmpty()) {
                println("==== Iteration ====")
                recompute.forEach { println(it.prettyPrint(model)) }
                val step = recompute.flatMap { it.successors(true).asSequence().map { it.target }.toList() }.toSet()
                recompute = step - discovered
                discovered += recompute
            }


        }

