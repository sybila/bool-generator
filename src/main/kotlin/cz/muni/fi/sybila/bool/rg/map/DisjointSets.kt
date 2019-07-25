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
        capacity: Int, solver: BDDSolver
) {

    private val data: Array<Pointer> = Array(capacity) { s -> hashMapOf(s to solver.unit.toBDD()) }



}