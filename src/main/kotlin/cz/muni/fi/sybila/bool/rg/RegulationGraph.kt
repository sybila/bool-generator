package cz.muni.fi.sybila.bool.rg

/**
 * Regulatory graph contains a list of entities interacting with each other.
 */
data class RegulationGraph(
        val entities: List<String>,
        val regulations: List<Regulation>
) {

    /**
     * Regulation gives all necessary info about one regulation:
     *  - who regulates (regulator)
     *  - what is regulated (target)
     *  - whether the regulation is observable:
     *      exists context such that enabling the regulation changes the outcome
     *  - type of influence (unknown = null):
     *      positive - exists context which leads to positive value
     *      negative - exists context which leads to negative value
     */
    data class Regulation(
            val regulator: String,
            val target: String,
            val observable: Boolean,
            val influence: Influence?
    )

    enum class Influence {
        POSITIVE, NEGATIVE
    }
}