package cz.muni.fi.sybila.bool.lattice

import cz.muni.fi.sybila.bool.common.Solver
import cz.muni.fi.sybila.bool.rg.BooleanNetwork
import cz.muni.fi.sybila.bool.rg.BooleanParamEncoder
import cz.muni.fi.sybila.bool.rg.BooleanStateEncoder
import java.util.concurrent.atomic.AtomicInteger

typealias LatticeSet = Set<Lattice>

class LatticeSolver(
        network: BooleanNetwork
) : Solver<LatticeSet>() {

    private val params = BooleanParamEncoder(network)
    private val states = BooleanStateEncoder(network)

    private var simplificationThreshold = AtomicInteger(4)

    override val empty: LatticeSet = setOf()
    override val unit: LatticeSet = setOf(Lattice(params.parameterCount))

    override infix fun LatticeSet.or(that: LatticeSet): LatticeSet {
        if (this.isEmpty()) return that
        if (that.isEmpty()) return this
        return (this + that).simplify()
    }

    override infix fun LatticeSet.and(that: LatticeSet): LatticeSet {
        if (this.isEmpty()) return empty
        if (that.isEmpty()) return empty
        val newSet = HashSet<Lattice>()
        for (a in this) {
            for (b in that) {
                (a intersect b)?.let(newSet::add)
            }
        }
        return newSet.simplify()
    }

    override fun not(it: LatticeSet): LatticeSet {
        //println("Size: ${it.size}")
        if (it.isEmpty()) return unit
        return it.map { it.invert() }.fold(unit) { a, b -> a and b }
    }

    override fun LatticeSet.subsetEq(that: LatticeSet): Boolean {
        //return (this and not(that)).isEmpty()
        for (lattice in this) {
            var remaining = setOf(lattice)
            for (larger in that) {
               remaining = remaining.flatMapTo(HashSet()) { it.subtract(larger) }
                if (remaining.isEmpty()) break
            }
            if (remaining.isNotEmpty()) return false
        }
        return true
    }

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    override fun LatticeSet.isEmpty(): Boolean = this.isEmpty()

    override fun resetStats() {
        simplificationThreshold.set(4)
    }

    private fun LatticeSet.simplify(): LatticeSet {
        if (this.size < 2 * simplificationThreshold.get()) return this
        else {
            val simplified = HashSet<Lattice>()
            for (lattice in this.sortedByDescending { it.cardinality() }) {
                if (setOf(lattice) subsetEq simplified) continue
                var unionWith: Lattice? = null
                for (candidate in simplified) {
                    val union = lattice tryUnion candidate
                    if (union != null) {
                        unionWith = candidate; break
                    }
                }
                if (unionWith != null) {
                    simplified.remove(unionWith)
                    simplified.add((lattice tryUnion unionWith)!!)
                } else {
                    simplified.add(lattice)
                }
            }
            // insert a lattice into a lattice set, possibly merging it with existing lattice in the set
            /*fun push(set: MutableSet<Lattice>, lattice: Lattice) {
                var unionWith: Lattice? = null
                for (candidate in set) {
                    val union = lattice tryUnion candidate
                    if (union != null) {
                        unionWith = candidate; break
                    }
                }
                if (unionWith != null) {
                    set.remove(unionWith)
                    set.add((lattice tryUnion unionWith)!!)
                } else {
                    set.add(lattice)
                }
            }
            for (lattice in this.sortedByDescending { it.cardinality() }) {
                // First, find all intersection lattices.
                val intersection = setOf(lattice) intersect simplified
                // If the intersection is empty, just add the lattice
                if (intersection.isEmpty()) {
                    push(simplified, lattice)
                // Otherwise, we are going to add only the remaining necessary lattices that are not present already
                } else {
                    var toInsert = setOf(lattice)
                    for (intersectionLattice in intersection) {
                        toInsert = toInsert.flatMapTo(HashSet()) { it.subtract(intersectionLattice) }
                    }
                    toInsert.forEach { push(simplified, it) }
                }
            }*/
            //println("Simplified ${this.size} to ${simplified.size}")
            if (simplified.size > simplificationThreshold.get()) {
                simplificationThreshold.set(simplified.size)
                println("New threshold: ${simplified.size} for $simplified")
            }
            return simplified
        }
    }

    override fun transitionParams(from: Int, dimension: Int): LatticeSet {
        val isActive = states.isActive(from, dimension)
        val parameterIndex = params.transitionParameter(from, dimension)
        // If we are active, we want to go down, so p = 0, otherwise we want to go up, so p = 1
        val lattice = Lattice(params.parameterCount)
        return setOf(lattice.copyWithUpdate(parameterIndex, if (!isActive) ONE else ZERO))
    }

}