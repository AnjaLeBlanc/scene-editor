/*
 * @(#)StreamSource.java
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

package rtspd;

import java.io.BufferedInputStream;
//import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
//import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;

/**
 * A Stream to be played back
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class StreamSource implements Comparable<StreamSource> {

    // The maximum NTP LSW
    private static final long MAX_NTP_LSW = 0x100000000L;

    // The 4th position
    private static final int POS_4 = 3;

    // The 3rd position
    private static final int POS_3 = 2;

    // The 2nd position
    private static final int POS_2 = 1;

    // The 1st position
    private static final int POS_1 = 0;

    // The maximum sequence wrap divisor
    private static final int MAX_WRAP_DIVISOR = 2;

    // The number of usecs per ms
    private static final int USECS_PER_MS = 1000;

    // The number of ms per second
    private static final int MS_PER_SEC = 1000;

    // The mask for a short from an int
    private static final int SHORT_MASK = 0x0000FFFF;

    // The second last item in an array
    private static final int SECOND_LAST = 2;

    private static final String EXCEPTION_MESSAGE = "Exception ";

    // The start position of the octet count in the RTCP Sender Report
    private static final int RTCP_OCTET_COUNT_START = 24;

    // The start position of the packet count in the RTCP Sender Report
    private static final int RTCP_PACKET_COUNT_START = 20;

    // The start position of the timestamp in the RTCP Sender report
    private static final int RTCP_TIMESTAMP_START = 16;

    // The start position of the NTP LSW in the RTCP Sender report
    private static final int RTCP_LSW_START = 12;

    // The start position of the NTP MSW in the RTCP Sender report
    private static final int RTCP_MSW_START = 8;

    // The start position of the SSRC in the RTP packet
    private static final int PACKET_SSRC_START = 8;

    // The start position of the timestamp in the RTP packet
    private static final int PACKET_TIMESTAMP_START = 4;

    // The start position of the sequence number in the RTP packet
    private static final int PACKET_SEQUENCE_START = 2;

    // The amount of delay between packets when paused
    private static final int PAUSE_DELAY = 1000;

    // The size of an IP address in bytes
    private static final int IP_ADDRESS_SIZE = 4;

    // The prefix to add to name of the recorded streams
    private static final String RECORD_PREFIX = "*R* ";

    // The first time an RTCP packet should be sent out
    private static final long RTCP_TIMER_FIRST = 2000;

    // The time between RTCP packets
    private static final long RTCP_TIMER_OFFSET = 5000;

    // The maximum packet timestamp
    private static final long MAX_TIMESTAMP = 0xFFFFFFFFL;

    // The minimum time before a packet is scheduled
    private static final long MIN_DELAY = 10;

    // The maximum time before a packet is dropped
    private static final long MAX_NEG_DELAY = -500;

    // The log file
    private static Log logger = LogFactory.getLog(StreamSource.class.getName());

    // The last timestamp seen
    private long lastPacketTimestamp = 0;

    // The current packet timestamp
    private long packetTimestamp = 0;

    // The timestamp to send in the next packet
    private long currentTimestamp = 0;

    // The last time a packet was sent
    private long lastSendTime = 0;

    // The new ssrc of the stream
    private long newSSRC = 0;

    // The new sequence number of the packetsatagramPacket packe
    private int newSequence = 0;

    // The current packet group to send
    private Vector<ReusablePacket> packets = new Vector<ReusablePacket>(0);

    // The last packet sent
    private byte[] lastRTCPPacketBuffer = new byte[0];

    // True if the end of the file has been reached
    private boolean qEof = false;

    // True if there has been an error
    private boolean qError = false;

    // True if an RTP packet was foundtime in the file
    private boolean qFoundRtpPacket = false;

    // The data channel of the stream file
    private DataInputStream streamFile = null;

    // The control channel of the stream file
    private FileChannel streamFileControl = null;

    // The ssrc of the stream file
    private String streamSpec = "";

    // The first timestamp in this stream
    private long startTime = 0;

    // The synchronizer of the stream
    private PlayStreamSynchronizer synch = null;

    // The transport to send the stream
    private PlaybackNetworkTransport netTrans = null;

    // A timer for sending packets at the right time
    private Timer timer = new Timer();

    // The type of the stream
    private int type = 0;

    // The length of the current group of packetsatagramPacket packe
    private Vector<Integer> lengths = new Vector<Integer>();

    // The offset of the current packet from the first packet
    private Vector<Long> offsets = new Vector<Long>();

    // The offset of the first sent packet
    private long firstOffset = 0;

    // The amount by which the offset is to be shifted
    private long offsetShift = 0;

    // The path to the files to be played
    private String sFilePath = "";

    // The time at which all streams should start in clock time
    private long allStartTime = 0;

    // True if the first packet has been sent
    private boolean firstPacketSent = false;

    // The speed at which packets are played back
    private double scale = 1.0;

    // A vector of positions of packets in the stream file
    private Vector<Long> packetPositions = new Vector<Long>();

    // A vector of offsets of packets in the stream file
    private Vector<Long> packetOffsets = new Vector<Long>();

    // The current position in the packetPositions vector
    private int currentPos = 0;

    // The number of packets sent
    private int packetCount = 0;

    // The number of octets sent
    private int octetCount = 0;

    // True if the index file has started being read
    private boolean indexFileReadBegun = false;

    // True if the index file has finished being read
    private boolean indexFileReadDone = false;

    // An object to synchronize on for the reading of the index file
    private Integer indexFileSync = new Integer(0);

    // True if the stream has been terminated
    private boolean terminated = false;

    // The lowest sequence number in the current block
    private int lowestSequence = 0;

    /**
     * Creates a new StreamSource
     *
     * @param sFilePath
     *            The path of the files to be played back
     */
    public StreamSource(String sFilePath) {
        this.sFilePath = sFilePath;
    }

    /**
     * Returns after the first packet has been played
     *
     */
    public synchronized void waitForFirstPacket() {
        while (!firstPacketSent) {
            try {
                wait();
            } catch (InterruptedException e) {
                logger.error(EXCEPTION_MESSAGE, e);
            }
        }
    }

    // Notifies a reciever that the first packet has been sent
    private synchronized void notifyFirstPacket() {
        System.err.println("Notification of first packet");
        firstPacketSent = true;
        notifyAll();
    }

    /**
     * Handles the case where the timer ticks
     *
     * @param task
     *            The task to handle
     */
    public void handleTimeout(TimerTask task) {
        if (task instanceof CounterTimer) {

            // Do Nothing
        } else if (task instanceof UpdateTimer) {

            // Time to send the next packet{
            sendCurrentAndScheduleNextPacket();
        } else if (task instanceof RTCPTimer) {

            // Resend the last RTCP packet
            sendRTCPPacket();
        } else {

            // Do Nothing
        }
    }




    /**
     * Sets up the stream to be sent
     *
     * @param db
     *            The database containing stream metadata
     * @param setupRequest
     *            The request to setup the stream
     * @param synch
     *            The synchronizer of the stream
     * @param netTrans
     *            The transport to send the stream with
     * @param session The session containing the stream
     * @param streamId The id of the stream to play
     * @throws RTSPResponse
     */
    public void setup(RTSPSetupRequest setupRequest,
            PlayStreamSynchronizer synch, PlaybackNetworkTransport netTrans,
            Session session, String streamId)
            throws RTSPResponse {

        boolean qSuccess = false;
        HashMap<String, String> values =
            session.getStream(streamId).getValues();

        // Store values
        this.synch = synch;
        this.netTrans = netTrans;
        currentTimestamp = (long) (Math.random() * Integer.MAX_VALUE);
        newSSRC = netTrans.allocateSSRC(this);
        newSequence = (int) (Math.random() * RTPHeader.MAX_SEQUENCE);

        // Open a connection to the data and process some meta-info
        if (openStream(streamId) && readHeader()
                /*&& readInfoFile()*/) {
            qSuccess = true;
        }

        // Register this stream with the synchronizer
        if (qSuccess && (synch != null)) {
            synch.registerStream(this, startTime);
        } else {
            throw new RTSPResponse(Headers.RTSP_SERVER_ERROR,
                    "The stream file " + sFilePath + " could not be read",
                    setupRequest.getRequestPacket());
        }

        try {

            // Calculate the SDES length
            int sdesLength = 0;
            int sdesPadding = 0;
            RTCPHeader srHeader = null;
            RTCPHeader sdesHeader = null;
            int pos = 0;
            for (int i = 1; i < Stream.SDES_ID.length; i++) {
                String item = (String) values.get(Stream.SDES_ID[i]);
                if (item != null) {
                    byte[] itemBytes = null;

                    // Add a recorded indicator
                    if (i == RTCPHeader.SDES_NAME) {
                        item = RECORD_PREFIX + item;
                    }

                    // Add the length plus 2 for the id and length
                    itemBytes = item.getBytes(RTCPHeader.SDES_ENCODING);
                    sdesLength += itemBytes.length
                        + RTCPHeader.SDES_TYPE_LENGTH
                        + RTCPHeader.SDES_LENGTH_LENGTH;
                }
            }

            // Add 1 for the sdes null item
            sdesLength += 1;

            // Add 8 for the sdes header
            sdesLength += RTCPHeader.SIZE;
            sdesPadding = RTCPHeader.BYTES_PER_INT
                    - (sdesLength % RTCPHeader.BYTES_PER_INT);
            if (sdesPadding == RTCPHeader.BYTES_PER_INT) {
                sdesPadding = 0;
            }

            // Prepare an RTCP packet - length is 28 for header plus SR
            lastRTCPPacketBuffer =
                new byte[RTCPHeader.SIZE + RTCPSenderInfo.SIZE + sdesLength
                         + sdesPadding];
            srHeader =
                new RTCPHeader(false, 0, RTCPHeader.PT_SR,
                        RTCPHeader.getLength(
                                RTCPHeader.SIZE + RTCPSenderInfo.SIZE),
                                newSSRC);
            srHeader.addBytes(lastRTCPPacketBuffer, 0);

            // Add SDES Packet
            sdesHeader =
                new RTCPHeader(false, 1, RTCPHeader.PT_SDES,
                        RTCPHeader.getLength(sdesLength + sdesPadding),
                        newSSRC);
            sdesHeader.addBytes(lastRTCPPacketBuffer,
                    RTCPHeader.SIZE + RTCPSenderInfo.SIZE);
            sdesLength = ((sdesLength + sdesPadding)
                    / RTCPHeader.BYTES_PER_INT) - 1;

            // Add the SDES Items
            pos = RTCPHeader.SIZE + RTCPSenderInfo.SIZE + RTCPHeader.SIZE;
            for (int i = 1; i < Stream.SDES_ID.length; i++) {
                String item = (String) values.get(Stream.SDES_ID[i]);
                if (item != null) {
                    byte[] itemBytes = null;
                    // Add a recorded indicator
                    if (i == RTCPHeader.SDES_NAME) {
                        item = RECORD_PREFIX + item;
                    }
                    itemBytes = item.getBytes(RTCPHeader.SDES_ENCODING);
                    lastRTCPPacketBuffer[pos++] = (byte) i;
                    lastRTCPPacketBuffer[pos++] = (byte) itemBytes.length;
                    for (int j = 0; j < itemBytes.length; j++) {
                        lastRTCPPacketBuffer[pos++] = itemBytes[j];
                    }
                }
            }

            // Add the null item
            lastRTCPPacketBuffer[pos++] = 0;

            // Add the padding
            for (int i = 0; i < sdesPadding; i++) {
                lastRTCPPacketBuffer[pos++] = (byte) 0;
            }
        } catch (UnsupportedEncodingException e) {
            logger.error(EXCEPTION_MESSAGE, e);
        }
    }
//    public void setup(Database db, RTSPSetupRequest setupRequest,
//            PlayStreamSynchronizer synch, PlaybackNetworkTransport netTrans,
//            Session session, String streamId)
//            throws RTSPResponse {
//
//        boolean qSuccess = false;
//        HashMap values =
//            session.getStream(streamId).getValues();
//
//        // Store values
//        this.synch = synch;
//        this.netTrans = netTrans;
//        currentTimestamp = (long) (Math.random() * Integer.MAX_VALUE);
//        newSSRC = netTrans.allocateSSRC(this);
//        newSequence = (int) (Math.random() * RTPHeader.MAX_SEQUENCE);
//
//        // Open a connection to the data and process some meta-info
//        if (openStream(session.getId(), streamId) && readHeader()
//                && readInfoFile()) {
//            qSuccess = true;
//        }
//
//        // Register this stream with the synchronizer
//        if (qSuccess && (synch != null)) {
//            synch.registerStream(this, startTime);
//        } else {
//            throw new RTSPResponse(Headers.RTSP_SERVER_ERROR,
//                    "The stream file " + sFilePath + " could not be read",
//                    setupRequest.getRequestPacket());
//        }
//
//        try {
//
//            // Calculate the SDES length
//            int sdesLength = 0;
//            int sdesPadding = 0;
//            RTCPHeader srHeader = null;
//            RTCPHeader sdesHeader = null;
//            int pos = 0;
//            for (int i = 1; i < Stream.SDES_ID.length; i++) {
//                String item = (String) values.get(Stream.SDES_ID[i]);
//                if (item != null) {
//                    byte[] itemBytes = null;
//
//                    // Add a recorded indicator
//                    if (i == RTCPHeader.SDES_NAME) {
//                        item = RECORD_PREFIX + item;
//                    }
//
//                    // Add the length plus 2 for the id and length
//                    itemBytes = item.getBytes(RTCPHeader.SDES_ENCODING);
//                    sdesLength += itemBytes.length
//                        + RTCPHeader.SDES_TYPE_LENGTH
//                        + RTCPHeader.SDES_LENGTH_LENGTH;
//                }
//            }
//
//            // Add 1 for the sdes null item
//            sdesLength += 1;
//
//            // Add 8 for the sdes header
//            sdesLength += RTCPHeader.SIZE;
//            sdesPadding = RTCPHeader.BYTES_PER_INT
//                    - (sdesLength % RTCPHeader.BYTES_PER_INT);
//            if (sdesPadding == RTCPHeader.BYTES_PER_INT) {
//                sdesPadding = 0;
//            }
//
//            // Prepare an RTCP packet - length is 28 for header plus SR
//            lastRTCPPacketBuffer =
//                new byte[RTCPHeader.SIZE + RTCPSenderInfo.SIZE + sdesLength
//                         + sdesPadding];
//            srHeader =
//                new RTCPHeader(false, 0, RTCPHeader.PT_SR,
//                        RTCPHeader.getLength(
//                                RTCPHeader.SIZE + RTCPSenderInfo.SIZE),
//                                newSSRC);
//            srHeader.addBytes(lastRTCPPacketBuffer, 0);
//
//            // Add SDES Packet
//            sdesHeader =
//                new RTCPHeader(false, 1, RTCPHeader.PT_SDES,
//                        RTCPHeader.getLength(sdesLength + sdesPadding),
//                        newSSRC);
//            sdesHeader.addBytes(lastRTCPPacketBuffer,
//                    RTCPHeader.SIZE + RTCPSenderInfo.SIZE);
//            sdesLength = ((sdesLength + sdesPadding)
//                    / RTCPHeader.BYTES_PER_INT) - 1;
//
//            // Add the SDES Items
//            pos = RTCPHeader.SIZE + RTCPSenderInfo.SIZE + RTCPHeader.SIZE;
//            for (int i = 1; i < Stream.SDES_ID.length; i++) {
//                String item = (String) values.get(Stream.SDES_ID[i]);
//                if (item != null) {
//                    byte[] itemBytes = null;
//                    // Add a recorded indicator
//                    if (i == RTCPHeader.SDES_NAME) {
//                        item = RECORD_PREFIX + item;
//                    }
//                    itemBytes = item.getBytes(RTCPHeader.SDES_ENCODING);
//                    lastRTCPPacketBuffer[pos++] = (byte) i;
//                    lastRTCPPacketBuffer[pos++] = (byte) itemBytes.length;
//                    for (int j = 0; j < itemBytes.length; j++) {
//                        lastRTCPPacketBuffer[pos++] = itemBytes[j];
//                    }
//                }
//            }
//
//            // Add the null item
//            lastRTCPPacketBuffer[pos++] = 0;
//
//            // Add the padding
//            for (int i = 0; i < sdesPadding; i++) {
//                lastRTCPPacketBuffer[pos++] = (byte) 0;
//            }
//        } catch (UnsupportedEncodingException e) {
//            logger.error(EXCEPTION_MESSAGE, e);
//        }
//    }

    /**
     * Starts playback of the stream
     *
     * @param playRequest
     *            The request to start playback
     * @param baseTime
     *            The earliest startTime time of all streams to be played back
     * @param allStartTime
     *            The literal time at which the first stream should start to
     *            play
     */
    public void play(RTSPPlayRequest playRequest, long baseTime,
            long allStartTime) {
        logger.debug("Stream_Source::play: Got a play request for "
                + streamSpec);

        // Wait until the index file has been read
        synchronized (indexFileSync) {
            if (!indexFileReadBegun) {
                readIndexFile();
            }
            while (!indexFileReadDone && !terminated) {
                try {
                    indexFileSync.wait();
                } catch (InterruptedException e) {

                    // Do Nothing
                }
            }
        }

        // Calculate new start time, so that we're aligned w/other streams in
        // this session
        qEof = false;
        qError = false;
        qFoundRtpPacket = false;
        firstPacketSent = false;
        terminated = false;
        offsetShift = startTime - baseTime;
        this.allStartTime = allStartTime;
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new RTCPTimer(this), RTCP_TIMER_FIRST,
                RTCP_TIMER_OFFSET);
        lastPacketTimestamp = 0;

        logger.debug("Stream_Source::play: SSRC = " + newSSRC + ", Offset = "
                + offsetShift);

        scale = playRequest.getScale();

        // If the scale is 0, we just need to send RTCP packets to say we are
        // still here
        if (scale != 0) {

            // Seek the stream to the starting position
            long seek = playRequest.getStartingOffset();
            long playoutDelay = 0;
            if (seek != -1) {
                streamSeek(seek);
            }

            // Get the first packet
            while (!qFoundRtpPacket && !qEof) {
                readPacket();
                if (qError || qEof) {
                    terminateStream();
                    return;
                }
            }

            // Calculate the initial delay for the first packet
            playoutDelay = computePlayoutDelay();
            logger.debug(newSSRC + ": Initial Delay is " + playoutDelay
                    + ", firstOffset = " + firstOffset);

            // Play the stream
            if (playoutDelay > MIN_DELAY) {
                scheduleTimer(playoutDelay);
            } else {
                sendCurrentAndScheduleNextPacket();
            }
        }
    }

    public void play(client.rtsp.RTSPPlayRequest playRequest, long baseTime,
            long allStartTime) {
        System.out.println("debug enabled ? " + logger.isDebugEnabled());
        logger.debug("Stream_Source::play: Got a play request for "
                + streamSpec);

        // Wait until the index file has been read
        synchronized (indexFileSync) {
            if (!indexFileReadBegun) {
                readIndexFile();
            }
            while (!indexFileReadDone && !terminated) {
                try {
                    indexFileSync.wait();
                } catch (InterruptedException e) {

                    // Do Nothing
                }
            }
        }

        // Calculate new start time, so that we're aligned w/other streams in
        // this session
        qEof = false;
        qError = false;
        qFoundRtpPacket = false;
        firstPacketSent = false;
        terminated = false;
        offsetShift = startTime - baseTime;
        this.allStartTime = allStartTime;
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new RTCPTimer(this), RTCP_TIMER_FIRST,
                RTCP_TIMER_OFFSET);
        lastPacketTimestamp = 0;

        logger.debug("Stream_Source::play: SSRC = " + newSSRC + ", Offset = "
                + offsetShift);

        scale = Double.parseDouble(playRequest.getHeaderString(Headers.RTSP_SCALE));
        System.out.println("scale " + scale);

        // If the scale is 0, we just need to send RTCP packets to say we are
        // still here
        if (scale != 0) {

            // Seek the stream to the starting position
            long seek = playRequest.getStartingOffset();
            System.out.println("starting offset " + seek);
            long playoutDelay = 0;
            if (seek != -1) {
                streamSeek(seek);
            }

            // Get the first packet
            while (!qFoundRtpPacket && !qEof) {
                readPacket();
                if (qError || qEof) {
                    terminateStream();
                    return;
                }
            }

            // Calculate the initial delay for the first packet
            playoutDelay = computePlayoutDelay();
            logger.debug(newSSRC + ": Initial Delay is " + playoutDelay
                    + ", firstOffset = " + firstOffset);

            // Play the stream
            if (playoutDelay > MIN_DELAY) {
                scheduleTimer(playoutDelay);
            } else {
                sendCurrentAndScheduleNextPacket();
            }
        }
    }


    /**
     * Stops the playback of the stream
     *
     */
    public void teardown() {
        terminated = true;
        if (synch != null) {
            synch.unregisterStream(this);
            synch = null;
        }
        cancelTimers();
        sendBye();
        for (int i = 0; i < packets.size(); i++) {
            ReusablePacket packet = (ReusablePacket) packets.get(i);
            packet.release();
        }
    }

    // Stops the timers
    private void cancelTimers() {
        if (timer != null) {
            timer.cancel();
        }
    }

    // Searches through the stream for the first packet to play after the given
    // time
    private void streamSeek(long seek) {
        long actualSeek = seek - offsetShift;
        int offsetPos = 0;
        firstOffset = seek;
        offsetPos = Collections.binarySearch(packetOffsets, new Long(
                actualSeek));
        System.out.println("offset " + offsetPos + " firstOffset" + firstOffset);
        if (offsetPos < 0) {
            offsetPos = (-1 * offsetPos) + 1;
        }
        currentPos = offsetPos;
        System.out.println("currentPos " + currentPos);
        try {
            if ((currentPos >= (packetOffsets.size() - 1)) && (scale < 0)) {
                currentPos = packetOffsets.size() - SECOND_LAST;
                int packetType = 1;
                while (packetType != 0) {
                    long pos = ((Long) packetPositions.get(currentPos))
                            .longValue();
                    System.out.println("pos "  + pos);
                    streamFileControl.position(pos);
                    streamFile.readShort();
                    packetType = streamFile.readShort() & SHORT_MASK;
                    System.out.println("packetType " + packetType);
                    currentPos--;
                }
            }
        } catch (IOException e) {
            logger.error(EXCEPTION_MESSAGE, e);
            System.err.println(e.toString());
            qEof = true;
        }
    }

//    // Open the stream file
//    private boolean openStream(String sessionId, String streamId) {
//        boolean qSuccess = false;
//        try {
//
//            // Gather data needed to retrieve from the db the name of the file
//            // that contains stream data
//            String host = InetAddress.getLocalHost().getHostName();
//            String slash = System.getProperty("file.separator");
//
//            logger.debug("Stream_Source::openStream: host " + host
//                    + ", sessionId " + sessionId + ", streamId " + streamId);
//            streamSpec = sFilePath + slash + sessionId + slash + streamId;
//
//            // Open the file for reading
//            if (openStreamFile(streamSpec)) {
//                logger.debug("Stream_Source::openStream: opened stream file");
//                qSuccess = true;
//            } else {
//                logger.debug("Stream_Source::openStream:  failed to open "
//                                + "stream file\n");
//            }
//        } catch (UnknownHostException e) {
//            logger.error(EXCEPTION_MESSAGE, e);
//        }
//
//        return qSuccess;
//    }

    // Open the stream file
    private boolean openStream(String streamId) {
        boolean qSuccess = false;
        try {

            // Gather data needed to retrieve from the db the name of the file
            // that contains stream data
            String host = InetAddress.getLocalHost().getHostName();
            String slash = System.getProperty("file.separator");

            logger.debug("Stream_Source::openStream: host " + host
                    + ", sessionId " + streamId);
            streamSpec = sFilePath + slash + streamId;

            // Open the file for reading
            if (openStreamFile(streamSpec)) {
                logger.debug("Stream_Source::openStream: opened stream file");
                qSuccess = true;
            } else {
                logger.debug("Stream_Source::openStream:  failed to open "
                                + "stream file\n");
            }
        } catch (UnknownHostException e) {
            logger.error(EXCEPTION_MESSAGE, e);
        }

        return qSuccess;
    }

    // Actually open the stream file
    private boolean openStreamFile(String filename) {
        boolean qSuccess = true;

        try {
            FileInputStream stream = new FileInputStream(filename);
            logger.debug("Stream Spec: <" + filename + ">");
            streamFile = new DataInputStream(stream);
            streamFileControl = stream.getChannel();
        } catch (IOException e) {
            logger.debug("Stream_Source::openStreamFile: bad open " + filename);
            logger.error(EXCEPTION_MESSAGE, e);
            qSuccess = false;
        }

        return qSuccess;
    }

    // Read the header from the stream file
    private boolean readHeader() {
        boolean qSuccess = true;

        try {

            // Read the start time of the stream
            long seconds =
                (streamFile.readInt() & RTPHeader.UINT_TO_LONG_CONVERT);
            long uSeconds =
                (streamFile.readInt() & RTPHeader.UINT_TO_LONG_CONVERT);

            byte[] addr = new byte[IP_ADDRESS_SIZE];
            startTime = (seconds * MS_PER_SEC) + (uSeconds / USECS_PER_MS);

            // Read the sender of the original stream
            streamFile.read(addr, 0, IP_ADDRESS_SIZE);
            streamFile.readUnsignedShort();
        } catch (IOException e) {
            logger.error(EXCEPTION_MESSAGE, e);
            qSuccess = false;
        }

        return qSuccess;
    }

    /**
     * Tells the source to read it's index file in preparation for a play
     *
     */
    public void readIndexFile() {
        try {

            synchronized (indexFileSync) {
                indexFileReadBegun = true;
                indexFileReadDone = false;
            }

            // Open the index file
            String filename = streamSpec + Stream.INDEX_FILE_EXTENSION;
            DataInputStream indexFile = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(filename)));

            try {
                while (true) {
                    long off = indexFile.readLong();
                    long pos = indexFile.readLong();

                    packetOffsets.add(new Long(off));
                    packetPositions.add(new Long(pos));
                }
            } catch (EOFException e) {
                indexFile.close();
            }
            synchronized (indexFileSync) {
                indexFileReadDone = true;
                indexFileSync.notify();
            }
        } catch (IOException e) {
            logger.error(EXCEPTION_MESSAGE, e);
        }

    }

//    // Read the metadata file -- not used anymore I think
//    private boolean readInfoFile() {
//        boolean qSuccess = true;
//
//        try {
//
//            // Open the file
//            BufferedReader infoFile = new BufferedReader(
//                    new InputStreamReader(new FileInputStream(streamSpec
//                            + Stream.INFO_FILE_EXTENSION)));
//
//            // Go through each line in the file
//            String line = infoFile.readLine();
//            while ((line != null) && qSuccess) {
//
//                // Split the line into key, value
//                String[] parts = line.split(Stream.INFO_ITEM_SEPARATOR);
//
//                // Deal with the key and value
//                if (parts.length > 1) {
//                    qSuccess = handleInfoItem(parts[0].trim(), parts[1].trim());
//                }
//                line = infoFile.readLine();
//            }
//            infoFile.close();
//        } catch (IOException e) {
//            logger.error(EXCEPTION_MESSAGE, e);
//            qSuccess = false;
//        }
//        return qSuccess;
//    }

//    // Handle an item of information from the info file
//    private boolean handleInfoItem(String tag, String value) {
//        boolean qSuccess = true;
//
//        if (tag.equals(Stream.TYPES)) {
//
//            // Do Nothing
//        }
//
//        return qSuccess;
//    }

    // Read a packet from the stream file
    private void readPacket() {
        ReusablePacket packet = null;
        try {
            long pos = 0;
            long lastReadTimestamp = -1;
            lengths.clear();
            packets.clear();
            offsets.clear();
            qError = false;
            qFoundRtpPacket = false;
            qEof = false;

            // Move into the next position
            if (currentPos >= packetPositions.size() || currentPos < 0) {
                qEof = true;
                System.err.println("qEof = true " + currentPos + " "
                		+ packetPositions.size());
                return;
            }
            pos = ((Long) packetPositions.get(currentPos)).longValue();
            streamFileControl.position(pos);
            currentPos += Double.valueOf(scale).intValue();
            lowestSequence = -1;

            // Read packets while the timestamps are the same
            while (!qFoundRtpPacket || (lastReadTimestamp == packetTimestamp)) {

                // Read packet header
                long offset = 0;
                packet = ReusablePacketQueue.getInstance().getFreePacket();
                byte[] packetBuffer = packet.getPacket().getData();
                int length =
                    streamFile.readShort() & RTPHeader.USHORT_TO_INT_CONVERT;
                type =
                    streamFile.readShort() & RTPHeader.USHORT_TO_INT_CONVERT;
                offset =
                    streamFile.readInt() & RTPHeader.UINT_TO_LONG_CONVERT;
                offset += offsetShift;

                // calculate packet body size and read it
                streamFile.readFully(packetBuffer, 0, length);

                // If this is an RTP packet, set it up to be read
                if (type == Stream.RTP_PACKET) {
                    RTPHeader header = new RTPHeader(packetBuffer, 0, length);
                    packetTimestamp = header.getTimestamp();
                    if ((packetTimestamp == lastReadTimestamp)
                            || (lastReadTimestamp == -1)) {
                        int sequence = header.getSequence();
                        lengths.add(new Integer(length));
                        packets.add(packet);
                        offsets.add(new Long(offset));
                        lastReadTimestamp = packetTimestamp;
                        if ((sequence < lowestSequence)
                                || (lowestSequence == -1)) {
                            lowestSequence = sequence;
                        }
                    } else {
                        packet.release();
                    }
                    qFoundRtpPacket = true;
                } else {
                    packet.release();
                }
            }
        } catch (EOFException e) {
            System.err.println(e.toString());
            qEof = true;
        } catch (IOException e) {
            logger.error(EXCEPTION_MESSAGE, e);
            e.printStackTrace();
            qError = true;
        }
        if (qEof || qError) {
            if (packet != null) {
                packet.release();
            }
            for (int i = 0; i < packets.size(); i++) {
                ReusablePacket p = (ReusablePacket) packets.get(i);
                p.release();
            }
        }
    }

    // Calculate the delay for the next packet
    private long computePlayoutDelay() {

        long timeOffset = 0;
        long delay = 0;

        // If the scale is stopped, pause by 5 second
        if (scale == 0) {
            return PAUSE_DELAY;
        }

        // Get the current offset
        timeOffset = System.currentTimeMillis() - allStartTime;
        timeOffset = (long) (timeOffset * scale);

        // Calculate the delay before sending the next packet
        delay = (((Long) offsets.get(0)).longValue() - firstOffset)
                - timeOffset;
        delay = (long) (delay / scale);

        return delay;
    }

    // Set up a timer to play the next packet
    private void scheduleTimer(long playoutDelay) {
        if (!terminated) {
            timer.schedule(new UpdateTimer(this), playoutDelay);
        }
    }

    // Send the current packet and prepare the next one
    private void sendCurrentAndScheduleNextPacket() {

        if (terminated) {
            return;
        }

        // Send out the packet that's waiting
        sendPacket();
        boolean qTimerScheduled = false;

        // Execute this loop until we've queued up the next packet
        while (!qTimerScheduled && !qEof && !qError) {

            // Read the next packet
            readPacket();

            if (qError) {
                terminateStream();

                // We read some data; look at what we have
            } else if (!qEof) {
                if (qFoundRtpPacket) {
                    long playoutDelay = 0;

                    // If this packet is the same as the last, send it now
                    if ((packetTimestamp == lastPacketTimestamp)
                            && (scale != 0)) {
                        sendPacket();
                        continue;
                    }

                    // Work out the delay for the next packet
                    playoutDelay = computePlayoutDelay();

                    // Schedule a timer for sending the packet if the delay is
                    // greater than 10ms. This stops the loop.
                    if (playoutDelay > MIN_DELAY) {
                        scheduleTimer(playoutDelay);
                        qTimerScheduled = true;

                    } else if (playoutDelay < MAX_NEG_DELAY) {

                        // Skip old packets
                        logger.debug(newSSRC + ": Skipping Late Packet ("
                                + playoutDelay + ")");

                        for (int i = 0; i < packets.size(); i++) {
                            ReusablePacket p = (ReusablePacket) packets.get(i);
                            p.release();
                        }
                        continue;
                    } else {

                        // send it right away.
                        sendPacket();
                    }
                } else {

                    // the data we read was an RTCP packet. Ignore it
                }
            } else {

                // Stream has finished. Clean up
                terminateStream();

                // Continue to send RTCP packets to keep the stream alive
                timer = new Timer();
                timer.schedule(new RTCPTimer(this), RTCP_TIMER_FIRST,
                        RTCP_TIMER_OFFSET);
            }
        }
    }

    // Sends a packet
    private void sendPacket() {

        // Notify that the first packet has been sent
        if (!firstPacketSent) {
            notifyFirstPacket();
        }

        // If the packet is an RTP packet, play it
        if (type == Stream.RTP_PACKET) {
            long firstPacketOffset = 0;
            long firstSendTime = 0;
            int lastSequence = lowestSequence - 1;

            // Set the timestamp
            long diffFromMax = MAX_TIMESTAMP - currentTimestamp;
            if (lastPacketTimestamp != 0) {
                long timestampDiff = packetTimestamp - lastPacketTimestamp;
                if (scale > 0) {
                    timestampDiff /= scale;
                } else if (scale < 0) {
                    timestampDiff /= -scale;
                }
                if (diffFromMax < timestampDiff) {
                    currentTimestamp = timestampDiff - diffFromMax;
                } else {
                    currentTimestamp += timestampDiff;
                }
            } else {
                if (diffFromMax == 0) {
                    currentTimestamp = 0;
                } else {
                    currentTimestamp += 1;
                }
            }

            lastSendTime = System.currentTimeMillis();
            firstPacketOffset = ((Long) offsets.get(0)).longValue();
            firstSendTime = System.currentTimeMillis();
            for (int i = 0; (i < lengths.size()) && !terminated; i++) {
                int length = ((Integer) lengths.get(i)).intValue();
                long currentOffset = ((Long) offsets.get(i)).longValue();
                ReusablePacket packet = (ReusablePacket) packets.get(i);
                try {
                    byte[] packetBuffer = packet.getPacket().getData();
                    int currentSequence = ((packetBuffer[PACKET_SEQUENCE_START]
                            & RTCPHeader.INT_TO_BYTE)
                            << RTCPHeader.SHORT1_TO_BYTE_SHIFT)
                            | ((packetBuffer[PACKET_SEQUENCE_START + 1]
                            & RTCPHeader.INT_TO_BYTE)
                            << RTCPHeader.SHORT2_TO_BYTE_SHIFT);
                    long waitTime = 0;
                    int diffSequence = currentSequence - lastSequence;
                    if (diffSequence < -(RTPHeader.MAX_SEQUENCE
                            / MAX_WRAP_DIVISOR)) {
                        diffSequence = (RTPHeader.MAX_SEQUENCE - lastSequence)
                            + currentSequence;
                    }
                    newSequence += diffSequence;
                    if (newSequence > RTPHeader.MAX_SEQUENCE) {
                        int diffFromMaxSeq = newSequence
                                              - RTPHeader.MAX_SEQUENCE;
                        newSequence = diffFromMaxSeq;
                    }
                    lastSequence = currentSequence;

                    waitTime = (currentOffset - firstPacketOffset)
                        - (System.currentTimeMillis() - firstSendTime);
                    waitTime /= scale;
                    if (scale < 0) {
                        waitTime = -waitTime;
                    }
                    if (waitTime > MIN_DELAY) {
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException e) {
                            // Do Nothing
                        }
                    }

                    packetBuffer[PACKET_TIMESTAMP_START + POS_1] =
                        (byte) ((currentTimestamp
                                >> RTCPHeader.INT1_TO_BYTE_SHIFT)
                                & RTCPHeader.INT_TO_BYTE);
                    packetBuffer[PACKET_TIMESTAMP_START + POS_2] =
                        (byte) ((currentTimestamp
                                >> RTCPHeader.INT2_TO_BYTE_SHIFT)
                                & RTCPHeader.INT_TO_BYTE);
                    packetBuffer[PACKET_TIMESTAMP_START + POS_3] =
                        (byte) ((currentTimestamp
                                >> RTCPHeader.INT3_TO_BYTE_SHIFT)
                                & RTCPHeader.INT_TO_BYTE);
                    packetBuffer[PACKET_TIMESTAMP_START + POS_4] =
                        (byte) ((currentTimestamp
                                >> RTCPHeader.INT4_TO_BYTE_SHIFT)
                                & RTCPHeader.INT_TO_BYTE);

                    // Remember items for next time
                    if (lastPacketTimestamp != packetTimestamp) {
                        lastPacketTimestamp = packetTimestamp;
                    }

                    // Set the ssrc
                    packetBuffer[PACKET_SSRC_START + POS_1] =
                        (byte) ((newSSRC >> RTCPHeader.INT1_TO_BYTE_SHIFT)
                                & RTCPHeader.INT_TO_BYTE);
                    packetBuffer[PACKET_SSRC_START + POS_2] =
                        (byte) ((newSSRC >> RTCPHeader.INT2_TO_BYTE_SHIFT)
                                & RTCPHeader.INT_TO_BYTE);
                    packetBuffer[PACKET_SSRC_START + POS_3] =
                        (byte) ((newSSRC >> RTCPHeader.INT3_TO_BYTE_SHIFT)
                                & RTCPHeader.INT_TO_BYTE);
                    packetBuffer[PACKET_SSRC_START + POS_4] =
                        (byte) ((newSSRC >> RTCPHeader.INT4_TO_BYTE_SHIFT)
                                & RTCPHeader.INT_TO_BYTE);

                    // Set the sequence number
                    packetBuffer[PACKET_SEQUENCE_START] =
                        (byte) ((newSequence
                                >> RTCPHeader.SHORT1_TO_BYTE_SHIFT)
                                & RTCPHeader.INT_TO_BYTE);
                    packetBuffer[PACKET_SEQUENCE_START + 1] =
                        (byte) ((newSequence
                                >> RTCPHeader.SHORT2_TO_BYTE_SHIFT)
                                & RTCPHeader.INT_TO_BYTE);

                    packet.getPacket().setLength(length);
                    if (!terminated) {
                        netTrans.playRtpPacket(packet.getPacket());
                    }
                    packet.release();
                    synch.setCurrentTime(currentOffset);
                    packetCount++;
                    octetCount += length - RTPHeader.SIZE;
                } catch (Exception e) {
                    packet.release();
                    logger.error(EXCEPTION_MESSAGE, e);
                }
            }
        }
    }

    // Sends a packet
    private void sendRTCPPacket() {
        long ntpMSW = lastSendTime / MS_PER_SEC;
        long ntpLSW = ((lastSendTime % MS_PER_SEC)
                * MAX_NTP_LSW) / MS_PER_SEC;
        DatagramPacket packet = null;

        // Notify that the first packet has been sent
        if (!firstPacketSent) {
            notifyFirstPacket();
        }

        // Add the NTP timestamp and the RTP time
        lastRTCPPacketBuffer[RTCP_MSW_START + POS_1] =
            (byte) ((ntpMSW >> RTCPHeader.INT1_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_MSW_START + POS_2] =
            (byte) ((ntpMSW >> RTCPHeader.INT2_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_MSW_START + POS_3] =
            (byte) ((ntpMSW >> RTCPHeader.INT3_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_MSW_START + POS_4] =
            (byte) ((ntpMSW >> RTCPHeader.INT4_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_LSW_START + POS_1] =
            (byte) ((ntpLSW >> RTCPHeader.INT1_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_LSW_START + POS_2] = (
                byte) ((ntpLSW >> RTCPHeader.INT2_TO_BYTE_SHIFT)
                        & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_LSW_START + POS_3] =
            (byte) ((ntpLSW >> RTCPHeader.INT3_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_LSW_START + POS_4] =
            (byte) ((ntpLSW >> RTCPHeader.INT4_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_TIMESTAMP_START + POS_1] =
            (byte) ((currentTimestamp >> RTCPHeader.INT1_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_TIMESTAMP_START + POS_2] =
            (byte) ((currentTimestamp >> RTCPHeader.INT2_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_TIMESTAMP_START + POS_3] =
            (byte) ((currentTimestamp >> RTCPHeader.INT3_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_TIMESTAMP_START + POS_4] =
            (byte) ((currentTimestamp >> RTCPHeader.INT4_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);

        // Add the counts
        lastRTCPPacketBuffer[RTCP_PACKET_COUNT_START + POS_1] =
            (byte) ((packetCount >> RTCPHeader.INT1_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_PACKET_COUNT_START + POS_2] =
            (byte) ((packetCount >> RTCPHeader.INT2_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_PACKET_COUNT_START + POS_3] =
            (byte) ((packetCount >> RTCPHeader.INT3_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_PACKET_COUNT_START + POS_4] =
            (byte) ((packetCount >> RTCPHeader.INT4_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_OCTET_COUNT_START + POS_1] =
            (byte) ((octetCount >> RTCPHeader.INT1_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_OCTET_COUNT_START + POS_2] =
            (byte) ((octetCount >> RTCPHeader.INT2_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_OCTET_COUNT_START + POS_3] =
            (byte) ((octetCount >> RTCPHeader.INT3_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);
        lastRTCPPacketBuffer[RTCP_OCTET_COUNT_START + POS_4] =
            (byte) ((octetCount >> RTCPHeader.INT4_TO_BYTE_SHIFT)
                    & RTCPHeader.INT_TO_BYTE);

        // Send the packet
        packet = new DatagramPacket(lastRTCPPacketBuffer,
                lastRTCPPacketBuffer.length);
        netTrans.playRtcpPacket(packet);
    }

    // Stop the stream
    private void terminateStream() {
        System.err.println("terminate ");
        cancelTimers();
        System.err.println("Timers cancelled");
        if (!firstPacketSent) {
            System.err.println("Notifying");
            notifyFirstPacket();
        }
        logger.debug("Stream Finished");
    }

    // Sends a bye packet for this stream
    private void sendBye() {
        byte[] bye = new byte[lastRTCPPacketBuffer.length + RTCPHeader.SIZE];
        RTCPHeader byeHeader =
            new RTCPHeader(false, 1, RTCPHeader.PT_BYE, 1, newSSRC);
        System.arraycopy(lastRTCPPacketBuffer, 0, bye, 0,
                lastRTCPPacketBuffer.length);
        byeHeader.addBytes(bye, lastRTCPPacketBuffer.length);

        lastRTCPPacketBuffer = bye;
        sendRTCPPacket();
    }

    /**
     * Pauses the playback of the stream
     *
     * @param off
     *            The offset at which to pause the streams
     */
    public void pause(long off) {
        terminated = true;
        terminateStream();
        timer = new Timer();
        timer.schedule(new RTCPTimer(this), RTCP_TIMER_FIRST,
                        RTCP_TIMER_OFFSET);
    }

    /**
     * Changes the ssrc of the stream due to a clash
     * @param ssrc The new ssrc
     *
     */
    public void changeSSRC(long ssrc) {
        sendBye();
        newSSRC = ssrc;
    }

    /**
     * Returns the network transport of this source
     * @return The transport
     */
    public PlaybackNetworkTransport getTransport() {
        return netTrans;
    }

    /**
     * Returns the start time of the source
     * @return The start time
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(StreamSource object) {
//        if (object instanceof StreamSource) {
            long objectStartTime = ((StreamSource) object).startTime;
            return (int) (startTime - objectStartTime);
//        }
//        return 0;
    }
}
