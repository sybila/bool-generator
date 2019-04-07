package cz.muni.fi.sybila.bool.rg

import java.lang.Exception
import kotlin.math.log2
import kotlin.system.exitProcess


class DecisionTree(
        private val parameters: Int,
        private val originalClasses: Map<List<String>, BSet>,
        private val solver: BDDSolver
) {

    private fun Map<List<String>, BSet>.entropy(): Double {
        var result = 0.0
        solver.run {
            val all = this@entropy.values.fold(empty) { a, b -> a or b.s }
            if (all.isEmpty()) return Double.POSITIVE_INFINITY
            for ((_, p) in this@entropy) {
                val proportion = (p.s.cardinality() / all.cardinality())
                //println("Proportion $proportion for ${p.cardinality()} of ${all.cardinality()}")
                result += -proportion * log2(proportion)
            }
            //println("Entropy: ${result} for ${all.cardinality()}")
        }
        return result
    }

    private fun Map<List<String>, BSet>.applyAttribute(set: BDDSet): Map<List<String>, BSet> {
        val original = this
        solver.run {
            val result = HashMap<List<String>, BSet>()
            for ((c, p) in original) {
                val restricted = p.s and set
                if (restricted.isNotEmpty()) {
                    result[c] = BSet(restricted)
                }
            }
            return result
        }
    }

    private class Attribute(val positive: BDDSet, val negative: BDDSet, val label: String) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Attribute

            if (!positive.contentEquals(other.positive)) return false
            if (!negative.contentEquals(other.negative)) return false
            if (label != other.label) return false

            return true
        }

        override fun hashCode(): Int {
            var result = positive.contentHashCode()
            result = 31 * result + negative.contentHashCode()
            result = 31 * result + label.hashCode()
            return result
        }
    }

    fun learn(): Int {
        val params = (0 until parameters) - solver.fixedOne - solver.fixedZero
        val attributes: List<Attribute> = (params).map {
            Attribute(solver.variable(it), solver.variableNot(it), "$it")
        }/* + ((params).flatMap { p1 ->
            (params).map { p2 ->
                // p1 >= p2 == p2 implies p1
                val imp = solver.run { variable(p2) uImp variable(p1) }
                solver.run { Attribute(imp, imp.uNot(), "$p1 >= $p2") }
            }
        })*/ + (params).flatMap { p1 ->
            (params).map { p2 ->
                if (p1 <= p2) null else {
                    val eq = solver.run { variable(p1) uBiImp variable(p2) }
                    solver.run { Attribute(eq,  eq.uNot(), "$p1 = $p2") }
                }
            }.filterNotNull()
        }/* + (params).flatMap { p1 ->
            (params).map { p2 ->
                if (p1 <= p2) null else {
                    val both = solver.run {variable(p1) and variable(p2) }
                    solver.run { Attribute(both, both.uNot(), "$p1, $p2") }
                }
            }.filterNotNull()
        }*/
        println("Attributes: ${attributes.size}")
        return (this.originalClasses.learn(attributes.filter {
            val gain = originalClasses.entropy() - (
                    0.5 * originalClasses.applyAttribute(it.positive).entropy() +
                    0.5 * originalClasses.applyAttribute(it.negative).entropy()
            )
            gain > Double.NEGATIVE_INFINITY
        }.toSet()) as Result.Decision).size
    }

    sealed class Result() {
        data class Leaf(val clazz: List<String>) : Result()
        data class Decision(
                val left: Leaf?,
                val right: Leaf?,
                val size: Int
        ) : Result()
    }

    var remaining: Double = run {
        solver.run {
            originalClasses.values.fold(empty) { a,b -> a or b.s }.cardinality()
        }
    }

    private fun Map<List<String>, BSet>.learn(attributes: Set<Attribute>): Result {
        val dataSet = this
        solver.run {
            //println("Learn data set cardinality ${dataSet.values.fold(empty) { a, b -> a or b.s }.cardinality()} for classes: ${dataSet.keys}")
            if (dataSet.size <= 1) {
                //println("Make leaf!")
                remaining -= dataSet.values.iterator().next().s.cardinality()
                println("Remaining: $remaining")
                return Result.Leaf(dataSet.keys.iterator().next())
            }
            var maxGain = Double.NEGATIVE_INFINITY
            var maxVar: Attribute? = null
            val gains = attributes.toList().mapParallel { v ->
                val gain = dataSet.entropy() - (
                        0.5 * dataSet.applyAttribute(v.positive).entropy() +
                                0.5 * dataSet.applyAttribute(v.negative).entropy()
                        )
                v to gain
            }
            for ((v, gain) in gains) {
                if (gain > maxGain) {
                    maxGain = gain
                    maxVar = v
                }
            }

            maxVar!!
            val resultPositive = dataSet.applyAttribute(maxVar.positive).learn(attributes - maxVar)
            val resultNegative = dataSet.applyAttribute(maxVar.negative).learn(attributes - maxVar)

            return when {
                resultPositive is Result.Leaf && resultNegative is Result.Leaf -> Result.Decision(resultNegative, resultPositive, 1)
                resultPositive is Result.Leaf -> {
                    resultNegative as Result.Decision
                    if (resultNegative.left == resultPositive) {
                        // merge with left leaf, right leaf can't be merged any more
                        Result.Decision(resultPositive, null, resultNegative.size)
                    } else if (resultNegative.right == resultPositive) {
                        // merge with right leaf, left leaf can't be merged any more
                        Result.Decision(null, resultPositive, resultNegative.size)
                    } else {
                        // can't be merged, add to correct side and increase size
                        Result.Decision(null, resultPositive, resultNegative.size + 1)
                    }
                }
                resultNegative is Result.Leaf -> {
                    resultPositive as Result.Decision
                    if (resultPositive.left == resultNegative) {
                        // merge with left leaf, right leaf can't be merged any more
                        Result.Decision(resultNegative, null, resultPositive.size)
                    } else if (resultPositive.right == resultNegative) {
                        // merge with right leaf, left leaf can't be merged any more
                        Result.Decision(null, resultNegative, resultPositive.size)
                    } else {
                        // can't be merged, add to correct side and increase size
                        Result.Decision(null, resultNegative, resultPositive.size + 1)
                    }
                }
                resultPositive is Result.Decision && resultNegative is Result.Decision -> {
                    Result.Decision(null, null, resultPositive.size + resultNegative.size + 1)
                }
                else -> error("unreachable")
            }


//            return (dataSet.applyAttribute(maxVar!!.positive).learn(attributes - maxVar) +
//                    dataSet.applyAttribute(maxVar.negative).learn(attributes - maxVar) + 1)
        }
    }

}

fun Map<List<String>, BSet>.joinToClass(clazz: List<String>, solver: BDDSolver): Map<List<String>, BSet> {
    val source = this
    val result = HashMap<List<String>, BSet>()
    solver.run {
        for ((c, p) in source) {
            if (c == clazz) {
                result[c] = p
            } else {
                result[listOf("other")] = BSet((result[listOf("other")]?.s ?: empty) or p.s)
            }
        }
    }
    return result
}