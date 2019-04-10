package cz.muni.fi.sybila.bool.rg

import cz.muni.fi.sybila.bool.rg.BooleanNetwork.Effect.*
import cz.muni.fi.sybila.bool.rg.map.PairMap
import kotlin.system.exitProcess

fun main() {

    /*val network = BooleanNetwork(
            species = listOf("A", "B"),
            regulations = listOf(
                    BooleanNetwork.Regulation(0, 0, true, INHIBITION),
                    BooleanNetwork.Regulation(1, 0, true, INHIBITION),
                    BooleanNetwork.Regulation(1, 1, false, ACTIVATION)
            )
    )*/
    val network = model

    val states = BooleanStateEncoder(network)
    val params = BooleanParamEncoder(network)
    val solver = BDDSolver(network)
    println("Solver ready!")
    val graph = ColouredGraph(network, solver)

    val classifier = Classifier(solver, states)
    val start = System.currentTimeMillis()
    solver.run {
        graph.findComponents { component ->
            println("Component: ${component.size}")
            classifier.push(component)
            /*component.forEach { (s, p) ->
                println("State ${states.decode(s).toList()} for ${p.cardinality()} valuations")
            }*/
        }
    }

    val elapsed = (((System.currentTimeMillis() - start) / 1000.0) * 100).toInt() / 100.0
    println("Elapsed: ${elapsed}s")
    println("Ops: ${solver.BDDops}")

    classifier.print()
    val tree = DecisionTree(params.parameterCount, params.strictRegulationParamSets(), classifier.export(), solver)
    println("Tree size: ${tree.learn()}")
    /*val classes = classifier.export()
    for (cls in classes.keys) {
        println("Class $cls, tree: ${DecisionTree(params.parameterCount, params.strictRegulationParamSets(), classes.joinToClass(cls, solver), solver).learn()}")
    }*/
    pool.shutdownNow()

}