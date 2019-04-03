package cz.muni.fi.sybila.bool.rg

/*
    Overview of what we want to do:
    1) BN: Species, regulations, each regulation can be activation/inhibition and observable.
    2) Count the number of parameters (size of logical tables) ordered by species
    3) Make a BDD world for that.
    4) Apply static constraints and get the unit BDD.
        - need to produce "regulation pairs" of specie A: (off, on) where on = off except for A
        (A is 0 in off and 1 in on).
        - A constraint is a BDD built based on the regulation pairs.
 */

class BooleanNetwork(
        val species: List<String>,
        val regulations: List<Regulation>
) {

    init {
        // Check consistency - duplicate species, regulations and invalid indices in regulations.
        regulations.forEach { regulation ->
            if (regulation.regulator !in species.indices) error("Unknown regulator: $regulation")
            if (regulation.target !in species.indices) error("Unknown target: $regulation")
        }
        if (species.size != species.toSet().size) {
            error("Duplicate species: $species")
        }
        if (regulations.size != regulations.map { it.regulator to it.target }.toSet().size) {
            error("Duplicate regulations: $regulations")
        }
    }

    val dimensions: Int
        get() = species.size

    private val indices = species.mapIndexed { i, s -> s to i }.toMap()

    private val contexts = Array(species.size) { target ->
        regulations.filter { it.target == target }
    }

    /**
     * Return the regulations of given [specie].
     */
    fun regulatoryContext(specie: Int): List<Regulation> = contexts[specie]

    fun specieIndex(specie: String): Int = indices.get(specie) ?: error("Unknown specie $specie")

    data class Regulation(
            val regulator: Int, val target: Int,
            val observable: Boolean,
            val effect: Effect?
    )

    enum class Effect {
        ACTIVATION, INHIBITION
    }

}

