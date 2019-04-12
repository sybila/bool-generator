package cz.muni.fi.sybila.bool.rg.bdd

import cz.muni.fi.sybila.bool.rg.*
import java.util.*
import kotlin.collections.ArrayList


/**
 * BDD is an ordered binary decision diagram. BDDs are manipulated using a particular BDDSolver
 * which knows the context of the BDD (number and ordering of variables, unit BDD, etc.).
 *
 * The representation is centered around storing a binary DAG in an array. The array
 * contains two terminal nodes - indices 0 and 1 (except for empty BDD which contains just 0).
 * These have special "format" - simply an integer -1 is stored (so one can't distinguish between
 * 0 and one aside from its index - this is intentional, because you really really should not
 * do anything to the values in the array unless you know the index). Other nodes of the BDD
 * are stored as triples: (left, right, var) where var is the index of variable based on
 * which the nodes decides and left/right are indices into the array for positions where
 * respective subtrees reside. Pointer always points to var.
 *
 * Nodes are stored in the array with respect to their position in the graph - lower nodes have
 * smaller indices than their parents (the order of incomparable nodes can be arbitrary, but
 * it should be as fixed within the application, for example DFS post-order,
 * so that equivalent BDDs have equivalent representations).
 *
 * This ensures that the root of the BDD is always the last element of the array and allows easy references
 * to "sub-BDDs" as the BDD is just a slice of the original array.
 *
 *
 */

class BDDSolverOldAndUgly(
        private val network: BooleanNetwork
) {

    private val params = BooleanParamEncoder(network)
    private val states = BooleanStateEncoder(network)

    val zero = intArrayOf(-1)
    val one = intArrayOf(-1, -1)

    /* Maps our parameter indices to BDD sets. */
    private val parameterVarNames = Array(params.parameterCount) { variable(it) }
    //private val parameterNotVarNames = Array(params.parameterCount) { notVariable(it) }

    val empty: BDD = zero
    val unit: BDD = run {
        var result = one
        println("Num. parameters: ${params.parameterCount}")
        // Compute the "unit" BDD of valid parameters:
        var i = 0
        for (r in network.regulations) {
            //println("Regulation $i ${result.size}")
            i += 1
            //System.gc()
            val pairs = params.regulationPairs(r.regulator, r.target).map { (off, on) ->
                parameterVarNames[off] to parameterVarNames[on]
            }
            if (r.observable) {
                val constraint = pairs.map { (off, on) -> off uBiImp on }.merge { a, b -> a uAnd b }
                result = result uAnd constraint.uNot()
            }
            if (r.effect == BooleanNetwork.Effect.ACTIVATION) {
                val constraint = pairs.map { (off, on) -> off uImp on }.merge { a, b -> a uAnd b }
                result = result uAnd constraint
            }
            if (r.effect == BooleanNetwork.Effect.INHIBITION) {
                val constraint = pairs.map { (off, on) -> on uImp off }.merge { a, b -> a uAnd b }
                result = result uAnd constraint
            }
        }
        println("Unit BDD size: ${result.size} and cardinality ${cardinality(result)}")
        result
    }

    inline fun List<BDD>.merge(crossinline action: (BDD, BDD) -> BDD): BDD {
        var items = this
        while (items.size > 1) {
            items = items.mergePairs(action)
        }
        return items[0]
    }

    /**
     * Decide on [index]. If false (left), go to zero. If true (right), go to one.
     */
    fun variable(index: Int) = intArrayOf(-1, -1, 0, 1, index)

    /**
     * Decide on [index]. If false (left), go to one. If true (right), go to zero.
     */
    fun notVariable(index: Int) = intArrayOf(-1, -1, 1, 0, index)

    infix fun BDD.uAnd(that: BDD) = apply(this, that) { a, b -> a and b }
    infix fun BDD.uOr(that: BDD) = apply(this, that) { a, b -> a or b }
    infix fun BDD.uImp(that: BDD) = apply(this, that) { a, b -> ((a + 1)%2) or b }
    infix fun BDD.uBiImp(that: BDD) = apply(this, that) { a, b -> if (a == b) 1 else 0 }
    fun BDD.uNot() = negation(this)

    private fun cardinality(a: BDD): Double {
        val cache = DoubleArray(a.size) { -1.0 }
        cache[0] = 0.0
        cache[1] = 1.0
        val workStack = ArrayList<Int>()
        workStack.add(a.lastIndex)
        do {
            val node = workStack.last()
            if (cache[node] >= 0.0) {
                workStack.removeAt(workStack.lastIndex)
                continue
            }
            val left = a.getLeft(node)
            val right = a.getRight(node)
            if (cache[left] >= 0.0 && cache[right] >= 0.0) {
                cache[node] =
                        Math.pow(2.0, (a.getVar(node) - a.getVar(left) - 1).toDouble()) * cache[left] +
                        Math.pow(2.0, (a.getVar(node) - a.getVar(right) - 1).toDouble()) * cache[right]
                workStack.removeAt(workStack.lastIndex)
            } else {
                if (cache[right] < 0.0) {
                    workStack.add(right)
                }
                if (cache[left] < 0.0) {
                    workStack.add(left)
                }
            }
        } while (workStack.isNotEmpty())
        return cache.last()
    }

    private fun negation(a: BDD): BDD {
        if (a.isZero()) return one
        if (a.isOne()) return zero
        val result = a.clone()
        // Replace all occurrences of index 0 with index 1 and vice versa.
        // Note that this does not change the DFS post order of the graph except for leaves
        // which have a special position anyway.
        var i = result.lastIndex
        while (i > 2) {
            if (result[i - 1] < 2) { // update right
                result[i - 1] = (result[i - 1] + 1) % 2
            }
            if (result[i - 2] < 2) { // update left
                result[i - 2] = (result[i - 2] + 1) % 2
            }
            i -= 3  // skip (left, right, var) and jump to next var
        }
        return result
    }

    private inline fun apply(a: BDD, b: BDD, op: (Int, Int) -> Int): BDD {
        if (a.isLeaf() and b.isLeaf()) {
            val result = op(a.lastIndex, b.lastIndex)
            return if (result == 0) zero else one
        }

        // work stack is used to explore the two BDDs
        // it stores pairs of pointers (a, b) subsequently (to avoid pair allocation)
        val workStack = ArrayList<Int>()
        workStack.add(a.lastIndex)
        workStack.add(b.lastIndex)
        val result = ArrayList<Int>()
        result.add(-1)
        result.add(-1)
        // this extra variable is used to detect whether the resulting BDD is empty, because we have
        // already added the 1 node, we need to remove it if it was not used anywhere.
        // We can't deduce this from the graph alone, because it can actually represent one in the end, so that
        // There are no decision nodes to check or anything.
        var isZero = true
        val cache = HashMap<Long, Int>()
        do {
            val nodeB = workStack[workStack.lastIndex]
            val nodeA = workStack[workStack.lastIndex - 1]
            if (cache[key(nodeA, nodeB)] != null) {
                // some values are put on the stack twice, we therefore have to
                // check if they were not computed meanwhile
                workStack.removeAt(workStack.lastIndex)
                workStack.removeAt(workStack.lastIndex)
                continue
            }
            val varA = a.getVar(nodeA)
            val varB = b.getVar(nodeB)
            // Try to compute value for node A and B. If the value cannot be computed yet
            // due to missing cache entries, keep the values on the stack and
            // push missing indices into cache.
            if (varA == varB) {
                val leftA = a.getLeft(nodeA)
                val leftB = b.getLeft(nodeB)
                val rightA = a.getRight(nodeA)
                val rightB = b.getRight(nodeB)
                val newLeft = if (leftA.isLeaf() and leftB.isLeaf()) {
                    op(leftA, leftB).also { if (it == 1) isZero = false }
                } else {
                    cache[key(leftA, leftB)] ?: -1
                }
                val newRight = if (rightA.isLeaf() and rightB.isLeaf()) {
                    op(rightA, rightB).also { if (it == 1) isZero = false }
                } else {
                    cache[key(rightA, rightB)] ?: -1
                }
                if (newLeft > -1 && newRight > -1) {
                    // Both values exist, we can resolve this pair of values!
                    if (newLeft == newRight) {
                        // this decision node is useless, just point to either child
                        cache[key(nodeA, nodeB)] = newLeft
                    } else {
                        // create a new node
                        result.add(newLeft)
                        result.add(newRight)
                        result.add(a.getVar(nodeA))
                        cache[key(nodeA, nodeB)] = result.lastIndex
                    }
                    // mark current pair as done on the stack
                    workStack.removeAt(workStack.lastIndex)
                    workStack.removeAt(workStack.lastIndex)
                } else {
                    // the pair is not done, push the missing values:
                    if (newRight == -1) {
                        workStack.add(rightA)
                        workStack.add(rightB)
                    }
                    if (newLeft == -1) {
                        workStack.add(leftA)
                        workStack.add(leftB)
                    }
                }
            } else if (varA > varB) {
                // node A is "higher" in the graph than node B (remember, we assume variable zero is the "bottom" level.
                // split on A, but keep B
                val leftA = a.getLeft(nodeA)
                val newLeft = if (leftA.isLeaf() and nodeB.isLeaf()) {
                    op(leftA, nodeB).also { if (it == 1) isZero = false }
                } else {
                    cache[key(leftA, nodeB)] ?: -1
                }
                val rightA = a.getRight(nodeA)
                val newRight = if (rightA.isLeaf() and nodeB.isLeaf()) {
                    op(rightA, nodeB).also { if (it == 1) isZero = false }
                } else {
                    cache[key(rightA, nodeB)] ?: -1
                }
                if (newLeft > -1 && newRight > -1) {
                    // Both values exist, we can resolve this pair of values!
                    if (newLeft == newRight) {
                        // this decision node is useless, just point to either child
                        cache[key(nodeA, nodeB)] = newLeft
                    } else {
                        // create a new node
                        result.add(newLeft)
                        result.add(newRight)
                        result.add(a.getVar(nodeA)) // use "higher" node
                        cache[key(nodeA, nodeB)] = result.lastIndex
                    }
                    // mark current pair as done on the stack
                    workStack.removeAt(workStack.lastIndex)
                    workStack.removeAt(workStack.lastIndex)
                } else {
                    // the pair is not done, push the missing values:
                    if (newRight == -1) {
                        workStack.add(rightA)
                        workStack.add(nodeB)
                    }
                    if (newLeft == -1) {
                        workStack.add(leftA)
                        workStack.add(nodeB)
                    }
                }
            } else {
                // varA < varB - symmetric to previous case
                // node A is "higher" in the graph than node B (remember, we assume variable zero is the "bottom" level.
                // split on A, but keep B
                val leftB = b.getLeft(nodeB)
                val newLeft = if (nodeA.isLeaf() and leftB.isLeaf()) {
                    op(nodeA, leftB).also { if (it == 1) isZero = false }
                } else {
                    cache[key(nodeA, leftB)] ?: -1
                }
                val rightB = b.getRight(nodeB)
                val newRight = if (nodeA.isLeaf() and rightB.isLeaf()) {
                    op(nodeA, rightB).also { if (it == 1) isZero = false }
                } else {
                    cache[key(nodeA, rightB)] ?: -1
                }
                if (newLeft > -1 && newRight > -1) {
                    // Both values exist, we can resolve this pair of values!
                    if (newLeft == newRight) {
                        // this decision node is useless, just point to either child
                        cache[key(nodeA, nodeB)] = newLeft
                    } else {
                        // create a new node
                        result.add(newLeft)
                        result.add(newRight)
                        result.add(b.getVar(nodeB)) // use "higher node"
                        cache[key(nodeA, nodeB)] = result.lastIndex
                    }
                    // mark current pair as done on the stack
                    workStack.removeAt(workStack.lastIndex)
                    workStack.removeAt(workStack.lastIndex)
                } else {
                    // the pair is not done, push the missing values:
                    if (newRight == -1) {
                        workStack.add(nodeA)
                        workStack.add(rightB)
                    }
                    if (newLeft == -1) {
                        workStack.add(nodeA)
                        workStack.add(leftB)
                    }
                }
            }
        } while (workStack.isNotEmpty())

        if (isZero) return zero
        if (result.size == 2) return one

        return result.toIntArray()
    }

    private fun BDD.isOne() = this.size == 2
    private fun BDD.isZero() = this.size == 1
    private fun key(nodeA: Int, nodeB: Int): Long = nodeA.toLong().shl(31) + nodeB
    private fun BDD.isLeaf(): Boolean = this.size <= 2
    private fun Int.isLeaf(): Boolean = this < 2
    private fun BDD.getVar(node: Int): Int = this[node]
    private fun BDD.getLeft(node: Int): Int = this[node - 2]
    private fun BDD.getRight(node: Int): Int = this[node - 1]

}