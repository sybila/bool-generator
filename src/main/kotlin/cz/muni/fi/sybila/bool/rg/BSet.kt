package cz.muni.fi.sybila.bool.rg

class BSet(val s: BDDSet) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BSet

        if (!s.contentEquals(other.s)) return false

        return true
    }

    override fun hashCode(): Int {
        return s.contentHashCode()
    }
}
