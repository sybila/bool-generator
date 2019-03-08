package cz.muni.fi.sybila.bool.rg

typealias SCC = Set<Int>

data class State(
        val p53: Int,
        val Mdm2cyt: Int,
        val DNAdam: Int,
        val Mdm2nuc: Int
) {

    override fun toString(): String = "[$p53, $Mdm2cyt, $DNAdam, $Mdm2nuc]"
}

/**
 * State is an integer, edges are pairs of indices into the vertex list.
 */
data class Graph(
        val stateCount: Int,
        val edges: Set<Pair<Int, Int>>
) {

    private val successors: Map<Int, List<Int>> = edges.groupBy({ it.first }, { it.second })
    private val predecessors: Map<Int, List<Int>> = edges.groupBy({ it.second }, { it.first })

    private fun Set<Int>.reachForward(): Set<Int> {
        val result = this.toMutableSet()
        var new = result.next() - result
        while (new.isNotEmpty()) {
            result += new
            new = result.next() - result
        }
        return result
    }

    private fun Set<Int>.reachBackward(): Set<Int> {
        val result = this.toMutableSet()
        var new = result.prev() - result
        while (new.isNotEmpty()) {
            result += new
            new = result.prev() - result
        }
        return result
    }

    private fun Set<Int>.next(): Set<Int> = this.flatMap {
        successors[it] ?: emptyList()
    }.toSet()

    private fun Set<Int>.prev(): Set<Int> = this.flatMap {
        predecessors[it] ?: emptyList()
    }.toSet()

    fun tscc(universe: Set<Int>): Set<SCC> {
        if (universe.isEmpty()) return emptySet()
        val pivot = universe.iterator().next()
        val reachableComponents = setOf(pivot).reachForward()
        val thisComponent = setOf(pivot).reachBackward() intersect reachableComponents
        val basinOfReachableComponents = thisComponent.reachBackward()

        val reachableBeyondThisComponent = reachableComponents - thisComponent
        val result = mutableSetOf<SCC>()
        result += if (reachableBeyondThisComponent.isEmpty()) {
            // B is a terminal component
            listOf(thisComponent)
        } else {
            // B is not a terminal component, run recursively
            tscc(reachableBeyondThisComponent)
        }

        val unreachableComponents = universe - basinOfReachableComponents
        if (unreachableComponents.isNotEmpty()) {
            result += tscc(unreachableComponents)
        }

        return result
    }

}

fun main() {
    val stateSpace: List<State> = (0..2).flatMap { p53 ->
        (0..2).flatMap { Mdm2cyt ->
            (0..1).flatMap { DNAdam ->
                (0..1).map {  Mdm2nuc ->
                    State(p53, Mdm2cyt, DNAdam, Mdm2nuc)
                }
            }
        }
    }

    // domain [0,1,2], one boolean regulation, size of table = 2
    val p53Parametrisations = buildParametrisations(2, 2).filter { p ->
        // apply static constraints
        // Logical table
        // 0: p0
        // 1: p1
        // Constrains:
        val observable = p[0] != p[1]
        val inhibition = p[1] <= p[0]
        observable && inhibition
    }.toList()

    // domain [0,1], one [0,1] regulation, one [0,1,2] regulation - size of table = 6
    val DNAParametrisations = buildParametrisations(1, 6).filter { p ->
        // Logical table
        // DNA p53
        // 0    0   p0
        // 0    1   p1
        // 0    2   p2
        // 1    0   p3
        // 1    1   p4
        // 1    2   p5
        val p53Interval = p[0] == p[1] && p[3] == p[4]
        val p53observability = p[0] != p[2] || p[3] != p[5] // assuming interval holds...
        val p53inhibition = p[2] <= p[0] && p[5] <= p[3]
        val DNAobservability = p[0] != p[3] || p[1] != p[4] || p[2] != p[5]
        val DNAactivation = p[3] >= p[0] && p[4] >= p[1] && p[5] >= p[2]
        p53Interval && p53observability && p53inhibition && DNAobservability && DNAactivation
    }.toList()

    println("p53 num parametrisations: ${p53Parametrisations.size}")
    println("DNA num parametrisations: ${DNAParametrisations.size}")
/*
    val indices = stateSpace.mapIndexed { index, state -> state to index }.toMap()

    fun Int.transition(update: State.() -> State): Pair<Int, Int> {
        val from = stateSpace[this]
        val to = from.update()
        return this to indices.getValue(to)
    }

    val edges = mutableListOf<Pair<Int, Int>>()
    for (i in stateSpace.indices) {
        edges += i.transition(State::updatep53)
        edges += i.transition(State::updateMdm2cyt)
        edges += i.transition(State::updateMdm2nuc)
        edges += i.transition(State::updateDNAdam)
    }

    val graph = Graph(stateSpace.size, edges.toSet())

    println(graph.tscc(stateSpace.indices.toSet()).map { scc -> scc.map { stateSpace[it] } })*/
}

fun Int.update(target: Int): Int = when {
    this < target -> this + 1
    this > target -> this - 1
    else -> this
}

typealias TargetValue = Int
typealias Parametrisation = List<Int>

/**
 * Create a sequence of all possible combinations of [numParams] parameters over a certain domain
 * (given by its [max] value).
 *
 * i.e. for domain [0,1,2] and number of parameters 2, the parametrisations are [0,0], [1,0], [2,0], [0,1], [1,1],
 * [2,1], [0,2], [1,2], [2,2]
 */
fun buildParametrisations(max: Int, numParams: Int): Sequence<Parametrisation> = sequence {
    val parametrisation = (1..numParams).map { 0 }.toMutableList()
    // standard incremental counter, but in max-based radix
    while (true) {
        yield(parametrisation.toList()) // make copy
        var i = 0
        while (i < numParams && parametrisation[i] == max) {
            parametrisation[i] = 0
            i += 1
        }
        if (i == numParams) break   // overflow - we are done!
        parametrisation[i] += 1
    }
}

/**
 * Update function takes a state and computes the target value which should be used when computing next state.
 *
 * Update function assumes a parametrisation vector which specifies the results of the function for specific
 * inputs. The parametrisations are sorted lexicographically, so for a ternary function where arguments
 * have domains [2,1,2], we get:
 * 0,0,0 p0
 * 1,0,0 p1
 * 2,0,0 p2
 * 0,1,0 p3
 * 1,1,0 p4
 * 2,1,0 p5
 * 0,0,1 p6
 * 1,0,1 p7
 * 2,0,1 p8
 * 0,1,1 p9
 * 1,1,1 p10
 * 2,1,1 p11
 * 0,0,2 p12
 * 1,0,2 p13
 * 2,0,2 p14
 * 0,1,2 p15
 * 1,1,2 p16
 * 2,1,2 p17
 */
abstract class UpdateFunction(
        private val parametrisation: Parametrisation
) : (State) -> TargetValue {
    override fun invoke(p1: State): TargetValue = parametrisation[p1.index()]
    abstract fun State.index(): Int
}

// implementations of update functions based on the regulatory network

class Mdm2nucUpdateFunction(parametrisation: Parametrisation) : UpdateFunction(parametrisation) {
    override fun State.index(): Int = Mdm2cyt + (3 * p53) + (9 * DNAdam)
}

class DNAdamUpdateFunction(parametrisation: Parametrisation) : UpdateFunction(parametrisation) {
    override fun State.index(): Int = DNAdam + (2 * p53)
}

class P53UpdateFunction(parametrisation: Parametrisation) : UpdateFunction(parametrisation) {
    override fun State.index(): Int = Mdm2nuc
}

class Mdm2cytUpdateFunction(parametrisation: Parametrisation) : UpdateFunction(parametrisation) {
    override fun State.index(): Int = p53
}