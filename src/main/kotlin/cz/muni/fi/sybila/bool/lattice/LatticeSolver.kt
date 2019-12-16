package cz.muni.fi.sybila.bool.lattice

import java.util.concurrent.atomic.AtomicInteger

typealias LatticeSet = Set<Lattice>

class LatticeSolver(
        val numVars: Int
) {

    private var simplificationThreshold = AtomicInteger(4)

    val empty: LatticeSet = setOf()
    val unit: LatticeSet = setOf(Lattice(numVars))

    infix fun LatticeSet.or(that: LatticeSet): LatticeSet {
        if (this.isEmpty()) return that
        if (that.isEmpty()) return this
        return (this + that).simplify()
    }

    infix fun LatticeSet.and(that: LatticeSet): LatticeSet {
        if (this.isEmpty()) return empty
        if (that.isEmpty()) return empty
        val newSet = HashSet<Lattice>()
        for (a in this) {
            for (b in that) {
                (a intersect b)?.let(newSet::add)
            }
        }
        return newSet
    }

    infix fun LatticeSet.subsetEq(that: LatticeSet): Boolean {
        TODO()
    }

    private fun LatticeSet.simplify(): LatticeSet {
        if (this.size < simplificationThreshold.get()) return this
        else {
            // TODO
            return this
        }
    }
}