package cz.muni.fi.sybila.bool.common

import cz.muni.fi.sybila.bool.rg.BooleanStateEncoder
import cz.muni.fi.sybila.bool.rg.mapParallel
import cz.muni.fi.sybila.bool.rg.parallel
import cz.muni.fi.sybila.bool.rg.parallel.RepeatingConcurrentStateQueue
import cz.muni.fi.sybila.bool.rg.pool

typealias StateSet<T> = ConcurrentArrayStateMap<T>

interface ParametrisedGraph<P: Any> {

    val solver: Solver<P>
    val dimensions: Int
    val stateCount: Int
    val states: BooleanStateEncoder

    fun newMap(): StateSet<P>

    private inline fun <R> withSolver(action: Solver<P>.() -> R): R = solver.run { action() }

    /**
     * Extract terminal components in this graph, calling the [onComponent] function with
     * individual parametrized components.
     */
    fun findComponents(onComponent: (StateSet<P>) -> Unit) = solver.run {
        val workQueue = ArrayList<StateSet<P>>()
        // initially, all states need to be considered
        workQueue.add(newMap().apply {
            for (s in 0 until stateCount) {
                println("Added: ${union(s, solver.unit)}")
            }
        })
        // run until all state sets are exhausted
        while (workQueue.isNotEmpty()) {
            val universe = workQueue.removeAt(workQueue.lastIndex)
            println("Universe state count: ${universe.size} Remaining work queue: ${workQueue.size}")
            val pivots = findPivots(universe)
            println("Pivots state count: ${pivots.size}")

            // Find universe of terminal components reachable from pivot (and the component containing pivot)
            val forward = pivots.reachForward(universe)
            val currentComponent = pivots.reachBackward(forward)
            val reachableTerminalComponents = forward.subtract(currentComponent)

            // current component can be terminal for some subset of parameters
            val terminal = not(allColours(reachableTerminalComponents))

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
     * Subtract one set of states from the other (using parallel map)
     */
    fun StateSet<P>.subtract(that: StateSet<P>): StateSet<P> {
        val result = newMap()
        (0 until stateCount).toList()
                .map { s -> Triple(s, this.get(s), that.get(s)) }
                .mapParallel { (s, a, b) ->
                    withSolver { (s to (a and not(b))).takeIf { it.second.isNotEmpty() } }
                }
                .filterNotNull()
                // this is actually quite fast because result is empty, so there are not unions, just replacing nulls
                .forEach { (s, p) -> result.union(s, p) }
        return result
    }

    /**
     * Restrict a set of states to the given set of parameters.
     */
    fun StateSet<P>.restrict(colours: P): StateSet<P> {
        val result = newMap()
        (0 until stateCount).toList()
                .mapNotNull { s -> getOrNull(s)?.let { s to it } }
                .mapParallel { (s, c) ->
                    withSolver { s to solver.run { c and colours } }
                }
                .forEach { (s, p) -> result.union(s, p) }
        return result
    }

    /**
     * Pick a state for each parametrisation present in the given universe.
     */
    private fun findPivots(universe: StateSet<P>): StateSet<P> = solver.run {
        val result = newMap()
        var toCover = allColours(universe)
        var remaining = (0 until stateCount)
                .mapNotNull { s -> universe.getOrNull(s)?.let { s to (it) } }
        while (toCover.isNotEmpty()) {
            // there must be a gain in the first element of remaining because we remove all empty elements
            val (s, gain) = remaining.first().let { (s, p) -> s to (p and toCover) }
            toCover = toCover and not(gain)
            result.union(s, gain)
            remaining = remaining.mapParallel { (s, p) ->
                (s to (p and toCover)).takeIf { it.second.isNotEmpty() }
            }.filterNotNull()
        }
        result
    }

    /**
     * Negate (complement) this set of states.
     */
    fun StateSet<P>.invert(): StateSet<P> {
        val result = newMap()
        (0 until stateCount).toList()
                .map { s -> s to get(s) }
                .mapParallel { (s, c) ->
                    withSolver { (s to not(c) ).takeIf { it.second.isNotEmpty() } }
                }
                .filterNotNull()
                .forEach { (s, p) ->
                    result.union(s, p)
                }
        return result
    }

    /**
     * Find parameter set that contains all parameters in the given universe.
     */
    private fun allColours(universe: StateSet<P>): P = withSolver {
        val list = (0 until stateCount)
                .mapNotNull { universe.getOrNull(it) }
        return if (list.isEmpty()) empty else {
            list.merge { a, b -> a or b }
        }
    }

    fun StateSet<P>.reachForward(universe: StateSet<P>?): StateSet<P> {
        val shouldUpdate = RepeatingConcurrentStateQueue(stateCount)
        val result = newMap()
        // init reach
        for (s in 0 until stateCount) {
            val c = this.getOrNull(s)
            if (c != null) {
                result.union(s, this.get(s))
                shouldUpdate.set(s)
            }
        }
        println("Start reach forward.")
        // repeat
        pool.parallel {
            var state = shouldUpdate.next(0)
            while (state > -1) {
                while (state > -1) {
                    // go through all neighbours
                    for (d in 0 until dimensions) {
                        solver.run {
                            val target = states.flipValue(state, d)
                            val edgeParams = solver.transitionParams(state, d)
                            // bring colors from source state, bounded by guard
                            val bound = if (universe == null) result.get(state) else {
                                result.get(state) and universe.get(target)
                            }
                            // update target -> if changed, mark it as working
                            val changed = result.union(target, edgeParams and bound)
                            if (changed) {
                                shouldUpdate.set(target)
                            }
                        }
                    }
                    state = shouldUpdate.next(state + 1)
                }
                state = shouldUpdate.next(0)
            }
        }

        return result
    }

    fun StateSet<P>.reachBackward(universe: StateSet<P>?): StateSet<P> {
        val shouldUpdate = RepeatingConcurrentStateQueue(stateCount)
        val result = newMap()
        // init reach
        for (s in 0 until stateCount) {
            val c = this.getOrNull(s)
            if (c != null) {
                result.union(s, this.get(s))
                shouldUpdate.set(s)
            }
        }
        println("Start reach backward.")
        // repeat
        pool.parallel {
            var state = shouldUpdate.next(0)
            while (state > -1) {
                while (state > -1) {
                    // go through all neighbours
                    for (d in 0 until dimensions) {
                        solver.run {
                            val source = states.flipValue(state, d)
                            val edgeParams = solver.transitionParams(source, d)
                            // bring colors from source state, bounded by guard
                            val bound = if (universe == null) result.get(state) else {
                                result.get(state) and universe.get(source)
                            }
                            // update target -> if changed, mark it as working
                            val changed = result.union(source, edgeParams and bound)
                            if (changed) {
                                shouldUpdate.set(source)
                            }
                        }
                    }
                    state = shouldUpdate.next(state + 1)
                }
                // double check - maybe someone added another thing
                state = shouldUpdate.next(0)
            }
        }

        return result
    }

}