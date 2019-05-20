package cz.muni.fi.sybila.bool

import cz.muni.fi.sybila.bool.rg.network
import cz.muni.fi.sybila.bool.solver.BDDSolver

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
    val solver = BDDSolver(model.parameterCount)


    solver.run {
        val admissibleParameters = G2A.staticConstraintsBDD(this)
    }
}