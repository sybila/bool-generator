package cz.muni.fi.sybila.bool.solver

import com.github.sybila.checker.Solver
import cz.muni.fi.sybila.bool.BooleanSolver
import java.nio.ByteBuffer

data class BDD(val ref: Int)

class BDDSolver(
        varCount: Int
) : BooleanSolver {


    private val bdd = jdd.bdd.BDD(1000, 1000)
    val vars = ArrayList<Int>()

    // bdd.ref() only increases refferences to prevent accidental garbage collection

    init {
        for (i in 0 until varCount) {
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
        // is not possible without changing ordering
    }

    override fun BDD.not(): BDD {
        return BDD(bdd.ref(bdd.not(this.ref)))
    }

    override fun BDD.or(other: BDD): BDD {
        return BDD(bdd.ref(bdd.or(this.ref, other.ref)))
    }

    override fun BDD.prettyPrint(): String {
        return when (ref) {
            1 -> "TRUE"
            0 -> "FALSE"
            else -> {
                VarSetCreator.getVarSets(this.ref, bdd.numberOfVariables(), bdd)
                        .joinToString("\n") { resultSet ->
                            resultSet.joinToString("")
                        }
            }
        }

    }

    override fun BDD.satCount(): Double {
        return bdd.satCount(this.ref)
        /*return when (ref) {
            1 -> 0
            0 -> 0
            else -> {
                VarSetCreator.getVarSets(this.ref, bdd.numberOfVariables(), bdd, true).size
            }
        }*/
    }

    /**
     * Returns BDD which is satisfied exactly when variable [varIndex] is one, regardless of other variables.
     */
    override fun one(varIndex: Int): BDD {
        return BDD(bdd.and(1, vars[varIndex]))
    }

    /**
     * Returns BDD which is satisfied exactly when variable [varIndex] is zero, regardless of other variables.
     */
    override fun zero(varIndex: Int): BDD {
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

        sets.forEach {
            it.forEach { char ->
                when (char) {
                    '0' -> {
                        this.put(0)
                    }
                    '1' -> {
                        this.put(1)
                    }
                    '-' -> {
                        this.put(2)
                    }
                    else -> {
                        throw IllegalStateException("Unknown char in set.")
                    }
                }
            }
            this.put(3)
        }

        return this
    }


    override fun BDD.byteSize(): Int {
        return if (ref > 1) {
            val sets = VarSetCreator.getVarSets(this.ref, bdd.numberOfVariables(), bdd)
            // we need 1 byte for each var and 1 extra for set ending indication, for each set
            return sets.size * (bdd.numberOfVariables() + 1)
        } else {
            1
        }
    }

    private fun ByteBuffer.compareByteAtIndex(index: Int, byte: Int) = this.array()[index] == byte.toByte()

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

        // count vars in one set
        var varsize = 0
        while (this.array()[varsize] != 3.toByte()) {
            varsize += 1
        }

        val varModulo = varsize + 1

        if (bdd.numberOfVariables() > varModulo) {
            throw IllegalArgumentException("Can't create BDD - too many vars")
        }

        var resultBDD: BDD? = null
        var bufferBDD: BDD? = null

        for (index in 0 until this.array().size) {
            when {

                // 0 -> var must be negative
                this.compareByteAtIndex(index, 0) -> {
                    bufferBDD = bufferBDD?.and(zero(index % varModulo)) ?: zero(index % varModulo)
                }
                // 1 -> var must be positive
                this.compareByteAtIndex(index, 1) -> {
                    bufferBDD = bufferBDD?.and(one(index % varModulo)) ?: one(index % varModulo)
                }
                // 2 -> var can be either positive or negative, pass

                // 3 -> end of one set, append as "or" to result and continue further
                this.compareByteAtIndex(index, 3) -> {
                    // println("buffer")
                    // println(bufferBDD?.prettyPrint())
                    if (bufferBDD != null && bufferBDD.ref != 1) {
                        resultBDD = BDD(resultBDD?.or(bufferBDD)?.ref ?: bufferBDD.ref)
                    }
                    bufferBDD = null
                }
            }
        }

        return resultBDD ?: BDD(1)

    }

}

fun main(args: Array<String>) {

    // for testing purposes only

    val test = BDDSolver(1)
    test.run {
        val a = one(0)
        println(a.prettyPrint())
    }

    val solver = BDDSolver(10)

    println("__________")
    solver.run {

        val moreOptionsA = one(1).and(zero(3)).and(one(5))
        val moreOptionsB = zero(1).and(one(2)).and(one(4))
        val moreOptions = moreOptionsB.or(moreOptionsA)

        println(moreOptions.prettyPrint())

        println("___________")

        val buffer = ByteBuffer.allocate(moreOptions.byteSize())
        buffer.putColors(moreOptions)

        val transformedBDD = buffer.getColors()
        println(transformedBDD.prettyPrint())
    }


}