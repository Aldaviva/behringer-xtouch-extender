package com.aldaviva.midi

internal fun Byte.toUnsignedInt(): Int = this.toInt() and 0xFF