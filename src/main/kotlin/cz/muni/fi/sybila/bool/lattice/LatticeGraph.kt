package cz.muni.fi.sybila.bool.lattice

import cz.muni.fi.sybila.bool.common.ConcurrentArrayStateMap
import cz.muni.fi.sybila.bool.common.ParametrisedGraph
import cz.muni.fi.sybila.bool.common.Solver
import cz.muni.fi.sybila.bool.common.StateSet
import cz.muni.fi.sybila.bool.rg.BooleanNetwork
import cz.muni.fi.sybila.bool.rg.BooleanStateEncoder

class LatticeGraph(
        network: BooleanNetwork,
        override val solver: Solver<LatticeSet>
) : ParametrisedGraph<LatticeSet> {

    override val dimensions = network.species.size
    override val states = BooleanStateEncoder(network)

    override val stateCount: Int = states.stateCount

    override fun newMap(): StateSet<LatticeSet> = ConcurrentArrayStateMap(stateCount, solver)

}