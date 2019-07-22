package cz.muni.fi.sybila.bool

import weka.classifiers.trees.J48
import weka.core.Instances
import java.io.File
import weka.core.converters.CSVLoader



fun main() {
    /*val csv = File("/Users/daemontus/Downloads/data.csv").readLines().drop(1).map { it.split(',') }
    val params = "v3,v6,v9,v10,v11,v14,v15,v16".split(',')
    for (p in params.indices) {
        for (q in (p+1) until params.size) {
            print("${params[p]} = ${params[q]},")
        }
    }
    println()

    for (line in csv) {
        for (p in 0 until 8) {
            print("${line[p]},")
        }
        for (p in 0 until 8) {
            for (q in (p+1) until 8) {
                print("${if (line[p] == line[q]) "1" else "0"},")
            }
        }
        print(line.last())
        println()
    }*/
    wekaMain()
    /*for (v in vectors(8)) {
        // M2C observability
        if (v[0] == v[4] && v[1] == v[5] && v[2] == v[6] && v[3] == v[7]) continue
        // M2C activation
        if (v[4] < v[0] || v[5] < v[1] || v[6] < v[2] || v[7] < v[3]) continue
        // DNA observability
        if (v[0] == v[2] && v[1] == v[3] && v[4] == v[6] && v[5] == v[7]) continue
        // DNA inhibition
        if (v[0] < v[2] || v[1] < v[3] || v[4] < v[6] || v[5] < v[7]) continue
        // P53 observability
        if (v[0] == v[1] && v[2] == v[3] && v[4] == v[5] && v[6] == v[7]) continue
        // P53 inhibition
        if (v[0] < v[1] || v[2] < v[3] || v[4] < v[5] || v[6] < v[7]) continue
        //println("Vector 8: $v")

        for (v2 in vectors(4)) {
            // DNA activation
            if (v2[2] < v2[0] || v2[3] < v2[1]) continue
            // P53 observability
            if (v2[0] == v2[1] && v2[2] == v2[3]) continue
            // P53 inhibition
            if (v2[0] < v2[1] || v2[2] < v2[3]) continue
            println((v2 + v).joinToString())
        }
    }*/
}

fun vectors(dim: Int): List<List<Int>> {
    if (dim == 0) return listOf(emptyList())
    return vectors(dim - 1).flatMap {
        listOf(it + 0, it + 1)
    }
}

fun wekaMain() {
    val classifier = J48()
    classifier.confidenceFactor = 1.0f
    classifier.minNumObj = 1
    classifier.unpruned = true

    val loader = CSVLoader()
    loader.setSource(File("/Users/daemontus/Downloads/data_with_eq.csv"))
    val data = loader.dataSet
    data.setClassIndex(data.numAttributes() - 1)
    classifier.buildClassifier(data)

    println("Classifier: ${classifier.graph()}")
}