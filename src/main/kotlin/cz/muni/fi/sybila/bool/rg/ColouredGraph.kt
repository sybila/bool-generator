package cz.muni.fi.sybila.bool.rg

import cz.muni.fi.sybila.bool.common.ConcurrentArrayStateMap
import cz.muni.fi.sybila.bool.common.ParametrisedGraph
import cz.muni.fi.sybila.bool.common.StateSet
import cz.muni.fi.sybila.bool.rg.map.DisjointSets
import cz.muni.fi.sybila.bool.rg.parallel.RepeatingConcurrentStateQueue
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList

class ColouredGraph(
        network: BooleanNetwork,
        override val solver: BDDSolver,
        private val trimEnabled: Boolean = false
) : ParametrisedGraph<BDDSet> {

    override val states = BooleanStateEncoder(network)
    override val dimensions = states.dimensions

    override val stateCount = states.stateCount

    override fun newMap(): StateSet<BDDSet> {
        return ConcurrentArrayStateMap(stateCount, solver)
    }

    /*override fun findComponents(onComponent: (StateSet<BDDSet>) -> Unit) = solver.run {
        // First, detect all sinks - this will prune A LOT of state space...
        val sinks = newMap()
        println("Detecting sinks!")
        (0 until stateCount).toList().mapParallel { s ->
            if (s%10000 == 0) println("Sink progress $s/$stateCount")
            val hasNext = (0 until dimensions)
                    .map { d -> solver.transitionParams(s, d) }
                    .merge { a, b -> a or b }
            val isSink = not(hasNext)
            if (isSink.isNotEmpty()) {
                sinks.union(s, isSink)
                val map = newMap()
                map.union(s, isSink)
                onComponent(map)
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
    }*/

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

    /*fun dfs() = solver.run {
        /*val reallyDead = newMap()

        val expectedTotal = unit.cardinality() * stateCount
        val remaining: AtomicReference<Double> = AtomicReference(expectedTotal)
        pool.parallelWithId { id ->
            val dead = newMap()
            val start = System.currentTimeMillis()
            var iter = 0
            val stack = ArrayList<StackEntry>()
            val shift = (id * (stateCount.toDouble() / parallelism)).roundToInt()
            for (rootOrig in 0 until stateCount) {
                val root = (rootOrig + shift) % stateCount
                if (id == 0) {
                    //println("Root: $root")
                }
                val undead = dead.get(root).not()
                if (undead.isEmpty()) continue

                stack.add(StackEntry(root, undead, dimensions))
                dead.union(root, undead)
                println("New root!")
                println("Push $root for ${undead.cardinality()}")
                //remaining.decrement(undead.cardinality())
                while (stack.isNotEmpty()) {
                    iter += 1
                    val top = stack.last()
                    val (s, sPOriginal) = top
                    val sP = sPOriginal and reallyDead.get(s).not()
                    if (sP.isNotEmpty() && top.hasNext()) {
                        val d = top.next()
                        val t = states.flipValue(s, d)
                        val edgeP = solver.transitionParams(s, d)
                        val newInT = sP and edgeP and dead.get(t).not()
                        if (newInT.isNotEmpty()) {
                            dead.union(t, newInT)
                            println("Push $t for ${newInT.cardinality()} from $s")
                            //remaining.decrement(newInT.cardinality())
                            stack.add(StackEntry(t, newInT, dimensions))
                        }
                    } else {
                        // pop empty iterator
                        stack.removeAt(stack.lastIndex)
                        reallyDead.union(s, sP)
                        remaining.decrement(sP.cardinality())
                        if (id == 0) {
                            //print("\r Stack size: ${stack.size}, remaining: ${remaining.get()}/$expectedTotal (${(remaining.get()/expectedTotal * 10000.0).roundToInt()/100.0}%) Throughput: ${(iter.toDouble() / (System.currentTimeMillis() - start)) * 1000.0}/s")
                        }
                    }
                }
            }
        }*/
        val visited = newMap()

        val expectedTotal = unit.cardinality() * stateCount
        var toDo = expectedTotal

        val stack = ArrayList<StackEntry>()
        for (root in 0 until stateCount) {

            val notVisited = not(visited.get(root))
            if (notVisited.isEmpty()) continue
            stack.add(StackEntry(root, notVisited, dimensions))
            visited.union(root, notVisited)
            toDo -= notVisited.cardinality()
            println("New root!")
            println("Push $root for ${notVisited.cardinality()}")

            while (stack.isNotEmpty()) {
                val top = stack.last()
                val (s, sP) = top
                if (top.hasNext()) {
                    val d = top.next()
                    val t = states.flipValue(s, d)
                    val edgeP = solver.transitionParams(s, d)
                    val newInT = sP and edgeP and not(visited.get(t))
                    if (newInT.isNotEmpty()) {
                        visited.union(t, newInT)
                        toDo -= newInT.cardinality()
                        println("Push $t for ${newInT.cardinality()} from $s")
                        stack.add(StackEntry(t, newInT, dimensions))
                    }
                } else {
                    stack.removeAt(stack.lastIndex)
                    /*val remaining = visited.get(s).not()
                    if (remaining.isNotEmpty()) {
                        println("Restarting in $s for ${remaining.cardinality()}")
                        visited.union(s, remaining)
                        toDo -= remaining.cardinality()
                        stack.add(StackEntry(s, remaining, dimensions))
                    }*/
                    //print("\r Stack: ${stack.size}; Remaining: $toDo/$expectedTotal ${(toDo / expectedTotal) * 100}%")
                }
            }
        }

        println()

    }

    private fun scc() = solver.run {
        val sets = DisjointSets(stateCount, solver)
        val onStack = newMap()
        val stack = ArrayList<StackEntry>()

        for (root in 0 until stateCount) {
            val notDead = sets.notDead(root)
            if (notDead.isEmpty()) continue

            stack.add(StackEntry(root, notDead, dimensions))
            sets.initStateBottom(root, 0, notDead)
            onStack.union(root, notDead)

            while (stack.isNotEmpty()) {
                val top = stack.last()
                val (s, sP) = top
                if (top.hasNext()) {

                } else {
                    stack.removeAt(stack.lastIndex)
                }
            }
        }
        /*
            val sets = DisjointSets(...)
            val dead = newMap()
            val onStack = newMap()
            val stack = ArrayList<StackEntry>()

            for (root in 0 until stateCount) {
                val notDead = sets.notDead(root)

                //sets.setOf(root).map { (setRoot, setParams) ->
                //    // parameters for which the set of root is not dead
                //    dead.get(setRoot).not() and setParams
                //}.fold(empty) { a, b -> a or b }

                if (notDead.isEmpty()) continue

                stack.add(StackEntry(root, notDead, dimensions))
                onStack.union(root, notDead)
                sets.update_bottom(root, 0, notDead)

                while (stack.isNotEmpty()) {
                    val top = stack.last()
                    val (s, sP) = top
                    if (top.hasNext()) {
                        val d = top.next()
                        val t = states.flipValue(s, d)
                        val edgeParams = solver.transitionParams(s, d)
                        val tNotDead = sets.notDead(t) and edgeParams and sP
                        val foundCycle = onStack.get(t) and tNotDead
                        if (foundCycle.isNotEmpty()) {
                            var i = stack.lastIndex
                            while (sets.sameSet(stack[i].s,t,foundCycle)) {
                                sets.union(stack[i].s, t, foundCycle)
                                i -= 1
                            }
                        }
                        val fresh = onStack.get(t).not() and tNotDead
                        if (fresh.isNotEmpty()) {
                            onStack.union(t, fresh)
                            sets.update_bottom(t, stack.size, notDead)
                            stack.add(StackEntry(t, fresh, dimensions))
                        }
                    } else {
                        stack.removeAt(stack.lastIndex)
                        val isSetDone = sets.setBottom(s)
                        if (isSetDone.isNotEmpty()) {
                            sets.markDead(s, isSetDone)
                        }
                    }
                }
            }
         */
    }

    private fun AtomicReference<Double>.decrement(value: Double) {
        this.accumulateAndGet(value) { old, v -> old - v }
    }

    private class StackEntry(
            val s: Int, val sP: BDDSet, dimensions: Int
    ) : Iterator<Int> {

        private var d = dimensions

        override fun hasNext(): Boolean = d > 0

        override fun next(): Int {
            d -= 1
            return d
        }

        operator fun component1(): Int = s
        operator fun component2(): BDDSet = sP

    }*/

}