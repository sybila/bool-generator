package cz.muni.fi.sybila.bool

import com.github.sybila.checker.Model
import com.github.sybila.checker.StateMap
import com.github.sybila.checker.Transition
import com.github.sybila.checker.map.mutable.HashStateMap
import com.github.sybila.huctl.DirectionFormula
import com.github.sybila.huctl.Facet
import com.github.sybila.huctl.Formula
import cz.muni.fi.sybila.bool.solver.BDD
import cz.muni.fi.sybila.bool.solver.BDDSolver

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

    // TODO: Computing transitions every time is expensive. We will want to cache the results of successor/predecessor
    // computation in some map (or array, since state IDs are continuous).

    override fun Int.successors(timeFlow: Boolean): Iterator<Transition<BDD>> {
        // if going back in time, just return predecessors
        if (!timeFlow) return predecessors(true) else {
            var hasTransition = ff
            val fromState = this
            val transitions = ArrayList<Transition<BDD>>()
            // For each system variable, compute the update function and compute parameters for which
            // the current variable value can be flipped (so if value is 0, use result of update function,
            // if value is 1, use complement).
            var i = model.variables.size - 1


            model.variables.forEach {
                val updateFunctionResult = it.updateFunction.invoke(this@BooleanStateSpaceGenerator, fromState)
                val oldValue = getVarValue(i)

                println(updateFunctionResult.prettyPrint())

                val transitionParams = if (!oldValue) updateFunctionResult else updateFunctionResult.not()

                // If this parameter set is not empty, create a corresponding transition:

                if (transitionParams.isSat()) {

                    val toState = fromState.setVarValue(i, oldValue.not())

                    transitions.add(Transition(
                            target = toState, bound = transitionParams,
                            direction = DirectionFormula.Atom.Proposition(
                                    // Name of variable which is being changed
                                    name = it.name,
                                    // Positive/Negative facet depending on the direction of the change.
                                    // 0 -> 1 = Positive, 1 -> 0 = Negative
                                    facet = if (!oldValue) Facet.POSITIVE else (Facet.NEGATIVE)
                            )
                    ))
                }

                i -= 1
                hasTransition = hasTransition or transitionParams
            }

                // In order to handle self-loops, keep track of parameters which have a transition:



                // At the very end, you get parameters where no transition is allowed by computing complement:
                val hasSelfLoop = hasTransition.not()
                if (hasSelfLoop.isSat()) {
                    val loop = Transition(
                            target = fromState,
                            bound = hasSelfLoop,
                            direction = DirectionFormula.Atom.Loop)
                    transitions.add(loop)

                }
            return transitions.iterator()
        }
    }

    override fun Int.predecessors(timeFlow: Boolean): Iterator<Transition<BDD>> {
        // if going back in time, just return successors
        if (!timeFlow) return successors(true) else {
            // Compute predecessors, similar to successors, but inverse logic needs to be applied.
            val toState = this
        }
        TODO()
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
        val result = HashStateMap(ff)
        TODO("not implemented")
    }

    override fun Formula.Atom.Transition.eval(): StateMap<BDD> {
        TODO("Transition propositions are not supported in boolean models.")
    }
}