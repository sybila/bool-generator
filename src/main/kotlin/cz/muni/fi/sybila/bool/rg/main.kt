package cz.muni.fi.sybila.bool.rg

import cz.muni.fi.sybila.bool.rg.BooleanNetwork.Effect.*
import cz.muni.fi.sybila.bool.rg.map.PairMap

fun main() {

    /*val network = BooleanNetwork(
            species = listOf("A", "B"),
            regulations = listOf(
                    BooleanNetwork.Regulation(0, 0, true, INHIBITION),
                    BooleanNetwork.Regulation(1, 0, true, INHIBITION),
                    BooleanNetwork.Regulation(1, 1, false, ACTIVATION)
            )
    )*/
    val network = Network.FissionYeast2008

    val states = BooleanStateEncoder(network)
    val solver = BDDSolver(network)
    println("Solver ready!")
    val graph = ColouredGraph(network, solver)

    val start = System.currentTimeMillis()
    solver.run {
        graph.findComponents { component ->
            println("Component: ${component.size}")
            /*component.forEach { (s, p) ->
                println("State ${states.decode(s).toList()} for ${p.cardinality()} valuations")
            }*/
        }
    }

    val elapsed = (((System.currentTimeMillis() - start) / 1000.0) * 100).toInt() / 100.0
    println("Elapsed: ${elapsed}s")
    println("Ops: ${solver.BDDops}")

    println("PairMap: ${PairMap.tableResize} ${PairMap.newBlock} ${PairMap.resizeBlock}")
    pool.shutdownNow()

}