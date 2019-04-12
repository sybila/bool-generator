package cz.muni.fi.sybila.bool

import cz.muni.fi.sybila.bool.solver.BDD

/**
 * Parametrised union function allows to specify the behaviour of the union function with respect to
 * parameters.
 *
 * Standard union function always gives boolean true/false result. On the other hand, parametrised
 * union function returns a set of parameters for which the output of the function equals true.
 *
 * If you need to obtain parameters for which the value is zero, simply negate the result of the function.
 *
 * In order to work with parameters, the function needs an instance of [BooleanSolver] which will be used to
 * manipulate the parameters. Additionally, the function needs a [BooleanStateEncoder] in order to
 * interpret the state variables.
 *
 * This requirement is combined in the [BooleanContext] interface which the function has to interpret.
 *
 * The function then takes a state index and returns the parameter set as described.
 *
 * For examples on how to write union functions directly in kotlin, see state space generator tests.
 */
typealias UpdateFunction = BooleanContext.(Int) -> BDD

interface BooleanContext : BooleanSolver, BooleanStateEncoder