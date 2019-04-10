package cz.muni.fi.sybila.bool.rg

@Suppress("LocalVariableName")
object Network {

    // #P = 54, |P| = 2.07e5, |S| = 512
    val BuddingYeast2008 = network {
        val CLN3 = specie("CLN3")
        val MBF = specie("MBF")
        val SBF = specie("SBF")
        val YOX1 = specie("YOX1")
        val HCM1 = specie("HCM1")
        val YHP1 = specie("YHP1")
        val SSF = specie("SSF")
        val ACE2 = specie("ACE2")
        val SWI5 = specie("SWI5")

        SSF activates SWI5

        SSF activates ACE2

        SBF activates SSF
        HCM1 activates SSF

        MBF activates YHP1
        SBF activates YHP1

        MBF activates HCM1
        SBF activates HCM1

        MBF activates YOX1
        SBF activates YOX1

        CLN3 activates SBF
        MBF activates SBF
        YHP1 inhibits SBF
        YOX1 inhibits SBF

        CLN3 activates MBF

        ACE2 activates CLN3
        YHP1 inhibits CLN3
        SWI5 activates CLN3
        YOX1 inhibits CLN3
    }

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

    // #P = 127, |P| = 4.77e14
    val FissionYeast2008 = network {
        val start = specie("start")
        val SK = specie("SK")
        val Ste9 = specie("Ste9")
        val Rum1 = specie("Rum1")
        val Cdc2 = specie("Cdc2")
        val Cdc25 = specie("Cdc25")
        val PP = specie("PP")
        val Slp1 = specie("Slp1")
        val Wee1 = specie("Wee1")
        val Cdc2A = specie("Cdc2A")

        start activates SK

        Cdc2 inhibits Ste9
        PP activates Ste9
        SK inhibits Ste9
        Ste9 activates Ste9
        Cdc2A inhibits Ste9

        Cdc2 inhibits Rum1
        PP activates Rum1
        SK inhibits Rum1
        Rum1 activates Rum1
        Cdc2A inhibits Rum1

        Ste9 inhibits Cdc2
        Rum1 inhibits Cdc2
        Slp1 inhibits Cdc2

        Cdc2 activates Cdc25
        Cdc25 activates Cdc25
        PP inhibits Cdc25

        Slp1 activates PP

        Cdc2A activates Slp1

        Cdc2 inhibits Wee1
        PP activates Wee1
        Wee1 activates Wee1

        Cdc25 activates Cdc2A
        Wee1 inhibits Cdc2A
        Ste9 inhibits Cdc2A
        Rum1 inhibits Cdc2A
        Slp1 inhibits Cdc2A

        SK given listOf(start)

        Ste9 given listOf(PP)
        //redundant Ste9 given listOf(PP, Ste9)
        Ste9 given listOf(Ste9)
        Ste9 given listOf(PP, SK, Ste9)
        Ste9 given listOf(PP, Cdc2, Ste9)
        Ste9 given listOf(PP, Cdc2A, Ste9)

        //Rum1 given listOf(PP)
        Rum1 given listOf(Rum1)
        //redundant Rum1 given listOf(PP, Rum1)
        Rum1 given listOf(PP, SK, Rum1)
        Rum1 given listOf(PP, Cdc2, Rum1)
        Rum1 given listOf(PP, Cdc2A, Rum1)

        PP given listOf(Slp1)

        Slp1 given listOf(Cdc2A)

        Wee1 given listOf(PP)
        Wee1 given listOf(Wee1)
        //redundant Wee1 given listOf(PP, Wee1)
        Wee1 given listOf(PP, Cdc2, Wee1)

        Cdc2A given listOf(Cdc25)

        //Cdc25 given listOf(Cdc2)
        Cdc25 given listOf(Cdc25)
        //redundant Cdc25 given listOf(Cdc2, Cdc25)
        Cdc25 given listOf(Cdc2, Cdc25, PP)
    }

    // #P = 195, |P| = 8.04e21
    val ErbB2 = network {
        val EGF = specie("EGF")
        val ERBB1 = specie("ERBB1")
        val ERBB2 = specie("ERBB2")
        val ERBB3 = specie("ERBB3")
        val ERBB1_2 = specie("ERBB1_2")
        val ERBB1_3 = specie("ERBB1_3")
        val ERBB2_3 = specie("ERBB2_3")
        val IGF1R = specie("IGF1R")
        val AKT1 = specie("AKT1")
        val MEK1 = specie("MEK1")
        val ERalpha = specie("ERalpha")
        val MYC = specie("MYC")
        val CyclinD1 = specie("CyclinD1")
        val P27 = specie("P27")
        val P21 = specie("P21")
        val CyclinE1 = specie("CyclinE1")
        val CDK4 = specie("CDK4")
        val CDK6 = specie("CDK6")
        val CDK2 = specie("CDK2")
        val pRB1 = specie("pRB1")

        CDK2 activates pRB1
        CDK4 activates pRB1
        CDK6 activates pRB1

        P27 inhibits CDK4
        P21 inhibits CDK4
        CyclinD1 activates CDK4

        CyclinD1 activates CDK6

        CyclinE1 activates CDK2
        P27 inhibits CDK2
        P21 inhibits CDK2

        MEK1 activates CyclinD1
        ERalpha activates CyclinD1
        MYC activates CyclinD1
        AKT1 activates CyclinD1

        CDK4 inhibits P27
        ERalpha activates P27
        MYC inhibits P27
        CDK2 inhibits P27
        AKT1 inhibits P27

        CDK4 inhibits P21
        ERalpha activates P21
        MYC inhibits P21
        AKT1 inhibits P21

        MYC activates CyclinE1

        MEK1 activates ERalpha
        AKT1 activates ERalpha

        MEK1 activates MYC
        ERalpha activates MYC
        AKT1 activates MYC

        ERBB2_3 activates AKT1
        ERBB1 activates AKT1
        IGF1R activates AKT1
        ERBB1_3 activates AKT1
        ERBB1_2 activates AKT1

        ERBB2_3 activates MEK1
        ERBB1 activates MEK1
        IGF1R activates MEK1
        ERBB1_3 activates MEK1
        ERBB1_2 activates MEK1

        ERBB2_3 inhibits IGF1R
        ERalpha activates IGF1R
        AKT1 activates IGF1R

        ERBB2 activates ERBB2_3
        ERBB3 activates ERBB2_3

        ERBB1 activates ERBB1_3
        ERBB3 activates ERBB1_3

        ERBB1 activates ERBB1_2
        ERBB2 activates ERBB1_2

        EGF activates ERBB1
        EGF activates ERBB2
        EGF activates ERBB3
    }

    // #P = 207, |P| = 2.44e25
    val DrosophilaCellCycle = network {
        val Ago = specie("Ago")
        val CycE = specie("CycE")
        val Dap = specie("Dap")
        val CycD = specie("CycD")
        val Rb = specie("Rb")
        val CycA = specie("CycA")
        val Notch = specie("Notch")
        val E2F = specie("E2F")
        val Rux = specie("Rux")
        val CycB = specie("CycB")
        val Stg = specie("Stg")
        val Wee = specie("Wee")
        val Fzr = specie("Fzr")
        val Fzy = specie("Fzy")

        Rb inhibits CycE
        E2F activates CycE
        Dap inhibits CycE
        Ago inhibits CycE

        CycE activates Dap
        Notch inhibits Dap

        CycD inhibits Rb
        Rux activates Rb
        CycA inhibits Rb
        CycE inhibits Rb
        CycB inhibits Rb

        Rb inhibits CycA
        Fzy inhibits CycA
        E2F activates CycA
        Fzr inhibits CycA

        Rb inhibits E2F
        Rux activates E2F
        CycA inhibits E2F
        CycB inhibits E2F

        Rux activates Rux
        CycA inhibits Rux
        CycB inhibits Rux
        CycD inhibits Rux
        CycE inhibits Rux

        Wee inhibits CycB
        Fzy inhibits CycB
        Fzr inhibits CycB
        Stg activates CycB

        Rb inhibits Stg
        Rux inhibits Stg
        E2F activates Stg
        Notch inhibits Stg
        CycB activates Stg

        Rux activates Wee
        CycB inhibits Wee

        Rux activates Fzr
        CycA inhibits Fzr
        CycE inhibits Fzr
        Notch activates Fzr
        CycB inhibits Fzr

        Rux inhibits Fzy
        CycB activates Fzy

    }

    // #P = 233, |P| = ???
    val CellCycle = network {
        val CycD = specie("CycD")
        val CycE = specie("CycE")
        val P27 = specie("P27")
        val CycA = specie("CycA")
        val E2F = specie("E2F")
        val Rb = specie("Rb")
        val Cdc20 = specie("Cdc20")
        val UbcH10 = specie("UbcH10")
        val Cdh1 = specie("cdh1")
        val CycB = specie("CycB")

        CycA inhibits CycE
        CycE inhibits CycE
        E2F activates CycE
        Rb inhibits CycE
        P27 activates CycE

        CycA inhibits P27
        CycD inhibits P27
        CycE inhibits P27
        CycB inhibits P27
        P27 activates P27

        CycA activates CycA
        Cdc20 inhibits CycA
        E2F activates CycA
        Rb inhibits CycA
        UbcH10 inhibits CycA
        Cdh1 inhibits CycA

        CycA inhibits E2F
        CycB inhibits E2F
        Rb inhibits E2F
        P27 activates E2F

        CycA inhibits Rb
        CycD inhibits Rb
        CycE inhibits Rb
        CycB inhibits Rb
        P27 activates Rb

        CycB activates Cdc20
        Cdh1 inhibits Cdc20

        CycA inhibits Cdh1
        Cdc20 activates Cdh1
        CycB inhibits Cdh1
        P27 activates Cdh1

        CycA activates UbcH10
        Cdc20 activates UbcH10
        CycB activates UbcH10
        UbcH10 activates UbcH10
        Cdh1 inhibits UbcH10

        Cdc20 inhibits CycB
        Cdh1 inhibits CycB
    }

    // Too small

    // 9 species
    // 0s / P = 8
    /*val G2B = network {
        val PleC = specie("PleC")
        val DivJ = specie("DivJ")
        val DivK = specie("DivK")
        val DivL = specie("DivL")
        val CckA = specie("CckA")
        val ChpT = specie("ChpT")
        val CtrAb = specie("CtrAb")
        val CpdR = specie("CpdR")
        val ClpXP = specie("ClpXP")

        DivK inhibits PleC

        DivK activates DivJ
        PleC inhibits DivJ

        PleC inhibits DivK
        DivJ activates DivK

        DivK inhibits DivL

        DivL activates CckA

        CckA activates ChpT

        ChpT activates CpdR

        CpdR inhibits ClpXP

        ChpT activates CtrAb
        ClpXP inhibits CtrAb
    }


    // 6 species
    // 0s / P = 162
    val Zebra = network {
        val miR9 = specie("miR9")
        val Her6 = specie("Her6")
        val Zic5 = specie("Zic5")
        val HuC = specie("HuC")
        val P = specie("P")
        val N = specie("N")

        Her6 inhibits miR9
        N maybeInhibits miR9

        miR9 inhibits Her6
        N maybeInhibits Her6

        miR9 inhibits Zic5
        N maybeInhibits Zic5

        miR9 inhibits HuC
        P maybeInhibits HuC

        Zic5 activates P
        Her6 activates P

        HuC activates N
    }*/

}