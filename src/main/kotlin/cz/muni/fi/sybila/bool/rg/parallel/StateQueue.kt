package cz.muni.fi.sybila.bool.rg.parallel

class StateQueue(private val stateCount: Int) {

    private var empty = true
    private val set = IntArray((stateCount/31) + 1)
    private var iterator = 0

    val isEmpty: Boolean
        get() = empty

    fun set(state: Int) {
        empty = false
        set[state/31] = set[state/31] or (1.shl(state%31))
    }

    fun next(): Int {
        while (iterator < stateCount && set[iterator/31].shr(iterator%31).and(1) == 0) {
            iterator += 1
        }
        return if (iterator == stateCount) -1 else iterator.also { iterator += 1 }
    }
}