package cz.muni.fi.sybila.bool

/**
 * Boolean model is a data class which represents a boolean network with parametrised update functions.
 *
 * The network consists of boolean [variables], where each variable has a name (this name should be
 * used in logical propositions when referring to a variable) and parametrised update function.
 *
 * The network contains of [parameterCount] unnamed parameters (to refer to parameters,
 * use indices in [0..parameterCount). The update functions for each variable assume presence of
 * (at least) these parameters.
 */
data class BooleanModel(
        val parameterCount: Int,
        val variables: List<Variable>
) {

    constructor(parameterCount: Int, vararg variables: Variable) : this(parameterCount, variables.toList())

    init {
        if (variables.size > 30) error("Model too big. Max. 30 variables supported, ${variables.size} given.")
    }

    data class Variable(val name: String, val updateFunction: UpdateFunction)

}