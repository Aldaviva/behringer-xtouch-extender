package com.aldaviva.midi

fun Byte.toUnsignedInt(): Int = this.toInt() and 0xFF