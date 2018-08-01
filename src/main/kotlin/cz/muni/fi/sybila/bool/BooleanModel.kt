package cz.muni.fi.sybila.bool

data class BooleanModel(
    val variables: List<Variable>
) {

    constructor(vararg variables: Variable) : this(variables.toList())

    init {
        val myIndices = variables.indices.toSet()
        variables.forEach { v ->
            v.levelFunctions.forEach { f ->
                val extra = f.usedVariableIndices - myIndices
                if (extra.isNotEmpty()) error("Unknown variable indices: $extra in function $f for variable ${v.name}.")
            }
        }

        //TODO: Test function values are within maxLevel ranges
    }

    val encoder: Encoder = Encoder(variables.map { it.maxLevel }.toIntArray())

    data class Variable(
            val name: String,
            /**
             * Max value of this variable. Boolean networks have maxLevel = 1, Thomas networks have higher levels.
             */
            val maxLevel: Int,
            /**
             * The level functions in ascending order. Each function sets the value of the variable to the level
             * of its index in the list + 1.
             * levelFunctions.size == maxLevel
             */
            val levelFunctions: List<BooleanFunction>
    ) {

        constructor(name: String, maxLevel: Int, vararg levelFunctions: BooleanFunction) : this(name, maxLevel, levelFunctions.toList())

        //TODO : Test function ambiguity

        init {
            if (maxLevel <= 0) error("Variable $name has level $maxLevel <= 0.")
        }

        val isMultiLevel: Boolean
            get() = maxLevel > 1

    }

    class Encoder constructor(variableLevels: IntArray) {

        private val coefficients = variableLevels.map { it + 1 }.toIntArray()

        /**
         * The number of states in the model.
         */
        val stateCount = coefficients.map { it.toLong() }.fold(1L) { a, b -> a * b }.let {
            if (it > 0 && it < Int.MAX_VALUE)it.toInt() else {
                error("Model is too big. $it detected, but only ${Int.MAX_VALUE} supported.")
            }
        }

        /**
         * Convert state ID to variable values (stored as an integer array - value index = variable index).
         */
        fun extractValues(state: Int, result: IntArray = IntArray(coefficients.size)): IntArray {
            var remainder = state
            for (v in coefficients.indices) {
                result[v] = remainder % coefficients[v]
                remainder /= coefficients[v]
            }
            return result
        }

        fun encodeState(values: IntArray): Int {
            var result = 0
            var total = 1
            for (v in coefficients.indices) {
                result += values[v] * total
                total *= coefficients[v]
            }
            return result
        }

    }

}