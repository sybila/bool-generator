package cz.muni.fi.sybila.bool

import com.github.sybila.checker.Model
import com.github.sybila.checker.Solver
import com.github.sybila.checker.StateMap
import com.github.sybila.checker.Transition
import com.github.sybila.checker.map.mutable.ContinuousStateMap
import com.github.sybila.checker.solver.BoolSolver
import com.github.sybila.huctl.CompareOp
import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.huctl.Expression
import com.github.sybila.huctl.Formula
import com.github.sybila.huctl.Formula.Atom.Float

class BooleanFragment(private val model: BooleanModel) : Model<Boolean>, Solver<Boolean> by BoolSolver() {

    override val stateCount: Int = model.encoder.stateCount
    private val varCount = model.variables.size

    private val nameToIndex = model.variables.mapIndexed { i, variable -> variable.name to i }.toMap()

    private val valueCache = ThreadLocal.withInitial { IntArray(varCount) }

    private val localValueCache: IntArray
        get() = valueCache.get()

    override fun Float.eval(): StateMap<Boolean> {
        val result = ContinuousStateMap(0, stateCount, false)

        val valueCache = IntArray(varCount)
        for (state in 0 until stateCount) {
            val value = model.encoder.extractValues(state, valueCache)
            val left = this.left.evalAtState(value)
            val right = this.right.evalAtState(value)
            if (this.cmp.eval(left, right)) {
                result[state] = true
            }
        }

        return result
    }

    private fun CompareOp.eval(left: Double, right: Double): Boolean = when (this) {
        CompareOp.GT -> left >  right
        CompareOp.GE -> left >= right
        CompareOp.LT -> left <  right
        CompareOp.LE -> left <= right
        CompareOp.EQ -> left == right
        CompareOp.NEQ -> left != right
    }

    private fun Expression.evalAtState(state: IntArray): Double = when (this) {
        is Expression.Variable -> state[nameToIndex[name] ?: error("Unknown variable $name")].toDouble()
        is Expression.Constant -> this.value
        is Expression.Arithmetic.Add -> left.evalAtState(state) + right.evalAtState(state)
        is Expression.Arithmetic.Sub -> left.evalAtState(state) - right.evalAtState(state)
        is Expression.Arithmetic.Mul -> left.evalAtState(state) * right.evalAtState(state)
        is Expression.Arithmetic.Div -> left.evalAtState(state) / right.evalAtState(state)
    }

    override fun Formula.Atom.Transition.eval(): StateMap<Boolean> {
        error("Transition propositions are not supported for boolean models.")
    }

    private val successorCache: Array<IntArray> = Array(stateCount) { s ->
        val value = model.encoder.extractValues(s, localValueCache)
        val result = HashSet<Int>(varCount)
        fun pushResultWithoutCopy(v: Int, newValue: Int) {
            val originalValue = value[v]
            value[v] = newValue
            result.add(model.encoder.encodeState(value))
            value[v] = originalValue
        }
        for (v in 0 until varCount) {
            val levelFunctions = model.variables[v].levelFunctions
            var found = false
            for (iF in levelFunctions.indices.reversed()) {
                if (levelFunctions[iF](value)) {
                    pushResultWithoutCopy(v, iF + 1)
                    found = true
                    break
                }
            }
            if (!found) {
                pushResultWithoutCopy(v, 0)
            }
        }
        if (s in result && result.size > 1) result.remove(s)
        result.toIntArray()
    }

    private val predecessorCache: Array<IntArray> = successorCache
            .mapIndexed { i, successors -> i to successors.toList() }
            .flatMap { (i, successors) -> successors.map { i to it } }
            .groupBy({ it.second }, { it.first })
            .let { map ->
                Array(stateCount) { s ->
                    val predecessors = map[s] ?: emptyList()
                    predecessors.toIntArray()
                }
            }

    override fun Int.predecessors(timeFlow: Boolean): Iterator<Transition<Boolean>> = successors(!timeFlow)

    override fun Int.successors(timeFlow: Boolean): Iterator<Transition<Boolean>> {
        val cache = if (timeFlow) successorCache else predecessorCache
        return cache[this].map { Transition(it, DirectionFormula.Atom.True, true) }.iterator()
    }

}