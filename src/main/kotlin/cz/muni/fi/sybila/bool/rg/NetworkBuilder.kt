package cz.muni.fi.sybila.bool.rg

class NetworkBuilder {

    private val species = ArrayList<String>()
    private val regulations = ArrayList<BooleanNetwork.Regulation>()
    private val explicit = ArrayList<Pair<Int, List<Int>>>()

    fun specie(name: String): String {
        if (name in species) error("Duplicate specie $name")
        species.add(name)
        return name
    }

    infix fun String.maybeActivates(that: String) = activation(this, that, false)
    infix fun String.activates(that: String) = activation(this, that)
    infix fun String.maybeInhibits(that: String) = inhibition(this, that, false)
    infix fun String.inhibits(that: String) = inhibition(this, that)
    infix fun String.regulates(that: String) = anyRegulation(this, that)

    infix fun String.given(regulators: List<String>) {
        // TODO: Safety checks
        val target = species.indexOf(this)
        explicit.add(target to regulators.map { species.indexOf(it) })
    }

    private fun anyRegulation(regulator: String, target: String) {
        val regulatorIndex = species.indexOf(regulator)
        if (regulatorIndex < 0) error("Unknown regulator $regulator")
        val targetIndex = species.indexOf(target)
        if (targetIndex < 0) error("Unknown target $target")
        regulations.add(BooleanNetwork.Regulation(
                regulator = regulatorIndex, target = targetIndex,
                observable = false, effect = BooleanNetwork.Effect.ANY
        ))
    }

    private fun activation(regulator: String, target: String, observable: Boolean = true) {
        val regulatorIndex = species.indexOf(regulator)
        if (regulatorIndex < 0) error("Unknown regulator $regulator")
        val targetIndex = species.indexOf(target)
        if (targetIndex < 0) error("Unknown target $target")
        regulations.add(BooleanNetwork.Regulation(
                regulator = regulatorIndex, target = targetIndex,
                observable = observable, effect = BooleanNetwork.Effect.ACTIVATION
        ))
    }

    private fun inhibition(regulator: String, target: String, observable: Boolean = true) {
        val regulatorIndex = species.indexOf(regulator)
        if (regulatorIndex < 0) error("Unknown regulator $regulator")
        val targetIndex = species.indexOf(target)
        if (targetIndex < 0) error("Unknown target $target")
        regulations.add(BooleanNetwork.Regulation(
                regulator = regulatorIndex, target = targetIndex,
                observable = observable, effect = BooleanNetwork.Effect.INHIBITION
        ))
    }

    fun build() = BooleanNetwork(species, regulations, explicit)

}

fun network(build: NetworkBuilder.() -> Unit): BooleanNetwork = NetworkBuilder().also { it.run(build) }.build()