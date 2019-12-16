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
                println("Class: $c, cardinality: ${p.s.cardinality()}, size: ${p.s.nodeSize()}")
            }
        }
    }

    fun push(component: StateMap) {
        solver.run {
            /*val notSinkParams = (0 until component.capacity).map { it to component.get(it) }.mapParallel { (s, p) ->
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
            }.merge { a, b -> a or b }*/

            var hasNotSink = false
            val componentWithoutSinks = StateMap(states.stateCount, solver)
            for (s in 0 until states.stateCount) {
                val p = component.get(s)
                if (p.isNotEmpty()) {
                    val hasSuccessor = (0 until states.dimensions).fold(empty) { a, d -> a or transitionParams(s, d) }
                    val isSink = p and not(hasSuccessor)
                    val isNotSink = p and hasSuccessor
                    if (isSink.isNotEmpty()) {
                        push("sink", isSink)
                    }
                    if (isNotSink.isNotEmpty()) {
                        hasNotSink = true
                        componentWithoutSinks.union(s, isNotSink)
                    }
                }
            }

            if (hasNotSink) {
                val notSinkParams = (0 until states.stateCount).fold(empty) { a, s -> a or componentWithoutSinks.get(s) }
                val pivots = StateMap(states.stateCount, solver).apply {
                    var remainingParams = notSinkParams
                    while (remainingParams.isNotEmpty()) {
                        val (pivotState, pivotParams) = (0 until states.stateCount)
                                .asSequence()
                                .map { s -> s to (componentWithoutSinks.get(s) and remainingParams) }
                                .first { it.second.isNotEmpty() }
                        this.union(pivotState, pivotParams)
                        remainingParams = remainingParams and not(pivotParams)
                    }
                }

                val cycleClassifier = OscillationClassifier(states.stateCount, solver)
                cycleClassifier.initFrom(pivots)

                var disorder = empty
                var paramsToMatch = notSinkParams
                var currentLevel = pivots
                while (paramsToMatch.isNotEmpty()) {
                    val reachable = StateMap(states.stateCount, solver).apply {
                        for (s in 0 until states.stateCount) {
                            currentLevel.getOrNull(s)?.takeIf { it.isNotEmpty() }?.let { sourceParams ->
                                for (d in 0 until states.dimensions) {
                                    val targetParams = sourceParams and transitionParams(s, d) and paramsToMatch
                                    this.union(states.flipValue(s, d), targetParams)
                                }
                            }
                        }
                    }

                    val (notOscillating, continueWith) = cycleClassifier.pushWave(reachable)
                    disorder = disorder or notOscillating
                    paramsToMatch = paramsToMatch and continueWith
                    currentLevel = reachable
                }

                val oscillates = notSinkParams and not(disorder)

                if (disorder.isNotEmpty()) {
                    push("disorder", disorder)
                }
                if (oscillates.isNotEmpty()) {
                    push("cycle", oscillates)
                }
            }


            // now detect cycles in the union
            /*val disorder = (0 until component.capacity).toList().mapParallel { s ->
                if (component.get(s).isEmpty()) empty else {
                    var oneSuccessor = component.get(s) and notSinkParams
                    val successorParams = (0 until states.dimensions).map { d ->
                        val successor = states.flipValue(s, d)
                        component.get(s) and component.get(successor) and notSinkParams and transitionParams(s, d)
                    }

                    for (d1 in 0 until states.dimensions) {
                        val d1Successor = successorParams[d1]
                        for (d2 in (d1+1) until states.dimensions) {
                            val d2Successor = successorParams[d2]
                            val hasBoth = d1Successor and d2Successor
                            if (hasBoth.isNotEmpty()) {
                                oneSuccessor = oneSuccessor and hasBoth.not()
                            }
                        }
                    }
                    (oneSuccessor.not() and component.get(s) and notSinkParams)
                }
            }.merge { a, b -> a or b }

            push("disorder", disorder)
            push("cycle", (notSinkParams and disorder.not()))*/
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
                        classes[c] = BSet(classes[c]!!.s and not(shouldMoveUp))
                        if (classes[c]?.s?.isEmpty() == true) classes.remove(c)
                        classes[largerClass] = BSet((classes[largerClass]?.s ?: empty) or shouldMoveUp)
                    }
                }
            }
        }
    }

    fun export(): Map<List<String>, BSet> = classes.toMap()

}