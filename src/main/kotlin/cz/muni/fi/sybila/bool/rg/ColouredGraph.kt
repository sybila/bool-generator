package cz.muni.fi.sybila.bool.rg

import cz.muni.fi.sybila.bool.rg.map.DecreasingStateMap
import cz.muni.fi.sybila.bool.rg.parallel.ConcurrentStateQueue
import cz.muni.fi.sybila.bool.rg.parallel.RepeatingConcurrentStateQueue
import cz.muni.fi.sybila.bool.rg.parallel.StateQueue
import java.util.*
import kotlin.collections.ArrayList

class ColouredGraph(
        network: BooleanNetwork,
        private val solver: BDDSolver,
        private val trimEnabled: Boolean = false
) {

    private val states = BooleanStateEncoder(network)
    private val dimensions = states.dimensions

    private val stateCount = states.stateCount

    private fun newMap(): StateMap = StateMap(stateCount, solver)

    private fun StateMap.reachForward(guard: StateMap? = null): StateMap {
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
                            val bound = if (guard == null) result.get(state) else {
                                result.get(state) and guard.get(target)
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

    private fun StateMap.isTheSame(that: StateMap): Boolean {
        for (s in 0 until stateCount) {
            if (!Arrays.equals(this.get(s), that.get(s))) {
                return false
            }
        }
        return true
    }

    private fun StateMap.reachBackward(guard: StateMap? = null): StateMap {
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
                            val bound = if (guard == null) result.get(state) else {
                                result.get(state) and guard.get(source)
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

    private fun StateMap.subtract(that: StateMap): StateMap {
        val result = newMap()
        for (s in 0 until stateCount) {
            val a = this.get(s)
            val b = that.get(s)
            val params = solver.run { a and b.not() }
            result.union(s, params)
        }
        return result
    }

    private fun StateMap.invert(): StateMap {
        val result = newMap()
        for (s in 0 until stateCount) {
            val c = this.get(s)
            val notC = solver.run { c.not() }
            result.union(s, notC)
        }
        return result
    }

    fun findComponents(onComponents: (StateMap) -> Unit) = solver.run {
        // First, detect all sinks - this will prune A LOT of state space...
        val sinks = newMap()
        println("Detecting sinks!")
        for (s in 0 until stateCount) {
            val hasNext = (0 until dimensions)
                    .map { d -> solver.transitionParams(s, d) }
                    .merge { a, b -> a or b }
            val isSink = hasNext.not()
            if (isSink.isNotEmpty()) {
                sinks.union(s, isSink)
                val map = newMap()
                map.union(s, isSink)
                onComponents(map)
            }
        }
        val canReachSink = sinks.reachBackward()
        //val canReachSink = newMap()
        val workQueue = ArrayList<StateMap>()
        val groundZero = canReachSink.invert()
        if (groundZero.size > 0) workQueue.add(groundZero)
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
            val terminal = allColours(reachableTerminalComponents).not()

            if (terminal.isNotEmpty()) {
                onComponents(currentComponent.restrict(terminal))
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

    /*private fun StateMap.printFull() {
        this.forEach { (s, p) ->
            solver.run {
                println("S: ${states.decode(s).toList()} has ${p.cardinality()}")
                p.print()
            }
        }
    }*/

    /*private fun StateMap.trim(): StateMap {
        if (!trimEnabled) return this
        val trimmed = DecreasingStateMap(this, solver)
        val update = BitSet(stateCount)
        for (s in 0 until stateCount) {
            val c = trimmed.getOrNull(s)
            if (c != null) {
                update.set(s)
            }
        }
        solver.run {
            while (!update.isEmpty) {
                var state = update.nextSetBit(0)
                while (state > -1) {
                    // compute trim colours
                    var hasPredecessor = solver.empty
                    for (d in 0 until dimensions) {
                        val predecessor = states.flipValue(state, d)
                        val predecessorParams = trimmed.get(predecessor)
                        if (predecessorParams.isNotEmpty()) {
                            val edgeParams = solver.transitionParams(predecessor, d)
                            hasPredecessor = hasPredecessor or (edgeParams and predecessorParams)
                        }
                    }
                    if (trimmed.intersect(state, hasPredecessor)) {
                        // State params decreased - this means some successors may be trimmed
                        for (d in 0 until dimensions) {
                            val successor = states.flipValue(state, d)
                            update.set(successor)
                        }
                    }
                    update.clear(state)
                    state = update.nextSetBit(state + 1)
                }
            }
        }
        println("Trimmed ${this.size} -> ${trimmed.size}")
        return trimmed.toStateMap()
    }*/

    private fun StateMap.restrict(colours: BDDSet): StateMap {
        val result = newMap()
        for (s in 0 until stateCount) {
            val c = this.getOrNull(s)
            if (c != null) {
                result.union(s, solver.run { c and colours })
            }
        }
        return result
    }

    private fun allColours(map: StateMap): BDDSet = solver.run {
        var result = empty
        for (s in 0 until stateCount) {
            val c = map.getOrNull(s)
            if (c != null) result = result or c
        }
        result
    }

    private fun findPivots(map: StateMap): StateMap = solver.run {
        val result = newMap()
        var toCover = allColours(map)
        while (toCover.isNotEmpty()) {
            for (s in 0 until stateCount) {
                val c = map.getOrNull(s)
                if (c != null) {
                    val gain = c and toCover
                    if (gain.isNotEmpty()) {
                        result.union(s, gain)
                        toCover = toCover and gain.not()
                    }
                }
            }
        }
        result
    }

}