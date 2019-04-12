package cz.muni.fi.sybila.bool.rg.map

import cz.muni.fi.sybila.bool.rg.BDDSet
import cz.muni.fi.sybila.bool.rg.BDDSolver
import cz.muni.fi.sybila.bool.rg.BooleanStateEncoder
import cz.muni.fi.sybila.bool.rg.State

class TreeStateMap(
        encoder: BooleanStateEncoder,
        private val solver: BDDSolver
) {

    // number of bits stored in a leaf node
    private val leafWidth = (encoder.dimensions + 1) / 2
    // number of bits stored in the tree structure
    private val treeDepth = encoder.dimensions - leafWidth
    private var tree: Tree? = null

    fun get(state: State): BDDSet {
        var tree = this.tree
        var key = state
        while (tree is Tree.Node) {
            tree = if (key % 2 == 0) tree.left else tree.right
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


    fun increase(state: State, value: BDDSet): Boolean {
        var key = state
        val leaf: Tree.Leaf = if (treeDepth == 0) {    // if 0, just make a leaf
            if (tree == null) tree = Tree.Leaf(arrayOfNulls(1.shl(leafWidth)))
            tree as Tree.Leaf
        } else {
            if (tree == null) tree = Tree.Node()
            var root: Tree.Node = tree as Tree.Node
            var depth = 1
            while (depth < treeDepth) {
                root = if (key % 2 == 0) {
                    if (root.left == null) root.left = Tree.Node()
                    root.left as Tree.Node
                } else {
                    if (root.right == null) root.right = Tree.Node()
                    root.right as Tree.Node
                }
                key = key.shr(1)
                depth += 1
            }
            if (key % 2 == 0) {
                if (root.left == null) root.left = Tree.Leaf(arrayOfNulls(1.shl(leafWidth)))
                root.left as Tree.Leaf
            } else {
                if (root.right == null) root.right = Tree.Leaf(arrayOfNulls(1.shl(leafWidth)))
                root.right as Tree.Leaf
            }
        }
        solver.run {
            val current = leaf.data[key]
            return if (current == null) {
                leaf.data[key] = value
                true
            } else if ((value and current.not()).isNotEmpty()) {
                leaf.data[key] = current or value
                true
            } else {
                false
            }
        }
    }

    private sealed class Tree {
        class Leaf(val data: Array<BDDSet?>) : Tree()
        class Node : Tree() {
            var left: Tree? = null
            var right: Tree? = null
        }
    }

}