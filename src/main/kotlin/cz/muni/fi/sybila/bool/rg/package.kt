package cz.muni.fi.sybila.bool.rg

import cz.muni.fi.sybila.bool.rg.map.ArrayStateMap

// The bit vector of values in the state
typealias State = Int

// The index of the parameter in the standardized ordering (given by the network)
typealias Parameter = Int

// The index of a variable in the standardized ordering (given by the network)
typealias Dimension = Int

// The index of a specie in the standardized ordering (given by the network)
typealias Specie = Int

typealias StateMap = ArrayStateMap