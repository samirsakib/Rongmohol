package com.example.domain.game

import kotlin.random.Random

/**
 * Pure shuffling abstraction so game logic can run deterministically in unit tests.
 */
interface ShuffleAlgorithm {
  fun <T> shuffle(items: List<T>): List<T>
}

/**
 * Unbiased Fisher-Yates (Knuth) shuffle producing a new list without mutating the source.
 */
class FisherYatesShuffleAlgorithm(
  private val random: Random = Random.Default,
) : ShuffleAlgorithm {

  override fun <T> shuffle(items: List<T>): List<T> {
    if (items.size <= 1) return items.toList()
    val result = items.toMutableList()
    for (i in result.lastIndex downTo 1) {
      val swapIndex = random.nextInt(i + 1)
      if (i != swapIndex) {
        val temp = result[i]
        result[i] = result[swapIndex]
        result[swapIndex] = temp
      }
    }
    return result.toList()
  }
}
