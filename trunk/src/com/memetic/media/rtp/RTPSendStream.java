/*
 * @(#)RTPSendStream.java
 * Created: 29-Oct-2005
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.protocol.PushDataSource;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.SourceTransferHandler;
import javax.media.rtp.OutputDataStream;
import javax.media.rtp.Participant;
import javax.media.rtp.SendStream;
import javax.media.rtp.TransmissionStats;
import javax.media.rtp.rtcp.SenderReport;
import javax.media.rtp.rtcp.SourceDescription;

/**
 * Represnts an RTP sending stream
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public class RTPSendStream implements SendStream, 
        SourceTransferHandler, BufferTransferHandler {
    
    // The conversion factor between ms and seconds
    private static final int MS_TO_SECS = 1000;

    // The shift for the third byte of an int
    private static final int INT_TO_BYTE_3_SHIFT = 8;

    // The shift for the second byte of an int
    private static final int INT_TO_BYTE_2_SHIFT = 16;

    // The shift for the first byte of an int
    private static final int INT_TO_BYTE_1_SHIFT = 24;

    // The position of the 4th SSRC byte in the RTP packet
    private static final int SSRC_BYTE_4 = 11;

    // The position of the 3rd SSRC byte in the RTP packet
    private static final int SSRC_BYTE_3 = 10;

    // The position of the 2nd SSRC byte in the RTP packet
    private static final int SSRC_BYTE_2 = 9;

    // The position of the 1st SSRC byte in the RTP packet
    private static final int SSRC_BYTE_1 = 8;

    // The position of the 4th timestamp byte in the RTP packet
    private static final int TIME_BYTE_4 = 7;

    // The position of the 3rd timestamp byte in the RTP packet
    private static final int TIME_BYTE_3 = 6;

    // The position of the 2nd timestamp byte in the RTP packet
    private static final int TIME_BYTE_2 = 5;

    // The position of the 1st timestamp byte in the RTP packet
    private static final int TIME_BYTE_1 = 4;

    // The position of the 2nd sequence byte in the RTP packet
    private static final int SEQ_BYTE_2 = 3;

    // The position of the 1st sequence byte in the RTP packet
    private static final int SEQ_BYTE_1 = 2;

    // The shift for the 1st byte in a short
    private static final int SHORT_TO_BYTE_1_SHIFT = 8;

    // The mask for the marker bit
    private static final int MARKER_MASK = 0x80;

    // The conversion mask from a byte to an int
    private static final int BYTE_TO_INT_MASK = 0xFF;

    // The size of an SDES item header
    private static final int SDES_HEADER_SIZE = 2;

    // The video clock rate in RTP
    private static final int VIDEO_CLOCK_RATE = 90000;
    
    // The first byte of the rtp header
    private static final byte HEADER0 = (byte) 0x80;

    // The ssrc of the stream
    private long ssrc = 0;
    
    // The data source being sent
    private DataSource dataSource = null;
    
    // The rtp output stream
    private OutputDataStream rtpDataStream = null;
    
    // The source description objects
    private HashMap<Integer,SourceDescription> sourceDescriptions = 
    	new HashMap<Integer,SourceDescription>();
    
    // The size of the sdes items
    private int sdesSize = 0;
    
    // The index of the stream to use from the data source
    private int index = 0;
    
    // The rtp format being sent
    private int format = 0;
    
    // The clock rate of the format
    private double clockRate = VIDEO_CLOCK_RATE;
    
    // True if the source has been started
    private boolean started = false;
    
    // The local participant
    private RTPLocalParticipant participant = null;
    
    // The buffer used to send the data
    private byte[] buffer = new byte[0];
    
    // The last sequence number
    private int lastSequence = (int) (Math.random() * RTPHeader.MAX_SEQUENCE);
    
    // The last time at which a value was sent
    private long lastSendTime = -1;
    
    // The last timestamp sent
    private long lastTimestamp = (long) (Math.random() * Integer.MAX_VALUE);
    
    // The transmission statistics
    private RTPTransmissionStats stats = new RTPTransmissionStats();
    
    
    /**
     * Creates a new RTPSendStream
     * @param ssrc The ssrc of the stream
     * @param dataSource The datasource of the stream
     * @param rtpDataStream The rtp output
     * @param index The index of the stream in the data source
     * @param participant The participant sending the stream
     * @param format The RTP format of the stream
     * @param clockRate The clock rate of the stream
     */
    public RTPSendStream(long ssrc, DataSource dataSource, 
            OutputDataStream rtpDataStream, int index, 
            RTPLocalParticipant participant, int format, double clockRate) {
        this.ssrc = ssrc;
        this.dataSource = dataSource;
        this.rtpDataStream = rtpDataStream;
        this.index = index;
        this.participant = participant;
        this.format = format;
        this.clockRate = clockRate;
        addSourceDescription(
                new SourceDescription(SourceDescription.SOURCE_DESC_CNAME, 
                        participant.getCNAME(), 1, false));
        addSourceDescription(
                new SourceDescription(SourceDescription.SOURCE_DESC_NAME, 
                        participant.getCNAME(), 1, false));
    }
    
    /**
     * Adds a source description to this send stream
     * @param sdes The description to add
     */
    public void addSourceDescription(SourceDescription sdes) {
        SourceDescription oldSdes = 
            (SourceDescription) sourceDescriptions.get(
                    new Integer(sdes.getType()));
        if (oldSdes != null) {
            sdesSize -= oldSdes.getDescription().length();
            sdesSize -= SDES_HEADER_SIZE;
        }
        sourceDescriptions.put(new Integer(sdes.getType()), sdes);
        sdesSize += SDES_HEADER_SIZE;
        sdesSize += sdes.getDescription().length();
    }

    /**
     * 
     * @see javax.media.rtp.SendStream#setSourceDescription(
     *     javax.media.rtp.rtcp.SourceDescription[])
     */
    public void setSourceDescription(SourceDescription[] sourceDesc) {
        for (int i = 0; i < sourceDesc.length; i++) {
            addSourceDescription(sourceDesc[i]);
        }
    }

    /**
     * 
     * @see javax.media.rtp.SendStream#close()
     */
    public void close() {
        if (started) {
            stop();
        }
    }

    /**
     * 
     * @see javax.media.rtp.SendStream#stop()
     */
    public void stop() {
        started = false;
    }

    /**
     * 
     * @see javax.media.rtp.SendStream#start()
     */
    public void start() {
        if (!started) {
            if (dataSource instanceof PushBufferDataSource) {
                PushBufferStream[] streams = 
                    ((PushBufferDataSource) dataSource).getStreams();
                streams[index].setTransferHandler(this);
            } else if (dataSource instanceof PushDataSource) {
                PushSourceStream[] streams = 
                    ((PushDataSource) dataSource).getStreams();
                streams[index].setTransferHandler(this);
            }
        }
    }

    /**
     * 
     * @see javax.media.rtp.SendStream#setBitRate(int)
     */
    public int setBitRate(int bitRate) {
        return -1;
    }

    /**
     * 
     * @see javax.media.rtp.SendStream#getSourceTransmissionStats()
     */
    public TransmissionStats getSourceTransmissionStats() {
        return stats;
    }

    /**
     * 
     * @see javax.media.rtp.RTPStream#getParticipant()
     */
    public Participant getParticipant() {
        return participant;
    }

    /**
     * 
     * @see javax.media.rtp.RTPStream#getSenderReport()
     */
    public SenderReport getSenderReport() {
        return null;
    }

    /**
     * 
     * @see javax.media.rtp.RTPStream#getSSRC()
     */
    public long getSSRC() {
        return ssrc;
    }

    /**
     * 
     * @see javax.media.rtp.RTPStream#getDataSource()
     */
    public DataSource getDataSource() {
        return dataSource;
    }
    
    private void writeHeaderToBuffer(boolean marker, long timestamp) {
        
        // Write the marker bit and packet type
        buffer[0] = HEADER0;
        buffer[1] = (byte) (format & BYTE_TO_INT_MASK);
        if (marker) {
            buffer[1] |= MARKER_MASK;
        }
        lastSequence++;
        if (lastSequence > RTPHeader.MAX_SEQUENCE) {
            lastSequence = 0;
        }
        buffer[SEQ_BYTE_1] = (byte) ((lastSequence >> SHORT_TO_BYTE_1_SHIFT)
                & BYTE_TO_INT_MASK);
        buffer[SEQ_BYTE_2] = (byte) (lastSequence & BYTE_TO_INT_MASK);
        buffer[TIME_BYTE_1] = (byte) ((timestamp >> INT_TO_BYTE_1_SHIFT)
                & BYTE_TO_INT_MASK);
        buffer[TIME_BYTE_2] = (byte) ((timestamp >> INT_TO_BYTE_2_SHIFT)
                & BYTE_TO_INT_MASK);
        buffer[TIME_BYTE_3] = (byte) ((timestamp >> INT_TO_BYTE_3_SHIFT)
                & BYTE_TO_INT_MASK);
        buffer[TIME_BYTE_4] = (byte) (timestamp & BYTE_TO_INT_MASK);
        buffer[SSRC_BYTE_1] = (byte) ((ssrc >> INT_TO_BYTE_1_SHIFT)
                & BYTE_TO_INT_MASK);
        buffer[SSRC_BYTE_2] = (byte) ((ssrc >> INT_TO_BYTE_2_SHIFT)
                & BYTE_TO_INT_MASK);
        buffer[SSRC_BYTE_3] = (byte) ((ssrc >> INT_TO_BYTE_3_SHIFT)
                & BYTE_TO_INT_MASK);
        buffer[SSRC_BYTE_4] = (byte) (ssrc & BYTE_TO_INT_MASK);
    }

    /**
     * 
     * @see javax.media.protocol.SourceTransferHandler#transferData(
     *     javax.media.protocol.PushSourceStream)
     */
    public void transferData(PushSourceStream stream) {
        if (!stream.endOfStream()) {
            int size = stream.getMinimumTransferSize();
            if (buffer.length < size + RTPHeader.SIZE) {
                buffer = new byte[size + RTPHeader.SIZE];
            }
            try {
                int length = stream.read(buffer, RTPHeader.SIZE, 
                        buffer.length - RTPHeader.SIZE);
                if (length > 0) {
                    long time = System.currentTimeMillis();
                    if (lastSendTime != -1) {
                        lastTimestamp += 
                            ((time - lastSendTime) * clockRate) / MS_TO_SECS;
                    }
                    lastSendTime = time;
                    writeHeaderToBuffer(false, lastTimestamp);
                    rtpDataStream.write(buffer, 0, length + RTPHeader.SIZE);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 
     * @see javax.media.protocol.BufferTransferHandler#transferData(
     *     javax.media.protocol.PushBufferStream)
     */
    public void transferData(PushBufferStream stream) {
        if (!stream.endOfStream()) {
            try {
                Buffer recvBuffer = new Buffer();
                recvBuffer.setData(buffer);
                recvBuffer.setOffset(RTPHeader.SIZE);
                recvBuffer.setLength(buffer.length - RTPHeader.SIZE);
                stream.read(recvBuffer);
                if (recvBuffer.getLength() > 0) {
                    long time = System.currentTimeMillis();
                    if (lastSendTime != -1) {
                        lastTimestamp += 
                            ((time - lastSendTime) * clockRate) / MS_TO_SECS;
                    }
                    lastSendTime = time;
                    writeHeaderToBuffer(false, lastTimestamp);
                    rtpDataStream.write(buffer, 0, 
                            recvBuffer.getLength() + RTPHeader.SIZE);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Returns the source description for this source
     * @return The source description objects
     */
    public Vector<SourceDescription> getSourceDescription() {
        return new Vector<SourceDescription>(sourceDescriptions.values());
    }
    
    /**
     * Returns the number of bytes of SDES that this participant requires
     * @return the size of the SDES in bytes
     */
    public int getSdesSize() {
        return sdesSize;
    }
    
    /**
     * Returns the last time a packet was sent
     * @return the last send time
     */
    public long getLastSendTime() {
        return lastSendTime;
    }
    
    /**
     * Returns the last timestamp of a packet sent
     * @return the last time stamp
     */
    public long getLastTimestamp() {
        return lastTimestamp;
    }

}
