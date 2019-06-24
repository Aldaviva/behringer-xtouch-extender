package com.aldaviva.midi

interface MidiEventFromDevice {
    val trackId: Int
}

/**
 * Event fired when a button or other touchable control is pushed down or touched.
 * @param trackId The number of the track or channel on the controller. There are eight tracks, numbered from <code>1</code> on the
 * far left to <code>8</code> on the far right (1-indexed).
 * @param buttonType The label on the button, such as Rec, Solo, Mute, etc. This also includes pushing on the rotary encoders and
 * touching the faders.
 * @see ButtonReleased when the button stops being pushed down or touched
 */
data class ButtonPressed(override val trackId: Int, val buttonType: PressableButtonType) : MidiEventFromDevice

/**
 * Event fired when a button or other touchable control is stops being pushed down or touched.
 * @param trackId The number of the track or channel on the controller. There are eight tracks, numbered from <code>1</code> on the
 * far left to <code>8</code> on the far right (1-indexed).
 * @param buttonType The label on the button, such as Rec, Solo, Mute, etc. This also includes releasing the rotary encoders and
 * stopping touching the faders.
 * @see ButtonPressed when the button is pushed down or touched
 */
data class ButtonReleased(override val trackId: Int, val buttonType: PressableButtonType) : MidiEventFromDevice

/**
 * Event fired when a knob, such as a rotary encoder, has been turned.
 * <p>This event is only fired when the controller is in MIDI Controller Relative (CtrlRel) mode.</p>
 * @param trackId The number of the track or channel on the controller. There are eight tracks, numbered from <code>1</code> on the
 * far left to <code>8</code> on the far right (1-indexed).
 * @param distanceRotatedClockwise how much the knob was rotated. If it was rotated clockwise, this value will be <code>1.0</code>,
 * otherwise, if it was rotated counter-clockwise, this value will be <code>-1.0<code>. Turning the knob quickly results in more events
 * being sent, not larger magnitudes of values.
 */
data class KnobRotatedRelative(override val trackId: Int, val distanceRotatedClockwise: Int) : MidiEventFromDevice

/**
 * Event fired when a knob, such as a rotary encoder, has been turned.
 * <p>This event is only fired when the controller is in MIDI Controller Absolute (Ctrl) mode.</p>
 * @param trackId The number of the track or channel on the controller. There are eight tracks, numbered from <code>1</code> on the
 * far left to <code>8</code> on the far right (1-indexed).
 * @param distanceFromMinimumValue how far the knob has been rotated from the lowest possible position. This value is linear, with a
 * minimum value of <code>0.0</code> and a maximum value of <code>1.0</code>. For the rotary encoders, this range is shown on the
 * device with 13 LEDs. The seventh LED is in the middle, at the 12 o'clock position, and corresponds to a value of <code>0.5</code>.
 */
data class KnobRotatedAbsolute(override val trackId: Int, val distanceFromMinimumValue: Double) : MidiEventFromDevice

/**
 * Event fired when a slider, such as a fader, has been moved.
 * @param trackId The number of the track or channel on the controller. There are eight tracks, numbered from <code>1</code> on the
 * far left to <code>8</code> on the far right (1-indexed).
 * @param distanceFromMinimumValue how far the slider is from the lowest possible position. This value is linear, with a minimum
 * value of <code>0.0</code> and a maximum value of <code>1.0</code>. For the faders, this range is shown on the device as -∞ dB to
 * 10 dB, with 0 dB located at about (96/127) ≈ <code>0.756</code>.
 */
data class SliderMoved(override val trackId: Int, val distanceFromMinimumValue: Double) : MidiEventFromDevice