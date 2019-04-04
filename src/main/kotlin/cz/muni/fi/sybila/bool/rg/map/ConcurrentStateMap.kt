package cz.muni.fi.sybila.bool.rg.map

import cz.muni.fi.sybila.bool.rg.BDDSet
import cz.muni.fi.sybila.bool.rg.BDDSolver
import cz.muni.fi.sybila.bool.rg.BooleanStateEncoder
import cz.muni.fi.sybila.bool.rg.State
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * Coloured set is a tree-like structure which maps states of a boolean network to parameter sets.
 *
 * Its efficiency depends highly on the ordering of variables in the boolean network, but when the
 * conditions are right, it can significantly reduce the memory consumption compared to standard HashMap.
 *
 * For a full set, the data structure is essentially an array. For a single value, the tree contains a single node.
 *
 * The set is based on the assumption that values are never removed, only inserted and updated.
 *
 * The tree is essentially an ordered decision tree which decides the value of one variable. Each leaf
 * then contains an array of parameter set pointers and the first state in the array. Hence computing index of
 * state in the array can be done subtracting the first state.
 *
 * Note that this relies on the fact that the state space is continuous, so the decision variables in the tree
 * need to be ordered in the inverse order as when numbering the states. (i.e. when creating states, least
 * significant bit is the first variable. Here, the first decision must be on the last variable)
 *
 * Tree = Nil | Leaf(Base, Array) | Node(left, right)
 *
 * tree.get(state) = tree.get(State, 0)
 * tree.get(state, depth) = when (tree) {
 *    nil -> nil
 *    leaf -> array[state - base]
 *    node -> (if (state[depth]) right else left).get(state, depth + 1)
 * }
 *
 * tree.set(state, value) = tree.set(state, value, 0)
 * tree.set(state, value, depth) = when (tree) {
 *    nil -> Leaf(state, [value])
 *    leaf -> if (state in array) {
 *      Leaf(base, array[state - base] = value)
 *    } else {
 *      if (base[depth] = state[depth]) {
 *          if (base[depth]) {
 *             Node(left = nil, right = this.set(state, value, depth + 1))
 *          } else {
 *             Node(left = this.set(state, value, depth + 1), right = nil)
 *          }
 *      } else {
 *          if (base[depth]) {
 *              Node(left = Leaf(state, [value], right = this)
 *          } else {
 *              Node(left = this, right = Leaf(state, [value])
 *          }
 *      }
 *    }
 *    node -> (if (state[depth]) right else left).set(state, value, depth + 1)
 * }
 *
 * Finally, when two sibling trees are full (number of leaves is 2^(dimensions - depth), they can be merged into
 * one leaf array.
 *
 * The whole thing is concurrent and lock-free, because we hate ourselves!
 */
class ConcurrentStateMap(
        private val encoder: BooleanStateEncoder,
        private val solver: BDDSolver
) {

    // number of bits stored in a leaf node
    private val leafWidth = (encoder.dimensions + 1) / 2
    private val leafLength = 1.shl(leafWidth)
    // number of bits stored in the tree structure
    private val treeDepth = encoder.dimensions - leafWidth
    private val tree: AtomicReference<Tree?> = AtomicReference(null)

    /**
     * Get the current parameter set stored in this map for the given [key].
     *
     * Concurrency: If someone modifies the tree, it can only contain more information, hence we
     * can safely traverse it. Yes, someone might add some stuff concurrently with us and modify the path,
     * but that does not concern us - either we see the union, or it comes "after" our read, but then
     * the next read must see it.
     */
    fun get(state: State): BDDSet {
        var tree = this.tree.get()
        var key = state
        while (tree is Tree.Node) {
            tree = if (key % 2 == 0) tree.left.get() else tree.right.get()
            key = key.shr(1)
        }
        return when (tree) {
            null -> solver.empty
            is Tree.Leaf -> {
                tree.data[key] ?: solver.empty
            }
            is Tree.Node -> error("unreachable")
        }
    }

    /**
     * Union the given [value] with currently stored value for the given [key].
     *
     * Return true if the value changed.
     */
    fun increase(state: State, value: BDDSet): Boolean {
        // first descend to the leaf, creating decision nodes on the way if necessary
        var tree: AtomicReference<Tree?> = this.tree
        var depth = 0
        var key = state
        while (depth < treeDepth) {
            var node = tree.get()
            if (node == null) {
                tree.compareAndSet(null, Tree.Node())
                node = tree.get()
            }
            if (node !is Tree.Node) error("Expected node, got: $node")
            tree = if (key % 2 == 0) node.left else node.right
            depth += 1
            key = key.shr(1)
        }
        // now create a leaf if necessary
        var leaf = tree.get()
        if (leaf == null) {
            tree.compareAndSet(null, Tree.Leaf(AtomicReferenceArray(leafLength)))
            leaf = tree.get()
        }
        if (leaf !is Tree.Leaf) error("Expected leaf, got $leaf")
        // and finally, union the values
        solver.run {
            do {
                val current = leaf.data.get(key)
                val update = if (current != null) (current or value) else value
                if (current != null && (current.not() and update).isEmpty()) {
                    return false
                }
            } while (!leaf.data.compareAndSet(key, current, update))
            return true
        }
    }


    private sealed class Tree {
        class Leaf(val data: AtomicReferenceArray<BDDSet?>) : Tree()
        class Node : Tree() {
            val left: AtomicReference<Tree?> = AtomicReference(null)
            val right: AtomicReference<Tree?> = AtomicReference(null)
        }
    }

}