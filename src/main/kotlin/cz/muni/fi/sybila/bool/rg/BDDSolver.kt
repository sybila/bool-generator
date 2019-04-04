package cz.muni.fi.sybila.bool.rg

import jdd.bdd.BDD

/**
 * Solver constructs a universe parameter set space based on a given boolean network
 * and then provides basic universe operations for these sets.
 */
class BDDSolver(
        private val network: BooleanNetwork
) {

    private val universe = BDD(1000, 1000)
    private val params = BooleanParamEncoder(network)
    private val states = BooleanStateEncoder(network)

    /* Maps our parameter indices to BDD sets. */
    private val parameterVarNames = Array(params.parameterCount) { BDDSet(universe.createVar(), universe) }

    private val zero = BDDSet(0, universe)
    private val one = BDDSet(1, universe)

    val empty: BDDSet = zero
    val unit: BDDSet = run {
        var result = one
        // Compute the "unit" BDD of valid parameters:
        for (r in network.regulations) {
            val pairs = params.regulationPairs(r.regulator, r.target).map { (off, on) ->
                parameterVarNames[off] to parameterVarNames[on]
            }
            if (r.observable) {
                result = result uAnd pairs.fold(zero) { formula, (off, on) ->
                    // off != on
                    formula or ((off uAnd on.uNot()) or (off.uNot() uAnd on))
                }
            }
            if (r.effect == BooleanNetwork.Effect.ACTIVATION) {
                result = result uAnd pairs.fold(one) { formula, (off, on) ->
                    // off <= on
                    formula uAnd (off uImp on)
                }
            }
            if (r.effect == BooleanNetwork.Effect.INHIBITION) {
                result = result and pairs.fold(one) { formula, (off, on) ->
                    // off >= on
                    formula uAnd (on uImp off)
                }
            }
        }
        println("Unit BDD cardinality: ${result.cardinality()}")
        result
    }

    // unsafe operations are needed to compute unit BDD
    private infix fun BDDSet.uAnd(that: BDDSet): BDDSet = BDDSet(universe.and(this.pointer, that.pointer), universe)
    private infix fun BDDSet.uImp(that: BDDSet): BDDSet = BDDSet(universe.imp(this.pointer, that.pointer), universe)
    private fun BDDSet.uNot(): BDDSet = BDDSet(universe.not(this.pointer), universe)

    infix fun BDDSet.or(that: BDDSet): BDDSet = BDDSet(universe.or(this.pointer, that.pointer), universe)
    infix fun BDDSet.and(that: BDDSet): BDDSet = BDDSet(universe.and(this.pointer, that.pointer), universe)

    // warning: Some operations need to be intersected with unit!

    infix fun BDDSet.implies(that: BDDSet): BDDSet = BDDSet(universe.imp(this.pointer, that.pointer), universe) and unit
    fun BDDSet.not(): BDDSet = BDDSet(universe.not(this.pointer), universe) and unit

    fun BDDSet.isEmpty(): Boolean = pointer == 0
    fun BDDSet.isNotEmpty(): Boolean = pointer != 0

    fun BDDSet.cardinality(): Double = universe.satCount(pointer)
    fun BDDSet.print() = universe.printSet(pointer)

    fun transitionParams(from: State, dimension: Dimension): BDDSet {
        val isActive = states.isActive(from, dimension)
        val parameterIndex = params.transitionParameter(from, dimension)
        // If we are active, we want to go down, so p = 0, otherwise we want to go up, so p = 1
        return parameterVarNames[parameterIndex].let { if (!isActive) it else it.not() }
    }

}