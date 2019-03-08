package cz.muni.fi.sybila.bool.rg



fun main() {
    build(12, emptyList())
}

fun build(remaining: Int, partial: List<Boolean>) {
    if (remaining == 0) check(partial)
    else {
        build(remaining-1, partial + true)
        build(remaining-1, partial + false)
    }
}

fun check(x: List<Boolean>) {
    /*
        Input table
        Cyt     p53     DNA
        0       0       0   x0
        0       0       1   x1
        0       [1,2]   0   x2
        0       [1,2]   1   x3
        1       0       0   x4
        1       0       1   x5
        1       [1,2]   0   x6
        1       [1,2]   1   x7
        2       0       0   x8
        2       0       1   x9
        2       [1,2]   0   x10
        2       [1,2]   1   x11
     */
    // DNA inhibition
    if (x[1] && !x[0]) return
    if (x[3] && !x[2]) return
    if (x[5] && !x[4]) return
    if (x[7] && !x[6]) return
    if (x[9] && !x[8]) return
    if (x[11] && !x[10]) return

    // p53 inhibition
    if (x[2] && !x[0]) return
    if (x[3] && !x[1]) return
    if (x[6] && !x[4]) return
    if (x[7] && !x[5]) return
    if (x[10] && !x[8]) return
    if (x[11] && !x[9]) return

    // Cyt activation
    if (x[0] && !x[4]) return
    if (x[1] && !x[5]) return
    if (x[2] && !x[6]) return
    if (x[3] && !x[7]) return
    if (x[4] && !x[8]) return
    if (x[5] && !x[9]) return
    if (x[6] && !x[10]) return
    if (x[7] && !x[11]) return

    // DNA observability
    if (x[0] == x[1] && x[2] == x[3] && x[4] == x[5] && x[6] == x[7] && x[8] == x[9] && x[10] == x[11]) return

    // p53 observability
    if (x[0] == x[2] && x[1] == x[3] && x[4] == x[6] && x[5] == x[7] && x[8] == x[10] && x[9] == x[11]) return

    // Cyt observability
    if (x[0] == x[4] && x[1] == x[5] && x[2] == x[6] && x[3] == x[7]) return
    if (x[4] == x[8] && x[5] == x[9] && x[6] == x[10] && x[7] == x[11]) return
    /*// dna activation
    if (v[3] && !v[0]) return
    if (v[4] && !v[1]) return
    if (v[5] && !v[2]) return
    // p53 inhibition
    if (v[0] && !v[1]) return
    if (v[0] && !v[2]) return
    if (v[3] && !v[4]) return
    if (v[3] && !v[5]) return
    // dna observability
    if (v[0] == v[3] && v[1] == v[4] && v[2] == v[5]) return
    // p53 observability
    if (v[0] == v[1] && v[0] == v[2] && v[3] == v[4] && v[3] == v[5]) return

    if (v[1] != v[2]) return
    if (v[4] != v[5]) return*/
    println("Valid: ${x.joinToString(separator = "\t") { if (it) "1" else "0" }}")
}