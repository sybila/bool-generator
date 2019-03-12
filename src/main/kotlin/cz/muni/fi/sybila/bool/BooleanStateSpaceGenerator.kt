package cz.muni.fi.sybila.bool

import com.github.sybila.checker.Model
import com.github.sybila.checker.MutableStateMap
import com.github.sybila.checker.StateMap
import com.github.sybila.checker.Transition
import com.github.sybila.checker.map.mutable.HashStateMap
import com.github.sybila.huctl.*
import cz.muni.fi.sybila.bool.solver.BDD
import cz.muni.fi.sybila.bool.solver.BDDSolver
import kotlin.math.roundToInt

/**
 * State space generator creates a parametrised transition system based on the given [model] (a boolean network
 * with parametrised update functions). Each transition has an associated parameter set and direction formula.
 * The parameter set indicates for which parameters the transition is enabled, whereas the direction formula
 * indicates which variable is updated by the transition.
 *
 * The generator implements an asynchronous update scheme, meaning that in every step, at most one variable
 * can change and the choice of updated variable is non-deterministic. For parameters where update functions
 * for all variables do not change the current state, a self-loop should be created.
 *
 * The generator also provides logical proposition evaluation. Here, transition propositions are currently
 * unsupported. In terms of numeric propositions, we support the following types of propositions:
 * var == const, var != const, var1 == var2, var1 != var2 (+ symmetric variants)
 * Here, const always corresponds either to 1 (true) or 0 (zero).
 *
 * Anything else is considered invalid proposition and results in an error.
 */
class BooleanStateSpaceGenerator(
        private val model: BooleanModel,
        private val stateEncoder: BooleanStateEncoder = BooleanStateEncoder.make(model.variables.size),
        private val solver: BooleanSolver = BDDSolver(model.parameterCount)
) : Model<BDD>, BooleanSolver by solver, BooleanStateEncoder by stateEncoder, BooleanContext {

    override val stateCount: Int = Math.pow(2.0, model.variables.size.toDouble()).toInt()

    private val successors: MutableMap<Int, List<Transition<BDD>>> = HashMap()
    private val predecessors: MutableMap<Int, List<Transition<BDD>>> = HashMap()

    init {
        for (i in 0 until stateCount) {
            successors[i] = i.calculateSuccessors()
        }
        // we must first have all successors in order to retroactively figure out predecessors
        for (i in 0 until stateCount) {
            predecessors[i] = i.obtainPredecessors()
        }
            val testBreakPoint = 5
    }


    private fun Int.calculateSuccessors(): List<Transition<BDD>> {
        var hasTransition = ff
        val fromState = this
        val transitions = ArrayList<Transition<BDD>>()
        // For each system variable, compute the update function and compute parameters for which
        // the current variable value can be flipped (so if value is 0, use result of update function,
        // if value is 1, use complement).

        model.variables.forEachIndexed { index, variable ->
            kotlin.run {
                val updateFunctionResult = variable.updateFunction.invoke(this@BooleanStateSpaceGenerator, fromState)
                val oldValue = getVarValue(index)

                val transitionParams = if (!oldValue) updateFunctionResult else updateFunctionResult.not()

                // If this parameter set is not empty, create a corresponding transition:

                if (transitionParams.isSat()) {

                    val toState = fromState.setVarValue(index, oldValue.not())

                    transitions.add(Transition(
                            target = toState, bound = transitionParams,
                            direction = DirectionFormula.Atom.Proposition(
                                    // Name of variable which is being changed
                                    name = variable.name,
                                    // Positive/Negative facet depending on the direction of the change.
                                    // 0 -> 1 = Positive, 1 -> 0 = Negative
                                    facet = if (!oldValue) Facet.POSITIVE else Facet.NEGATIVE
                            )
                    ))
                }

                // In order to handle self-loops, keep track of parameters which have a transition:
                hasTransition = hasTransition or transitionParams
            }
        }

        // At the very end, you get parameters where no transition is allowed by computing complement:
        val hasSelfLoop = hasTransition.not()
        if (hasSelfLoop.isSat()) {
            val loop = Transition(
                    target = fromState,
                    bound = hasSelfLoop,
                    direction = DirectionFormula.Atom.Loop)
            transitions.add(loop)

        }
        return transitions
    }

    private fun Int.obtainPredecessors(): List<Transition<BDD>> {
        val transitions = ArrayList<Transition<BDD>>()

        successors.entries.map { entry ->
            val transitionToReverse = entry.value.asSequence().firstOrNull { it.target == this }
            if (transitionToReverse != null) {
                transitions.add(
                        Transition(
                                target = entry.key, bound = transitionToReverse.bound,
                                direction = if (transitionToReverse.direction is DirectionFormula.Atom.Loop) {
                                    DirectionFormula.Atom.Loop
                                } else {
                                    val directionToReverse = transitionToReverse.direction as DirectionFormula.Atom.Proposition
                                    DirectionFormula.Atom.Proposition(
                                            // Name of variable which is being changed
                                            name = directionToReverse.name,
                                            // Positive/Negative facet inverse to reversed direction
                                            facet = if (directionToReverse.facet == Facet.NEGATIVE) Facet.POSITIVE else Facet.NEGATIVE
                                    )
                                })
                )
            }
        }
        return transitions
    }


    override fun Int.successors(timeFlow: Boolean): Iterator<Transition<BDD>> {
        // if going back in time, just return predecessors
        if (this >= stateCount) {
            throw IllegalStateException("This boolean generator's model cannot achieve given state.")
        }

        return if (!timeFlow) predecessors(true) else {
            successors[this]!!.iterator()
        }
    }

    override fun Int.predecessors(timeFlow: Boolean): Iterator<Transition<BDD>> {
        // if going back in time, just return successors
        if (this >= stateCount) {
            throw IllegalStateException("This boolean generator's model cannot achieve given state.")
        }
        return if (!timeFlow) successors(true) else {
            // Compute predecessors, similar to successors, but inverse logic needs to be applied.
            predecessors[this]!!.iterator()
        }
    }

    private fun CompareOp.compare(a: Boolean, b: Boolean): Boolean {
        return if (this == CompareOp.EQ) {
            a == b
        } else if (this == CompareOp.NEQ) {
            a != b
        } else {
            throw IllegalAccessException("Can only use for EQ and NEQ CompareOp")
        }
    }

    override fun Formula.Atom.Float.eval(): StateMap<BDD> {
        // Check if comparison operator is == or !=, if not, fail.
        // Check if left and right expressions are constants or variables (math is not allowed).
        // Check if expression is constant, it is either 1 or 0 (no other values allowed).
        // Run through all states and create a state map which contains all states
        // which satisfy the proposition. Set the parameter values of these states to tt.
        // (state map is just a fancy map implementation which returns a default value (usually ff) if
        // key is not present and allows a special setOrUnion operations which inserts the value if the key is missing
        // and if not missing, the key is set to the union of old and new value.

        val resultMap: MutableStateMap<BDD> = HashStateMap<BDD>(ff)
        // constant = variable -> vsetky stavy kedy sa stav variable rovna constant


        if (cmp != CompareOp.EQ && cmp != CompareOp.NEQ) {
            throw IllegalArgumentException("Unsupported comparing operation.")
        }
        when {
            left is Expression.Variable && right is Expression.Variable -> {
                val leftVar = left as Expression.Variable
                val rightVar = right as Expression.Variable

                val leftVarPosition = model.variables.indexOf(model.variables.firstOrNull { it.name == leftVar.name })
                val rightVarPosition = model.variables.indexOf(model.variables.firstOrNull { it.name == rightVar.name })

                if (leftVarPosition == -1 || rightVarPosition == -1) {
                    throw IllegalArgumentException("One of vars doesn't exist in model.")
                }

                for (i in 0 until stateCount) {
                    if (cmp.compare(i.getVarValue(leftVarPosition), i.getVarValue(rightVarPosition))) {
                        resultMap[i] = tt
                    }
                }

            }
            left is Expression.Variable && right is Expression.Constant -> {
                val leftVar = left as Expression.Variable
                val rightConst = right as Expression.Constant

                val leftVarPosition = model.variables.indexOf(model.variables.firstOrNull { it.name == leftVar.name })

                if (leftVarPosition == -1) {
                    throw IllegalArgumentException("One of vars doesn't exist in model.")
                }

                for (i in 0 until stateCount) {
                    if (cmp.compare(i.getVarValue(leftVarPosition), rightConst.value.roundToInt() != 0)) {
                        resultMap[i] = tt
                    }
                }

            }
            left is Expression.Constant && right is Expression.Variable -> {
                val rightVar = right as Expression.Variable
                val leftConst = left as Expression.Constant

                val rightVarPosition = model.variables.indexOf(model.variables.firstOrNull { it.name == rightVar.name })
                if (rightVarPosition == -1) {
                    throw IllegalArgumentException("One of vars doesn't exist in model.")
                }

                for (i in 0 until stateCount) {
                    if (cmp.compare(i.getVarValue(rightVarPosition), leftConst.value.roundToInt() != 0)) {
                        resultMap[i] = tt
                    }
                }


            }
            left is Expression.Constant && right is Expression.Constant -> {
                val leftConst = left as Expression.Constant
                val rightConst = right as Expression.Constant
                if (cmp.compare(leftConst.value.roundToInt() != 0, rightConst.value.roundToInt() != 0)) {
                    for (i in 0 until stateCount) {
                        resultMap[i] = tt
                    }
                }

            }
            else -> throw IllegalArgumentException("Unsupported expression type.")
        }

        return resultMap
    }

    override fun Formula.Atom.Transition.eval(): StateMap<BDD> {
        TODO("Transition propositions are not supported in boolean models.")
    }
}