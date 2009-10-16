/*
 * @(#)Stream.java
 * Created: 2005-04-21
 * Version: 2-0-alpha
 * Copyright (c) 2005-2006, University of Manchester All rights reserved. 
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials 
 * provided with the distribution. Neither the name of the University of 
 * Manchester nor the names of its contributors may be used to endorse or 
 * promote products derived from this software without specific prior written
 * permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package client.rtsp;

import java.text.NumberFormat;

/**
 * Represents an RTP stream
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class Stream {

    // The number of seconds per minute
    private static final int SECS_PER_MIN = 60;

    // The number of seconds per hour
    private static final int SECS_PER_HOUR = 3600;

    // The minimum digits in a duration integer
    private static final int MINIMUM_DIGITS_IN_DURATION = 2;

    // The string that separates elements of the time
    private static final String TIME_SEPARATOR = ":";

    // The name of the stream
    private String name = "";

    // The stream description
    private String description = "";

    // The MIME type of the stream
    private String type = "";

    // The SSRC of the stream
    private String ssrc = "";

    // The duration of the stream in seconds
    private double durationInSecs = 0;

    // True if the stream is currently selected for playback
    private boolean selected = true;

    /**
     * Creates a new Stream
     * 
     * @param name
     *            The name of the stream
     * @param description
     *            The description of the stream
     * @param type
     *            The Mime type of the stream
     * @param ssrc
     *            The ssrc of the stream
     * @param duration
     *            The duration of the stream in seconds
     */
    public Stream(String name, String description, String type, String ssrc,
            double duration) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.ssrc = ssrc;
        this.durationInSecs = duration;
        if (duration < 0) {
            durationInSecs = 0;
            selected = false;
        }
    }

    /**
     * Returns the name of the stream
     * @return The name of the stream
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description of the stream
     * @return The description of the stream
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the type of the stream
     * @return The type of the stream
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the ssrc of the stream
     * @return The ssrc of the stream
     */
    public String getSSRC() {
        return ssrc;
    }

    /**
     * Sets the selection status of the stream
     * 
     * @param selected
     *            True if the stream is selected for playback
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Returns true if the stream is selected for playback
     * @return True if the stream is selected for playback
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Returns the duration of the stream in seconds
     * @return the duration of the stream in seconds
     */
    public double getDuration() {
        return durationInSecs;
    }

    /**
     * Returns the length of the stream formated as hh:mm:ss
     * @return the duration as a string
     */
    public String getDurationString() {
        NumberFormat format = NumberFormat.getInstance();
        format.setMaximumFractionDigits(0);
        format.setMinimumIntegerDigits(MINIMUM_DIGITS_IN_DURATION);
        double duration = getDuration();
        long hours = (long) (duration / SECS_PER_HOUR);
        duration -= (hours * SECS_PER_HOUR);
        long minutes = (long) (duration / SECS_PER_MIN);
        duration -= (minutes * SECS_PER_MIN);
        long seconds = (long) duration;
        return (format.format(hours) + TIME_SEPARATOR + format.format(minutes) 
                + TIME_SEPARATOR + format.format(seconds));
    }

    /**
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o instanceof Stream) {
            Stream s = (Stream) o;
            return s.getSSRC().equals(getSSRC());
        }

        return false;
    }
    
    /**
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return (int) Long.parseLong(getSSRC());
    }
}