package cz.muni.fi.sybila.bool.common

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReferenceArray

class ConcurrentArrayStateMap<T>(
        val capacity: Int, private val solver: Solver<T>
) {

    private val sizeAtomic = AtomicInteger(0)

    val size: Int
        get() = sizeAtomic.get()

    private val data = AtomicReferenceArray<T?>(capacity)

    fun getOrNull(state: Int): T? = data[state]

    fun get(state: Int): T = data[state] ?: solver.empty

    fun union(state: Int, value: T): Boolean {
        solver.run {
            if (value.isEmpty()) return false
            var beforeUpdate: T?
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