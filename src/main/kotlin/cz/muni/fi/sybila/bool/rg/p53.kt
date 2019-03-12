package cz.muni.fi.sybila.bool.rg

typealias SCC = Set<Int>

data class State(
        val p53: Int,
        val Mdm2cyt: Int,
        val DNAdam: Int,
        val Mdm2nuc: Int
) {

    override fun toString(): String = "[$p53, $DNAdam, $Mdm2cyt, $Mdm2nuc]"
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

    fun Set<Int>.next(): Set<Int> = this.flatMap {
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
                    State(p53 = p53, Mdm2cyt = Mdm2cyt, DNAdam =  DNAdam, Mdm2nuc = Mdm2nuc)
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
    }.toList().map { P53UpdateFunction(it) }

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
    }.toList().map { DNAdamUpdateFunction(it) }

    // domain [0,1,2], one [0,1,2] regulation - size of table = 3
    val Mdm2cytParametrisations = buildParametrisations(2, 3).filter { p ->
        // Logical table
        // P53
        // 0    p0
        // 1    p1
        // 2    p2
        val p53Interval = p[0] == p[1]
        val p53observability = p[0] != p[2]
        val p53activation = p[2] >= p[0]
        p53Interval && p53observability && p53activation
    }.toList().map { Mdm2cytUpdateFunction(it) }

    // domain [0,1], two [0,1,2] regulations, one [0,1] regulation - size of table = 18
    val Mdm2nucParametrisations = buildParametrisations(1, 18).filter { p ->
        // Logical table
        // Mdm2cyt  p53 DNA
        //[1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1]
        // 0        0   0   p0
        // 1        0   0   p1
        // 2        0   0   p2
        // 0        1   0   p3
        // 1        1   0   p4
        // 2        1   0   p5
        // 0        2   0   p6
        // 1        2   0   p7
        // 2        2   0   p8
        // 0        0   1   p9
        // 1        0   1   p10
        // 2        0   1   p11
        // 0        1   1   p12
        // 1        1   1   p13
        // 2        1   1   p14
        // 0        2   1   p15
        // 1        2   1   p16
        // 2        2   1   p17
        // 1,2 are equivalent
        val p53Interval = p[3] == p[6] && p[4] == p[7] && p[5] == p[8] && p[12] == p[15] && p[13] == p[16] && p[14] == p[17]
        val p53Observability = p[0] != p[3] || p[1] != p[4] || p[2] != p[5] || p[9] != p[12] || p[10] != p[13] || p[11] != p[14]
        val p53inhibition = p[0] >= p[3] && p[1] >= p[4] && p[2] >= p[5] && p[9] >= p[12] && p[10] >= p[13] && p[11] >= p[14]
        val DNAObservability = p[0] != p[9] || p[1] != p[10] || p[2] != p[11] || p[3] != p[12] || p[4] != p[13] || p[5] != p[14] || p[6] != p[15] || p[7] != p[16] || p[8] != p[17]
        val DNAInhibition = p[0] >= p[9] && p[1] >= p[10] && p[2] >= p[11] && p[3] >= p[12] && p[4] >= p[13] && p[5] >= p[14] && p[6] >= p[15] && p[7] >= p[16] && p[8] >= p[17]
        val M2CObservability1 = p[0] != p[1] || p[3] != p[4] || p[6] != p[7] || p[9] != p[10] || p[12] != p[13] || p[15] != p[16]
        val M2CObservability2 = p[2] != p[1] || p[5] != p[4] || p[8] != p[7] || p[11] != p[10] || p[14] != p[13] || p[17] != p[16]
        val M2CActivation = p[1] >= p[0] && p[2] >= p[1] && p[4] >= p[3] && p[5] >= p[4] && p[7] >= p[6] && p[8] >= p[7] && p[10] >= p[9] && p[11] >= p[10] && p[13] >= p[12] && p[14] >= p[13] && p[16] >= p[15] && p[17] >= p[16]
        p53Interval && p53Observability && p53inhibition && DNAObservability && DNAInhibition && M2CObservability1 && M2CObservability2 && M2CActivation
    }.toList().map { Mdm2nucUpdateFunction(it) }

    println("p53 num parametrisations: ${p53Parametrisations.size}")
    println("DNA num parametrisations: ${DNAParametrisations.size}")
    println("Mdm2cyt num parametrisations: ${Mdm2cytParametrisations.size}")
    println("Mdm2nuc num parametrisations: ${Mdm2nucParametrisations.size}")

    val indices = stateSpace.mapIndexed { index, state -> state to index }.toMap()

    fun Int.transition(update: UpdateFunction): Pair<Int, Int> {
        val from = stateSpace[this]
        val to = update.transform(from)
        return this to indices.getValue(to)
    }

    val fenotypes = mutableSetOf<Set<SCC>>()

    /*p53Parametrisations.forEach { p53 ->
        DNAParametrisations.forEach { DNA ->
            Mdm2cytParametrisations.forEach { Mdm2cyt ->
                //Mdm2nucParametrisations.forEach { Mdm2nuc ->
                val Mdm2nuc = Mdm2nucUpdateFunction(listOf(1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1))
                    val edges = mutableListOf<Pair<Int, Int>>()
                    for (i in stateSpace.indices) {
                        edges += i.transition(p53)
                        edges += i.transition(DNA)
                        edges += i.transition(Mdm2cyt)
                        edges += i.transition(Mdm2nuc)
                    }

                    val graph = Graph(stateSpace.size, edges.toSet())

                    val tscc = graph.tscc(stateSpace.indices.toSet())
                    val sizes = tscc.map { it.size }
                    fenotypes.add(tscc)
                    println(sizes)
                    println(tscc.map { graph.probablyCycle(it) })
                    /*if (sizes == listOf(1)) {
                        println("Size: $sizes")
                        println("p53: ${p53.parametrisation}")
                        println("DNA: ${DNA.parametrisation}")
                        println("Mdm2cyt: ${Mdm2cyt.parametrisation}")
                        println("Mdm2nuc: ${Mdm2nuc.parametrisation}")
                        /*val scc = tscc.find { it.size == 27 }!!
                        println(scc.map { stateSpace[it] })
                        scc.forEach {
                            println("${stateSpace[it]} -> ${graph.run { setOf(it).next() }.map { stateSpace[it] }}")
                        }*/
                    }*/
                //}
            }
        }
    }*/

    val p53 = P53UpdateFunction(listOf(2,1))
    val DNA = DNAdamUpdateFunction(listOf(1,1,0,1,1,1))
    val Mdm2cyt = Mdm2cytUpdateFunction(listOf(1, 1, 2))
    val Mdm2nuc = Mdm2nucUpdateFunction(listOf(1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1))

    val edges = mutableSetOf<Pair<Int, Int>>()
    for (i in stateSpace.indices) {
        edges += i.transition(p53)
        edges += i.transition(DNA)
        edges += i.transition(Mdm2cyt)
        edges += i.transition(Mdm2nuc)
    }

    /*edges.forEach {
        println("${stateSpace[it.first]} -> ${stateSpace[it.second]}")
    }*/

    val graph = Graph(stateSpace.size, edges)

    val tscc = graph.tscc(stateSpace.indices.toSet())

    val sink = State(p53 = 1, DNAdam = 0, Mdm2nuc = 0, Mdm2cyt = 0)
    println(DNA.run { sink.index() })
    println(DNA.invoke(sink))
    println(tscc.map { scc -> scc.map { stateSpace[it] } })

    fenotypes.forEach {
        println(it.map { scc -> scc.map { stateSpace[it] } })
    }
}

fun Graph.probablyCycle(scc: SCC): Boolean {
    return scc.all { s ->
        (setOf(s).next() - s).size <= 1
    }
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
        val parametrisation: Parametrisation
) : (State) -> TargetValue {
    override fun invoke(p1: State): TargetValue = parametrisation[p1.index()]
    abstract fun State.index(): Int
    abstract fun transform(state: State): State
}

// implementations of update functions based on the regulatory network

class Mdm2nucUpdateFunction(parametrisation: Parametrisation) : UpdateFunction(parametrisation) {
    override fun State.index(): Int = Mdm2cyt + (3 * p53) + (9 * DNAdam)
    override fun transform(state: State): State = state.copy(Mdm2nuc = state.Mdm2nuc.update(invoke(state)))
}

class DNAdamUpdateFunction(parametrisation: Parametrisation) : UpdateFunction(parametrisation) {
    override fun State.index(): Int = p53 + (3 * DNAdam)
    override fun transform(state: State): State = state.copy(DNAdam = state.DNAdam.update(invoke(state)))
}

class P53UpdateFunction(parametrisation: Parametrisation) : UpdateFunction(parametrisation) {
    override fun State.index(): Int = Mdm2nuc
    override fun transform(state: State): State = state.copy(p53 = state.p53.update(invoke(state)))
}

class Mdm2cytUpdateFunction(parametrisation: Parametrisation) : UpdateFunction(parametrisation) {
    override fun State.index(): Int = p53
    override fun transform(state: State): State = state.copy(Mdm2cyt = state.Mdm2cyt.update(invoke(state)))
}