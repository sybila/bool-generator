package cz.muni.fi.sybila.bool

import com.github.sybila.checker.Solver
import cz.muni.fi.sybila.bool.solver.BDD

/**
 * Boolean solver is an extension of the basic [Solver] interface with the ability to create
 * BDD based parameter sets.
 *
 * In the future, we might consider adding more complex creation methods, but for now, this basic
 * specification is sufficient. (More complex sets can be then constructed using standard solver
 * operations)
 */
interface BooleanSolver : Solver<BDD> {

    /** Create a BDD set where variable [varIndex] is set to true and all other variables are unspecified. */
    fun one(varIndex: Int): BDD

    /** Create a BDD set where variable [varIndex] is set to false and all other variables are unspecified. */
    fun zero(varIndex: Int): BDD

}