package cz.muni.fi.sybila.bool

import com.github.sybila.checker.SequentialChecker
import com.github.sybila.huctl.*
import cz.muni.fi.sybila.bool.rg.network
import kotlin.contracts.contract

fun main(args: Array<String>) {
    // #P = 48, |P| = 4.67e5, |S| = 32
    val G2A = network {
        val CtrA = specie("CtrA")
        val SciP = specie("SciP")
        val CcrM = specie("CcrM")
        val DnaA = specie("DnaA")
        val GcrA = specie("GcrA")

        GcrA activates CtrA
        CtrA activates CtrA
        SciP inhibits CtrA
        CcrM inhibits CtrA

        CtrA activates SciP
        DnaA inhibits SciP

        CtrA activates CcrM
        SciP inhibits CcrM
        CcrM inhibits CcrM

        GcrA inhibits DnaA
        CtrA activates DnaA
        DnaA inhibits DnaA
        CcrM activates DnaA

        CtrA inhibits GcrA
        DnaA activates GcrA
/*
        GcrA given listOf(DnaA)

        CtrA given listOf(CtrA)
        CtrA given listOf(CtrA, GcrA)
        CtrA given listOf(GcrA)

        SciP given listOf(CtrA)

        CcrM given listOf(CtrA)

        DnaA given listOf(CtrA, CcrM)
 */
    }

    val model = G2A.asBooleanModel()
    val generator = BooleanStateSpaceGenerator(model)
    val admissibleParameters = G2A.staticConstraintsBDD(generator)

    val possibleStates = (0..31).toList()


    generator.run {

        println("Admissible Parameters: ${admissibleParameters}")


        // vyfiltruje vsetko okrem self loopov v transitions pre kazdy stav
        // pre kazdy self loop vypise jeho parametre
        // a ci su splnitelne zaroven s admissible parameters
        // vysledkom je, ze pre boundParameters.and(admissibleParameters) assignment je dana vec validny attractor ???

        val count = Count(this)
        val successors = possibleStates.asSequence().map { state ->
            state.successors(true)
                    .asSequence().filter { transition ->
                        (transition.direction == DirectionFormula.Atom.Loop) && (transition.bound and admissibleParameters).isSat()
                    }.onEach {
                        count.push(it.bound)
                        println("$state -> $it, bdd.and(admissibleParameters).ref= ${it.bound.and(admissibleParameters).ref}, " +
                                "\n bdd admissible = ${admissibleParameters.and(it.bound).ref != 0}," +
                                " bdd bez and = ${it.bound.prettyPrint()}")
                        println("")
                    }.toList()
        }.toList()

        for (i in 0 until count.max) {
            println("I have $i attractors for ${(count[i] and admissibleParameters).satCount()}")
        }

        /*val prop = AF(AG("GcrA".asVariable().eq(1.0.asConstant())))
        SequentialChecker(this).use { mc ->
            val result = mc.verify(prop)
            result.entries().forEach { (s, p) ->
                println("Holds in $s for ${p.prettyPrint()}")
            }
        }*/
    }
}