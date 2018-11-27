package cz.muni.fi.sybila.bool

/**
 * Boolean state encoder is an interface which provides basic functions for working with bit-represented
 * boolean states. The encoding is straightforward: value of each variable is encoded into one bit of an
 * integer with the least significant bits holding first variables.
 *
 * Specifically, a boolean vector [true, true, false, true, false] becomes 01011 (in standard big-endian notation).
 *
 * This encoding is limited to 31 variables, but this should be enough for our current usage.
 */
interface BooleanStateEncoder {

    val variableCount: Int

    /**
     * Extract boolean value of variable given by [varIndex].
     *
     * The method fails if [varIndex] does not represent a valid variable.
     */
    fun Int.getVarValue(varIndex: Int): Boolean

    /**
     * Create a new state with value of variable given by [varIndex] set to [newValue].
     *
     * The method fails if [varIndex] does not represent a valid variable.
     */
    fun Int.setVarValue(varIndex: Int, newValue: Boolean): Int

    /**
     * Decode the full state info into a provided boolean array [dest].
     *
     * This function avoids unnecessary allocation of new boolean array (use it when you need to
     * repeatedly transform multiple states which are quickly discarded, for example when printing).
     *
     * The function will fail is size of [dest] is not equal to the number of system variables.
     */
    fun Int.decodeState(dest: BooleanArray)

    /**
     * Same as [decodeState], but returns an explicit fresh copy of the state array data.
     */
    fun Int.decodeState(): BooleanArray

    /**
     * Transform full state data array into a compressed state index.
     *
     * The function will fail when the size of the array is not equal to the number of system variables.
     */
    fun BooleanArray.encodeState(): Int

    companion object {

        /**
         * Provides an implementation of the [BooleanStateEncoder] with system variables given by [variableCount].
         */
        fun make(variableCount: Int): BooleanStateEncoder = object : BooleanStateEncoder {

            override val variableCount: Int = variableCount

            override fun Int.getVarValue(varIndex: Int): Boolean {
                if (varIndex < 0 || varIndex >= variableCount) error("Variable index $varIndex not in [0, $variableCount).")
                return 1 == (shr(varIndex) % 2)
            }

            // xor with zero preserves value, xor with one inverts value
            override fun Int.setVarValue(varIndex: Int, newValue: Boolean): Int {
                if (varIndex < 0 || varIndex >= variableCount) error("Variable index $varIndex not in [0, $variableCount).")
                return if (getVarValue(varIndex) == newValue) this else {
                    this xor 1.shl(varIndex)
                }
            }

            override fun Int.decodeState(dest: BooleanArray) {
                if (dest.size != variableCount) error("Invalid destination size. Expected $variableCount, actual ${dest.size}")
                for (v in 0 until variableCount) {
                    dest[v] = getVarValue(v)
                }
            }

            override fun Int.decodeState(): BooleanArray =
                    BooleanArray(variableCount).also { dest -> decodeState(dest) }

            override fun BooleanArray.encodeState(): Int {
                if (this.size != variableCount) error("Invalid source size. Expected $variableCount, actual ${this.size}")
                return (0 until variableCount).fold(0) { r, i -> r.setVarValue(i, this[i]) }
            }
        }

    }

}