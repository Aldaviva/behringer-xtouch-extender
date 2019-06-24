package com.aldaviva.midi

import com.aldaviva.midi.behringer.xtouch.extender.ScribbleStripBackgroundColor
import com.aldaviva.midi.behringer.xtouch.extender.ScribbleStripTextColor

interface MidiControlSurface : AutoCloseable {
    /**
     * @throws DeviceNotFoundException if the required MIDI devices can not be found
     */
    fun open()

    /**
     * Register a callback to be called when the MIDI device sends a message to the computer. The callback will be called once per
     * message until <link href="#close()">close()</link> is called.
     * @param eventListener an instance of <code>EventListener&lt;MidiEventFromDevice&gt;</code> on which
     * <code>onEvent(MidiEventFromDevice)</code> will be called. Inspect the <link href="MidiEventFromDevice">MidiEventFromDevice</link>
     * parameter to retrieve information about the event that occurred. The subclasses of <code>MidiEventFromDevice</code> contain
     * information about the specific event.
     * @see ButtonPressed
     * @see ButtonReleased
     * @see KnobRotatedRelative
     * @see SliderMoved
     */
    fun listenForValueChanges(eventListener: EventListener<MidiEventFromDevice>)

    /**
     * Turn an illuminated button on or off, or make it blink.
     * @param trackId The number of the track or channel on the controller. There are eight tracks, numbered from <code>1</code> on the
     * far left to <code>8</code> on the far right (1-indexed).
     * @param buttonType Which of the buttons to control. Only the buttons that have lights in them are eligible: Rec, Solo, Mute, and
     * Select.
     * @param illuminatedButtonState Whether the button's light should be on, off, or blinking.
     */
    fun setButtonLight(trackId: Int, buttonType: IlluminatedButtonType, illuminatedButtonState: IlluminatedButtonState)

    /**
     * Set the rotation of a knob control by illuminating its LED collar. Rotary encoders are surrounded by 13 orange LEDs, exactly
     * one of which can be illuminated at a time.
     * @param trackId The number of the track or channel on the controller. There are eight tracks, numbered from <code>1</code> on the
     * far left to <code>8</code> on the far right (1-indexed).
     * @param distanceFromMinimumValue Which LED around the knob should be lit up. The leftmost LED (at the counter-clockwise limit) has
     * position <code>0.0</code>, the middle LED at 12 o'clock position is <code>0.5</code>, and the rightmost LED (at the clockwise
     * limit) has the value <code>1.0</code>.
     */
    fun rotateKnob(trackId: Int, distanceFromMinimumValue: Double)

    /**
     * Set the position of a slider, like the faders.
     * @param trackId The number of the track or channel on the controller. There are eight tracks, numbered from <code>1</code> on the
     * far left to <code>8</code> on the far right (1-indexed).
     * @param distanceFromMinimumValue The distance from the lowest position of the slider travel, starting at <code>0.0</code> and
     * increasing to <code>1.0</code>. The faders are labeled on the device from -∞ dB to 10 dB, with 0 dB at about (96/127) ≈
     * <code>0.756</code>.
     */
    fun moveSlider(trackId: Int, distanceFromMinimumValue: Double)

    /**
     * Print text onto LCD screens, like the scribble strips on each track. Each screen has room for two lines of 7 characters each.
     * @param trackId The number of the track or channel on the controller. There are eight tracks, numbered from <code>1</code> on the
     * far left to <code>8</code> on the far right (1-indexed).
     * @param topText The text to show in the first row of the screen. Can be at most 7 characters. Strings less than 7 characters long
     * will be left-aligned. Character set is limited to ASCII. To leave the row empty, pass the empty string.
     * @param bottomText The text to show in the second row of the screen. Can be at most 7 characters. Strings less than 7 characters
     * long will be left-aligned. Character set is limited to ASCII. To leave the row empty, pass the empty string.
     * @param topTextColor The color of the first row of text. <code>BLACK</code> will render dark text with a light background, and
     * <code>WHITE</code> will render light text with a dark background. Both dark and light regions will have the hue from
     * <code>backgroundColor</code>.
     * @param bottomTextColor The color of the second row of text. <code>BLACK</code> will render dark text, and
     * <code>WHITE</code> will render light text. Both dark and light regions will have the hue from <code>backgroundColor</code>.
     * @param backgroundColor The background color of the LCD screen.
     */
    fun setText(
        trackId: Int,
        topText: String = "",
        bottomText: String = "",
        topTextColor: ScribbleStripTextColor,
        bottomTextColor: ScribbleStripTextColor,
        backgroundColor: ScribbleStripBackgroundColor
    )
}