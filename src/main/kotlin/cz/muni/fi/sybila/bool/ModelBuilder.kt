package cz.muni.fi.sybila.bool

import cz.muni.fi.sybila.bool.rg.BooleanNetwork
import cz.muni.fi.sybila.bool.rg.BooleanParamEncoder
import cz.muni.fi.sybila.bool.rg.mergePairs
import cz.muni.fi.sybila.bool.solver.BDD

fun BooleanNetwork.asBooleanModel(): BooleanModel {
    val params = BooleanParamEncoder(this)
    val variables = (0 until dimensions).map { d ->
        BooleanModel.Variable(species[d]) { state ->
            val parameter = params.transitionParameter(state, d)
            one(parameter)
        }
    }
    return BooleanModel(parameterCount = params.parameterCount, variables = variables)
}

fun BooleanNetwork.staticConstraintsBDD(solver: BooleanSolver): BDD = solver.run {
    val network = this@staticConstraintsBDD
    val params = BooleanParamEncoder(network)
    val alwaysOne = params.explicitOne.map { one(it) }.let {
        if (it.isEmpty()) tt else it.merge { a, b -> a and b }
    }
    var result = alwaysOne
    for (r in network.regulations) {
        val pairs = params.regulationPairs(r.regulator, r.target).map { (off, on) ->
            (alwaysOne and one(off)) to (alwaysOne and one(on))
        }
        if (r.observable) {
            val constraint = pairs
                    .map { (off, on) -> alwaysOne and ((off and on) or (off.not() and on.not())) }
                    .merge { a, b -> a and b }
            result = result and constraint.not()
        }
        if (r.effect == BooleanNetwork.Effect.ACTIVATION) {
            val constraint = pairs
                    .map { (off, on) -> alwaysOne and (off.not() or on) }
                    .merge { a, b -> a and b }
            result = result and constraint
        }
        if (r.effect == BooleanNetwork.Effect.INHIBITION) {
            val constraint = pairs
                    .map { (off, on) -> alwaysOne and (on.not() or off) }
                    .merge { a, b -> a and b }
            result = result and constraint
        }
    }
    result
}

inline fun List<BDD>.merge(crossinline action: (BDD, BDD) -> BDD): BDD {
    var items = this
    while (items.size > 1) {
        items = items.mergePairs(action)
    }
    return items[0]
}