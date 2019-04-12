package cz.muni.fi.sybila.bool.rg.parallel

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray

/**
 * We need an atomic way to keep nodes which need to be updated and iterate them.
 */
class ConcurrentStateQueue(
        private val stateCount: Int
) {

    private val empty = AtomicBoolean(true)
    private val set = AtomicIntegerArray((stateCount / 31) + 1)

    private val iterator = AtomicInteger(0)

    val isEmpty: Boolean
        get() = empty.get()

    fun set(state: Int) {
        empty.set(false) // only decrease
        set.accumulateAndGet(state / 31, 1.shl(state % 31)) { value, mask -> value or mask }
    }

    fun unsafeSize(): Int {
        var size = 0
        for (i in 0 until stateCount) {
            if (set.get(i / 31).shr(i % 31).and(1) == 1) size += 1
        }
        return size
    }

    fun print(): String {
        return (0 until stateCount).map { set.get(it / 31).shr(it % 31).and(1) }.toString()
    }

    fun next(): Int {
        var result: Int
        do {
            val oldIt = iterator.get()
            var it = oldIt
            while (it < stateCount && set.get(it / 31).shr(it % 31).and(1) == 0) { it += 1 }
            if (it >= stateCount) return -1 // at the end
            result = it
            // repeat until we are the ones who actually found the next item
        } while (!iterator.compareAndSet(oldIt, it + 1))
        return result
    }

}