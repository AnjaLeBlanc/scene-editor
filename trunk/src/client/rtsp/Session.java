/*
 * @(#)Session.java
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

import java.util.Vector;

/**
 * Represents a session in Arena
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class Session {

    // The id of the session
    private String id = "";

    // The name of the session
    private String name = "";

    // The session description
    private String description = "";

    // The ownere of the session
    private String owner = "";

    // The length of the session
    private double durationInSecs = 0;

    // True if the session has been recorded
    private boolean recorded = false;

    // The server session Id
    private String serverSessionId = null;

    // The streams in the session
    private Vector<Stream> streams = null;

    // True if the session is to be timer recorded
    private boolean timerRecord = false;

    // The start time of the session
    private long startTime = 0;

    // The end time of the session
    private long endTime = 0;

    /**
     * Creates a new Session
     * 
     * @param id
     *            The id of the session
     */
    public Session(String id) {
        this.id = id;
    }

    /**
     * Returns the id of the session
     * @return The id of the session
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the name of the session
     * 
     * @param name
     *            The name to give the session
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the owner of the session
     * 
     * @param owner
     *            The owner of the session
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Sets the description of the session
     * 
     * @param desc
     *            The description to give the session
     */
    public void setDescription(String desc) {
        description = desc;
    }

    /**
     * Sets the streams of this session
     * 
     * @param streams
     *            The streams to set
     */
    public void setStreams(Vector<Stream> streams) {
        this.streams = streams;
    }

    /**
     * Sets the recorded status of the session
     * 
     * @param isRecorded
     *            The recorded status if the session
     */
    public void setRecorded(boolean isRecorded) {
        this.recorded = isRecorded;
    }

    /**
     * Sets the timer record status of the session
     * 
     * @param isTimerRecording
     *            True if the session is to be timer recorded
     */
    public void setTimerRecording(boolean isTimerRecording) {
        timerRecord = isTimerRecording;
    }

    /**
     * Sets the start time of the session
     * 
     * @param startTime
     *            The start time in ms
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Sets the end time of the session
     * 
     * @param endTime
     *            The end time in ms
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * Returns the name of the session
     * @return The name of the session
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the owner of the session
     * @return The owner of the session
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Returns the description of the session
     * @return The description of the session
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the duration of the session
     * @return The duration of the session in seconds
     */
    public double getDurationInSecs() {
        return durationInSecs;
    }

    /**
     * Sets the duration of the session
     * 
     * @param duration
     */
    public void setDurationInSecs(double duration) {
        System.err.println("Duration = " + duration);
        durationInSecs = duration;
    }

    /**
     * Returns true if the session has been recorded
     * @return True if the session has been recorded
     */
    public boolean isRecorded() {
        return recorded;
    }

    /**
     * Returns true if the session is to be timer recorded
     * @return true if the recording is via timer
     */
    public boolean isTimerRecording() {
        return timerRecord;
    }

    /**
     * Returns the start time of the session
     * @return the start time in ms since the epoch
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Returns the end time of the session
     * @return the end time in ms since the epoch
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return name;
    }

    /**
     * Sets the server session id
     * 
     * @param id
     *            The id to set
     */
    public void setServerSessionId(String id) {
        serverSessionId = id;
    }

    /**
     * Returns the session id of the recording or playback
     * @return The server session id
     */
    public String getServerSessionId() {
        return serverSessionId;
    }

    /**
     * Returns the streams in the session
     * @return A vector of Streams
     */
    public Vector<Stream> getStreams() {
        return streams;
    }
}