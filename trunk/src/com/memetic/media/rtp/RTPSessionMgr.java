/*
 * @(#)RTPManager.java
 * Created: 25-Oct-2005
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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.UnsupportedFormatException;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.EncryptionInfo;
import javax.media.rtp.GlobalReceptionStats;
import javax.media.rtp.GlobalTransmissionStats;
import javax.media.rtp.LocalParticipant;
import javax.media.rtp.OutputDataStream;
import javax.media.rtp.RTPConnector;
import javax.media.rtp.RTPStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.RemoteListener;
import javax.media.rtp.SendStream;
import javax.media.rtp.SendStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.SessionListener;
import javax.media.rtp.SessionManager;
import javax.media.rtp.TransmissionStats;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.NewParticipantEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.ReceiverReportEvent;
import javax.media.rtp.event.RemoteEvent;
import javax.media.rtp.event.SenderReportEvent;
import javax.media.rtp.event.SessionEvent;
import javax.media.rtp.event.StreamMappedEvent;
import javax.media.rtp.rtcp.SourceDescription;

/**
 * 
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public class RTPSessionMgr extends javax.media.rtp.RTPManager 
	implements SessionManager 
	{
    
    // The string to send when leaving
    private static final String BYE_STRING = "Leaving";

    // The conversion mask from an int to a short
    private static final int INT_TO_SHORT_MASK = 0xFFFF;

    // The shift for the first short in the packet loss
    private static final int PACKET_LOSS_SHORT_1_SHIFT = 16;

    // The resolution of each delay unit
    private static final int DELAY_RESOLUTION = 65536;

    // The resolution of the loss fraction
    private static final int LOSS_FRACTION_MULTIPLIER = 256;

    // The conversion factor from unix time to NTP time
    private static final long UNIX_TO_NTP_CONVERTER = 22089888000000L;

    // The multiplier for getting the unsigned max int from the signed max int
    private static final int UNSIGNED_MAX_INT_MULTIPLIER = 2;

    // The number of bits in a byte
    private static final int BITS_PER_BYTE = 8;

    // The maximum number of Reception sources to report on
    private static final int MAX_RC_COUNT = 31;

    // The padding flag bit
    private static final int PADDING_FLAG = 0x20;

    // The number of bytes in a word
    private static final int BYTES_PER_WORD = 4;

    // The number of bytes in a SSRC
    private static final int BYTES_PER_SSRC = 4;

    // The size of the SDES RTCP header in words - 1
    private static final int SDES_HEADER_SIZE = 5;

    // The RTCP sender bandwidth fraction minimum threshold
    private static final double SENDER_THRESHOLD = 0.25;

    // The RTCP minimum bandwidth fraction
    private static final double MIN_RTCP_BANDWIDTH = 0.1;

    // The amount of ms to add to the delay
    private static final int DELAY_CONSTANT = 500;

    // The conversion factor between seconds and ms
    private static final int SECS_TO_MS = 1000;

    // The default multicast TTL
    private static final int DEFAULT_TTL = 127;

    // The IP + UDP header size per packet
    private static final int IP_UDP_HEADER_SIZE = 28;

    // The video clock rate
    private static final int VIDEO_CLOCK_RATE = 90000;

    // The mask for the SC in the RTP header
    private static final int SOURCE_COUNT_MASK = 0x1F;

    // The mask from a long to an int
    private static final int LONG_TO_INT_MASK = 0xFFFFFFFF;

    // The mask from an int to a byte
    private static final int INT_TO_BYTE_MASK = 0xFF;

    // The RTCP default header byte
    private static final int RTCP_HEADER_BYTE = 0x80;

    // The relative frequency with which to send the RTCP Sender Name
    private static final int NAME_FREQUENCY = 3;

    // The relative frequency with which to send the RTCP CNAME
    private static final int CNAME_FREQUENCY = 1;

    // The RTP type for H263 Video
    private static final int H263_RTP = 34;

    // The RTP type for MPEG video
    private static final int MPEG_RTP = 32;

    // The RTP type for H261 video
    private static final int H261_RTP = 31;

    // The RTP type for JPEG video
    private static final int JPEG_RTP = 26;

    // The RTP type for DVI 22K audio
    private static final int DVI_22K_MONO_RTP = 17;

    // The Audio Format for DVI 22K audio
    private static final AudioFormat DVI_22K_MONO = 
        new AudioFormat(AudioFormat.DVI_RTP, 22050, 4, 1);

    // The RTP type for DVI 11K audio
    private static final int DVI_11K_MONO_RTP = 16;

    // The Audio Format for DVI 11K audio
    private static final AudioFormat DVI_11K_MONO = 
        new AudioFormat(AudioFormat.DVI_RTP, 11025, 4, 1);

    // The RTP type for MPEG Audio
    private static final int MPA_RTP = 14;

    // The Audio Format for MPEG Audio
    private static final AudioFormat MPA = 
        new AudioFormat(AudioFormat.MPEG_RTP);

    // The RTP type for DVI 8K Audio
    private static final int DVI_8K_MONO_RTP = 5;

    // The Audio Format for DVI 8K Audio
    private static final AudioFormat DVI_8K_MONO = 
        new AudioFormat(AudioFormat.DVI_RTP, 8000, 4, 1);

    // The RTP Type for G723 8K Audio
    private static final int G723_8K_MONO_RTP = 4;

    // The Audio Format for G723 8K Audio
    private static final AudioFormat G723_8K_MONO = 
        new AudioFormat(AudioFormat.G723_RTP, 8000, Format.NOT_SPECIFIED, 1);

    // The RTP Type for GSM 8K Audio
    private static final int GSM_8K_MONO_RTP = 3;

    // The Audio Format for GSM 8K Audio
    private static final AudioFormat GSM_8K_MONO = 
        new AudioFormat(AudioFormat.GSM_RTP, 8000, Format.NOT_SPECIFIED, 1);

    // The RTP Type for PCMU 8K Audio
    private static final int PCMU_8K_MONO_RTP = 0;

    // The Audio Format for PCMU 8k Audio
    private static final AudioFormat PCMU_8K_MONO = 
        new AudioFormat(AudioFormat.ULAW_RTP, 8000, 8, 1);

    // The separator between the username and the host name
    private static final String USER_HOST_SEPARATOR = "@";

    // The system property for the username
    private static final String USERNAME_PROPERTY = "user.name";

    // The default fraction of the bandwidth to use for Sender RTCP
    private static final double DEFAULT_SENDER_BANDWIDTH_FRACTION = 0.0125;

    // The default fraction of the bandwidth to use for Receiver RTCP
    private static final double DEFAULT_RECEIVER_BANDWIDTH_FRACTION = 0.0375;

    // The minimum RTCP interval in ms
    private static final int MIN_RTCP_INTERVAL = 5000;
    
    // The ssrc if we are not sending
    private static long ssrc = (long) (Math.random() * Integer.MAX_VALUE);

    // The map of Integer -> format recognised
    private HashMap<Integer,Format> formatMap = new HashMap<Integer,Format>();

    // A vector of receive stream listeners
    private Vector<ReceiveStreamListener> receiveStreamListeners = 
    	new Vector<ReceiveStreamListener>();

    // A vector of remote listeners
    private Vector<RemoteListener> remoteListeners = new Vector<RemoteListener>();

    // A vector of send stream listeners
    private Vector<SendStreamListener> sendStreamListeners = 
    	new Vector<SendStreamListener>();

    // A vector of session listeners
    private Vector<SessionListener> sessionListeners = new Vector<SessionListener>();

    // The local participant
    private RTPLocalParticipant localParticipant = null;

    // A map of active participants (cname -> participant)
    private HashMap<String,RTPRemoteParticipant> activeParticipants = 
    	new HashMap<String,RTPRemoteParticipant>();

    // A map of inactive participants (cname -> participant)
    private HashMap<String,RTPRemoteParticipant> inactiveParticipants = 
    	new HashMap<String,RTPRemoteParticipant>();

    // The global reception statistics
    private RTPGlobalReceptionStats globalReceptionStats = 
        new RTPGlobalReceptionStats();

    // The global transmission statistics
    private RTPGlobalTransmissionStats globalTransmissionStats = 
        new RTPGlobalTransmissionStats();

    // A map of receive streams (ssrc -> stream)
    private HashMap<Long,RTPReceiveStream> receiveStreams = new HashMap<Long,RTPReceiveStream>();

    // A map of send streams (ssrc -> stream)
    private HashMap<Long,RTPSendStream> sendStreams = new HashMap<Long,RTPSendStream>();
    
    // A map of streams that are ignored as they cannot be recognised
    private HashMap<Long,Integer> ignoredStreams = new HashMap<Long,Integer>();
    
    // A map of ssrcs to cnames
    private HashMap<Long,String> senders = new HashMap<Long,String>();

    // The RTCP Receiver Bandwidth fraction
    private double rtcpReceiverBandwidthFraction =
        DEFAULT_RECEIVER_BANDWIDTH_FRACTION;

    // The RTCP Sender Bandwidth fraction
    private double rtcpSenderBandwidthFraction = 
        DEFAULT_SENDER_BANDWIDTH_FRACTION;
    
    // The local address to use to bind sockets to
    private SessionAddress localAddress = null;
    
    // The RTP Connectors for targets that have been set up 
    // (address -> connector)
    private HashMap<SessionAddress,RTPConnector> targets = 
    	new HashMap<SessionAddress,RTPConnector>();
    
    // The RTP Handler
    private RTPHandler rtpHandler = null;
    
    // The RTCP Handler
    private RTCPHandler rtcpHandler = null;
    
    // The lock for sending events (so events are received in order)
    private Integer eventLock = new Integer(0);
    
    // True if the event lock has been obtained
    private boolean eventLocked = false;
    
    // True when the session is finished with
    private boolean done = false;
    
    // An RTCP timer
    private Timer rtcpTimer = new Timer();
    
    // The time at which the last rtcpPacket was sent
    private long lastRTCPSendTime = -1;
    
    // The average size of an RTCP packet received
    private int averageRTCPSize = 0;

    /**
     * Creates a new RTPManager
     */
    public RTPSessionMgr() {
        String user = System.getProperty(USERNAME_PROPERTY);
        String host = "localhost";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // Do Nothing
        }
        this.localParticipant = new RTPLocalParticipant(user
                + USER_HOST_SEPARATOR + host);
        addFormat(PCMU_8K_MONO, PCMU_8K_MONO_RTP);
        addFormat(GSM_8K_MONO, GSM_8K_MONO_RTP);
        addFormat(G723_8K_MONO, G723_8K_MONO_RTP);
        addFormat(DVI_8K_MONO, DVI_8K_MONO_RTP);
        addFormat(MPA, MPA_RTP);
        addFormat(DVI_11K_MONO, DVI_11K_MONO_RTP);
        addFormat(DVI_22K_MONO, DVI_22K_MONO_RTP);
        addFormat(new VideoFormat(VideoFormat.JPEG_RTP), JPEG_RTP);
        addFormat(new VideoFormat(VideoFormat.H261_RTP), H261_RTP);
        addFormat(new VideoFormat(VideoFormat.MPEG_RTP), MPEG_RTP);
        addFormat(new VideoFormat(VideoFormat.H263_RTP), H263_RTP);
    }
    
    private void getEventLock() {
        synchronized (eventLock) {
            while (eventLocked) {
                try {
                    eventLock.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
            eventLocked = true;
        }
    }
    
    private void releaseEventLock() {
        synchronized (eventLock) {
            eventLocked = false;
            eventLock.notifyAll();
        }
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#addFormat(javax.media.Format, int)
     */
    public void addFormat(Format format, int payload) {
        formatMap.put(new Integer(payload), format);
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#addReceiveStreamListener(
     *     javax.media.rtp.ReceiveStreamListener)
     */
    public void addReceiveStreamListener(ReceiveStreamListener listener) {
    	System.out.println("addReceiveStreamListenre " + listener + " " + receiveStreamListeners.size()+1);
        receiveStreamListeners.add(listener);
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#addRemoteListener(
     *     javax.media.rtp.RemoteListener)
     */
    public void addRemoteListener(RemoteListener listener) {
    	System.out.println("addRemoteListener " + listener + " " + remoteListeners.size()+1);
    	remoteListeners.add(listener);
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#addSendStreamListener(
     *     javax.media.rtp.SendStreamListener)
     */
    public void addSendStreamListener(SendStreamListener listener) {
    	System.out.println("addSendStreamListener " + listener + " " + sendStreamListeners.size()+1);
    	sendStreamListeners.add(listener);
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#addSessionListener(
     *     javax.media.rtp.SessionListener)
     */
    public void addSessionListener(SessionListener listener) {
        sessionListeners.add(listener);
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#removeReceiveStreamListener(
     *     javax.media.rtp.ReceiveStreamListener)
     */
    public void removeReceiveStreamListener(ReceiveStreamListener listener) {
        receiveStreamListeners.remove(listener);
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#removeRemoteListener(
     *     javax.media.rtp.RemoteListener)
     */
    public void removeRemoteListener(RemoteListener listener) {
        remoteListeners.remove(listener);
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#removeSendStreamListener(
     *     javax.media.rtp.SendStreamListener)sendStreamListeners
     */
    public void removeSendStreamListener(SendStreamListener listener) {
        sendStreamListeners.remove(listener);
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#removeSessionListener(
     *     javax.media.rtp.SessionListener)
     */
    public void removeSessionListener(SessionListener listener) {
        sessionListeners.remove(listener);
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#getActiveParticipants()
     */
    public Vector<RTPParticipant> getActiveParticipants() {
        Vector<RTPParticipant> participants = new Vector<RTPParticipant>(activeParticipants.values());
        if (localParticipant.isActive()) {
            participants.add(localParticipant);
        }
        return participants;
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#getAllParticipants()
     */
    public Vector<RTPParticipant> getAllParticipants() {
        Vector<RTPParticipant> participants = new Vector<RTPParticipant>();
        participants.addAll(activeParticipants.values());
        participants.addAll(inactiveParticipants.values());
        participants.add(localParticipant);
        return participants;
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#getLocalParticipant()
     */
    public LocalParticipant getLocalParticipant() {
        return localParticipant;
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#getPassiveParticipants()
     */
    public Vector<RTPParticipant> getPassiveParticipants() {
        Vector<RTPParticipant> participants = new Vector<RTPParticipant>(inactiveParticipants.values());
        if (!localParticipant.isActive()) {
            participants.add(localParticipant);
        }
        return participants;
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#getRemoteParticipants()
     */
    public Vector<RTPParticipant> getRemoteParticipants() {
        Vector<RTPParticipant> participants = new Vector<RTPParticipant>();
        participants.addAll(activeParticipants.values());
        participants.addAll(inactiveParticipants.values());
        return participants;
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#getGlobalReceptionStats()
     */
    public GlobalReceptionStats getGlobalReceptionStats() {
        return globalReceptionStats;
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#getGlobalTransmissionStats()
     */
    public GlobalTransmissionStats getGlobalTransmissionStats() {
        return globalTransmissionStats;
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#getReceiveStreams()
     */
    public Vector<RTPReceiveStream> getReceiveStreams() {
        return new Vector<RTPReceiveStream>(receiveStreams.values());
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#getSendStreams()
     */
    public Vector<RTPSendStream> getSendStreams() {
        return new Vector<RTPSendStream>(sendStreams.values());
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#initialize(
     *     javax.media.rtp.SessionAddress)
     */
    public void initialize(SessionAddress localAddress)
            throws IOException {
    	System.out.println("init RTPSessionMgr ");
        String user = System.getProperty(USERNAME_PROPERTY);
        initialize(new SessionAddress[] {localAddress},
                new SourceDescription[] {
                        new SourceDescription(
                                SourceDescription.SOURCE_DESC_CNAME, user
                                        + USER_HOST_SEPARATOR
                                        + InetAddress.getLocalHost()
                                                .getHostName(), 
                                                CNAME_FREQUENCY, false),
                        new SourceDescription(
                                SourceDescription.SOURCE_DESC_NAME, user
                                        + USER_HOST_SEPARATOR
                                        + InetAddress.getLocalHost()
                                                .getHostName(), NAME_FREQUENCY,
                                                false) },
                DEFAULT_SENDER_BANDWIDTH_FRACTION 
                + DEFAULT_RECEIVER_BANDWIDTH_FRACTION, 
                DEFAULT_SENDER_BANDWIDTH_FRACTION, null);
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#initialize(
     *     javax.media.rtp.SessionAddress[], 
     *     javax.media.rtp.rtcp.SourceDescription[], double, double, 
     *     javax.media.rtp.EncryptionInfo)
     */
    public void initialize(SessionAddress[] localAddresses,
            SourceDescription[] sourceDescription,
            double rtcpBandwidthFraction, double rtcpSenderBandwidthFraction,
            EncryptionInfo encryptionInfo) {
    	System.out.println("init RTPSessenMgr");
        this.rtcpSenderBandwidthFraction = rtcpBandwidthFraction
                * rtcpBandwidthFraction;
        this.rtcpReceiverBandwidthFraction = rtcpBandwidthFraction
                - this.rtcpSenderBandwidthFraction;
        this.localParticipant = new RTPLocalParticipant("");
        localParticipant.setSourceDescription(sourceDescription);
        localAddress = localAddresses[0];
        System.out.println("before start RTPSessenMgr");
        start();
        System.out.println("after start RTPSessenMgr");
    }
    
    /**
     * 
     * @see javax.media.rtp.RTPManager#addTarget(
     *     javax.media.rtp.SessionAddress)
     */
    public void addTarget(SessionAddress remoteAddress)
            throws IOException {
        RTPSocketAdapter socket = new RTPSocketAdapter(
                localAddress.getDataAddress(),
                remoteAddress.getDataAddress(), remoteAddress.getDataPort(),
                remoteAddress.getTimeToLive());
        rtpHandler = new RTPHandler(this);
        rtcpHandler = new RTCPHandler(this);
        socket.getControlInputStream().setTransferHandler(rtcpHandler);
        socket.getDataInputStream().setTransferHandler(rtpHandler);
        targets.put(remoteAddress, socket);
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#initialize(javax.media.rtp.RTPConnector)
     */
    public void initialize(RTPConnector connector) {
        try {
            rtpHandler = new RTPHandler(this);
            rtcpHandler = new RTCPHandler(this);
            connector.getControlInputStream().setTransferHandler(rtcpHandler);
            connector.getDataInputStream().setTransferHandler(rtpHandler);
            targets.put(null, connector);
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#removeTarget(
     *     javax.media.rtp.SessionAddress, java.lang.String)
     */
    public void removeTarget(SessionAddress remoteAddress, String reason) {
        RTPConnector connector = (RTPConnector) targets.get(remoteAddress);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        
        try {
            Vector<RTPStream> sendStreams = localParticipant.getStreams();

            // Send a bye packet
            output.writeByte(RTCP_HEADER_BYTE);
            output.writeByte(RTCPPacket.PT_RR & INT_TO_BYTE_MASK);
            output.writeShort(1);
            output.writeInt((int) (ssrc & LONG_TO_INT_MASK));
            output.writeByte(RTCP_HEADER_BYTE 
                    | ((sendStreams.size() + 1) & SOURCE_COUNT_MASK));
            output.writeByte(RTCPPacket.PT_BYE & INT_TO_BYTE_MASK);
            output.writeShort(sendStreams.size() + 1);
            output.writeInt((int) (ssrc & LONG_TO_INT_MASK));
            for (int i = 0; i < sendStreams.size(); i++) {
                output.writeInt((int) (((RTPSendStream) 
                        sendStreams.get(i)).getSSRC() & LONG_TO_INT_MASK));
            }
            output.close();
            bytes.close();
            byte[] data = bytes.toByteArray();
            OutputDataStream out = connector.getControlOutputStream();
            out.write(data, 0, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (connector != null) {
            targets.remove(remoteAddress);
        }
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#removeTargets(java.lang.String)
     */
    public void removeTargets(String reason) {
        Iterator<SessionAddress> iter = targets.keySet().iterator();
        while (iter.hasNext()) {
            SessionAddress addr = (SessionAddress) iter.next();
            removeTarget(addr, reason);
        }
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#createSendStream(
     *     javax.media.protocol.DataSource, int)
     */
    public SendStream createSendStream(DataSource dataSource, int streamIndex)
            throws UnsupportedFormatException, IOException {
        int format = 0;
        Format fmt = null;
        double clockRate = VIDEO_CLOCK_RATE;
        if (dataSource instanceof PushBufferDataSource) {
            PushBufferStream stream = 
                ((PushBufferDataSource) dataSource).getStreams()[streamIndex];
            fmt = stream.getFormat();
        } else if (dataSource instanceof PullBufferDataSource) {
            PullBufferStream stream = 
                ((PullBufferDataSource) dataSource).getStreams()[streamIndex];
            fmt = stream.getFormat();
        } else {
            throw new IOException("Cannot use stream sources");
        }
        Iterator<Integer> iter = formatMap.keySet().iterator();
        while (iter.hasNext()) {
            Integer id = iter.next();
            Format testFormat = (Format) formatMap.get(id);
            if (testFormat.matches(fmt)) {
                format = id.intValue();
            }
        }
        if (format == 0) {
            throw new UnsupportedFormatException(fmt);
        }
        if (fmt instanceof AudioFormat) {
            clockRate = ((AudioFormat) fmt).getSampleRate();
        }
        Iterator<RTPConnector> iterator = targets.values().iterator();
        RTPConnector connector = iterator.next();
        OutputDataStream stream = connector.getDataOutputStream();
        return new RTPSendStream((long) (Math.random() * Integer.MAX_VALUE),
                dataSource, stream, streamIndex, localParticipant, format,
                clockRate);
    }

    /**
     * 
     * @see javax.media.rtp.RTPManager#dispose()
     */
    public void dispose() {
        removeTargets("Quitting");
        done = true;
    }

    /**
     * 
     * @see javax.media.Controls#getControl(java.lang.String)
     */
    public Object getControl(String controlClass) {
        return null;
    }

    /**
     * 
     * @see javax.media.Controls#getControls()
     */
    public Object[] getControls() {
        return new Object[] {};
    }
    
    /**
     * Handles an incoming RTP packet
     * @param data The packet data
     * @param offset The packet offset
     * @param length The packet length 
     */
    protected void handleRTPPacket(byte[] data, int offset, int length) {
        try {
            globalReceptionStats.addPacketRecd();
            globalReceptionStats.addBytesRecd(length);
            RTPHeader header = new RTPHeader(data, offset, length);
            long ssrc = header.getSsrc();
            Integer packetType = (Integer) ignoredStreams.get(new Long(ssrc));
            if (packetType != null) {
                if (packetType.intValue() != header.getPacketType()) {
                    ignoredStreams.remove(new Long(ssrc));
                    packetType = null;
                }
            }
            if (packetType == null) {
                RTPReceiveStream stream = 
                    (RTPReceiveStream) receiveStreams.get(new Long(ssrc));
                if (stream == null) {
                    int type = header.getPacketType();
                    Format format = (Format) formatMap.get(new Integer(type));
                    if (format == null) {
                        globalReceptionStats.addUnknownType();
                        System.err.println("Unknown format identifier: " 
                                + type);
                        ignoredStreams.put(new Long(ssrc), new Integer(type));
                    } else {
                        RTPDataSource dataSource = 
                            new RTPDataSource(ssrc, format);
                        stream = new RTPReceiveStream(dataSource, ssrc);
                        receiveStreams.put(new Long(ssrc), stream);
                        ReceiveStreamEvent event = new NewReceiveStreamEvent(
                                this, stream);
                        new ReceiveStreamNotifier(receiveStreamListeners, 
                                event);
                    }
                }
                if (stream != null) {
                    RTPDataSource dataSource = 
                        (RTPDataSource) stream.getDataSource();
                    dataSource.handleRTPPacket(header, 
                            data, offset + header.getSize(), 
                            length - header.getSize());
                }
            }
        } catch (IOException e) {
            globalReceptionStats.addBadRTPkt();
        }
    }
    
    /**
     * Handles an incoming RTCP packet
     * @param data The packet data
     * @param offset The packet offset
     * @param length The packet length
     */
    protected void handleRTCPPacket(byte[] data, int offset, int length) {
        try {
            int avgeRTCPSize = 
                averageRTCPSize * globalReceptionStats.getRTCPRecd();
            globalReceptionStats.addRTCPRecd();
            globalReceptionStats.addBytesRecd(length);
            averageRTCPSize = 
                (avgeRTCPSize + length + IP_UDP_HEADER_SIZE)
                / globalReceptionStats.getRTCPRecd();
            RTCPHeader header = new RTCPHeader(data, offset, length);
            
            // Get the stream of the participant, if available
            long ssrc = header.getSsrc();
            RTPReceiveStream stream = 
                (RTPReceiveStream) receiveStreams.get(new Long(ssrc));
            
            RTCPReport report = null;
            RemoteEvent remoteEvent = null;
            
            // If the packet is SR, read the sender info
            if (header.getPacketType() == RTCPPacket.PT_SR) {
                report = new RTCPSenderReport(data, offset, length);
                ((RTCPSenderReport) report).setStream(stream);
                remoteEvent = new SenderReportEvent(this, 
                        (RTCPSenderReport) report);
                globalReceptionStats.addSRRecd();
            }
            
            // If the packet is RR, read the receiver info
            if (header.getPacketType() == RTCPPacket.PT_RR) {
                report = new RTCPReceiverReport(data, offset, length);
                remoteEvent = new ReceiverReportEvent(this, 
                        (RTCPReceiverReport) report);
            }
            
            // If the report is not null
            if (report != null) {
                String cname = report.getCName();
                if (cname == null) {
                    cname = (String) senders.get(new Long(ssrc));
                }
                
                if (stream != null) {
                    stream.setReport(report);
                }
                
                // If the cname is in the report
                if (cname != null) {
                    
                    // Store the cname for later
                    senders.put(new Long(ssrc), cname);
                    
                    // Get the participant
                    RTPRemoteParticipant participant = 
                        (RTPRemoteParticipant) activeParticipants.get(cname);
                    if (participant == null) {
                        participant = (RTPRemoteParticipant) 
                                          inactiveParticipants.get(cname);
                    }
                    
                    // If there is no participant, create one
                    if (participant == null) {
                        participant = new RTPRemoteParticipant(cname);
                        getEventLock();
                        SessionEvent event = 
                            new NewParticipantEvent(this, participant);
                        new SessionNotifier(sessionListeners, event);
                        inactiveParticipants.put(cname, participant);
                    }
                    
                    // Set the participant of the report
                    report.setParticipant(participant);
                    participant.addReport(report);
                    
                    // If this is a bye packet, remove the stream
                    if (report.isByePacket()) {
                        participant.removeStream(stream);
                        getEventLock();
                        new ReceiveStreamNotifier(receiveStreamListeners,
                                new ByeEvent(this, participant, stream, 
                                        report.getByeReason(), 
                                        participant.getStreams().size() == 0));
                        if (participant.getStreams().size() == 0) {
                            activeParticipants.remove(cname);
                            inactiveParticipants.put(cname, participant);
                        }
                    } else {
                        
                        // If the stream is not null, map the stream
                        if (stream != null) {
                            if (!activeParticipants.containsKey(cname)) {
                                inactiveParticipants.remove(cname);
                                activeParticipants.put(cname, participant);
                            }
                            
                            if (stream.getParticipant() == null) {
                                participant.addStream(stream);
                                stream.setParticipant(participant);
                                getEventLock();
                                ReceiveStreamEvent event = 
                                    new StreamMappedEvent(this, stream, 
                                            participant);
                                new ReceiveStreamNotifier(
                                        receiveStreamListeners, event);
                            }
                        }
                    }
                }
                
                // Notify listeners of this packet
                getEventLock();
                new RemoteNotifier(remoteListeners, remoteEvent);
            } else {
                throw new IOException("Unknown report type: "
                        + header.getPacketType());
            }
                
        } catch (IOException e) {
            globalReceptionStats.addBadRTCPPkt();
        }
    }
    
    // A notifier of receive stream events
    private class ReceiveStreamNotifier extends Thread {
        
        // The receive stream listener
        private Vector<ReceiveStreamListener> listeners = null;
        
        // The event
        private ReceiveStreamEvent event = null;
        
        private ReceiveStreamNotifier(Vector<ReceiveStreamListener> listeners, 
                ReceiveStreamEvent event) {
            this.listeners = listeners;
            this.event = event;
            start();
        }
        
        /**
         * 
         * @see java.lang.Runnable#run()
         */
        public void run() {
            for (int i = 0; i < listeners.size(); i++) {
                ReceiveStreamListener listener = 
                    (ReceiveStreamListener) listeners.get(i);
                listener.update(event);
            }
            releaseEventLock();
        }
    }
    
    // A notifier of receive session events
    private class SessionNotifier extends Thread {
        
        // The session listener
        private Vector<SessionListener> listeners = null;
        
        // The event
        private SessionEvent event = null;
        
        private SessionNotifier(Vector<SessionListener> listeners, 
                SessionEvent event) {
            this.listeners = listeners;
            this.event = event;
            start();
        }
        
        /**
         * 
         * @see java.lang.Runnable#run()
         */
        public void run() {
            for (int i = 0; i < listeners.size(); i++) {
                SessionListener listener = 
                    (SessionListener) listeners.get(i);
                listener.update(event);
            }
            releaseEventLock();
        }
    }
    
    // A notifier of remote events
    private class RemoteNotifier extends Thread {
        
        // The remote listeners
        private Vector<RemoteListener> listeners = null;
        
        // The event
        private RemoteEvent event = null;
        
        private RemoteNotifier(Vector<RemoteListener> listeners, 
                RemoteEvent event) {
            this.listeners = listeners;
            this.event = event;
            start();
        }
        
        /**
         * 
         * @see java.lang.Runnable#run()
         */
        public void run() {
            for (int i = 0; i < listeners.size(); i++) {
                RemoteListener listener = 
                    (RemoteListener) listeners.get(i);
                listener.update(event);
            }
            releaseEventLock();
        }
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#initSession(
     *     javax.media.rtp.SessionAddress, long, 
     *     javax.media.rtp.rtcp.SourceDescription[], double, double)
     */
    public int initSession(SessionAddress localAddress, long defaultSSRC, 
            SourceDescription[] defaultUserDesc, double rtcpBWFraction, 
            double rtcpSenderBWFraction) {
        initialize(new SessionAddress[]{localAddress}, defaultUserDesc, 
                rtcpBWFraction, 
                rtcpSenderBWFraction, null);
        return 0;
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#initSession(
     *     javax.media.rtp.SessionAddress, 
     *     javax.media.rtp.rtcp.SourceDescription[], double, double)
     */
    public int initSession(SessionAddress localAddress, 
            SourceDescription[] defaultUserDesc, double rtcpBWFraction, 
            double rtcpSenderBWFraction) {
        initialize(new SessionAddress[]{localAddress}, defaultUserDesc, 
                rtcpBWFraction, 
                rtcpSenderBWFraction, null);
        return 0;
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#startSession(
     *     javax.media.rtp.SessionAddress, int, javax.media.rtp.EncryptionInfo)
     */
    public int startSession(SessionAddress destAddress, int mcastScope,
            EncryptionInfo encryptionInfo) throws IOException {
        addTarget(destAddress);
        return 0;
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#startSession(
     *     javax.media.rtp.SessionAddress, javax.media.rtp.SessionAddress, 
     *     javax.media.rtp.SessionAddress, javax.media.rtp.EncryptionInfo)
     */
    public int startSession(SessionAddress localReceiverAddress, 
            SessionAddress localSenderAddress, 
            SessionAddress remoteReceiverAddress, 
            EncryptionInfo encryptionInfo) throws IOException {
        addTarget(remoteReceiverAddress);
        return 0;
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#getDefaultSSRC()
     */
    public long getDefaultSSRC() {
        return 0;
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#getStream(long)
     */
    public RTPStream getStream(long filterssrc) {
        return null;
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#getMulticastScope()
     */
    public int getMulticastScope() {
        return DEFAULT_TTL;
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#setMulticastScope(int)
     */
    public void setMulticastScope(int multicastScope) {
        // Does Nothing
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#closeSession(java.lang.String)
     */
    public void closeSession(String reason) {
        removeTargets(reason);
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#generateCNAME()
     */
    public String generateCNAME() {
        return localParticipant.getCNAME();
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#generateSSRC()
     */
    public long generateSSRC() {
        return (long) (Math.random() * Integer.MAX_VALUE);
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#getSessionAddress()
     */
    public SessionAddress getSessionAddress() {
        return null;
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#getLocalSessionAddress()
     */
    public SessionAddress getLocalSessionAddress() {
        return localAddress;
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#createSendStream(int, 
     *     javax.media.protocol.DataSource, int)
     */
    public SendStream createSendStream(int ssrc, DataSource ds, 
            int streamindex) throws UnsupportedFormatException, 
            IOException {
        return createSendStream(ds, streamindex);
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#startSession(int, 
     *     javax.media.rtp.EncryptionInfo)
     */
    public int startSession(int mcastScope, EncryptionInfo encryptionInfo) {
        return -1;
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#addPeer(
     *     javax.media.rtp.SessionAddress)
     */
    public void addPeer(SessionAddress peerAddress) throws IOException {
        addTarget(peerAddress);
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#removePeer(
     *     javax.media.rtp.SessionAddress)
     */
    public void removePeer(SessionAddress peerAddress) {
        removeTarget(peerAddress, BYE_STRING);
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#removeAllPeers()
     */
    public void removeAllPeers() {
        removeTargets(BYE_STRING);
    }

    /**
     * 
     * @see javax.media.rtp.SessionManager#getPeers()
     */
    public Vector<RTPParticipant> getPeers() {
        return getAllParticipants();
    }
    
    /**
     * Starts the sending of RTCP packets
     */
    public void start() {
        // Send the first RTCP packet
        long delay = (long) (Math.random() * SECS_TO_MS) + DELAY_CONSTANT;
        rtcpTimer.schedule(new RTCPTimerTask(this), delay);
        globalReceptionStats.resetBytesRecd();
        lastRTCPSendTime = System.currentTimeMillis();
    }
    
    private long calculateRTCPDelay() {
        long delay = MIN_RTCP_INTERVAL;
        double bandwidth = ((double) globalReceptionStats.getBytesRecd()
                / (System.currentTimeMillis() - lastRTCPSendTime));
        if (bandwidth < MIN_RTCP_BANDWIDTH) {
            delay = MIN_RTCP_INTERVAL;
        } else {
            double senderFraction = 0;
            if ((activeParticipants.size() > 0)
                    || (inactiveParticipants.size() > 0)) {
                senderFraction = 
                    activeParticipants.size()
                    / (inactiveParticipants.size() + activeParticipants.size());
            }
            if ((activeParticipants.size() > 0) 
                    && (senderFraction < SENDER_THRESHOLD)) {
                if (localParticipant.getStreams().size() > 0) {
                    delay = (long) ((averageRTCPSize
                            * activeParticipants.size())
                            / (bandwidth * rtcpSenderBandwidthFraction));
                } else {
                    delay = (long) ((averageRTCPSize
                            * inactiveParticipants.size())
                            / (bandwidth * rtcpReceiverBandwidthFraction));
                }
            } else {
                delay = (long) ((averageRTCPSize * (activeParticipants.size()
                        + inactiveParticipants.size()))
                        / (bandwidth * (rtcpSenderBandwidthFraction
                                + rtcpReceiverBandwidthFraction)));
            }
            if (delay < MIN_RTCP_INTERVAL) {
                delay = MIN_RTCP_INTERVAL;
            }
        }
        return delay;
    }
    
    private int writeSDESHeader(DataOutputStream output, int ssrcs, int size) 
            throws IOException {
        int packetSize = size + SDES_HEADER_SIZE + (BYTES_PER_SSRC * ssrcs);
        int padding = BYTES_PER_WORD - (packetSize % BYTES_PER_WORD);
        if (padding == BYTES_PER_WORD) {
           padding = 0;
        }
        packetSize += padding;
        int pBit = 0;
        if (padding > 0) {
            pBit = PADDING_FLAG;
        }
        // Add a RTCP header
        output.writeByte(RTCP_HEADER_BYTE | pBit | (ssrcs & SOURCE_COUNT_MASK));
        output.writeByte(RTCPPacket.PT_SDES & INT_TO_BYTE_MASK);
        output.writeShort((packetSize / BYTES_PER_WORD) - 1);
        
        return padding;
    }
    
    private void writeSDES(DataOutputStream output, Vector<SourceDescription> sdesItems, 
            long ssrc) 
            throws IOException {
        output.writeInt((int) (ssrc & LONG_TO_INT_MASK));
        for (int i = 0; i < sdesItems.size(); i++) {
            SourceDescription sdes = 
                (SourceDescription) sdesItems.get(i);
            int type = sdes.getType();
            String description = sdes.getDescription();
            byte[] desc = description.getBytes("UTF-8");
            output.writeByte(type & INT_TO_BYTE_MASK);
            output.writeByte(desc.length & INT_TO_BYTE_MASK);
            output.write(desc);
        }
        output.writeByte(0);
    }

    /**
     * Sends an RTCP packet, and schedules the next one
     */
    public void sendRTCPPacket() {
        int rc = receiveStreams.size();
        if (rc > MAX_RC_COUNT) {
            rc = MAX_RC_COUNT;
        }
        long delay = calculateRTCPDelay();
        long now = System.currentTimeMillis();
        
        // If now is too early to send a packet, wait until later
        if (now < (lastRTCPSendTime + delay)) {
            rtcpTimer.schedule(new RTCPTimerTask(this), 
                    (lastRTCPSendTime + delay) - now);
        } else {
            
            // Reset the stats
            lastRTCPSendTime = System.currentTimeMillis();
            globalReceptionStats.resetBytesRecd();
            
            // Get the packet details
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);

            try {
                
                // Determine the packet type
                int packetType = RTCPPacket.PT_RR;
                int packetSize = (rc * RTCPFeedback.SIZE) + BITS_PER_BYTE;
                if (localParticipant.getStreams().size() > 0) {
                    packetType = RTCPPacket.PT_SR;
                    packetSize += RTCPSenderInfo.SIZE;
                }
                
                // Add a RTCP header
                output.writeByte(RTCP_HEADER_BYTE | (rc & SOURCE_COUNT_MASK));
                output.writeByte(packetType & INT_TO_BYTE_MASK);
                output.writeShort(((packetSize) / BYTES_PER_WORD) - 1);
                output.writeInt((int) (ssrc & LONG_TO_INT_MASK));

                // If we are a sender, add sender stats
                if (localParticipant.getStreams().size() > 0) {
                    packetType = RTCPPacket.PT_SR;
                    int senderIndex = (int) (Math.random()
                                * localParticipant.getStreams().size());
                    RTPSendStream sendStream = 
                        (RTPSendStream) localParticipant.getStreams().get(
                                senderIndex);
                    TransmissionStats stats = 
                        sendStream.getSourceTransmissionStats();
                    long sendtime = sendStream.getLastSendTime();
                    sendtime += UNIX_TO_NTP_CONVERTER;
                    long sendTimeSeconds = sendtime / SECS_TO_MS;
                    long sendTimeFractions = 
                        ((sendtime - (sendTimeSeconds * SECS_TO_MS))
                                / SECS_TO_MS) * (Integer.MAX_VALUE
                                        * UNSIGNED_MAX_INT_MULTIPLIER);
                    long timestamp = sendStream.getLastTimestamp();
                    output.writeInt((int) (sendTimeSeconds & LONG_TO_INT_MASK));
                    output.writeInt((int) (sendTimeFractions
                            & LONG_TO_INT_MASK));
                    output.writeInt((int) (timestamp & LONG_TO_INT_MASK));
                    output.writeInt(stats.getPDUTransmitted());
                    output.writeInt(stats.getBytesTransmitted());
                }
                
                // Add the receiver reports
                Vector<RTPReceiveStream> streams = new Vector<RTPReceiveStream>(receiveStreams.values());
                now = System.currentTimeMillis();
                for (int i = 0; i < rc; i++) {
                    int pos = (int) (Math.random() * streams.size());
                    RTPReceiveStream stream = 
                        (RTPReceiveStream) streams.get(pos);
                    RTPReceptionStats stats = 
                        (RTPReceptionStats) stream.getSourceReceptionStats();
                    RTPDataSource dataSource = 
                        (RTPDataSource) stream.getDataSource();
                    RTPDataStream dataStream = 
                        (RTPDataStream) dataSource.getStreams()[0];
                    long streamSSRC = stream.getSSRC();
                    int lossFraction = 0;
                    if (stats.getPDUProcessed() > 0) {
                        lossFraction = (LOSS_FRACTION_MULTIPLIER
                                * stats.getPDUlost())
                                / stats.getPDUProcessed();
                    }
                    long lastESequence = 
                        (stats.getSequenceWrap() * RTPHeader.MAX_SEQUENCE)
                        + dataStream.getLastSequence();
                    long packetsExpected = 
                        lastESequence - dataStream.getFirstSequence(); 
                    int cumulativePacketLoss = (int) (packetsExpected
                        - (stats.getPDUProcessed() + stats.getPDUDuplicate()));
                    long jitter = 
                        ((RTPDataSource) stream.getDataSource()).getJitter();
                    long lsrMSW = stream.getLastSRReportTimestampMSW();
                    long lsrLSW = stream.getLastSRReportTimestampLSW();
                    long dSLR = ((now - stream.getLastSRReportTime())
                            * SECS_TO_MS) / DELAY_RESOLUTION;
                    if (stream.getLastSRReportTime() == 0) {
                        dSLR = 0;
                    }
                    output.writeInt((int) (streamSSRC & LONG_TO_INT_MASK));
                    output.writeByte(lossFraction & INT_TO_BYTE_MASK);
                    output.writeByte((cumulativePacketLoss
                            >> PACKET_LOSS_SHORT_1_SHIFT)
                            & INT_TO_BYTE_MASK);
                    output.writeShort((cumulativePacketLoss
                            & INT_TO_SHORT_MASK));
                    output.writeInt((int) (lastESequence & LONG_TO_INT_MASK));
                    output.writeInt((int) (jitter & LONG_TO_INT_MASK));
                    output.writeShort((int) (lsrMSW & INT_TO_SHORT_MASK));
                    output.writeShort((int) ((lsrLSW
                            >> PACKET_LOSS_SHORT_1_SHIFT)
                            & INT_TO_SHORT_MASK));
                    output.writeInt((int) (dSLR & LONG_TO_INT_MASK));
                    streams.remove(pos);
                }
                
                // Add the SDES items
                if (localParticipant.getStreams().size() == 0) {
                    Vector<SourceDescription> sdesItems = 
                        localParticipant.getSourceDescription();
                    if (sdesItems.size() > 0) {
                        int padding = writeSDESHeader(output, 1,
                                localParticipant.getSdesSize());
                        writeSDES(output, sdesItems, ssrc);
                        
                        // Add the sdes padding
                        for (int i = 0; i < padding; i++) {
                            output.writeByte(padding);
                        }
                    }
                } else {
                    Vector<RTPStream> sendStreams = localParticipant.getStreams();
                    int totalSDES = 0;
                    for (int i = 0; i < sendStreams.size(); i++) {
                        totalSDES += 
                            ((RTPSendStream) 
                                    sendStreams.get(i)).getSdesSize();
                    }
                    int padding = writeSDESHeader(output, sendStreams.size(),
                            totalSDES);
                    for (int i = 0; i < sendStreams.size(); i++) {
                        RTPSendStream sendStream = 
                            (RTPSendStream) sendStreams.get(i);
                        writeSDES(output, sendStream.getSourceDescription(),
                                sendStream.getSSRC());
                    }
                    
                    // Add the sdes padding
                    for (int i = 0; i < padding; i++) {
                        output.writeByte(padding);
                    }
                }
                
                
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            Iterator<RTPConnector> iterator = targets.values().iterator();
            while (iterator.hasNext()) {
                RTPConnector connector = (RTPConnector) iterator.next();
                try {
                    OutputDataStream outputStream = 
                        connector.getControlOutputStream();
                    output.close();
                    bytes.close();
                    byte[] data = bytes.toByteArray();
                    outputStream.write(data, 0, data.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            // Prepare to send the next packet
            if (!done) {
                rtcpTimer.schedule(new RTCPTimerTask(this), delay);
            }
        }
    }
    
    // A timer task for sending RTCP packets
    private class RTCPTimerTask extends TimerTask {
        
        private RTPSessionMgr rtpSessionManager = null;
        
        private RTCPTimerTask(RTPSessionMgr rtpSessionManager) {
            this.rtpSessionManager = rtpSessionManager;
        }
        
        /**
         * 
         * @see java.lang.Runnable#run()
         */
        public void run() {
            rtpSessionManager.sendRTCPPacket();
        }
    }
}
