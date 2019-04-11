package cz.muni.fi.sybila.bool.rg

class Classifier(
        private val solver: BDDSolver, private val states: BooleanStateEncoder
) {

    private val classes = HashMap<List<String>, BSet>()

    init {
        classes[emptyList()] = BSet(solver.unit)
    }

    fun print() {
        solver.run {
            for ((c, p) in classes) {
                println("Class: ${c}, cardinality: ${p.s.cardinality()}, size: ${p.s.nodeSize()}")
            }
        }
    }

    fun push(component: StateMap) {
        solver.run {
            val notSinkParams = (0 until component.capacity).map { it to component.get(it) }.mapParallel { (s, p) ->
                try {
                    var hasSuccessor = empty
                    for (d in 0 until states.dimensions) {
                        hasSuccessor = hasSuccessor or transitionParams(s, d)
                    }
                    val isSink = p and hasSuccessor.not()
                    val isNotSink = p and hasSuccessor
                    if (isSink.isNotEmpty()) {
                        push("sink", isSink)
                    }
                    isNotSink
                } catch (e: Exception) {
                    println("Error $e")
                    e.printStackTrace()
                    throw e
                }
            }.merge { a, b -> a or b }
            /*var notSinkParams = empty
            for (s in 0 until component.capacity) {
                if (component.get(s).isEmpty()) continue
                var hasSuccessor = empty
                for (d in 0 until states.dimensions) {
                    hasSuccessor = hasSuccessor or transitionParams(s, d)
                }
                val isSink = component.get(s) and hasSuccessor.not()
                val isNotSink = component.get(s) and hasSuccessor
                if (isSink.isNotEmpty()) {
                    push("sink", isSink)
                }
                if (isNotSink.isNotEmpty()) {
                    notSinkParams = notSinkParams or isNotSink
                }
            }*/

            // now detect cycles in the union
            val disorder = (0 until component.capacity).toList().mapParallel { s ->
                var oneSuccessor = component.get(s) and notSinkParams
                for (d1 in 0 until states.dimensions) {
                    val succ1 = states.flipValue(s, d1)
                    val d1Successor = component.get(s) and component.get(succ1) and notSinkParams and transitionParams(s, d1)
                    for (d2 in (d1+1) until states.dimensions) {
                        val succ2 = states.flipValue(s, d2)
                        val d2Successor = component.get(s) and component.get(succ2) and notSinkParams and transitionParams(s, d2)
                        val hasBoth = d1Successor and d2Successor
                        if (hasBoth.isNotEmpty()) {
                            oneSuccessor = oneSuccessor and hasBoth.not()
                        }
                    }
                }
                (oneSuccessor.not() and component.get(s) and notSinkParams)
            }.merge { a, b -> a or b }
            /*var disorder = empty
            for (s in 0 until component.capacity) {
                var oneSuccessor = component.get(s) and notSinkParams
                for (d1 in 0 until states.dimensions) {
                    val succ1 = states.flipValue(s, d1)
                    val d1Successor = component.get(s) and component.get(succ1) and notSinkParams and transitionParams(s, d1)
                    for (d2 in (d1+1) until states.dimensions) {
                        val succ2 = states.flipValue(s, d2)
                        val d2Successor = component.get(s) and component.get(succ2) and notSinkParams and transitionParams(s, d2)
                        val hasBoth = d1Successor and d2Successor
                        if (hasBoth.isNotEmpty()) {
                            oneSuccessor = oneSuccessor and hasBoth.not()
                        }
                    }
                }
                disorder = disorder or (oneSuccessor.not() and component.get(s) and notSinkParams)
            }*/

            push("disorder", disorder)
            push("cycle", (notSinkParams and disorder.not()))
        }
    }

    fun push(clazz: String, params: BDDSet) {
        synchronized(classes) {
            solver.run {
                // classes are sorted from largest to smallest so that moving
                // colours cannot be moved twice -> small-larger, larger-largest.
                val originalClasses = classes.keys.toList().sortedByDescending { it.size }
                for (c in originalClasses) {
                    val shouldMoveUp = classes[c]!!.s and params
                    if (shouldMoveUp.isNotEmpty()) {
                        val largerClass = (c + clazz).sorted()
                        classes[c] = BSet(classes[c]!!.s and shouldMoveUp.not())
                        if (classes[c]?.s?.isEmpty() == true) classes.remove(c)
                        classes[largerClass] = BSet((classes[largerClass]?.s ?: empty) or shouldMoveUp)
                    }
                }
            }
        }
    }

    fun export(): Map<List<String>, BSet> = classes.toMap()

}