package cz.muni.fi.sybila.bool

fun Int.prettyPrint(model: BooleanModel): String {
    val values = model.encoder.extractValues(this)
    return values.mapIndexed { index, value -> "${model.variables[index].name}: $value" }
            .joinToString(separator = ", ", prefix = "[", postfix = "]")
}