package cz.muni.fi.sybila.bool.rg

@Suppress("LocalVariableName")
object Network {

    /**
     * Small model with no parameter constraints for testing
     */
    val exampleUnspecified = network {
        val p53 = specie("p53")
        val dna = specie("DNA")
        val m2c = specie("M2C")
        val m2n = specie("M2N")

        p53 regulates dna
        p53 regulates m2c
        p53 regulates m2n

        m2c regulates m2n

        m2n regulates p53

        dna regulates dna
        dna regulates m2n
    }

    val paper = network {
        // order of species is super duper important because it matches the order in tables in the paper
        // so the BDDs are actually compatible...
        val p53 = specie("p53")
        val dna = specie("DNA")
        val m2c = specie("M2C")
        val m2n = specie("M2N")

        p53 inhibits dna
        p53 activates m2c
        p53 inhibits m2n

        m2c activates m2n

        m2n inhibits p53

        dna maybeActivates dna
        dna inhibits m2n
    }

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

        ERBB1 given listOf(EGF)

        ERBB2 given listOf(EGF)

        ERBB3 given listOf(EGF)

        ERBB1_2 given listOf(ERBB1, ERBB2)
        ERBB1_3 given listOf(ERBB1, ERBB3)
        ERBB2_3 given listOf(ERBB2, ERBB3)

        IGF1R given listOf(ERalpha)
        IGF1R given listOf(AKT1)
        IGF1R given listOf(ERalpha, AKT1)

        AKT1 given listOf(IGF1R)
        AKT1 given listOf(IGF1R, ERBB2_3)
        AKT1 given listOf(ERBB1)
        AKT1 given listOf(ERBB1_2)
        AKT1 given listOf(ERBB1_3)
        AKT1 given listOf(IGF1R, ERBB2_3)
        AKT1 given listOf(IGF1R, ERBB1)
        AKT1 given listOf(IGF1R, ERBB1_2)
        AKT1 given listOf(IGF1R, ERBB1_3)
        AKT1 given listOf(ERBB2_3, ERBB1)
        AKT1 given listOf(ERBB2_3, ERBB1_2)
        AKT1 given listOf(ERBB2_3, ERBB1_3)
        AKT1 given listOf(ERBB1, ERBB1_2)
        AKT1 given listOf(ERBB1, ERBB1_3)
        AKT1 given listOf(ERBB1_2, ERBB1_3)
        AKT1 given listOf(IGF1R, ERBB2_3, ERBB1)
        AKT1 given listOf(IGF1R, ERBB2_3, ERBB1_3)
        AKT1 given listOf(IGF1R, ERBB2_3, ERBB1_2)
        AKT1 given listOf(ERBB2_3, ERBB1_2, ERBB1_3)
        AKT1 given listOf(ERBB1, ERBB1_2, ERBB1_3)
        AKT1 given listOf(ERBB1, ERBB2_3, ERBB1_2)
        AKT1 given listOf(IGF1R, ERBB1, ERBB1_3)
        AKT1 given listOf(IGF1R, ERBB1, ERBB1_2)
        AKT1 given listOf(IGF1R, ERBB1_2, ERBB1_3)
        AKT1 given listOf(ERBB1, ERBB2_3, ERBB1_3)
        AKT1 given listOf(IGF1R, ERBB1, ERBB2_3, ERBB1_2)
        AKT1 given listOf(ERBB2_3, ERBB1, ERBB1_2, ERBB1_3)
        AKT1 given listOf(IGF1R, ERBB1, ERBB1_2, ERBB1_3)
        AKT1 given listOf(IGF1R, ERBB2_3, ERBB1, ERBB1_3)
        AKT1 given listOf(IGF1R, ERBB2_3, ERBB1_2, ERBB1_3)
        AKT1 given listOf(IGF1R, ERBB2_3, ERBB1, ERBB1_2, ERBB1_3)

        MEK1 given listOf(IGF1R)
        MEK1 given listOf(ERBB2_3)
        MEK1 given listOf(ERBB1)
        MEK1 given listOf(ERBB1_2)
        MEK1 given listOf(ERBB1_3)
        MEK1 given listOf(IGF1R, ERBB2_3)
        MEK1 given listOf(IGF1R, ERBB1)
        MEK1 given listOf(IGF1R, ERBB1_2)
        MEK1 given listOf(IGF1R, ERBB1_3)
        MEK1 given listOf(ERBB2_3, ERBB1)
        MEK1 given listOf(ERBB2_3, ERBB1_2)
        MEK1 given listOf(ERBB2_3, ERBB1_3)
        MEK1 given listOf(ERBB1, ERBB1_2)
        MEK1 given listOf(ERBB1, ERBB1_3)
        MEK1 given listOf(ERBB1_2, ERBB1_3)
        MEK1 given listOf(IGF1R, ERBB2_3, ERBB1)
        MEK1 given listOf(ERBB2_3, ERBB1, ERBB1_2)
        MEK1 given listOf(ERBB1, ERBB1_2, ERBB1_3)
        MEK1 given listOf(IGF1R, ERBB1, ERBB1_3)
        MEK1 given listOf(IGF1R, ERBB1_2, ERBB1_3)
        MEK1 given listOf(IGF1R, ERBB1, ERBB1_2)
        MEK1 given listOf(ERBB2_3, ERBB1_2, ERBB1_3)
        MEK1 given listOf(ERBB2_3, ERBB1, ERBB1_3)
        MEK1 given listOf(IGF1R, ERBB2_3, ERBB1_3)
        MEK1 given listOf(IGF1R, ERBB2_3, ERBB1_2)
        MEK1 given listOf(IGF1R, ERBB1, ERBB2_3, ERBB1_2)
        MEK1 given listOf(ERBB1, ERBB2_3, ERBB1_2, ERBB1_3)
        MEK1 given listOf(IGF1R, ERBB1, ERBB1_2, ERBB1_3)
        MEK1 given listOf(IGF1R, ERBB2_3, ERBB1_2, ERBB1_3)
        MEK1 given listOf(IGF1R, ERBB2_3, ERBB1, ERBB1_3)
        MEK1 given listOf(IGF1R, ERBB2_3, ERBB1, ERBB1_2, ERBB1_3)

        MYC given listOf(MEK1)
        MYC given listOf(AKT1)
        MYC given listOf(ERalpha)
        MYC given listOf(MEK1, AKT1)
        MYC given listOf(AKT1, ERalpha)
        MYC given listOf(MEK1, ERalpha)
        MYC given listOf(MEK1, AKT1, ERalpha)

        ERalpha given listOf(MEK1)
        ERalpha given listOf(AKT1)
        ERalpha given listOf(MEK1, AKT1)

        CyclinD1 given listOf(ERalpha, MYC, MEK1)
        CyclinD1 given listOf(ERalpha, MYC, AKT1)
        CyclinD1 given listOf(ERalpha, MYC, MEK1, AKT1)

        P27 given listOf(ERalpha)

        P21 given listOf(ERalpha)

        CyclinE1 given listOf(MYC)

        CDK4 given listOf(CyclinD1)
        CDK6 given listOf(CyclinD1)
        CDK2 given listOf(CyclinE1)

        pRB1 given listOf(CDK4, CDK6, CDK2)
        pRB1 given listOf(CDK4, CDK6)
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

        CycE given listOf(E2F)
        CycE given listOf(E2F, Ago)
        CycE given listOf(E2F, Dap)

        Dap given listOf(CycE)
        Dap given listOf(Notch, CycE)

        Rb given listOf(Rux)
        Rb given listOf(Rux, CycA)
        Rb given listOf(Rux, CycB)
        Rb given listOf(Rux, CycA, CycB)

        CycA given listOf(E2F)

        E2F given listOf(Rux)
        E2F given listOf(Rux, CycA)
        E2F given listOf(Rux, CycB)
        E2F given listOf(Rux, CycA, CycB)

        Rux given listOf(Rux)
        Rux given listOf(Rux, CycA)
        Rux given listOf(Rux, CycB)
        //Rux given listOf(CycE)
        //Rux given listOf(Rux, CycE)
        //Rux given listOf(Rux, CycA, CycE)
        //Rux given listOf(Rux, CycB, CycE)
        //Rux given listOf(CycD)
        //Rux given listOf(Rux, CycD)
        //Rux given listOf(Rux, CycA, CycD)
        //Rux given listOf(Rux, CycB, CycD)

        Wee given listOf(Rux)
        Wee given listOf(Rux, CycB)

        Fzy given listOf(CycB)

        Stg given listOf(E2F)
        Stg given listOf(E2F, Rux)
        Stg given listOf(E2F, CycB)
        Stg given listOf(E2F, CycB, Rux)
        Stg given listOf(CycB)
        Stg given listOf(CycB, Rb)
        Stg given listOf(E2F, CycB, Rb)

        CycB given listOf(Stg)
        CycB given listOf(Wee, Stg)

        Fzr given listOf(Rux)
        Fzr given listOf(Rux, CycA)
        Fzr given listOf(Rux, CycB)
        Fzr given listOf(Rux, CycA, CycB)
        Fzr given listOf(Notch)
        Fzr given listOf(Notch, Rux)
        Fzr given listOf(Notch, Rux, CycA)
        Fzr given listOf(Notch, Rux, CycB)
        Fzr given listOf(Notch, Rux, CycA, CycB)
        Fzr given listOf(Notch, CycE)
        Fzr given listOf(Notch, CycE, Rux)
        Fzr given listOf(Notch, CycE, Rux, CycA)
        Fzr given listOf(Notch, CycE, Rux, CycB)
        Fzr given listOf(Notch, CycE, CycB, CycA, Rux)
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

        CycE given listOf(E2F)
        CycE given listOf(E2F, CycA)
        CycE given listOf(E2F, CycE)
        CycE given listOf(E2F, CycE, CycA)
        CycE given listOf(E2F, P27)
        CycE given listOf(CycA, E2F, P27)
        CycE given listOf(CycE, CycA, E2F, P27)
        CycE given listOf(CycE, P27, E2F)

        P27 given listOf(P27)
        P27 given listOf(P27, CycE)
        P27 given listOf(P27, CycA)

        E2F given listOf(CycA, P27)
        E2F given listOf(P27)

        Rb given listOf(CycA, P27)
        Rb given listOf(CycE, P27)
        Rb given listOf(CycA, CycE, P27)
        Rb given listOf(P27)

        Cdc20 given listOf(CycB)
        Cdc20 given listOf(CycB, Cdh1)

        UbcH10 given listOf(CycA)
        UbcH10 given listOf(CycB)
        UbcH10 given listOf(Cdc20)
        UbcH10 given listOf(UbcH10)
        UbcH10 given listOf(CycA, CycB)
        UbcH10 given listOf(CycB, Cdc20)
        UbcH10 given listOf(CycA, Cdc20)
        UbcH10 given listOf(CycA, CycB, Cdc20)
        UbcH10 given listOf(UbcH10, Cdc20)
        UbcH10 given listOf(CycA, CycB, UbcH10)
        UbcH10 given listOf(CycB, UbcH10, Cdc20)
        UbcH10 given listOf(CycA, UbcH10, Cdc20)
        UbcH10 given listOf(CycA, CycB, UbcH10, Cdc20)
        UbcH10 given listOf(CycA, Cdh1, UbcH10)
        UbcH10 given listOf(CycB, Cdh1, UbcH10)
        UbcH10 given listOf(CycA, CycB, Cdh1, UbcH10)
        UbcH10 given listOf(CycB, Cdh1, UbcH10, Cdc20)
        UbcH10 given listOf(CycA, Cdh1, UbcH10, Cdc20)
        UbcH10 given listOf(CycA, CycB, Cdh1, UbcH10, Cdc20)

        Cdh1 given listOf(Cdc20)
        Cdh1 given listOf(Cdc20, CycB)
        Cdh1 given listOf(Cdc20, CycA)
        Cdh1 given listOf(Cdc20, CycA, CycB)
        Cdh1 given listOf(Cdc20, P27)
        Cdh1 given listOf(CycB, Cdc20, P27)
        Cdh1 given listOf(CycA, Cdc20, P27)
        Cdh1 given listOf(CycA, CycB, Cdc20, P27)
        Cdh1 given listOf(CycA, P27)
        Cdh1 given listOf(P27)

        CycA given listOf(E2F)
        CycA given listOf(E2F, CycA)
        CycA given listOf(E2F, Cdh1)
        CycA given listOf(CycA, E2F, Cdh1)
        CycA given listOf(E2F, UbcH10)
        CycA given listOf(CycA, UbcH10)
        CycA given listOf(CycA, E2F, UbcH10)
        CycA given listOf(CycA)
        CycA given listOf(CycA, Cdh1)
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