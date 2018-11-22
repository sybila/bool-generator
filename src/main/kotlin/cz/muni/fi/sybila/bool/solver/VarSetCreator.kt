package cz.muni.fi.sybila.bool.solver

import jdd.bdd.NodeTable
import jdd.util.Allocator

/**
 *
 * @author Jakub Polacek
 */

class VarSetCreator {
    companion object {


        lateinit var set_chars: CharArray
        var set_chars_len: Int = 0

        lateinit var nt: NodeTable
        lateinit var result: ArrayList<ArrayList<Char>>

        fun getVarSets(bdd: Int, max: Int, nt: NodeTable): ArrayList<ArrayList<Char>> {


            set_chars = Allocator.allocateCharArray(max + 5)

            return if (bdd < 2) {
                ArrayList() //return if (bdd == 0) "FALSE" else "TRUE"
            } else {

                set_chars_len = max
                this.nt = nt

                result = ArrayList<ArrayList<Char>>()
                createSets(bdd, 0, ArrayList())

                result
            }
        }


        private fun createSets(bdd: Int, level: Int, resultRow: ArrayList<Char>) {

            if (level == set_chars_len) {
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
    }
}