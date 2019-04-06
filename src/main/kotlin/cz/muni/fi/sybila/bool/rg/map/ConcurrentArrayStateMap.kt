package cz.muni.fi.sybila.bool.rg.map

import cz.muni.fi.sybila.bool.rg.BDDSet
import cz.muni.fi.sybila.bool.rg.BDDSolver
import cz.muni.fi.sybila.bool.rg.State
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReferenceArray

class ConcurrentArrayStateMap(
        capacity: Int, private val solver: BDDSolver
) {

    private val sizeAtomic = AtomicInteger(0)

    val size: Int
        get() = sizeAtomic.get()

    private val data = AtomicReferenceArray<BDDSet?>(capacity)

    fun getOrNull(state: State): BDDSet? = data[state]

    fun get(state: State): BDDSet = data[state] ?: solver.empty

    fun union(state: State, value: BDDSet): Boolean {
        solver.run {
            if (value.isEmpty()) return false
            var current: BDDSet?
            do {
                current = data[state]
                val c = current ?: empty
                if (value subset c) return false
                val union = c or value
            } while (!data.compareAndSet(state, current, union))
            if (current == null) sizeAtomic.incrementAndGet()
            return true
        }
    }

}