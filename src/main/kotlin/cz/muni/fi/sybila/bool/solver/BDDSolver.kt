package cz.muni.fi.sybila.bool.solver

import com.github.sybila.checker.Solver
import cz.muni.fi.sybila.bool.solver.VarSetCreator.Companion.result
import jdd.bdd.NodeTable
import jdd.util.Allocator
import java.nio.ByteBuffer

data class BDD(val ref: Int)

class BDDSolver(
        varCount: Int
) : Solver<BDD> {


    val bdd = jdd.bdd.BDD(1000, 1000)
    val vars = ArrayList<Int>()

    // bdd.ref() only increases refferences to prevent accidental garbage collection
    // TODO: implement dereffing?

    init {
        for (i in 0..varCount) {
            val bddVar = bdd.createVar()
            vars.add(bddVar)
            bdd.ref(bddVar)
        }
    }

    override val ff: BDD
        get() = BDD(bdd.zero)
    override val tt: BDD
        get() = BDD(bdd.one)

    override fun BDD.and(other: BDD): BDD {
        return BDD(bdd.ref(bdd.and(this.ref, other.ref)))
    }

    override fun BDD.isSat(): Boolean {
        return this.ref != 0
    }

    override fun BDD.minimize() {
        TODO("not implemented")
        //bdd.simplify or  bdd.restrict
    }

    override fun BDD.not(): BDD {
        return BDD(bdd.ref(bdd.not(this.ref)))
    }

    override fun BDD.or(other: BDD): BDD {
        return BDD(bdd.ref(bdd.or(this.ref, other.ref)))
    }

    override fun BDD.prettyPrint(): String {

        // remove after done debugging
        // ------------
        /*
        bdd.print(this.ref)
        println("Set")
        bdd.printSet(this.ref)
        println("Cube")
        bdd.printCubes(this.ref)
        */
        // ------------


        when {
            ref == 1 -> return "TRUE"
            ref == 0 -> return "FALSE"
            else -> {
                val sets = VarSetCreator.getVarSets(this.ref, bdd.numberOfVariables(), bdd)
                return sets
                        .map { resultSet ->
                            resultSet.fold(StringBuilder(), { builder, char -> builder.append(char) }).toString() }
                        .fold(StringBuilder(), { builder, row -> builder.append(row).append("\n") }).toString()
            }
        }

    }

    private fun getVarSet() {
    }

    /**
     * Returns BDD which is satisfied exactly when variable [varIndex] is one, regardless of other variables.
     */
    fun one(varIndex: Int): BDD {
        return BDD(bdd.and(1, vars[varIndex]))
    }

    /**
     * Returns BDD which is satisfied exactly when variable [varIndex] is zero, regardless of other variables.
     */
    fun zero(varIndex: Int): BDD {
        return BDD(bdd.and(1, bdd.not(vars[varIndex])))
    }

    /* Serialisation */

    override fun BDD.transferTo(solver: Solver<BDD>): BDD {
        // z tohto solveru do druheho, ak naopak tak treba vymenit

        val buffer = ByteBuffer.allocate(this.byteSize()).putColors(this)
        solver.run {
            return buffer.getColors()
        }
    }

    override fun ByteBuffer.putColors(colors: BDD): ByteBuffer {
        val sets = VarSetCreator.getVarSets(colors.ref, bdd.numberOfVariables(), bdd)

        sets.forEach { set ->
            run {
                set.forEach { char -> when {
                    char == '0' -> {
                        this.put(0)
                        this.put(0)
                    }
                    char == '1' -> {
                        this.put(1)
                        this.put(1)
                    }
                    char == '-' -> {
                        this.put(0)
                        this.put(1)
                    }
                    else -> {
                        throw IllegalStateException("Unknown char in set.")
                    }
                }}
                this.put(1)
                this.put(0)
            }
        }

        return this
    }



    override fun BDD.byteSize(): Int {
        return if (ref > 1) {
            val sets = VarSetCreator.getVarSets(this.ref, bdd.numberOfVariables(), bdd)
            return sets.size * (2 * bdd.numberOfVariables() + 2)
        } else {
            0
        }
    }

    override fun ByteBuffer.getColors(): BDD {
        if (this.array().isEmpty()) {
            throw IllegalStateException("Faulty byte buffer")
        }
        if (this.array().size == 1) {
            return if (this.array()[0] == 0.toByte()) {
                BDD(0)
            } else {
                BDD(1)
            }
        }


        result = ArrayList<ArrayList<Char>>()


        var i = 0

        while ((this.array()[i] == 1.toByte() && this.array()[i+1] == 0.toByte()).not())
        {
            i += 2
        }

        val varsSize = i / 2

        if (bdd.numberOfVariables() > varsSize) {
            throw IllegalArgumentException("Can't create BDD - too many vars")
        }

        i = 0
        var resultBDD: BDD? = null
        var bufferBDD: BDD? = null
        while (i < this.array().size) {
            when {
                this.array()[i] == 1.toByte() && this.array()[i+1] == 1.toByte() -> {
                    bufferBDD = bufferBDD?.and(one((i / 2) % varsSize)) ?: one((i / 2) % (varsSize + 1))
                }
                this.array()[i] == 0.toByte() && this.array()[i+1] == 0.toByte() -> {
                    bufferBDD = bufferBDD?.and(zero((i / 2) % varsSize)) ?: zero((i / 2) % (varsSize + 1))
                }
                this.array()[i] == 1.toByte() && this.array()[i+1] == 0.toByte() -> {
                    println("buffer")
                    println(bufferBDD?.prettyPrint())
                    if (bufferBDD != null && bufferBDD.ref != 1) {
                        resultBDD = BDD(resultBDD?.or(bufferBDD)?.ref ?: bufferBDD.ref)
                    }
                    bufferBDD = null
                }
            }

            i += 2

        }

        return resultBDD ?: BDD(1)

    }

}

fun main(args: Array<String>) {
    val solver = BDDSolver(10)

    solver.run {

        val moreOptionsA = one(1).and(zero(3)).and(one(5))
        val moreOptionsB = zero(1).and(one(2)).and(one(4))
        val moreOptions = moreOptionsB.or(moreOptionsA)

        println(moreOptions.prettyPrint())


        //println(unsat.isSat())


        val buffer = ByteBuffer.allocate(moreOptions.byteSize())
        buffer.putColors(moreOptions).array().forEach { print(it) }

        val transformedBDD = buffer.getColors()
        println(transformedBDD.prettyPrint())
    }


}