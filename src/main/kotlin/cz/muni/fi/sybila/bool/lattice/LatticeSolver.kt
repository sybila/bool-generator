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
        if (false && this.size < 2 * simplificationThreshold.get()) return this
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
        val targetParameterValue = if (!states.isActive(from, dimension)) {
            // The value of [dimension] is going from 0 to 1 -> transition is increasing.
            ONE
        } else {
            // The value of [dimension] is going from 1 to 0 -> transition is decreasing.
            ZERO
        }

        val latticeTemplate = ByteArray(params.parameterCount)
        // In the lattice template, we want to set parameters to target value in cases
        // where we can infer monotonicity. Specifically, we start with x = current state and enumerate
        // all y such that:
        // If regulation y -> x is activating, then:
        // f(x) = 0 and y < x => f(y) = 0
        // f(x) = 1 and y > x => f(y) = 1
        // If regulation y -> x is inhibiting, then:
        // f(x) = 0 and y > x => f(y) = 0
        // f(x) = 1 and y < x => f(y) = 1
        val context = params.descendingContexts[dimension]
        // Notice that we can just manipulate transition parameter index directly, because the parameter id
        // is just a "compressed" state where all non-regulators are removed. So when we increase/decrease
        // specific bit values, it's like updating [from] state and recomputing the parameter index.
        fun buildLattice(parameterIndex: Int) {
            // Only continue if the value in the lattice hasn't been updated before
            if (latticeTemplate[parameterIndex] != targetParameterValue) {
                latticeTemplate[parameterIndex] = targetParameterValue
                for (regulatorIndex in context.indices) {   // try increasing different regulators
                    val regulation = context[regulatorIndex]
                    if (
                            (regulation.effect == BooleanNetwork.Effect.ACTIVATION && targetParameterValue == ONE) ||
                            (regulation.effect == BooleanNetwork.Effect.INHIBITION && targetParameterValue == ZERO)
                    ) {
                        // We should increase the value in the parameter index if possible!
                        val increasedParameter = parameterIndex.or(1.shl(regulatorIndex))
                        if (increasedParameter != parameterIndex) { // if the update did something
                            buildLattice(increasedParameter)
                        }
                    }

                    if (
                            (regulation.effect == BooleanNetwork.Effect.ACTIVATION && targetParameterValue == ZERO) ||
                            (regulation.effect == BooleanNetwork.Effect.INHIBITION && targetParameterValue == ONE)
                    ) {
                        // We should decrease the value in the parameter index if possible!
                        val decreasedParameter = parameterIndex.and(1.shl(regulatorIndex).inv())
                        if (decreasedParameter != parameterIndex) { // if the update did something
                            buildLattice(decreasedParameter)
                        }
                    }
                }
                //if (regulatorIndex >= context.size) return  // only go through valid regulations

            }
        }
        buildLattice(params.transitionParameter(from, dimension))

        return setOf(Lattice(latticeTemplate))
    }


}