package cz.muni.fi.sybila.bool.lattice

private const val ONE: Byte = 1
private const val ZERO: Byte = 2
private const val ANY: Byte = 0

private const val FAIL: Byte = 3

private const val VAR_UNKNOWN = -1
private const val UNION_FAIL = -2

/**
 * Lattice object represents a cube in n-dimensional space. Internally, this is stored
 * as an array of bytes (ONE/ZERO/ANY). Initially, all variables are set to ANY (full
 * parameter set). By setting a specific variable to a fixed value, you restrict the set.
 */
class Lattice(
        val cube: ByteArray
) {

    /**
     * Create a full lattice (no restrictions)
     */
    constructor(numVars: Int) : this(ByteArray(numVars))

    /**
     * Intersect the two lattices. If the result is empty, return null.
     */
    infix fun intersect(that: Lattice): Lattice? {
        val newCube = ByteArray(cube.size)
        for (i in cube.indices) {
            val result = this.cube[i] intersect that.cube[i]
            if (result == FAIL) return null
            newCube[i] = result
        }
        return Lattice(newCube)
    }


    /**
     * Return true if this lattice is a strict superset of the other lattice.
     */
    fun supersetOf(that: Lattice): Boolean {
        for (i in cube.indices) {
            // if this cube is set to fixed value, the other cube has to be set to the same value
            if (this.cube[i] == ONE || this.cube[i] == ZERO) {
                if (this.cube[i] != that.cube[i]) return false
            }
            // if this cube is unset, we don't care about the value in the other cube
        }
        return true
    }

    /**
     * If possible, union the two lattices. Union is possible if one of the lattices is
     * a superset or if the lattices match on all values except for one (in which case this value
     * comes back to ANY).
     */
    fun tryUnion(that: Lattice): Lattice? {
        var thisIsSuperset = true
        var thatIsSuperset = true
        var unionAt = VAR_UNKNOWN
        for (i in cube.indices) {
            // If the cube has an exact value, the other cube needs to have the same
            // value in order to be a subset.
            if (this.cube[i].isExact()) {
                if (this.cube[i] != that.cube[i]) thisIsSuperset = false
            }
            if (that.cube[i].isExact()) {
                if (this.cube[i] != that.cube[i]) thatIsSuperset = false
            }

            // If the values are different, `i` might be a union variable, but only
            // if there is no other union variable...
            if (this.cube[i] != that.cube[i] && i != UNION_FAIL) {
                unionAt = if (unionAt == VAR_UNKNOWN) i else UNION_FAIL
            }
        }
        return when {
            thisIsSuperset -> this
            thatIsSuperset -> that
            unionAt in cube.indices -> {
                val newCube = this.cube.clone()
                newCube[unionAt] = ANY
                return Lattice(newCube)
            }
            else -> null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Lattice

        if (!cube.contentEquals(other.cube)) return false

        return true
    }

    override fun hashCode(): Int {
        return cube.contentHashCode()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for (i in cube) {
            builder.append(when (i) {
                ONE -> '1'
                ZERO -> '0'
                else -> '-'
            })
        }
        return builder.toString()
    }

}

private fun Byte.isExact(): Boolean = this == ONE || this == ZERO

private infix fun Byte.intersect(that: Byte): Byte {
    if (this == that) return this
    if (that == ANY) return this
    if (this == ANY) return that
    return FAIL
}