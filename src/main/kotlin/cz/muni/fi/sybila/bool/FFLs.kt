package cz.muni.fi.sybila.bool

import com.github.sybila.checker.SequentialChecker
import com.github.sybila.huctl.*

fun main(args: Array<String>) {
    val parser = BooleanModelParser()

    println("Verification of some properties of all FFL types using asynchronous logic and Boolean Models")
    /** model generator here **/
    val types = listOf("C1","I1","I3","C3","I4","C4","C2","I2")
    println("The types are: "+types)
    var count = 0
    data class triple(val first:Boolean,val second:Boolean,val third:Boolean)
    var models: HashMap< triple, Array<Int>> = hashMapOf()
    for( xy in listOf(true,false)){
        for( xz in listOf(true,false)){
            for( yz in listOf(true,false)){
                models[triple(xy, xz, yz)] = arrayOf(0,0,0)
                for( i in listOf("|","&")){
                    var model = parser.readString("""
                        sx <- sx
                        sy <- sy
                        x <- sx
                        y <- ${if (xy) "" else "!"}x&sy
                        z <- ${if (xz) "" else "!"}x$i ${if (yz) "" else "!"}y
                    """.trimIndent())
                    println("======= Run Model ${count+1} -- ${types[count/2]} ${if (i=="&") "AND" else "OR"} =======")
                    count++
                    runExperiments(model)
                }
            }
        }
    }

    println("Verification of some properties of all FFL types using asynchronous logic and Multivalued Models")
    println("The types are: "+types)
    count = 0
    models = hashMapOf()
    for( xy in listOf(true,false)){
        for( xz in listOf(true,false)){
            for( yz in listOf(true,false)){
                models[triple(xy, xz, yz)] = arrayOf(0,0,0)
                for( i in listOf("|","&")){
                    for(xy_thresh in listOf(1,2)){
                        for( xz_thresh in listOf(1,2)){
                            if ((xy_thresh == 2) and (xz_thresh == 2)) continue
                            var model = parser.readString("""
                                sx <- sx
                                sy <- sy
                                x <- sx
                                y <- ${if (xy) "" else "!"}x:${xy_thresh}&sy
                                z <- ${if (xz) "" else "!"}x:${xz_thresh} $i ${if (yz) "" else "!"}y
                            """.trimIndent())
                            //println("$count, $xy, $xz, $yz, $i, $xy_thresh, $xz_thresh")
                            println("======= Run Model ${count+1} -- ${types[count/6]} ${if (i=="&") "-AND" else "-OR"} ${if (xy_thresh==xz_thresh) "xy_thresh = xz_thresh" else if (xy_thresh>xz_thresh) "xy_thresh > xz_thresh" else "xy_thresh < xz_thresh" }  =======")
                            count++
                            runExperiments(model)
                        }
                    }
                }
            }
        }
    }
}

fun BooleanFragment.check(formula: Formula):Boolean {
    SequentialChecker(this).use { checker ->
        val states = checker.verify(formula)
        /** check wheter there is a state in satysfying state space**/
        return (states.entries().hasNext())
    }
}

fun BooleanFragment.checkAndPrint(formula: Formula){
    SequentialChecker(this).use { checker ->
        val states = checker.verify(formula)
        for ((s, _) in states.entries()) {
            println(s.prettyPrint(this.model))
        }
    }
}

fun BooleanFragment.checkAll(formulas: Set<Formula>):Boolean{
    for(formula in formulas){
        if(check(formula)) continue else return false
    }
    return true
}



fun runExperiments(model: BooleanModel) = BooleanFragment(model).run {
            /*val aGreaterOne = "a".asVariable() ge 1.0.asConstant()
            val states = aGreaterOne.eval()*/
            val zero = 0.0.asConstant()
            val one = 1.0.asConstant()
            val allZero = ("x".asVariable() eq zero) and ("y".asVariable() eq zero) and ("z".asVariable() eq zero)

            /** properties are here **/
            println("Verify EF(x = 0 & y = 0 & z = 0)")
            val reachZero = EF(allZero)
            println(check(reachZero))
            //checkAndPrint(reachZero)

            println("Verify delay_on: If(up, AU((z = 0),(y = 1)))")
            val delay_on = ((("sx".asVariable() eq one) and ("sy".asVariable() eq one) and ("x".asVariable() eq zero) and ("y".asVariable() eq zero) and ("z".asVariable() eq zero)) and (("z".asVariable() eq zero)AU("y".asVariable() eq one)))
            println(check(delay_on))
            //checkAndPrint(delay_on)

            println("Verify no_delay_off AF(~(S(z=1,y=1,sx=0,sy=1) & EX(S(y=0))) | EX(S(y=0,z=0)))")
            val no_delay_off = AF(not( ("z".asVariable() eq one) and ("y".asVariable() eq one) and ("sx".asVariable() eq zero) and ("sy".asVariable() eq one)) and EX("y".asVariable() eq zero)) or EX(("y".asVariable() eq zero) and ("z".asVariable() eq zero))
            println(check(no_delay_off))
            //checkAndPrint(no_delay_off)

            println("Verify delay_off: If(down, AU((z = 1),(y = 0)))")
            val delay_off = ( (("sx".asVariable() eq zero) and ("sy".asVariable() eq one) and ("x".asVariable() eq one) and ("y".asVariable() eq one) and ("z".asVariable() eq one)) and (("z".asVariable() eq one)AU("y".asVariable() eq zero)))
            println(check(delay_off))
            //checkAndPrint(delay_off)

            println("Verify no_delay_on AF(~(S(z=0,y=0,sx=1,sy=1) & EX(S(y=1))) | EX(S(y=1,z=1)))")
            val no_delay_on = AF(not( ("z".asVariable() eq zero) and ("y".asVariable() eq zero) and ("sx".asVariable() eq one) and ("sy".asVariable() eq one)) and EX("y".asVariable() eq one)) or EX(("y".asVariable() eq one) and ("z".asVariable() eq one))
            println(check(no_delay_on))
            //checkAndPrint(no_delay_on)

            println("Verify apeak If(up, AF (z_on & AF(AG(z_off))))")
            val apeak =(("sx".asVariable() eq one) and ("sy".asVariable() eq one) and ("x".asVariable() eq zero) and ("y".asVariable() eq zero) and ("z".asVariable() eq zero)) and AF(("z".asVariable() eq one) and AF(AG(("z".asVariable() eq zero))) )
            println(check(apeak))
            //checkAndPrint(apeak)

            /*
            println("Reachability for sx=1, sy=1, x=0, y=1, z=1")
            val init = intArrayOf(1, 1, 0, 1, 1)
            var recompute = setOf<Int>(model.encoder.encodeState(init))
            var discovered = recompute
            while (recompute.isNotEmpty()) {
                println("==== Iteration ====")
                recompute.forEach { println(it.prettyPrint(model)) }
                val step = recompute.flatMap { it.successors(true).asSequence().map { it.target }.toList() }.toSet()
                recompute = step - discovered
                discovered += recompute
            }
            */
        }



