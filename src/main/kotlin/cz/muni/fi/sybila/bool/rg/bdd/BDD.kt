package cz.muni.fi.sybila.bool.rg.bdd

import java.io.File
import java.util.*
import kotlin.collections.HashMap

/**
 * BDD worker manipulates BDDs represented as integer arrays.
 *
 * BDD worker is not thread-safe, because it uses local data structures for
 * caching and other operations. However, array-based BDDs are completely thread
 * safe and can be safely manipulated by multiple workers at a time or serialized.
 *
 * Array-based BDD: We store the BDD inside an array of elements (left, right, var)
 * where 'var' is the variable based on which the node decides and left/right are pointers
 * (array indices) into the smaller array. (var is usually much smaller than array size, but
 * we need the values to align properly for faster manipulation). Special cases are the terminal
 * nodes (0 and 1) which are stored just as a single value on positions 0 a 1 (so it is easy to
 * check if something is a terminal node).
 *
 * The values in the array are stored in a DFS post-order of the BDD where negative edge is
 * always taken first. In particular, this means that nodes "lower" in the graph
 * have smaller indices. This allows us to create a "BDD slice" where we use the same array
 * but with different root to mean a sub-BDD.
 *
 * Dimension of terminal nodes is -1. The remaining nodes are ordered from largest to smallest
 * i.e. dimension 0 is closest to the terminal nodes. This means that variable indices are ordered,
 * including terminal nodes.
 */
class BDDWorker(
        /**
         * Number of variables managed by this BDD worker.
         *
         * (usually, this can be inferred from the BDD, but sometimes there may be less
         * variables and then we need this to properly compute cardinality, etc.)
         */
        private val numVars: Int
) {


    private val control = HashMap<List<Int>, Int>()

    val one = intArrayOf(numVars, numVars).also {
        control[it.toList()] = 1
    }
    val zero = intArrayOf(numVars).also {
        control[it.toList()] = 0
    }

    fun variable(v: Int) = intArrayOf(numVars, numVars, 0, 1, v)
    fun notVariable(v: Int) = intArrayOf(numVars, numVars, 1, 0, v)

    fun and(a: BDD, b: BDD) = apply(a, b) { i, j -> i and j }

    fun or(a: BDD, b: BDD) = apply(a, b) { i, j -> i or j }

    fun imp(a: BDD, b: BDD) = apply(a, b) { i, j -> if (i == 0) 1 else j }

    fun biImp(a: BDD, b: BDD) = apply(a, b) { i, j -> if (i == j) 1 else 0 }

    fun not(a: BDD) = negation(a)
    fun isUnit(a: BDD): Boolean = a.isOne()
    fun isEmpty(a: BDD): Boolean = a.isZero()
    fun satCount(a: BDD): Double = cardinality(a)
    fun nodeCount(a: BDD): Int = (a.size - 2) / 3

    fun printDot(a: BDD, filename: String) {
        val done = IntArray(a.size)
        File(filename).bufferedWriter().use {
            push(a.root())
            it.write("""
                digraph G {
	                init__ [label="", style=invis, height=0, width=0];
	                init__ -> ${a.root()};
            """.trimIndent())
            do {
                val node = peek()
                pop()
                if (done[node] == 1 || node.isTerminal()) continue
                it.write("$node[label=\"v${a.v(node)+1}\"];")
                it.newLine()
                if (a.left(node) != 0) {
                    it.write("$node-> ${a.left(node)} [style=dotted];")
                    it.newLine()
                    push(a.left(node))
                }
                if (a.right(node) != 0) {
                    it.write("$node-> ${a.right(node)} [style=filled];")
                    it.newLine()
                    push(a.right(node))
                }
                done[node] = 1
            } while (stackNotEmpty)
            it.write("""
                0 [shape=box, label="0", style=filled, shape=box, height=0.3, width=0.3];
                1 [shape=box, label="1", style=filled, shape=box, height=0.3, width=0.3];

                }
            """.trimIndent())
        }
    }

    fun cardinality(a: BDD): Double {
        // this is rather wasteful, but at this point computing cardinality is not a bottleneck so we keep it as is.
        val cache = DoubleArray(a.size) { -1.0 }
        cache[0] = 0.0; cache[1] = 1.0
        push(a.root())
        do {
            val node = peek()
            if (cache[node] >= 0.0) {
                pop(); continue // sometimes, values can appear multiple times on the stack
            }
            val left = a.left(node); val right = a.right(node)
            if (cache[left] >= 0.0 && cache[right] >= 0.0) {
                val leftCardinality = cache[left] * Math.pow(2.0, (a.v(left) - a.v(node) - 1).toDouble())
                val rightCardinality = cache[right] * Math.pow(2.0, (a.v(right) - a.v(node) - 1).toDouble())
                cache[node] = leftCardinality + rightCardinality
                pop()
            } else {
                if (cache[right] < 0.0) push(right)
                if (cache[left]  < 0.0) push(left)
            }
        } while (stackNotEmpty)
        return cache.last() * Math.pow(2.0, (a.v(a.root())).toDouble())
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
            // update right
            if (result[i - 1] < 2) result[i - 1] = result[i - 1] xor 1
            // update left
            if (result[i - 2] < 2) result[i - 2] = result[i - 2] xor 1
            i -= 3  // skip (left, right, var) and jump to next var
        }
        return result
    }

    private data class Triple(val a: Int, val b: Int, val c: Int)

    private inline fun apply(a: BDD, b: BDD, op: (Int, Int) -> Int): BDD {
        if (a.isTerminal() and b.isTerminal()) {
            val result = op(a.root(), b.root())
            return if (result == 0) zero else one
        }

        val triples = HashMap<Triple, Int>()
        // in this case, work stack saves pairs of nodes
        push(a.root()); push(b.root())           // init BDD exploration
        pushWork(numVars); pushWork(numVars)  // add terminal nodes to result

        // this extra variable is used to detect whether the resulting BDD is empty, because we have
        // already added the 1 node, we need to remove it if it was not used anywhere.
        // We can't deduce this from the graph alone, because it can actually represent one in the end, so that
        // There are no decision nodes to check or anything.

        /*
            If the BDD is empty, both terminal nodes will still be there. Hence we have to detect if there
            is something in it. If it is not, we return zero explicitly later
         */
        var isZero = true
        do {
            val nodeB = peek1(); val nodeA = peek2() // order is reversed (stack)
            if (getCache(nodeA, nodeB) != null) {
                pop(); pop(); continue
            }
            val varA = a.v(nodeA); val varB = b.v(nodeB)
            // Try to compute value for node A and B. If the value cannot be computed yet
            // due to missing cache entries, keep the values on the stack and
            // push missing indices into cache.
            if (varA == varB) {
                val leftA = a.left(nodeA); val rightA = a.right(nodeA)
                val leftB = b.left(nodeB); val rightB = b.right(nodeB)

                val leftNew = if (leftA.isTerminal() and leftB.isTerminal()) {
                    op(leftA, leftB).also { if (it == 1) isZero = false }
                } else {
                    getCache(leftA, leftB) ?: -1
                }

                val rightNew = if (rightA.isTerminal() and rightB.isTerminal()) {
                    op(rightA, rightB).also { if (it == 1) isZero = false }
                } else {
                    getCache(rightA, rightB) ?: -1
                }

                if (leftNew > -1 && rightNew > -1) {
                    // Both value exist, we can resolve this pair of values!
                    if (leftNew == rightNew) {
                        saveCache(nodeA, nodeB, leftNew)
                    } else {
                        // new node needs to be created
                        val triple = Triple(leftNew, rightNew, a.v(nodeA))
                        var existing = triples[triple]
                        if (existing == null) {
                            pushWork3(leftNew, rightNew, a.v(nodeA))
                            existing = lastWork()
                            triples[triple] = existing
                        }
                        saveCache(nodeA, nodeB, existing)
                    }
                    pop(); pop()
                } else {
                    if (rightNew == -1) {
                        push(rightA); push(rightB)
                    }
                    if (leftNew == -1) {
                        push(leftA); push(leftB)
                    }
                }
            } else if (varA < varB) {
                // node A is "higher" in the graph than node B (remember, we assume variable zero is the "bottom" level.
                // split on A, but keep B
                val leftA = a.left(nodeA); val rightA = a.right(nodeA)

                val leftNew = if (leftA.isTerminal() and nodeB.isTerminal()) {
                    op(leftA, nodeB).also { if (it == 1) isZero = false }
                } else {
                    getCache(leftA, nodeB) ?: -1
                }

                val rightNew = if (rightA.isTerminal() and nodeB.isTerminal()) {
                    op(rightA, nodeB).also { if (it == 1) isZero = false }
                } else {
                    getCache(rightA, nodeB) ?: -1
                }

                if (leftNew > -1 && rightNew > -1) {
                    if (leftNew == rightNew) {
                        saveCache(nodeA, nodeB, leftNew)
                    } else {
                        // new node needs to be created
                        val triple = Triple(leftNew, rightNew, a.v(nodeA))
                        var existing = triples[triple]
                        if (existing == null) {
                            pushWork3(leftNew, rightNew, a.v(nodeA))
                            existing = lastWork()
                            triples[triple] = existing
                        }
                        saveCache(nodeA, nodeB, existing)
                    }
                    pop(); pop()
                } else {
                    // the pair is not done, push the missing values:
                    if (rightNew == -1) {
                        push(rightA); push(nodeB)
                    }
                    if (leftNew == -1) {
                        push(leftA); push(nodeB)
                    }
                }
            } else {
                // varA < varB - symmetric to previous case
                val leftB = b.left(nodeB); val rightB = b.right(nodeB)

                val leftNew = if (nodeA.isTerminal() and leftB.isTerminal()) {
                    op(nodeA, leftB).also { if (it == 1) isZero = false }
                } else {
                    getCache(nodeA, leftB) ?: -1
                }

                val rightNew = if (nodeA.isTerminal() and rightB.isTerminal()) {
                    op(nodeA, rightB).also { if (it == 1) isZero = false }
                } else {
                    getCache(nodeA, rightB) ?: -1
                }

                if (leftNew > -1 && rightNew > -1) {
                    if (leftNew == rightNew) {
                        saveCache(nodeA, nodeB, leftNew)
                    } else {
                        // new node needs to be created
                        val triple = Triple(leftNew, rightNew, b.v(nodeB))
                        var existing = triples[triple]
                        if (existing == null) {
                            pushWork3(leftNew, rightNew, b.v(nodeB))
                            existing = lastWork()
                            triples[triple] = existing
                        }
                        saveCache(nodeA, nodeB, existing)
                    }
                    pop(); pop()
                } else {
                    // the pair is not done, push the missing values:
                    if (rightNew == -1) {
                        push(nodeA); push(rightB)
                    }
                    if (leftNew == -1) {
                        push(nodeA); push(leftB)
                    }
                }
            }
        } while (stackNotEmpty)

        clearCache()

        return when {
            isZero -> {
                clearWork()
                zero
            }
            lastWork() == 1 -> {
                clearWork()
                one
            }
            else -> exportWork()
        }
    }


    // Utility methods for BDD inspection
    private fun BDD.v(node: Int): Int = this[node]
    private fun BDD.left(node: Int): Int = this[node - 2]
    private fun BDD.right(node: Int): Int = this[node - 1]
    private fun BDD.root(): Int = this.lastIndex

    private fun BDD.isTerminal(): Boolean = this.size < 2
    private fun Int.isTerminal(): Boolean = this < 2
    private fun BDD.isZero(): Boolean = this.size == 1
    private fun BDD.isOne(): Boolean = this.size == 2

    /**
     * Stack is used by various algorithms when traversing the BDD.
     * It has to dynamically grow as needed, so do not add to it directly
     * but used helper methods instead
     */
    private var stack = IntArray(1024)
    private var stackTop = -1   // pointer to the current top of the stack

    private val stackNotEmpty: Boolean
        get() = stackTop >= 0

    private fun push(item: Int) {
        if (stackTop + 1 == stack.size) {
            // realloc stack
            val newStack = IntArray(stack.size * 2)
            System.arraycopy(stack, 0, newStack, 0, stack.size)
            stack = newStack
        }
        stackTop += 1
        stack[stackTop] = item
    }

    private fun peek(): Int = stack[stackTop]
    private fun peek1(): Int = peek()
    private fun peek2(): Int = stack[stackTop - 1]

    private fun pop() {
        stackTop -= 1
    }

    /**
     * Work array is essentially another stack but it is a bit more versatile as it can be exported
     * and cleared (we don't assume it is cleared by popping)
     */
    private var workArray = IntArray(1024)
    private var workEnd = -1 // pointer to the last element of the work array

    private fun pushWork(value: Int) {
        if (workEnd + 1 == workArray.size) {
            // realloc work array
            val newWork = IntArray(workArray.size * 2)
            System.arraycopy(workArray, 0, newWork, 0, workArray.size)
            workArray = newWork
        }
        workEnd += 1
        workArray[workEnd] = value
    }

    private fun pushWork3(a: Int, b: Int, c: Int) {
        pushWork(a); pushWork(b); pushWork(c)
    }

    private fun lastWork(): Int = workEnd

    private fun exportWork(): IntArray {
        val array = IntArray(workEnd + 1)
        System.arraycopy(workArray, 0, array, 0, workEnd + 1)
        Arrays.fill(workArray, 0)
        workEnd = -1
        return array
    }

    private fun clearWork() {
        workEnd = -1
    }

    /**
     * Node cache is used when merging two BDDs to reduce duplicate trees.
     *
     * We use long as keys to avoid extra allocation (we will have to alloc int objects anyway :/)
     * TODO: Find some map that can work with native types
     */
    private val nodeCache = HashMap<Long, Int>()

    private fun saveCache(a: Int, b: Int, value: Int) {
        nodeCache[a.toLong().shl(31) + b] = value
    }

    private fun getCache(a: Int, b: Int): Int? {
        return nodeCache[a.toLong().shl(31) + b]
    }

    private fun clearCache() {
        nodeCache.clear()
    }

}