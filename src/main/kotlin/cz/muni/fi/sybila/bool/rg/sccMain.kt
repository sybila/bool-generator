package cz.muni.fi.sybila.bool.rg

import kotlin.system.measureTimeMillis

fun main() {
    val network = Network.paper

    println("Species: ${network.species}")

    val solver = BDDSolver(network)
    println("Solver ready!")
    val graph = ColouredGraph(network, solver)

    val elapsed = measureTimeMillis {
        graph.dfs()
    }

    println("Dfs time: $elapsed")

    pool.shutdown()
}