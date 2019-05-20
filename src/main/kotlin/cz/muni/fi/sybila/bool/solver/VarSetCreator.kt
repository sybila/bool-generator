package cz.muni.fi.sybila.bool.solver

import jdd.bdd.NodeTable
import jdd.util.Allocator

/**
 *
 * @author Jakub Polacek
 */

object VarSetCreator {
    lateinit var chars: CharArray
    var charsLength: Int = 0

    lateinit var nt: NodeTable
    lateinit var result: ArrayList<ArrayList<Char>>

    fun getVarSets(bdd: Int, max: Int, nt: NodeTable, pure: Boolean = false): ArrayList<ArrayList<Char>> {

        chars = Allocator.allocateCharArray(max)

        return if (bdd < 2) {
            ArrayList() //return if (bdd == 0) "FALSE" else "TRUE"
        } else {

            charsLength = max
            this.nt = nt

            result = ArrayList()
            if (pure) {
                createSetsPure(bdd, 0, ArrayList())
            } else {
                createSets(bdd, 0, ArrayList())
            }

            result
        }
    }


    fun getVarSetsPure(bdd: Int, max: Int, nt: NodeTable): ArrayList<ArrayList<Char>> {

        chars = Allocator.allocateCharArray(max)

        return if (bdd < 2) {
            ArrayList() //return if (bdd == 0) "FALSE" else "TRUE"
        } else {

            charsLength = max
            this.nt = nt

            result = ArrayList()
            createSetsPure(bdd, 0, ArrayList())

            result
        }
    }


    private fun createSets(bdd: Int, level: Int, resultRow: ArrayList<Char>) {

        if (level == charsLength) {
            result.add(resultRow)
            return
        }


        if (nt.getVar(bdd) > level || bdd == 1) {
            resultRow.add('-')
            createSets(bdd, level + 1, resultRow)
            return
        }

        val low = nt.getLow(bdd)
        val high = nt.getHigh(bdd)

        if (low != 0) {
            val resultRowPlusZero = ArrayList<Char>()
            resultRowPlusZero.addAll(resultRow)
            resultRowPlusZero.add('0')
            createSets(low, level + 1, resultRowPlusZero)
        }

        if (high != 0) {
            val resultRowPlusOne = ArrayList<Char>()
            resultRowPlusOne.addAll(resultRow)
            resultRowPlusOne.add('1')
            createSets(high, level + 1, resultRowPlusOne)
        }
    }

    /**
     * Pure version does not put - in strings, but creates 2 substrings with 1 and 0 instead
     */
    private fun createSetsPure(bdd: Int, level: Int, resultRow: ArrayList<Char>) {

        if (level == charsLength) {
            result.add(resultRow)
            return
        }

        val low = nt.getLow(bdd)
        val high = nt.getHigh(bdd)

        if (low != 0) {
            val resultRowPlusZero = ArrayList<Char>()
            resultRowPlusZero.addAll(resultRow)
            resultRowPlusZero.add('0')
            createSetsPure(low, level + 1, resultRowPlusZero)
        }

        if (high != 0) {
            val resultRowPlusOne = ArrayList<Char>()
            resultRowPlusOne.addAll(resultRow)
            resultRowPlusOne.add('1')
            createSetsPure(high, level + 1, resultRowPlusOne)
        }
    }
}
