package com.aldaviva.midi.behringer.xtouch.extender

import com.aldaviva.midi.*
import javax.sound.midi.*
import kotlin.math.roundToInt

/**
 * Behringer X-Touch Extender
 *
 * ## Requirements
 * - Ensure the device appears as a MIDI device on your computer, either over USB or RTP (Ethernet)
 * - Device must be in Ctrl or CtrlRel mode (to change, hold Track 1 Select while powering it on)
 *
 * ## Sample usage
 * ```
 * val controlSurface: MidiControlSurface = BehringerXTouchExtender(MidiControlMode.RELATIVE)
 * controlSurface.use {
 *     controlSurface.open()
 *     controlSurface.registerForEvents(object : EventListener<MidiEventFromDevice>() {
 *         override fun onEvent(event: MidiEventFromDevice) {
 *             if (event is ButtonPressed && event.buttonType == PressableButtonType.RECORD) {
 *                 controlSurface.setButtonLight(event.trackId, IlluminatedButtonType.RECORD, IlluminatedButtonState.ON)
 *             }
 *         }
 *     })
 *     println("Press a REC button to turn on its LED")
 * }
 * ```
 *
 * ## OEM Links
 * - [Product page](https://www.behringer.com/Categories/Behringer/Computer-Audio/Desktop-Controllers/X-TOUCH-EXTENDER/p/P0CCR#googtrans(en|en))
 * - [Downloads](https://www.behringer.com/Categories/Behringer/Computer-Audio/Desktop-Controllers/X-TOUCH-EXTENDER/p/P0CCR/Downloads#googtrans(en|en))
 * - [Quick Start Guide](https://media63.music-group.com/media/sys_master/hd3/h51/8849820287006.pdf)
 */
class BehringerXTouchExtender(private val controlMode: MidiControlMode) : MidiControlSurface, Receiver {
    private var receiverDevice: MidiDevice? = null
    private var transmitterDevice: MidiDevice? = null
    private var midiOutToDevice: Receiver? = null

    private val emitterForEventsFromDevice = SimpleEventEmitter<MidiEventFromDevice>()
    private val currentTimestamp: Long = -1

    /*
     * Device ID is not 0x42 as documented.
     * Thanks https://community.musictribe.com/t5/Recording/X-Touch-Extender-Scribble-Strip-Midi-Sysex-Command/td-p/251306
     */
    private val deviceId = 0x15
    private val trackCount = 8

    /**
     * @throws DeviceNotFoundException if the Behringer X-Touch Controller is not connected to the computer or installed correctly
     * @throws MidiUnavailableException if the device is out of resources, for example, if another program on your computer is already
     * using the device
     */
    override fun open() {
        if (isOpen) {
            throw IllegalStateException("This MidiControlSurface instance has already been opened. Call MidiControlSurface.isOpen")
        }

        val availableDevices = MidiSystem.getMidiDeviceInfo()

        val receiverDeviceInfo =
            availableDevices.find { device -> device.name == "X-Touch-Ext" && device.description == "External MIDI Port" }
                ?: throw DeviceNotFoundException(
                    """Could not find connected MIDI device with name "X-Touch-Ext" and description "External MIDI Port" to send MIDI 
                        |messages to. Ensure the X-Touch Extender is plugged in to this computer, turned on, and the USB drivers have 
                        |been automatically installed.""".trimMargin()
                )
        val transmitterDeviceInfo =
            availableDevices.find { device -> device.name == "X-Touch-Ext" && device.description == "No details available" }
                ?: throw DeviceNotFoundException(
                    """Could not find connected MIDI device with name "X-Touch-Ext" and description "No details available" to get MIDI 
                        |messages from. Ensure the X-Touch Extender is plugged in to this computer, turned on, and the USB drivers have 
                        |been automatically installed.""".trimMargin()
                )

        receiverDevice = MidiSystem.getMidiDevice(receiverDeviceInfo)
        transmitterDevice = MidiSystem.getMidiDevice(transmitterDeviceInfo)

        receiverDevice!!.open()
        transmitterDevice!!.open()

        midiOutToDevice = receiverDevice!!.receiver
        transmitterDevice!!.transmitter.receiver = this
    }

    override val isOpen: Boolean
        get() = receiverDevice?.isOpen == true || transmitterDevice?.isOpen == true

    override fun registerForEvents(eventListener: EventListener<MidiEventFromDevice>) {
        emitterForEventsFromDevice.register(eventListener)
    }

    override fun setButtonLight(trackId: Int, buttonType: IlluminatedButtonType, illuminatedButtonState: IlluminatedButtonState) {
        assertOpen()
        assertTrackIdInBounds(trackId)

        val noteId = trackId - 1 + when (buttonType) {
            IlluminatedButtonType.RECORD -> 8
            IlluminatedButtonType.SOLO -> 16
            IlluminatedButtonType.MUTE -> 24
            IlluminatedButtonType.SELECT -> 32
        }
        val velocity = when (illuminatedButtonState) {
            IlluminatedButtonState.OFF -> 0
            IlluminatedButtonState.ON -> 127
            IlluminatedButtonState.BLINKING -> 64
        }
        midiOutToDevice?.send(ShortMessage(ShortMessage.NOTE_ON, noteId, velocity), currentTimestamp)
    }

    override fun rotateKnob(trackId: Int, distanceFromMinimumValue: Double) {
        changeControl(80, trackId, distanceFromMinimumValue)
    }

    override fun moveSlider(trackId: Int, distanceFromMinimumValue: Double) {
        changeControl(70, trackId, distanceFromMinimumValue)
    }

    override fun setMeterLevel(trackId: Int, distanceFromMinimumValue: Double) {
        changeControl(90, trackId, distanceFromMinimumValue)
    }

    private fun changeControl(track1ControlId: Int, trackId: Int, distanceFromMinimumValue: Double){
        assertOpen()
        assertTrackIdInBounds(trackId)
        if (distanceFromMinimumValue !in 0.0..1.0) {
            throw IllegalArgumentException("Parameter distanceFromMinimumValue value $distanceFromMinimumValue is out of bounds. Valid values are in the range [0.0, 1.0].")
        }

        val controlId = track1ControlId + trackId - 1
        val controlValue = (distanceFromMinimumValue * 127).roundToInt()
        midiOutToDevice?.send(ShortMessage(ShortMessage.CONTROL_CHANGE, controlId, controlValue), currentTimestamp)
    }

    override fun setText(
        trackId: Int,
        topText: String,
        bottomText: String,
        topTextColor: ScribbleStripTextColor,
        bottomTextColor: ScribbleStripTextColor,
        backgroundColor: ScribbleStripBackgroundColor
    ) {
        assertOpen()
        assertTrackIdInBounds(trackId)

        val textColumnCount = 7
        val payload = ByteArray(23)
        payload[0] = SysexMessage.SYSTEM_EXCLUSIVE.toByte()
        payload[1] = 0x00.toByte()
        payload[2] = 0x20.toByte()
        payload[3] = 0x32.toByte()
        payload[4] = deviceId.toByte()
        payload[5] = 0x4C.toByte()
        payload[6] = (trackId - 1).toByte()
        payload[7] = (backgroundColor.ordinal
                or (encodeTextColor(topTextColor) shl 4)
                or (encodeTextColor(bottomTextColor) shl 5)).toByte()
        for (column in 0 until textColumnCount) {
            payload[8 + column] = (topText.elementAtOrNull(column) ?: ' ').toByte()
            payload[8 + column + textColumnCount] = (bottomText.elementAtOrNull(column) ?: ' ').toByte()
        }
        payload[22] = SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE.toByte()

        midiOutToDevice?.send(SysexMessage(payload, payload.size), currentTimestamp)
    }

    private fun encodeTextColor(textColor: ScribbleStripTextColor): Int {
        return when (textColor) {
            ScribbleStripTextColor.LIGHT -> 0
            ScribbleStripTextColor.DARK -> 1
        }
    }

    /**
     * Called whenever the MIDI device sends a message to this library. Don't call this to send anything, use midiOutToDevice.send() for that.
     */
    override fun send(message: MidiMessage?, timeStamp: Long) {
        if (message == null) return
        val verbId = message.message[0].toUnsignedInt()
        val noteId = message.message[1].toUnsignedInt()
        val rawValue = message.message[2].toUnsignedInt()
        val eventToEmit: MidiEventFromDevice? = when (verbId) {
            ShortMessage.NOTE_ON -> onNoteOnMessage(noteId, rawValue)
            ShortMessage.CONTROL_CHANGE -> onControlChangeMessage(noteId, rawValue)
            else -> null
        }

        if (eventToEmit != null) {
            emitterForEventsFromDevice.trigger(eventToEmit)
        }
    }

    private fun onNoteOnMessage(noteId: Int, rawValue: Int): MidiEventFromDevice? {
        val trackId: Int
        val buttonType: PressableButtonType
        when (noteId) {
            in 0x00 until 0x00 + trackCount -> {
                trackId = noteId - 0x00 + 1
                buttonType = PressableButtonType.ROTARY_ENCODER
            }
            in 0x08 until 0x08 + trackCount -> {
                trackId = noteId - 0x08 + 1
                buttonType = PressableButtonType.RECORD
            }
            in 0x10 until 0x10 + trackCount -> {
                trackId = noteId - 0x10 + 1
                buttonType = PressableButtonType.SOLO
            }
            in 0x18 until 0x18 + trackCount -> {
                trackId = noteId - 0x18 + 1
                buttonType = PressableButtonType.MUTE
            }
            in 0x20 until 0x20 + trackCount -> {
                trackId = noteId - 0x20 + 1
                buttonType = PressableButtonType.SELECT
            }
            in 0x68 until 0x68 + trackCount -> {
                trackId = noteId - 0x68 + 1
                buttonType = PressableButtonType.FADER
            }
            else -> return null
        }

        return if (rawValue == 0x7F) {
            ButtonPressed(trackId, buttonType)
        } else {
            ButtonReleased(trackId, buttonType)
        }
    }

    private fun onControlChangeMessage(noteId: Int, rawValue: Int): MidiEventFromDevice? {
        return when (noteId) {
            in 80 until 80 + trackCount -> {
                val trackId = noteId - 80 + 1
                when (controlMode) {
                    MidiControlMode.RELATIVE -> {
                        val relativeDistanceMoved = when (rawValue) {
                            65 -> 1
                            1 -> -1
                            else -> 0
                        }
                        return KnobRotatedRelative(trackId, relativeDistanceMoved)
                    }
                    MidiControlMode.ABSOLUTE -> KnobRotatedAbsolute(trackId, rawValue / 127.0)
                }
            }
            in 70 until 70 + trackCount -> SliderMoved(noteId - 70 + 1, rawValue / 127.0)
            else -> null
        }
    }

    override fun close() {
        emitterForEventsFromDevice.unregisterAll()

        midiOutToDevice?.close()
        receiverDevice?.close()
        transmitterDevice?.close()

        midiOutToDevice = null
        receiverDevice = null
        transmitterDevice = null
    }

    private fun assertOpen() {
        if (!isOpen) {
            throw IllegalStateException("MidiControlSurface class instance has not been opened. Call MidiControlSurface.open() before calling this method.")
        }
    }

    private fun assertTrackIdInBounds(trackId: Int) {
        if (trackId !in 1..trackCount) {
            throw IllegalArgumentException("Parameter trackId value is out of bounds. Valid values are in the range [1, $trackCount].")
        }
    }
}