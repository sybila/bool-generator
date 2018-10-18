package cz.muni.fi.sybila.bool.solver

import com.github.sybila.checker.Solver
import java.nio.ByteBuffer

data class BDD(val ref: Int)

class BDDSolver(
        varCount: Int
) : Solver<BDD> {

    override val ff: BDD
        get() = TODO("not implemented")
    override val tt: BDD
        get() = TODO("not implemented")

    override fun BDD.and(other: BDD): BDD {
        TODO("not implemented")
    }

    override fun BDD.isSat(): Boolean {
        TODO("not implemented")
    }

    override fun BDD.minimize() {
        TODO("not implemented")
    }

    override fun BDD.not(): BDD {
        TODO("not implemented")
    }

    override fun BDD.or(other: BDD): BDD {
        TODO("not implemented")
    }

    override fun BDD.prettyPrint(): String {
        TODO("not implemented")
    }

    /**
     * Returns BDD which is satisfied exactly when variable [varIndex] is one, regardless of other variables.
     */
    fun one(varIndex: Int): BDD {
        TODO("not implemented")
    }

    /**
     * Returns BDD which is satisfied exactly when variable [varIndex] is zero, regardless of other variables.
     */
    fun zero(varIndex: Int): BDD {
        TODO("not implemented")
    }

    /* Serialisation */

    override fun BDD.transferTo(solver: Solver<BDD>): BDD {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun ByteBuffer.putColors(colors: BDD): ByteBuffer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun BDD.byteSize(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun ByteBuffer.getColors(): BDD {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

fun main(args: Array<String>) {
    val solver = BDDSolver(10)

    solver.run {
        val a = tt or ff
        val b = a and tt
    }

}