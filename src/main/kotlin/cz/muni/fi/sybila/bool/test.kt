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

    val parser = BooleanModelParser()
    val model2 = parser.readString("""
        a:1 <- b:1
        a:2 <- b:1&c
        b:2 <- a:2|!c
        b:1 <- a:2&c
        c <- c&a:2
    """.trimIndent())
    println("Parsed model: $model2")

    println("======= Run Model 1 =======")
    runExperiments(model1)
    println("======= Run Model 2 =======")
    runExperiments(model2)
}

fun runExperiments(model: BooleanModel) = BooleanFragment(model).run {
            /*val aGreaterOne = "a".asVariable() ge 1.0.asConstant()
            val states = aGreaterOne.eval()*/

            println("Verify EF(a = 0 & b = 0 & c = 0)")
            val zero = 0.0.asConstant()
            val allZero = ("a".asVariable() eq zero) and ("b".asVariable() eq zero) and ("c".asVariable() eq zero)
            val reachZero = EF(allZero)
            SequentialChecker(this).use { checker ->
                val states = checker.verify(reachZero)
                for ((s, _) in states.entries()) {
                    println(s.prettyPrint(model))
                }
            }
            println("Reachability for a=0, b=2, c=1")
            val init = intArrayOf(0, 2, 1)
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

