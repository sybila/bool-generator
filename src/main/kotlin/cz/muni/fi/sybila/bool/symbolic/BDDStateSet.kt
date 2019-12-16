package cz.muni.fi.sybila.bool.symbolic

import cz.muni.fi.sybila.bool.rg.*
import cz.muni.fi.sybila.bool.rg.bdd.BDDWorker

class BDDStateSet(
        private val network: BooleanNetwork
) {

    private val params = BooleanParamEncoder(network)
    private val states = BooleanStateEncoder(network)

    private val standardUniverse = BDDWorker(
            network.dimensions + params.parameterCount
    )

    private val extendedUniverse = BDDWorker(
            2 * network.dimensions + params.parameterCount
    )

    init {
        // Build transition BDD. For that, we compute for each row of regulatory table
        // the following:
        // [row] z = (x, y, !z) -> (x & y & !z) & (z' = p) & (x' = x & y' = y)

        println("Variables: ${network.dimensions}; Parameters: ${params.parameterCount}")

        var transitionBDD = extendedUniverse.zero
        for (specie in 0 until network.dimensions) {
            // Obtain context in a fixed order
            val context = network.regulatoryContext(specie).sortedByDescending { it.regulator }
            for (rowIndex in 0 until 1.shl(context.size)) {
                // Row index encodes the valuations of context variables,
                // we just need to extract them.

                // Transform the row into a BDD - first part of the formula
                var rowBDD = extendedUniverse.one
                for (regulatorIndex in context.indices) {
                    val regulation = context[regulatorIndex]
                    val regulationRowEffect = if (rowIndex.isSet(regulatorIndex)) {
                        extendedUniverse.variable(regulation.regulator)
                    } else {
                        extendedUniverse.notVariable(regulation.regulator)
                    }
                    rowBDD = extendedUniverse.and(rowBDD, regulationRowEffect)
                }

                // Create application effect BDD
                val parameterIndex = params.tableCoefficients[specie] + rowIndex
                val updateVariableBDD = extendedUniverse.variable(network.dimensions + specie)
                // parameters are stored *after* both sets of variables
                val rowParameterBDD = extendedUniverse.variable(parameterIndex + 2*network.dimensions)
                val updateBDD = extendedUniverse.biImp(updateVariableBDD, rowParameterBDD)
                rowBDD = extendedUniverse.and(rowBDD, updateBDD)

                // Create 'sustain' BDD for remaining variables
                for (otherSpecie in 0 until network.dimensions) {
                    if (otherSpecie == specie) continue
                    val oldVariableBDD = extendedUniverse.variable(otherSpecie)
                    val newVariableBDD = extendedUniverse.variable(network.dimensions + otherSpecie)
                    val sustainBDD = extendedUniverse.biImp(oldVariableBDD, newVariableBDD)
                    rowBDD = extendedUniverse.and(rowBDD, sustainBDD)
                }
                transitionBDD = extendedUniverse.or(transitionBDD, rowBDD)
            }
            println("Finished ${specie + 1}/${network.dimensions} Intermediate size: ${transitionBDD.size}")
        }

        println("Size of transition BDD: ${transitionBDD.size}")

        // Make parameter satisfaction BDD:
        val parameterVarNames = (0 until params.parameterCount).map { extendedUniverse.variable(it + 2*network.dimensions) }.toTypedArray()
        val parameterNotVarNames = parameterVarNames.map { extendedUniverse.not(it + 2*network.dimensions) }.toTypedArray()

        var parameterBDD = transitionBDD//extendedUniverse.one
        // Compute the "unit" BDD of valid parameters:
        for (r in network.regulations) {
            println("${network.species[r.regulator]} -> ${network.species[r.target]}")
            val pairs = params.regulationPairs(r.regulator, r.target).map { (off, on) ->
                parameterVarNames[off] to parameterVarNames[on]
            }
            /*if (r.observable) {
                val constraint = pairs.map { (off, on) ->
                    extendedUniverse.biImp(off, on)
                }.merge { a, b ->
                    val merged = extendedUniverse.and(a, b)
                    extendedUniverse.and(parameterBDD, merged)
                }
                parameterBDD = extendedUniverse.and(parameterBDD, extendedUniverse.not(constraint))
            }*/
            if (r.effect == BooleanNetwork.Effect.ACTIVATION) {
                val constraint = pairs.map { (off, on) ->
                    extendedUniverse.imp(off, on)
                }.merge { a, b ->
                    val merged = extendedUniverse.and(a, b)
                    extendedUniverse.and(parameterBDD, merged)  // restrict
                }
                parameterBDD = extendedUniverse.and(parameterBDD, constraint)
            }
            if (r.effect == BooleanNetwork.Effect.INHIBITION) {
                val constraint = pairs.map { (off, on) ->
                    extendedUniverse.imp(on, off)
                }.merge { a, b ->
                    val merged = extendedUniverse.and(a, b)
                    extendedUniverse.and(parameterBDD, merged)  // restrict
                }
                parameterBDD = extendedUniverse.and(parameterBDD, constraint)
            }
            println("Regulation ${network.regulations.indexOf(r)}/${network.regulations.size} Intermediate size: ${parameterBDD.size}")
        }

        println("Size of parameter BDD: ${parameterBDD.size}")

        val allBDD = extendedUniverse.and(parameterBDD, transitionBDD)

        println("Size of complete transition BDD: ${allBDD.size}")
        println("Cardinality: ${extendedUniverse.cardinality(allBDD)}")
    }

    private fun Int.isSet(bitIndex: Int): Boolean = this.shr(bitIndex).and(1) == 1

    private inline fun List<BDDSet>.merge(crossinline action: (BDDSet, BDDSet) -> BDDSet): BDDSet {
        var items = this
        while (items.size > 1) {
            items = items.mergePairs(action)
        }
        return items[0]
    }

}

fun main() {
    BDDStateSet(Network.ErbB2)
}