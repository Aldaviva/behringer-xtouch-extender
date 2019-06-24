package com.aldaviva.midi.behringer.xtouch.extender

import com.aldaviva.midi.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun main() {
    val buttonLightStates: MutableMap<Pair<Int, IlluminatedButtonType>, IlluminatedButtonState> = mutableMapOf()
    val fadersTouched: MutableMap<Int, Boolean> = mutableMapOf()
    val knobPositions: MutableMap<Int, Int> = mutableMapOf()

    val controller: MidiControlSurface = BehringerXTouchExtender(MidiControlMode.RELATIVE)
    controller.listenForValueChanges(object : EventListener<MidiEventFromDevice>() {
        override fun onEvent(event: MidiEventFromDevice) {
            when (event) {
                is ButtonPressed -> {
                    println("Button ${event.buttonType} ${event.trackId} pressed")
                    val buttonToIlluminate = when (event.buttonType) {
                        PressableButtonType.RECORD -> IlluminatedButtonType.RECORD
                        PressableButtonType.SOLO -> IlluminatedButtonType.SOLO
                        PressableButtonType.MUTE -> IlluminatedButtonType.MUTE
                        PressableButtonType.SELECT -> IlluminatedButtonType.SELECT
                        else -> null
                    }
                    if (buttonToIlluminate != null) {
                        val key = Pair(event.trackId, buttonToIlluminate)
                        val newButtonState = when (buttonLightStates[key]) {
                            IlluminatedButtonState.ON -> IlluminatedButtonState.BLINKING
                            IlluminatedButtonState.OFF -> IlluminatedButtonState.ON
                            IlluminatedButtonState.BLINKING -> IlluminatedButtonState.OFF
                            else -> IlluminatedButtonState.ON
                        }
                        buttonLightStates[key] = newButtonState
                        controller.setButtonLight(event.trackId, buttonToIlluminate, newButtonState)
                    }
                    if (event.buttonType == PressableButtonType.FADER) {
                        fadersTouched[event.trackId] = true
                    }
                }
                is ButtonReleased -> {
                    println("Button ${event.buttonType} ${event.trackId} released")
                    if (event.buttonType == PressableButtonType.FADER) {
                        fadersTouched[event.trackId] = false
                    }
                }
                is KnobRotatedRelative -> {
                    println("Rotary Encoder ${event.trackId} rotated " + if (event.distanceRotatedClockwise > 0) "1 right" else "1 left")
                    val oldValue = knobPositions[event.trackId] ?: 0
                    val newValue = max(min(oldValue + event.distanceRotatedClockwise, 12), 0)
                    knobPositions[event.trackId] = newValue
                    controller.rotateKnob(event.trackId, newValue / 12.0)
                }
                is SliderMoved -> {
                    println("Fader ${event.trackId} moved to ${event.distanceFromMinimumValue * 100}%")
                    if (fadersTouched[event.trackId] != true) {
                        controller.moveSlider(event.trackId, event.distanceFromMinimumValue)
                    }
                }
            }
        }
    })

    println("Connecting to Behringer X-Touch Extender...")
    controller.open()
    println("Connected.")

    for (trackId in 1..8) {
        controller.rotateKnob(trackId, 0.0)
        for (buttonType in IlluminatedButtonType.values()) {
            buttonLightStates[Pair(trackId, buttonType)] = IlluminatedButtonState.BLINKING
            controller.setButtonLight(trackId, buttonType, IlluminatedButtonState.BLINKING)
        }
        val controlValue = (trackId - 1.0) / 7.0
        controller.rotateKnob(trackId, controlValue)
        knobPositions[trackId] = (controlValue * 12).roundToInt()

        controller.moveSlider(trackId, controlValue)
        fadersTouched[trackId] = false

        controller.setText(
            trackId,
            "Track $trackId",
            ".".repeat(trackId - 1),
            ScribbleStripTextColor.BLACK,
            ScribbleStripTextColor.WHITE,
            ScribbleStripBackgroundColor.values()[trackId - 1]
        )

    }
}