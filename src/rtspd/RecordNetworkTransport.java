/*
 * @(#)RecordNetworkTransport.java
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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import memetic.crypto.EncryptedRTPSocket;
import memetic.crypto.RTPCrypt;

/**
 * Sets up the sockets to be used for receiving recorded streams
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RecordNetworkTransport extends TimerTask {

    // The time to wait between sending the RTCP packet
    private static final int PACKET_SEND_TIME = 5000;

    // How many SDES items are in the packet
    private static final int SDES_ITEM_COUNT = 2;

    // The log error message
    private static final String ERROR_MESSAGE = "Error";

    // The separator between port and address
    private static final String PORT_SEP = "/";

    // The size of the recieve buffer
    private static final int RECEIVE_BUFFER_SIZE = 64000;

    // The default name of the server
    private static final String SERVER_NAME = "Arena@";

    // The default host name
    private static final String DEFAULT_HOST = "ArenaServer";

    // The log file
    private static Log logger = 
        LogFactory.getLog(RecordNetworkTransport.class.getName());

    // The endpoint from which recordings are to be made
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
    private boolean isErrorSignalled = false;
    
    // The RR send timer
    private Timer timer = new Timer();
    
    // The ssrc of the report packets
    private long ssrc = (long) (Math.random() * Integer.MAX_VALUE);
    
    // The RTCP packet to send
    private byte[] rtcpPacket = new byte[0];

    /**
     * Creates a new RecordNetworkTransport
     * 
     * @param ePoint
     *            The endpoint from which to record
     * @param crypt
     *            The encryption or null for none
     */
    public RecordNetworkTransport(NetworkEndpoint ePoint, RTPCrypt crypt) {
        String localAddress = DEFAULT_HOST;
        byte[] name = new byte[0];
        int sdesItemLength = 0;
        int sdesLength = 0;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        
        // Set up the addressing
        endpoint = ePoint;
        rtpAddress = ePoint.getRtpAddress();
        rtcpAddress = ePoint.getRtcpAddress();
        isMulticast = rtpAddress.getAddress().isMulticastAddress();

        // Get the host name
        logger.debug("Record_Network_Transport::Record_Network_Transport: RTP"
                + " for " + rtpAddress.getAddress() + PORT_SEP
                + rtpAddress.getPort());
        logger.debug("Record_Network_Transport::Record_Network_Transport: "
                + "RTCP for " + rtcpAddress.getAddress() + PORT_SEP
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
                logger.error(ERROR_MESSAGE, e);
                isErrorSignalled = true;
            }
        } else {

            // If the endpoint is unicast, set up unicast sockets
            try {
                rtpSock = new EncryptedRTPSocket(crypt, rtpAddress.getPort());
                rtcpSock = new EncryptedRTPSocket(crypt, 
                        rtcpAddress.getPort());
            } catch (Exception e) {
                logger.error(ERROR_MESSAGE, e);
                isErrorSignalled = true;
            }
        }
        
        // Set the socket receive buffer size
        try {
            rtpSock.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
            rtcpSock.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
        } catch (Exception e) {
            logger.error(ERROR_MESSAGE, e);
        }
        
        // Generate an ssrc
        try {
            localAddress = InetAddress.getLocalHost().getHostName();
        } catch (IOException e) {
            
            // Do Nothing
        }
        
        // Setup a receiver report (size is RTCP RR header + RTCP SDES header 
        // + 2 sdes items (cname + name) + 2 "Arena@hostname" + padding
        try {
            name = (new String(SERVER_NAME + localAddress)).getBytes(
                    RTCPHeader.SDES_ENCODING);
        } catch (UnsupportedEncodingException e) {
            // Do Nothing
        }
        sdesItemLength = RTCPHeader.SDES_TYPE_LENGTH
                + RTCPHeader.SDES_LENGTH_LENGTH + name.length;
        sdesLength = RTCPHeader.SIZE + (SDES_ITEM_COUNT * sdesItemLength) + 1;
        
        // The RR packet
        try {
            RTCPHeader sdesHeader = null;
            RTCPHeader rrHeader = 
                new RTCPHeader(false, 0, RTCPHeader.PT_RR, 1, ssrc);
            int padding = RTCPHeader.BYTES_PER_INT
                    - (sdesLength % RTCPHeader.BYTES_PER_INT);
            output.write(rrHeader.getBytes());
            
            // Add SDES packet
            if (padding == RTCPHeader.BYTES_PER_INT) {
               padding = 0;
            }
            sdesLength += padding;
            sdesHeader = new RTCPHeader(false, 1, RTCPHeader.PT_SDES, 
                        (sdesLength / RTCPHeader.BYTES_PER_INT) - 1, ssrc);
            output.write(sdesHeader.getBytes());
            
            // The CNAME packet
            output.writeByte(RTCPHeader.SDES_CNAME);
            output.writeByte(name.length);
            output.write(name);
            
            // The NAME packet
            output.writeByte(RTCPHeader.SDES_NAME);
            output.writeByte(name.length);
            output.write(name);
            
            // The null item
            output.write(0);
            
            // Add the padding
            for (int i = 0; i < padding; i++) {
                output.writeByte(0);
            }
            
            output.close();
            bytes.close();
            rtcpPacket = bytes.toByteArray();
            
            // Schedule the sending of the packet
            timer.schedule(this, 0, PACKET_SEND_TIME);
        } catch (IOException e) {
            logger.debug(ERROR_MESSAGE, e);
        }
    }

    /**
     * Returns the RTP socket
     * @return The RTP socket
     */
    public DatagramSocket getRtpSocket() {
        return rtpSock;
    }

    /**
     * Returns the RTCP socket
     * @return The RTCP socket
     */
    public DatagramSocket getRtcpSocket() {
        return rtcpSock;
    }

    /**
     * Returns the destination
     * @return The host name of the destination
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Returns the endpoint
     * @return The endpoint where the recording is made from
     */
    public NetworkEndpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Returns true if multicast is in use
     * @return True if the endpoint is multicast
     */
    public boolean isMulticast() {
        return isMulticast;
    }

    /**
     * Determines if an error has been raised
     * @return Any error that has been raised
     */
    public boolean isErrorSignalled() {
        return isErrorSignalled;
    }
    
    private void sendBye() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        try {
            byte[] bye = null;
            RTCPHeader rrHeader = 
                new RTCPHeader(false, 0, RTCPHeader.PT_RR, 1, ssrc);
            RTCPHeader byeHeader =
                new RTCPHeader(false, 0, RTCPHeader.PT_BYE, 1, ssrc);
            output.write(rrHeader.getBytes());
            output.write(byeHeader.getBytes());
            output.close();
            bytes.close();
            bye = bytes.toByteArray();
            rtcpSock.send(new DatagramPacket(bye, bye.length, 
                    rtcpAddress.getAddress(), rtcpAddress.getPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the transport
     */
    public void close() {
        timer.cancel();
        sendBye();
        rtpSock.close();
        rtcpSock.close();
    }
    
    /**
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            rtcpSock.send(new DatagramPacket(rtcpPacket, rtcpPacket.length, 
                    rtcpAddress.getAddress(), rtcpAddress.getPort()));
        } catch (IOException e) {
            logger.debug(ERROR_MESSAGE, e);
        }
    }
}