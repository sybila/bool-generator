package cz.muni.fi.sybila.bool.rg

class OscillationClassifier(
        private val capacity: Int, private val solver: BDDSolver
) {

    private val classes = ArrayList<StateMap>()

    /**
     * Mark [pivots] as root states in this computation. Note that the union of all colours in pivots must
     * be equal to all available colours in the component.
     */
    fun initFrom(pivots: StateMap) {
        classes.add(pivots)
    }

    fun pushWave(wave: StateMap): Pair<BDDSet, BDDSet> = solver.run {
        val waveParams = (0 until capacity).fold(empty) { a, s -> a or wave.get(s) }
        /*
         * First, compute sets of parameters for which the wave intersects each class.
         *
         * If some parameters intersect two classes, these do not oscillate. If some parameters intersect no
         * class, these need to be pushed to a new class.
         */
        var alreadyFound = empty
        var notOscillating = empty
        var newClass = waveParams
        val intersections: List<BDDSet> = classes.map { clazz ->
            var classWaveIntersection = empty
            for (s in 0 until capacity) {
                val p = wave.get(s) and clazz.get(s)
                if (p.isNotEmpty()) {
                    classWaveIntersection = classWaveIntersection or p
                }
            }
            val noOscillation = (alreadyFound and classWaveIntersection) // parameters which already have intersection
            notOscillating = notOscillating or noOscillation
            alreadyFound = alreadyFound or classWaveIntersection
            newClass = newClass and classWaveIntersection.not()         // remove discovered parameters
            classWaveIntersection
        }

        if (newClass.isNotEmpty()) {
            val clazz = StateMap(capacity, solver)
            for (s in 0 until capacity) {
                (wave.get(s) and newClass).takeIf { it.isNotEmpty() }?.let { p ->
                    clazz.union(s, p)
                }

            }
            classes.add(clazz)
        }

        val oscillating = notOscillating.not()

        var continueParams = newClass
        // now union wave based on intersections
        for ((clazz, classIntersection) in intersections.mapIndexed { i, p -> i to p }) {
            for (s in 0 until capacity) {
                val stateParams = wave.get(s) and oscillating and classIntersection
                if (stateParams.isNotEmpty()) {
                    if (classes[clazz].union(s, stateParams)) {
                        continueParams = continueParams or stateParams
                    }
                }
            }
        }

        notOscillating to continueParams
    }

}