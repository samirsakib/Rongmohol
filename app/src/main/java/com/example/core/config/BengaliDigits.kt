package com.example.core.config

private val BENGALI_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

fun Int.toBengaliDigits(enabled: Boolean = true): String {
  val raw = this.toString()
  if (!enabled) return raw
  return buildString(raw.length) {
    for (ch in raw) {
      if (ch in '0'..'9') {
        append(BENGALI_DIGITS[ch - '0'])
      } else {
        append(ch)
      }
    }
  }
}

fun Long.toBengaliDigits(enabled: Boolean = true): String {
  val raw = this.toString()
  if (!enabled) return raw
  return buildString(raw.length) {
    for (ch in raw) {
      if (ch in '0'..'9') {
        append(BENGALI_DIGITS[ch - '0'])
      } else {
        append(ch)
      }
    }
  }
}
