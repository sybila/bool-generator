package cz.muni.fi.sybila.bool.solver

import com.github.sybila.checker.Solver
import java.nio.ByteBuffer

data class BDD(val ref: Int)

class BDDSolver(
        varCount: Int
) : Solver<BDD> {


    private val bdd = jdd.bdd.BDD(1000, 1000)
    private val vars = ArrayList<Int>()

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
        bdd.print(this.ref)
        println("Set")
        bdd.printSet(this.ref)
        println("Cube")
        bdd.printCubes(this.ref)
        return ""
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

        val d = b.and(one(0))
        val e = d.and(zero(1))
        val f = e.and(one(2))
        val g = f.and(zero(3))

        /*
        d.prettyPrint()
        e.prettyPrint()
        f.prettyPrint()
        g.prettyPrint()
        */
        val unsat = one(1).and(zero(1))
        //unsat.prettyPrint()

        val moreOptionsA = one(1).and(zero(3)).and(one(5))
        val moreOptionsB = zero(1).and(one(2)).and(one(4))
        val moreOptions = moreOptionsA.or(moreOptionsB)
        moreOptions.prettyPrint()


        //println(unsat.isSat())

        print(moreOptions.isSat())
    }

}