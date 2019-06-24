package com.aldaviva.midi

import java.lang.Exception

abstract class ControlSurfaceException(message: String?, cause: Throwable?) : Exception(message, cause)

class DeviceNotFoundException(message: String?): ControlSurfaceException(message, null)