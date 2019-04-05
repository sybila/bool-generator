package cz.muni.fi.sybila.bool.rg

import cz.muni.fi.sybila.bool.rg.bdd.BDDWorker
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

    private val params = BooleanParamEncoder(network)
    private val states = BooleanStateEncoder(network)

    private val universe = BDDWorker(params.parameterCount)

            /* Maps our parameter indices to BDD sets. */
    private val parameterVarNames = Array(params.parameterCount) { universe.variable(it) }
    private val parameterNotVarNames = Array(params.parameterCount) { universe.notVariable(it) }

    val empty: BDDSet = universe.zero
    val unit: BDDSet = run {
        var result = universe.one
        println("Num. parameters: ${params.parameterCount}")
        // Compute the "unit" BDD of valid parameters:
        for (r in network.regulations) {
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
        println("Unit BDD size: ${result.nodeSize()} and cardinality ${result.cardinality()}")
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
    private infix fun BDDSet.uAnd(that: BDDSet): BDDSet = universe.and(this, that)
    private infix fun BDDSet.uImp(that: BDDSet): BDDSet = universe.imp(this, that)
    private infix fun BDDSet.uBiImp(that: BDDSet): BDDSet = universe.biImp(this, that)
    private fun BDDSet.uNot(): BDDSet = universe.not(this)

    infix fun BDDSet.subset(that: BDDSet): Boolean {
        BDDops += 1
        val implication = universe.imp(this, that)
        return universe.isUnit(implication)
    }

    infix fun BDDSet.or(that: BDDSet): BDDSet {
        BDDops += 1
        return universe.or(this, that)
    }
    infix fun BDDSet.and(that: BDDSet): BDDSet {
        BDDops += 1
        return this uAnd that
    }

    fun BDDSet.not(): BDDSet {
        BDDops += 1
        return this.uNot() and unit
    }

    fun BDDSet.isEmpty(): Boolean = universe.isEmpty(this)
    fun BDDSet.isNotEmpty(): Boolean = !universe.isEmpty(this)

    fun BDDSet.cardinality(): Double = universe.satCount(this)
    fun BDDSet.nodeSize(): Int = universe.nodeCount(this)
    //fun BDDSet.print() = universe.printSet(pointer)
    //fun memory() = universe.memoryUsage

    fun transitionParams(from: State, dimension: Dimension): BDDSet {
        val isActive = states.isActive(from, dimension)
        val parameterIndex = params.transitionParameter(from, dimension)
        // If we are active, we want to go down, so p = 0, otherwise we want to go up, so p = 1
        return (if (!isActive) parameterVarNames else parameterNotVarNames)[parameterIndex]
        //return parameterVarNames[parameterIndex].let { if (!isActive) it else it.not() }
    }

}