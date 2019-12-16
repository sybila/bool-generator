package cz.muni.fi.sybila.bool.rg.map

import cz.muni.fi.sybila.bool.rg.BDDSet
import cz.muni.fi.sybila.bool.rg.BDDSolver
import cz.muni.fi.sybila.bool.rg.State

class DecreasingStateMap(
        initial: ArrayStateMap, private val solver: BDDSolver
) : Iterable<Pair<Int, BDDSet>> {

    var size = 0
        private set

    private val data = Array(initial.capacity) { s ->
        initial.getOrNull(s).also { if (it != null) size += 1 }
    }

    fun toStateMap() = ArrayStateMap(data, size, solver)

    operator fun contains(state: State): Boolean = data[state] != null

    fun getOrNull(state: State): BDDSet? = data[state]

    fun get(state: State): BDDSet {
        return data[state] ?: solver.empty
    }

    fun intersect(state: State, value: BDDSet): Boolean {
        solver.run {
            val current = data[state] ?: return false
            val update = current and value
            return if (update.isEmpty()) {
                // if completely removed, decrease size and set null
                size -= 1
                data[state] = null
                true
            } else {
                // if current is strict superset of update, set value and return true
                if ((current and not(update)).isEmpty()) false else {
                    data[state] = update
                    true
                }
            }
        }
    }

    // iterator works because we can't "decrease" value back to null
    override fun iterator(): Iterator<Pair<Int, BDDSet>> = object : Iterator<Pair<Int, BDDSet>> {
        private var i = 0

        init {
            // move to first position
            while (i < data.size && data[i] == null) i += 1
        }

        override fun hasNext(): Boolean = i < data.size

        override fun next(): Pair<Int, BDDSet> = (i to data[i]!!).also {
            // move to next position
            i += 1
            while (i < data.size && data[i] == null) i += 1
        }
    }

}