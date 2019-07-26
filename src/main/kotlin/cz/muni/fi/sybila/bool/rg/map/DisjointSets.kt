package cz.muni.fi.sybila.bool.rg.map

import cz.muni.fi.sybila.bool.rg.BDDSet
import cz.muni.fi.sybila.bool.rg.BDDSolver

// we must use a wrapper due to array
private class BDD(
        val set: BDDSet
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BDD

        if (!set.contentEquals(other.set)) return false

        return true
    }

    override fun hashCode(): Int {
        return set.contentHashCode()
    }
}

private fun BDDSet.toBDD() = BDD(this)
private fun BDD.toBDDSet() = this.set

private typealias Pointer = HashMap<Int, BDD>

class DisjointSets(
        capacity: Int, private val solver: BDDSolver
) {

    private val isDead: Array<BDDSet> = Array(capacity) { solver.empty }
    private val data: Array<Pointer> = Array(capacity) { s -> hashMapOf(s to solver.unit.toBDD()) }
    private val setBottoms: Array<Pointer> = Array(capacity) { Pointer() }


    /**
     * Compute the set of colours where the given state is not dead (i.e. the sets it is contained in are still
     * alive)
     */
    fun notDead(state: Int): BDDSet = solver.run {
        val roots = Pointer()
        findRoots(state, unit, roots)
        return roots
                // reduce to roots which are not dead
                .map { (k, v) -> (v.toBDDSet() and isDead[k].not()) }
                // union them all
                .fold(empty) { a, b -> a or b }
    }

    /**
     * Set stack bottom of the given [state] to be the specified [value] for the given [params].
     *
     * WARNING: This assumes that the bottom was never set before and that the state is a root for the
     * given params (i.e. it is newly discovered) - i.e. we don't look for set roots or anything.
     */
    fun initStateBottom(state: Int, value: Int, params: BDDSet) {
        setBottoms[state].union(value, params)
    }

    /**
     * Check if two states are in the same set for the given set of colours.
     */
    fun areSameSet(a: Int, b: Int, params: BDDSet): Boolean {
        val rootsA = findRoots(a, params)
        val rootsB = findRoots(b, params)
        // since both A and B have to contain all parameters in params (every parameter has to belong somewhere)
        // the check is essentially just equality (BDD wrapper object ensures arrays are correctly represented)
        return rootsA == rootsB
    }

    /**
     * Compute the set of colours in [params] for which [state] is the bottom of a set and that bottom
     * is [stackLocation].
     */
    fun getSetBottom(state: Int, stackLocation: Int, params: BDDSet): BDDSet = solver.run {
        val roots = findRoots(state, params)
        var result = empty
        for ((root, rootParams) in roots) {
            for ((bottom, bottomParams) in setBottoms[root]) {
                if (bottom == stackLocation) {
                    result = result or (rootParams.toBDDSet() and bottomParams.toBDDSet())
                }
            }
        }
        result
    }

    /**
     * Ensure that [a] and [b] are in the same set for [params].
     */
    fun union(a: Int, b: Int, params: BDDSet) {
        val rootsA = findRoots(a, params)
        val rootsB = findRoots(b, params)
        for ((rootA, rootAParams) in rootsA) {
            for ((rootB, rootBParams) in rootsB) {
                if (rootA < rootB) {
                    data[rootA].moveUp(rootA, rootB, rootAParams.toBDDSet())
                }
                if (rootB < rootA) {
                    data[rootB].moveUp(rootB, rootA, rootBParams.toBDDSet())
                }
            }
        }
    }

    private fun findRoots(state: Int, params: BDDSet) = Pointer().also { findRoots(state, params, it) }

    private fun findRoots(state: Int, params: BDDSet, result: Pointer): Unit = solver.run {
        // first - do compaction, but for all parameters at once (why not, right?)
        val newPointer = Pointer()
        for ((parent, parentParams) in data[state]) {
            if (parent == state) {
                // we are the root of the set, we can't do better
                newPointer[state] = parentParams
            } else {
                // parent >= state and parentsParent >= parent
                for ((parentsParent, parentsParentParams) in data[parent]) {
                    newPointer.union(parentsParent, parentParams.toBDDSet() and parentsParentParams.toBDDSet())
                }
            }
        }
        data[state] = newPointer
        // now follow the (compacted) pointer
        for ((parent, parentParamsAll) in data[state]) {
            val parentParams = parentParamsAll.toBDDSet() and params
            if (parentParams.isNotEmpty()) {
                if (parent == state) {
                    // parentParams are root params
                    result.union(state, parentParams)
                } else {
                    findRoots(state, parentParams, result)
                }
            }
        }
    }


    private fun Pointer.moveUp(from: Int, to: Int, params: BDDSet) = solver.run {
        val target = this@moveUp
        if (from !in target) return@run
        val moveUp = target.getValue(from).toBDDSet() and params
        target[from] = (target.getValue(from).toBDDSet() and moveUp.not()).toBDD()
        target[to] = ((target[to]?.toBDDSet() ?: empty) or moveUp).toBDD()
    }


    private fun Pointer.moveUp(params: BDDSet, from: Pointer) = solver.run {
        val target = this@moveUp
        // first - remove all parameters that will be moving up
        val pNot = params.not()
        for ((s, p) in target) {
            target[s] = (p.toBDDSet() and pNot).toBDD()
        }
        // second - add parameters as seen in parent
        for ((parent, parentParamsAll) in from) {
            val toMove = (parentParamsAll.toBDDSet() and params)
            if (toMove.isNotEmpty()) {
                val current = target[parent]?.toBDDSet() ?: empty
                target[parent] = (current or toMove).toBDD()
            }
        }
    }

    private fun Pointer.union(state: Int, params: BDDSet) {
        if (state in this) {
            this[state] = solver.run { getValue(state).toBDDSet() or params }.toBDD()
        } else {
            this[state] = params.toBDD()
        }
    }

}