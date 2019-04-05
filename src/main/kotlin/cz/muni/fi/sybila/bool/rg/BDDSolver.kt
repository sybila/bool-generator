package cz.muni.fi.sybila.bool.rg

import jdd.bdd.BDD
import jdd.util.Configuration

/**
 * Solver constructs a universe parameter set space based on a given boolean network
 * and then provides basic universe operations for these sets.
 */
class BDDSolver(
        private val network: BooleanNetwork
) {

    var BDDops = 0
        private set

    private val universe = BDD(100_000_000, 10_000_000)
    private val params = BooleanParamEncoder(network)
    private val states = BooleanStateEncoder(network)

    /* Maps our parameter indices to BDD sets. */
    private val parameterVarNames = Array(params.parameterCount) { BDDSet(universe.createVar(), universe) }
    private val parameterNotVarNames = Array(params.parameterCount) { i -> parameterVarNames[i].uNot() }

    private val zero = BDDSet(0, universe)
    private val one = BDDSet(1, universe)

    val empty: BDDSet = zero
    val unit: BDDSet = run {
        var result = one
        println("Num. parameters: ${params.parameterCount}")
        // Compute the "unit" BDD of valid parameters:
        var i = 0
        for (r in network.regulations) {
            System.gc()
            val pairs = params.regulationPairs(r.regulator, r.target).map { (off, on) ->
                parameterVarNames[off] to parameterVarNames[on]
            }
            if (r.observable) {
                val constraint = pairs.map { (off, on) -> off uBiImp on }.merge { a, b -> a uAnd b }
                result = result uAnd constraint.uNot()
            }
            if (r.effect == BooleanNetwork.Effect.ACTIVATION) {
                val constraint = pairs.map { (off, on) -> off uImp on }.merge { a, b -> a uAnd b }
                result = result uAnd constraint
            }
            if (r.effect == BooleanNetwork.Effect.INHIBITION) {
                val constraint = pairs.map { (off, on) -> on uImp off }.merge { a, b -> a uAnd b }
                result = result uAnd constraint
            }
        }
        println("Unit BDD cardinality: ${result.cardinality()}")
        result
    }

    inline fun List<BDDSet>.merge(action: (BDDSet, BDDSet) -> BDDSet): BDDSet {
        var items = this
        while (items.size > 1) {
            items = items.mergePairs(action)
        }
        return items[0]
    }

    // unsafe operations are needed to compute unit BDD
    private infix fun BDDSet.uAnd(that: BDDSet): BDDSet = BDDSet(universe.and(this.pointer, that.pointer), universe)
    private infix fun BDDSet.uImp(that: BDDSet): BDDSet = BDDSet(universe.imp(this.pointer, that.pointer), universe)
    private infix fun BDDSet.uBiImp(that: BDDSet): BDDSet = BDDSet(universe.biimp(this.pointer, that.pointer), universe)
    private fun BDDSet.uNot(): BDDSet = BDDSet(universe.not(this.pointer), universe)

    infix fun BDDSet.subset(that: BDDSet): Boolean {
        BDDops += 1
        val implication = universe.imp(this.pointer, that.pointer)
        return implication == one.pointer
    }

    infix fun BDDSet.or(that: BDDSet): BDDSet {
        BDDops += 1
        return BDDSet(universe.or(this.pointer, that.pointer), universe)
    }
    infix fun BDDSet.and(that: BDDSet): BDDSet {
        BDDops += 1
        return BDDSet(universe.and(this.pointer, that.pointer), universe)
    }

    fun BDDSet.not(): BDDSet {
        BDDops += 1
        return BDDSet(universe.not(this.pointer), universe) and unit
    }

    fun BDDSet.isEmpty(): Boolean = pointer == 0
    fun BDDSet.isNotEmpty(): Boolean = pointer != 0

    fun BDDSet.cardinality(): Double = universe.satCount(pointer)
    fun BDDSet.nodeSize(): Int = universe.nodeCount(pointer)
    fun BDDSet.print() = universe.printSet(pointer)
    fun memory() = universe.memoryUsage

    fun transitionParams(from: State, dimension: Dimension): BDDSet {
        val isActive = states.isActive(from, dimension)
        val parameterIndex = params.transitionParameter(from, dimension)
        // If we are active, we want to go down, so p = 0, otherwise we want to go up, so p = 1
        return (if (!isActive) parameterVarNames else parameterNotVarNames)[parameterIndex]
        //return parameterVarNames[parameterIndex].let { if (!isActive) it else it.not() }
    }

}