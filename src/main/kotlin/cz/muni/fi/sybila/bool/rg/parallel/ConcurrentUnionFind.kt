package cz.muni.fi.sybila.bool.rg.parallel

import cz.muni.fi.sybila.bool.rg.BDDSet
import cz.muni.fi.sybila.bool.rg.BDDSolver
import java.util.concurrent.atomic.AtomicReference

class ConcurrentUnionFind(capacity: Int, private val solver: BDDSolver) {

    private val parentLists: Array<AtomicReference<ParentListItem>> = Array(capacity) { node ->
        AtomicReference(ParentListItem(node, solver.unit))
    }

    fun union(left: Int, right: Int, params: BDDSet) {
        val parentLeft = find(left, params)
        val parentRight = find(right, params)


    }

    fun find(node: Int, params: BDDSet): List<Pair<Int, BDDSet>> {
        val result = ArrayList<Pair<Int, BDDSet>>()
        findRecursive(node, params, result)
        return result
    }

    // recursively find the set of set roots for the given node and given subset of parameters
    private fun findRecursive(node: Int, params: BDDSet, result: MutableList<Pair<Int, BDDSet>>): Unit = solver.run {
        var parentItem: ParentListItem? = parentLists[node].get()
        while (parentItem != null) {
            if (parentItem.parent == node) {
                // for these params, node is a root of a set
                val rootParams = parentItem.bdd.get() and params
                if (rootParams.isNotEmpty()) {
                    result.insertUnion(parentItem.parent, rootParams)
                }
            } else {
                // for these params, node is not a root but there is a link to follow
                val followParams = parentItem.bdd.get() and params
                if (followParams.isNotEmpty()) {
                    findRecursive(parentItem.parent, followParams, result)
                }
            }

            // TODO: Path compaction

            parentItem = parentItem.next.get()
        }
    }

    // Insert new node into a list of node-colour pairs - DEFINITELY NOT THREAD SAFE!
    private fun MutableList<Pair<Int, BDDSet>>.insertUnion(node: Int, bdd: BDDSet) {
        // find insertion index
        var i = 0
        while (i < size && node < this[i].first) i += 1
        when {
            i == size -> {
                // if insertion is the end of list, create new item at the end
                add(node to bdd)
            }
            node == this[i].first -> {
                // item with this node already exists, we can thus union
                this[i] = node to solver.run { get(i).second or bdd }
            }
            else -> {
                // we have to insert a new item in the middle of the list
                add(i, node to bdd)
            }
        }
    }

    /*
     * An atomic linked list such that the union of all BDDs in the list is always the unit set - i.e. every
     * parametrisation has a parent at all times!
     *
     * Once no work is being performed on the list, it should hold that every parametrisation is present
     * for exactly one parent node. During computation, parametrisation can be present in multiple
     * list items, but it is only "visible" in the smallest such item, i.e. when checking the parent of
     * a parametrisation, the list is traversed and first matching item is returned.
     *
     * This also allows us to move parametrisation from smaller to larger node by first inserting the parametrisation
     * into the larger node (creating node if needed) and then removing it from the smaller node. In case of concurrent
     * modification, TODO
     *
     * It also holds that no parameters can ever "decrease". This means we can safely remove any empty list items
     * at the beginning of the list (we can't do that in the middle because we would have to check the BDD is unchanged
     * when updating the next pointer).
     *
     * Another nice property of "ever increasing" is that the starting item cannot decrease. We can prune empty
     * initial items (see above), but as long as we are increasing, no parametrisation can move "below" the initial
     * item in the list.
     *
     * TODO: optimisation - implicitly, all parameter point to the "root" node? this should enable more removing
     */
    private class ParentListItem(
            // immutable - once created, list item has the same parent and should be unique in the parent list
            val parent: Int,
            // once created, can increase and decrease, but parameters can only move from smaller parents to larger parents
            val bdd: AtomicReference<BDDSet>,
            // standard atomic linked list
            val next: AtomicReference<ParentListItem?> = AtomicReference(null)
    ) {

        constructor(parent: Int, bdd: BDDSet, next: ParentListItem? = null) :
                this(parent, AtomicReference(bdd), AtomicReference(next))

        /**
         * Increase the [moveParams] from their current positions to the [targetNode]
         */
        fun increase(targetNode: Int, moveParams: BDDSet, solver: BDDSolver) {
            val target = ensure(targetNode, solver)  // first, make sure the target node has an item in this list
            increaseRecursive(target, moveParams, solver)   // then recursively process all items
        }

        private fun increaseRecursive(target: ParentListItem, moveParams: BDDSet, solver: BDDSolver) {

        }

        // ensures that the list contains (at least empty) item with the given node as parent
        private fun ensure(node: Int, solver: BDDSolver): ParentListItem {
            if (node < parent) error("Inserting $node into list starting with $parent")
            if (node == parent) return this // node already present
            while (true) {  // loop breaks by moving to another item or creating and setting a new item
                val currentNext = this.next.get()
                val newNext = if (currentNext == null) {
                    // the end of list - make a new node and add it
                    ParentListItem(node, solver.empty)
                } else {
                    if (node >= currentNext.parent) {
                        // node belongs somewhere after the next node - insert it there
                        return currentNext.ensure(node, solver)
                        // We can bypass CAS here, because:
                        // a) Modifications of this.next cannot add anything larger than currentNext.parent,
                        // hence the condition still holds regardless of the actual value of this.next.
                        // b) Modifications to currentNext.next are not relevant here but in the recursive
                        // ensure call.
                    } else {
                        // Node belongs after this, but before next = insert a new item.
                        // In case of modifications to this.next, the value is recomputed.
                        // Modifications to currentNext are irrelevant.
                        ParentListItem(node, solver.empty, currentNext)
                    }
                }
                if (this.next.compareAndSet(currentNext, newNext)) {
                    return newNext
                }
            }
        }

    }

}