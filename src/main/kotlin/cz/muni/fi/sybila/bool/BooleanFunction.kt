package cz.muni.fi.sybila.bool

sealed class BooleanFunction {

    abstract operator fun invoke(state: IntArray): Boolean

    abstract val usedVariableIndices: Set<Int>

    object TRUE : BooleanFunction() {

        override fun invoke(state: IntArray): Boolean = true

        override val usedVariableIndices: Set<Int>
            get() = emptySet()

        override fun toString(): String = "true"

    }

    object FALSE : BooleanFunction() {

        override fun invoke(state: IntArray): Boolean = false

        override val usedVariableIndices: Set<Int>
            get() = emptySet()

        override fun toString(): String = "false"

    }

    class IsValue(val variableIndex: Int, val value: Int) : BooleanFunction() {

        override fun invoke(state: IntArray): Boolean = state[variableIndex] == value

        override val usedVariableIndices: Set<Int>
            get() = setOf(variableIndex)

        override fun toString(): String = "var($variableIndex) = $value"

    }

    class AtLeast(val variableIndex: Int, val value: Int) : BooleanFunction() {

        override fun invoke(state: IntArray): Boolean = state[variableIndex] >= value

        override val usedVariableIndices: Set<Int>
            get() = setOf(variableIndex)

        override fun toString(): String = "var($variableIndex):$value"

    }

    class And(val left: BooleanFunction, val right: BooleanFunction) : BooleanFunction() {

        override fun invoke(state: IntArray): Boolean = left(state) && right(state)

        override val usedVariableIndices: Set<Int>
            get() = left.usedVariableIndices + right.usedVariableIndices

        override fun toString(): String = "($left & $right)"

    }

    class Or(val left: BooleanFunction, var right: BooleanFunction) : BooleanFunction() {

        override fun invoke(state: IntArray): Boolean = left(state) || right(state)

        override val usedVariableIndices: Set<Int>
            get() = left.usedVariableIndices + right.usedVariableIndices

        override fun toString(): String = "($left | $right)"

    }

    class Not(val inner: BooleanFunction) : BooleanFunction() {

        override fun invoke(state: IntArray): Boolean = !inner(state)

        override val usedVariableIndices: Set<Int>
            get() = inner.usedVariableIndices

        override fun toString(): String = "!$inner"

    }

}

fun isValue(i: Int, v: Int) = BooleanFunction.IsValue(i, v)
fun atLeast(i: Int, v: Int) = BooleanFunction.AtLeast(i, v)
infix fun BooleanFunction.and(other: BooleanFunction) = BooleanFunction.And(this, other)
infix fun BooleanFunction.or(other: BooleanFunction) = BooleanFunction.Or(this, other)
operator fun BooleanFunction.not() = BooleanFunction.Not(this)
