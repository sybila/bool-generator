package cz.muni.fi.sybila.bool.lattice

import cz.muni.fi.sybila.bool.rg.Network
import cz.muni.fi.sybila.bool.rg.pool

fun main(args: Array<String>) {

    val model = Network.paper

    val solver = LatticeSolver(model)
    val graph = LatticeGraph(model, solver)

    val start = System.currentTimeMillis()
    var count = 0
    var card = 0
    graph.findComponents { component ->
        println("Component: ${component.size}")
        count += 1
        card += component.size
    }

    println("Count: $count Card: $card")

    val elapsed = (((System.currentTimeMillis() - start) / 1000.0) * 100).toInt() / 100.0
    println("Elapsed: ${elapsed}s")

    pool.shutdownNow()
}