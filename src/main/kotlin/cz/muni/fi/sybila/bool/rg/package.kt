package cz.muni.fi.sybila.bool.rg

import cz.muni.fi.sybila.bool.rg.bdd.BDD
import cz.muni.fi.sybila.bool.rg.map.ArrayStateMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

// The bit vector of values in the state
typealias State = Int

// The index of the parameter in the standardized ordering (given by the network)
typealias Parameter = Int

// The index of a variable in the standardized ordering (given by the network)
typealias Dimension = Int

// The index of a specie in the standardized ordering (given by the network)
typealias Specie = Int

typealias StateMap = ArrayStateMap

typealias BDDSet = BDD

inline fun <T> List<T>.mergePairs(merge: (T, T) -> T): List<T> {
    val result = ArrayList<T>(this.size + 1)
    var i = 0
    while (i+1 < size) {
        result.add(merge(this[i], this[i+1]))
        i += 2
    }
    if (size % 2 == 1) {
        result.add(this.last())
    }
    return result
}

val parallelism = Runtime.getRuntime().availableProcessors() / 2
val pool: ExecutorService = Executors.newFixedThreadPool(parallelism)

fun ExecutorService.parallel(action: () -> Unit) {
    (1..parallelism).map {
        pool.submit(action)
    }.map { it.get() }
}