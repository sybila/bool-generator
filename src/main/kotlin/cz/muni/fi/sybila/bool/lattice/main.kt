package cz.muni.fi.sybila.bool.lattice

import cz.muni.fi.sybila.bool.rg.Network
import cz.muni.fi.sybila.bool.rg.pool

fun main(args: Array<String>) {

    val model = Network.smallTest

    val solver = LatticeSolver(model)
    val graph = LatticeGraph(model, solver)

    val start = System.currentTimeMillis()
    var count = 0
    var card = 0
    graph.findComponents { component ->
        println("Component: ${component.size}")
        for (s in 0 until graph.stateCount) {
            solver.run {
                if (component.get(s).isNotEmpty()) {
                    println("Component state: $s -> ${component.get(s).prettyPrint()}")
                }
            }
        }
        count += 1
        card += component.size
    }

    println("Count: $count Card: $card")

    val elapsed = (((System.currentTimeMillis() - start) / 1000.0) * 100).toInt() / 100.0
    println("Elapsed: ${elapsed}s")

    pool.shutdownNow()
}