package cz.muni.fi.sybila.bool.rg

/**
 * State encoder provides utility methods for managing states - finding neighbours, encoding/decoding states,
 * etc.
 */
class BooleanStateEncoder(
        private val network: BooleanNetwork
) {
    init {
        if (network.species.size > 31) error("Network too large :( At most 31 species supported")
    }

    val dimensions = network.dimensions
    val stateCount: Int = 1.shl(dimensions)

    /**
     * Flip the value of specie at [dimension] in given [state]:
     *
     * (1101, 2) -> 1001
     */
    fun flipValue(state: State, dimension: Dimension): State = state.xor(1.shl(dimension))

    /**
     * Check the bit of specific [dimension]
     */
    fun isActive(state: State, dimension: Dimension): Boolean = state.shr(dimension).and(1) == 1

    /**
     * Return a 1/0 array corresponding to the bits in our state.
     */
    fun decode(state: State): IntArray = IntArray(dimensions) { dim -> state.shr(dim).and(1) }

}