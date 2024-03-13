package gg.norisk.zerstoerung.util

import java.util.*

//thanks to https://stackoverflow.com/questions/6409652/random-weighted-selection-in-java
class RandomCollection<E> {
    private val map: NavigableMap<Double, E> = TreeMap()
    private val random: Random = Random()
    private var total = 0.0

    fun add(weight: Double, result: E): RandomCollection<E> {
        if (weight <= 0) return this
        total += weight
        map[total] = result
        return this
    }

    operator fun next(): E {
        val value = random.nextDouble() * total
        return map.higherEntry(value).value
    }
}
