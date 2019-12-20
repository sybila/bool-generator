package cz.muni.fi.sybila.bool.common

import cz.muni.fi.sybila.bool.rg.mergePairs

abstract class Solver<T> {

    abstract val empty: T
    abstract val unit: T

    abstract infix fun T.and(that: T): T
    abstract infix fun T.or(that: T): T
    abstract fun not(it: T): T

    abstract fun T.isEmpty(): Boolean

    abstract fun transitionParams(from: Int, dimension: Int): T

    // UTILITY METHODS

    fun T.isNotEmpty(): Boolean = !this.isEmpty()

    open infix fun T.subsetEq(that: T): Boolean = (this and not(that)).isEmpty()

    open fun resetStats() {}
    open fun T.prettyPrint(): String { return this.toString() }

    /**
     * Merge items in the list pair by pair. Essentially a fancy fold, but this reduction strategy
     * tends to be more efficient for parameter sets.
     */
    inline fun List<T>.merge(crossinline action: (T, T) -> T): T {
        var items = this
        while (items.size > 1) {
            items = items.mergePairs(action)
        }
        return items[0]
    }

}