package cz.muni.fi.sybila.bool.rg

@Suppress("LocalVariableName")
object Network {

    // 5s / P = 467856
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
    }

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
    }

    // 75s / P = 207936
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


}