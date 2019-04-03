package cz.muni.fi.sybila.bool.rg

/**
 * The responsibility of param encoder is deriving the parameter space from a boolean network.
 *
 * Specifically, it needs to figure our how many parameters (table row) are there in our network,
 * how to canonically sort them into a BDD and how to interface with them in a reasonable fashion.
 *
 * To this end, we define a fixed ordering on parameters:
 * 1) Update functions are ordered based on the order of species: a, b, c would produce functions Fa, Fb, Fc
 * 2) For each function, arguments are ordered the same as species.
 * 3) In the table, the rows are ordered lexicographically 0,0,0... being the first, 1,1,1... being the last.
 *
 * Notice that the nice thing about this approach is that we can compute row index in a table by projecting
 * the state into relevant variables, so that we actually get 000 or 111 for smallest/largest index.
 *
 * To compute global index, we need to add table sizes for all "smaller" variables.
 *
 *
 * Another responsibility is to compute "regulation pairs". A regulation pair of B with respect to some specie A
 * is a pair of parameter indexes (on, off) such that the rows in the function table of B are the same except for
 * the value of A. This mechanism is used to compute conditions for static constraints (because static constraint
 * always operates on pairs of rows).
 */
class BooleanParamEncoder(
        private val network: BooleanNetwork
) {

    private val descendingContexts = Array(network.dimensions) { specie ->
        network.regulatoryContext(specie).sortedByDescending { it.regulator }
    }

    /**
     * Size of the logical table for each specie.
     */
    private val tableSizes = IntArray(network.dimensions) {
        1.shl(network.regulatoryContext(it).size)
    }

    /**
     * Number of rows "below" the logical table of each specie.
     * Add this number to row index to obtain global parameter index.
     */
    private val tableCoefficients: IntArray = run {
        val coefficients = IntArray(network.dimensions)
        for (i in 1 until coefficients.size) {
            coefficients[i] = coefficients[i-1] + tableSizes[i-1]
        }
        coefficients
    }

    val parameterCount = tableSizes.sum()

    /**
     * Compute the index of the parameter which determines the value of [target] specie
     * given the current values of regulators specified in [state].
     */
    fun transitionParameter(state: Int, target: Int): Int {
        val tableRow = descendingContexts[target].fold(0) { index, regulation ->
            val regulator = regulation.regulator
            index.shl(1) + state.shr(regulator).and(1)
        }
        return tableRow + tableCoefficients[target]
    }

    /**
     * Return the pairs of parameter indices which correspond to on/off table rows
     * of [target] species with respect to the [regulator] specie.
     */
    fun regulationPairs(regulator: Int, target: Int): Iterable<Pair<Int, Int>> {
        val descContext = descendingContexts[target]
        val descRegulatorIndex = descContext.indexOfFirst { it.regulator == regulator }
        if (descRegulatorIndex == -1) error("Regulation ($regulator, $target) not present!")
        // The index of bit which is flipped on corresponding table rows
        val regulatorIndex = descContext.size - 1 - descRegulatorIndex
        val offIndices = (0 until tableSizes[target]).filter { it.shr(regulatorIndex).and(1) == 0 }
        return offIndices.map {
            (it + tableCoefficients[target]) to (it.or(1.shl(regulatorIndex)) + tableCoefficients[target])
        }
    }

}