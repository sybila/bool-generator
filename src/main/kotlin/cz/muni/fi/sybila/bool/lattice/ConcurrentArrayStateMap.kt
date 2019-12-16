package cz.muni.fi.sybila.bool.lattice

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReferenceArray

class ConcurrentArrayStateMap(
        val capacity: Int, private val solver: LatticeSolver
) {

    private val sizeAtomic = AtomicInteger(0)

    val size: Int
        get() = sizeAtomic.get()

    private val data = AtomicReferenceArray<LatticeSet?>(capacity)

    fun getOrNull(state: Int): LatticeSet? = data[state]

    fun get(state: Int): LatticeSet = data[state] ?: solver.empty

    fun union(state: Int, value: LatticeSet): Boolean {
        solver.run {
            if (value.isEmpty()) return false
            var beforeUpdate: LatticeSet?
            do {
                beforeUpdate = data[state]
                val current = beforeUpdate ?: empty
                val union = current or value
                if (union subsetEq current) return false
            } while (!data.compareAndSet(state, beforeUpdate, union))
            if (beforeUpdate == null) sizeAtomic.incrementAndGet()
            return true
        }
    }

}