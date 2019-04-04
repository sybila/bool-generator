package cz.muni.fi.sybila.bool.rg.map

import cz.muni.fi.sybila.bool.rg.BDDSet
import cz.muni.fi.sybila.bool.rg.BDDSolver
import cz.muni.fi.sybila.bool.rg.State

class ArrayStateMap(
        private val data: Array<BDDSet?>, size: Int, private val solver: BDDSolver
) : Iterable<Pair<Int, BDDSet>> {

    var size = size
        private set

    val capacity: Int
        get() = data.size

    constructor(capacity: Int, solver: BDDSolver) : this(arrayOfNulls<BDDSet?>(capacity), 0, solver)

    operator fun contains(state: State): Boolean = data[state] != null

    fun getOrNull(state: State): BDDSet? = data[state]

    fun get(state: State): BDDSet {
        return data[state] ?: solver.empty
    }

    fun union(state: State, value: BDDSet): Boolean {
        solver.run {
            if (value.isEmpty()) return false   // skip adding empty sets so that we keep nulls
            val current = get(state)
            return if ((value and current.not()).isEmpty()) false else {
                if (data[state] == null) size += 1  // update size if first inserting!
                data[state] = current or value
                true
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