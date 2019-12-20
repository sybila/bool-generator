package cz.muni.fi.sybila.bool.rg

fun main() {

    /*val network = BooleanNetwork(
            species = listOf("A", "B"),
            regulations = listOf(
                    BooleanNetwork.Regulation(0, 0, true, INHIBITION),
                    BooleanNetwork.Regulation(1, 0, true, INHIBITION),
                    BooleanNetwork.Regulation(1, 1, false, ACTIVATION)
            )
    )*/
    val network = Network.smallTest

    println("Species: ${network.species}")

    val states = BooleanStateEncoder(network)
    val params = BooleanParamEncoder(network)
    val solver = BDDSolver(network)
    println("Solver ready!")
    val graph = ColouredGraph(network, solver)

//    println("Regulators of M2N by M2C: ${params.regulationPairs(2, 3)}")
//    println("Regulators of M2N by DNA: ${params.regulationPairs(1, 3)}")
//    println("Regulators of M2N by P53: ${params.regulationPairs(0, 3)}")

    val classifier = Classifier(solver, states)
    val start = System.currentTimeMillis()
    var count = 0
    var card = 0
    solver.run {
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
            //classifier.push(component)
            /*component.forEach { (s, p) ->
                println("State ${states.decode(s).toList()} for ${p.cardinality()} valuations")
            }*/
        }
    }

    println("Count: $count Card: $card")

    val elapsed = (((System.currentTimeMillis() - start) / 1000.0) * 100).toInt() / 100.0
    println("Elapsed: ${elapsed}s")
    //println("Ops: ${solver.bddOps}")

    //classifier.print()
    /*val tree = DecisionTree(params.parameterCount, params.strictRegulationParamSets(), classifier.export(), solver)
    println("Tree size: ${tree.learn()}")
    solver.universe.printDot(solver.unit, "unit.dot")
    val classes = classifier.export()
    for (cls in classes.keys) {
        println("Class: $cls")
        val set = classes[cls]!!
        solver.universe.printDot(set.s, "${cls.joinToString()}.dot")
        //println("Class $cls, tree: ${DecisionTree(params.parameterCount, params.strictRegulationParamSets(), classes.joinToClass(cls, solver), solver).learn()}")
    }*/
    pool.shutdownNow()

}