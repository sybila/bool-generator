package cz.muni.fi.sybila.bool.lattice

import cz.muni.fi.sybila.bool.rg.BooleanNetwork
import cz.muni.fi.sybila.bool.rg.BooleanStateEncoder

typealias StateSet = ConcurrentArrayStateMap

class ParametrisedGraph(
        val network: BooleanNetwork,
        val solver: LatticeSolver
) {

    private val states = BooleanStateEncoder(network)
    private val dimensions = states.dimensions

    private val stateCount = states.stateCount

    private fun newMap(): StateSet = StateSet(stateCount, solver)

    fun findComponents(onComponent: (StateSet) -> Unit) = solver.run {
        val workQueue = ArrayList<StateSet>()
        // initially, all states need to be considered
        workQueue.add(newMap().apply {
            for (s in 0 until stateCount) {
                union(s, solver.unit)
            }
        })
        // run until all state sets are exhausted
        while (workQueue.isNotEmpty()) {
            val universe = workQueue.removeAt(workQueue.lastIndex)
            val pivots = findPivots(universe)

            // Find universe of terminal components reachable from pivot (and the component containing pivot)
            val forward = pivots.reachForward(universe)
            val currentComponent = pivots.reachBackward(forward)
            val reachableTerminalComponents = forward.subtract(currentComponent)

            // current component can be terminal for some subset of parameters
            val terminal = allColours(reachableTerminalComponents).not()

            if (terminal.isNotEmpty()) {
                onComponent(currentComponent.restrict(terminal))
            }

            if (reachableTerminalComponents.size > 0) {
                workQueue.add(reachableTerminalComponents)
            }

            // Find universe of terminal components not reachable from pivot
            val basinOfReachableComponents = forward.reachBackward(universe)
            val unreachableComponents = universe.subtract(basinOfReachableComponents)
            if (unreachableComponents.size > 0) {
                workQueue.add(unreachableComponents)
            }
        }
    }

    /**
     * Pick a state for each parametrisation present in the given universe.
     */
    private fun findPivots(universe: StateSet): StateSet {
        TODO()
    }

    /**
     * Compute all states that are reachable from [this] and are contained in [universe].
     */
    private fun StateSet.reachForward(universe: StateSet): StateSet {
        TODO()
    }

    /**
     * Compute all states that can reach [this] and are contained in [universe].
     */
    private fun StateSet.reachBackward(universe: StateSet): StateSet {
        TODO()
    }

    /**
     * Find parameter set that contains all parameters in the given universe.
     */
    private fun allColours(universe: StateSet): LatticeSet = solver.run {
        (0 until stateCount).fold(empty) { a, b ->
            universe.getOrNull(b)?.let { a or it } ?: a
        }
    }

}