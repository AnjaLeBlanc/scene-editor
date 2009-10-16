/*
 * @(#)PlaybackNetworkTransport.java
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import memetic.crypto.EncryptedRTPSocket;
import memetic.crypto.RTPCrypt;

/**
 * Transportation of Played Packets
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class PlaybackNetworkTransport implements RTPPacketSink, RTCPPacketSink {

    // The default port number to use if on the local machine
    private static final int DEFAULT_PORT = 60000;

    // The RTP version
    private static final int RTP_VERSION = 2;

    // The error message indicator
    private static final String ERROR_MESSAGE = "Error";

    // The separator between port and address
    private static final String ADDRESS_SEP = "/";

    // The constructor log message
    private static final String CONSTRUCTOR_MESSAGE =
        "Playback_Network_Transport::Playback_Network_Transport:";

    // The size of the buffer to send packets with
    private static final int SEND_BUFFER_SIZE = 64000;

    // The log file for the database
    private static Log logger =
        LogFactory.getLog(PlaybackNetworkTransport.class.getName());

    // The endpoint to which playbacks are made
    private NetworkEndpoint endpoint = null;

    // The RTP address of the endpoint
    private InetSocketAddress rtpAddress = null;

    // The RTCP address of the endpoint
    private InetSocketAddress rtcpAddress = null;

    // True if the address of the endpoint is multicast
    private boolean isMulticast = false;

    // The RTP socket for the endpoint
    private DatagramSocket rtpSock = null;

    // The RTCP socket for the endpoint
    private DatagramSocket rtcpSock = null;

    // The host name of the destination
    private String destination = "";

    // The error that has been returned
    private boolean errorSignalled = false;

    // The play session id using the transport
    private int playSessionId = 0;

    // A list of ssrcs allocated
    private HashMap<Long, Object> ssrcMap = new HashMap<Long, Object>();

    // The RTP Listener
    private RTPListener rtpListener = null;

    // The RTCP Listener
    private RTCPListener rtcpListener = null;

    /**
     * Creates a new PlaybackNetworkTransport
     *
     * @param ePoint
     *            The endpoint to play to
     * @param crypt The en/decrypter
     * @param id The id of the play session
     */
    public PlaybackNetworkTransport(NetworkEndpoint ePoint, RTPCrypt crypt,
            int id) {
        // Set up the addressing
        playSessionId = id;
        endpoint = ePoint;
        rtpAddress = ePoint.getRtpAddress();
        rtcpAddress = ePoint.getRtcpAddress();
        isMulticast = rtpAddress.getAddress().isMulticastAddress();

        // Get the host name
        logger.debug(CONSTRUCTOR_MESSAGE
                + " RTP for " + rtpAddress.getAddress() + ADDRESS_SEP
                + rtpAddress.getPort());
        logger.debug(CONSTRUCTOR_MESSAGE
                + " RTCP for " + rtcpAddress.getAddress() + ADDRESS_SEP
                        + rtcpAddress.getPort());
        destination = rtpAddress.getAddress().getHostAddress();

        // If the endpoint is multicast, set up multicast sockets
        if (isMulticast) {
            try {
                // Windows needs things to be set up differently:
                // You can't bind to the address on windows
                if (System.getProperty("os.name").toLowerCase().
                        indexOf("windows") == -1) {
                    rtpSock = new EncryptedRTPSocket(crypt,
                            rtpAddress.getAddress(),
                            rtpAddress.getPort());
                    rtcpSock = new EncryptedRTPSocket(crypt,
                            rtcpAddress.getAddress(), rtcpAddress.getPort());
                } else {
                    rtpSock = new EncryptedRTPSocket(crypt,
                            rtpAddress.getPort());
                    rtcpSock = new EncryptedRTPSocket(crypt,
                            rtcpAddress.getPort());
                }
                ((MulticastSocket) rtpSock).joinGroup(rtpAddress.getAddress());
                ((MulticastSocket) rtcpSock)
                        .joinGroup(rtcpAddress.getAddress());
                ((MulticastSocket) rtpSock).setTimeToLive(ePoint.getTtl());
                ((MulticastSocket) rtcpSock).setTimeToLive(ePoint.getTtl());
                ((MulticastSocket) rtpSock).setLoopbackMode(false);
                ((MulticastSocket) rtcpSock).setLoopbackMode(false);
            } catch (Exception e) {
                logger.error("Error " + e.getMessage(), e);
                errorSignalled = true;
            }
        } else {

            // If the endpoint is unicast, set up unicast sockets
            try {
                if (rtpAddress.getAddress().equals(
                        InetAddress.getLocalHost())) {
                    rtpSock = new EncryptedRTPSocket(crypt,
                            DEFAULT_PORT);
                } else {
                    rtpSock = new EncryptedRTPSocket(crypt,
                            rtpAddress.getPort());
                }
                rtpSock.setReuseAddress(true);
                if (rtcpAddress.getAddress().equals(
                        InetAddress.getLocalHost())) {
                    rtcpSock = new EncryptedRTPSocket(crypt,
                            DEFAULT_PORT + 1);
                } else {
                    rtcpSock = new EncryptedRTPSocket(crypt,
                            rtcpAddress.getPort());
                }
                rtcpSock.setReuseAddress(true);
            } catch (Exception e) {
                logger.error(ERROR_MESSAGE, e);
                errorSignalled = true;
            }
        }

        // Set the send buffer size
        try {
            rtpSock.setSendBufferSize(SEND_BUFFER_SIZE);
            rtcpSock.setSendBufferSize(SEND_BUFFER_SIZE);
        } catch (Exception e) {
            logger.error(ERROR_MESSAGE, e);
        }

        // If there is not an error, listen on the sockets
        if (!errorSignalled) {
            rtpListener = new RTPListener(rtpSock, this);
            rtcpListener = new RTCPListener(rtcpSock, this);
            rtpListener.record();
            rtcpListener.record();
        }
    }

    /**
     * Returns a new, unused ssrc
     * @param streamSource The source for which to get the SSRC
     * @return A new ssrc
     */
    public long allocateSSRC(StreamSource streamSource) {
        long ssrc = (long) (Math.random() * Integer.MAX_VALUE);
        boolean looped = false;
        while (ssrcMap.get(new Long(ssrc)) != null) {
            if (ssrc == Integer.MAX_VALUE) {
                if (!looped) {
                    ssrc = -1;
                    looped = true;
                } else {
                    return -1;
                }
            }
            ssrc++;
        }
        ssrcMap.put(new Long(ssrc), streamSource);
        return ssrc;
    }

    public void setSendPort(int port) {
    	rtpAddress = new InetSocketAddress(rtpAddress.getAddress(), port);
    	rtcpAddress = new InetSocketAddress(rtcpAddress.getAddress(), port + 1);
    }

    /**
     * Transmits an RTP packet
     *
     * @param packet
     *            The packet to transmit
     */
    public void playRtpPacket(DatagramPacket packet) {
        try {
            packet.setSocketAddress(rtpAddress);
            //System.out.println("send rtpPacket " +rtpAddress);
            rtpSock.send(packet);
        } catch (Exception e) {
            logger.error(ERROR_MESSAGE, e);
        }
    }

    /**
     * Transmits an RTCP packet
     *
     * @param packet
     *            The packet to transmit
     */
    public void playRtcpPacket(DatagramPacket packet) {
        try {
            packet.setSocketAddress(rtcpAddress);
            rtcpSock.send(packet);
        } catch (Exception e) {
            logger.error(ERROR_MESSAGE, e);
        }
    }

    /**
     * Returns the host name of the destination
     * @return The host name of the destination
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Returns the endpoint
     * @return The endpoint where the playback is made to
     */
    public NetworkEndpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Determines if an error has been raised
     * @return True if an error has been raised
     */
    public boolean isErrorSignalled() {
        return errorSignalled;
    }

    /**
     * Closes the transport
     *
     */
    public void close() {
        rtpListener.stop();
        rtcpListener.stop();
        rtpSock.close();
        rtcpSock.close();
    }

    /**
     * Returns the play session id using the transport
     * @return the id of the play session
     */
    public int getPlaySessionId() {
        return playSessionId;
    }

    // Handles the ssrc of incoming packets
    private void handleSsrc(long ssrc) {
        Object obj = ssrcMap.get(new Long(ssrc));
        if (obj == null) {
            ssrcMap.put(new Long(ssrc), new Long(ssrc));
        } else if (obj instanceof StreamSource) {
            ssrcMap.put(new Long(ssrc), new Long(ssrc));
            StreamSource source = (StreamSource) obj;
            source.changeSSRC(allocateSSRC(source));
        }
    }

    /**
     *
     * @see rtspd.RTPPacketSink#handleRTPPacket(rtspd.ReusablePacket)
     */
    public void handleRTPPacket(ReusablePacket packet) throws IOException {
        RTPHeader packetHeader = new RTPHeader(packet.getPacket());
        InetSocketAddress packetSourceHost =
            (InetSocketAddress) packet.getPacket().getSocketAddress();
        if (packetHeader.getVersion() == RTP_VERSION) {
            long ssrc = packetHeader.getSsrc();
            try {
                if (!packetSourceHost.equals(new InetSocketAddress(
                                InetAddress.getLocalHost(),
                                rtpAddress.getPort()))) {
                    handleSsrc(ssrc);
                }
            } catch (UnknownHostException e) {

                // Do Nothing
            }
        }
        packet.release();
    }


    /**
     *
     * @see rtspd.RTCPPacketSink#handleRTCPPacket(rtspd.ReusablePacket)
     */
    public void handleRTCPPacket(ReusablePacket packet) throws IOException {
        RTCPHeader packetHeader = new RTCPHeader(packet.getPacket());
        InetSocketAddress packetSourceHost =
            (InetSocketAddress) packet.getPacket().getSocketAddress();
        if (packetHeader.getVersion() == RTP_VERSION) {
            long ssrc = packetHeader.getSsrc();
            try {
                if (!packetSourceHost.equals(
                        new InetSocketAddress(
                                InetAddress.getLocalHost(),
                                rtcpAddress.getPort()))) {
                    handleSsrc(ssrc);
                }
            } catch (UnknownHostException e) {

                // Do Nothing
            }
        }
        packet.release();
    }
}