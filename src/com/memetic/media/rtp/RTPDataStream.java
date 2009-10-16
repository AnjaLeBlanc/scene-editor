/*
 * @(#)RTPDataStream.java
 * Created: 01-Dec-2005
 * Version: 1-1-alpha3
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

package com.memetic.media.rtp;

import java.awt.Component;
import java.io.IOException;
import java.util.Timer;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.GlobalReceptionStats;
import javax.media.rtp.RTPControl;
import javax.media.rtp.ReceptionStats;

/**
 * A generic RTP Data Stream
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public abstract class RTPDataStream implements PushBufferStream, RTPControl {
    
    // The divisor for the jitter
    private static final int JITTER_DIVISOR = 16;

    // The default threshold
    private static final int DEFAULT_THRESHOLD = 100;

    // The default clock rate for video
    private static final int VIDEO_CLOCK_RATE = 90000;

    // The format of the data
    private Format format = null;
    
    // The clock rate of the RTP clock for the format
    private double clockRate = VIDEO_CLOCK_RATE;
    
    // The transfer Handler
    private BufferTransferHandler handler = null;
    
    // The timer object for sending the next item
    private Timer timer = new Timer();
        
    // The threshold of the buffer before any playout is done in ms
    private int threshold = DEFAULT_THRESHOLD;
    
    // The controls of the stream
    private Object[] controls = new Object[0];
    
    // The reception statistics
    private RTPReceptionStats receptionStats = new RTPReceptionStats();
    
    // The inter-arrival jitter of the packets
    private long jitter = 0;
    
    // The last packet delay time
    private long lastDelay = -1;
    
    // The time of arrival of the last rtp packet
    private long lastRTPReceiveTime = -1;
    
    // The rtp timestamp of the last rtp packet
    private long lastRTPTimestamp = -1;
    
    // The first sequence number seen
    private long firstSequence = -1;
    
    // The last sequence number seen
    private long lastSequence = -1;
    
    /**
     * Creates a new RTPDataStream
     * @param ssrc The ssrc of the stream
     * @param format The format of the stream
     */
    public RTPDataStream(long ssrc, Format format) {
        this.format = format;
    }
    
    protected abstract void addPacket(RTPHeader header, byte[] data, 
            int offset, int length);
    
    /**
     * 
     * @see javax.media.rtp.RTPControl#getFormat()
     */
    public Format getFormat() {
        return format;
    }

    /**
     * 
     * @see javax.media.protocol.PushBufferStream#setTransferHandler(
     *     javax.media.protocol.BufferTransferHandler)
     */
    public void setTransferHandler(BufferTransferHandler transferHandler) {
        this.handler = transferHandler;
    }

    /**
     * 
     * @see javax.media.protocol.SourceStream#getContentDescriptor()
     */
    public ContentDescriptor getContentDescriptor() {
        return new ContentDescriptor(ContentDescriptor.RAW_RTP);
    }

    /**
     * 
     * @see javax.media.protocol.SourceStream#getContentLength()
     */
    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }

    /**
     * 
     * @see javax.media.protocol.SourceStream#endOfStream()
     */
    public boolean endOfStream() {
        return false;
    }

    /**
     * 
     * @see javax.media.Controls#getControls()
     */
    public Object[] getControls() {
        return controls;
    }

    /**
     * 
     * @see javax.media.Controls#getControl(java.lang.String)
     */
    public Object getControl(String controlType) {
        if (controlType.equals("javax.media.rtp.RTPControl")) {
            return this;
        }
        return null;
    }

    /**
     * 
     * @see javax.media.protocol.PushBufferStream#read(javax.media.Buffer)
     */
    public abstract void read(Buffer buffer) throws IOException;

    /**
     * 
     * @see javax.media.rtp.RTPControl#addFormat(javax.media.Format, int)
     */
    public void addFormat(Format fmt, int payload) {
        // Does Nothing
    }

    /**
     * 
     * @see javax.media.rtp.RTPControl#getReceptionStats()
     */
    public ReceptionStats getReceptionStats() {
        return receptionStats;
    }

    /**
     * 
     * @see javax.media.rtp.RTPControl#getGlobalStats()
     */
    public GlobalReceptionStats getGlobalStats() {
        return null;
    }

    /**
     * 
     * @see javax.media.rtp.RTPControl#getFormatList()
     */
    public Format[] getFormatList() {
        return new Format[0];
    }

    /**
     * 
     * @see javax.media.rtp.RTPControl#getFormat(int)
     */
    public Format getFormat(int payload) {
        return format;
    }

    /**
     * 
     * @see javax.media.Control#getControlComponent()
     */
    public Component getControlComponent() {
        return null;
    }
    
    /**
     * Performs jitter calculations
     */
    protected void calculateJitter(long rtpTimestamp) {
        if (lastRTPReceiveTime == -1) {
            lastRTPReceiveTime = System.currentTimeMillis();
            lastRTPTimestamp = rtpTimestamp;
        } else if (lastDelay == -1) {
            lastRTPReceiveTime = System.currentTimeMillis();
            lastRTPTimestamp = rtpTimestamp;
            long expChange = (long) ((System.currentTimeMillis()
                    - lastRTPReceiveTime) * clockRate);
            long actualChange = rtpTimestamp - lastRTPTimestamp;
            lastDelay = expChange - actualChange;
        } else {
            lastRTPReceiveTime = System.currentTimeMillis();
            lastRTPTimestamp = rtpTimestamp;
            long expChange = (long) ((System.currentTimeMillis()
                    - lastRTPReceiveTime) * clockRate);
            long actualChange = rtpTimestamp - lastRTPTimestamp;
            long delay = expChange - actualChange;
            long delaydiff = Math.abs(delay - lastDelay);
            lastDelay = delay;
            jitter = jitter + ((delaydiff - jitter) / JITTER_DIVISOR);
        }
    }
    
    /**
     * Returns the jitter calculation
     * @return the jitter
     */
    public long getJitter() {
        return jitter;
    }
    
    /**
     * Returns the last sequence number seen
     * @return the last sequence
     */
    public long getLastSequence() {
        return lastSequence;
    }
    
    /**
     * Returns the first sequence number seen
     * @return the first sequence
     */
    public long getFirstSequence() {
        return firstSequence;
    }
    
    protected void setClockRate(double clockRate) {
        this.clockRate = clockRate;
    }
    
    protected int getThreshold() {
        return threshold;
    }
    
    protected double getClockRate() {
        return clockRate;
    }
    
    protected void setFirstSequence(int firstSequence) {
        this.firstSequence = firstSequence;
    }
    
    protected void setLastSequence(int lastSequence) {
        this.lastSequence = lastSequence;
    }
    
    protected BufferTransferHandler getHandler() {
        return handler;
    }
    
    protected Timer getTimer() {
        return timer;
    }
}
