/*
 * @(#)RTPSocketAdapter.java
 * Created: 2005-04-21
 * Version: 1-1-alpha2
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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

import javax.media.protocol.PushSourceStream;
import javax.media.rtp.OutputDataStream;
import javax.media.rtp.RTPConnector;

import rtspd.RTCPPacketSink;
import rtspd.RTPPacketSink;

import memetic.crypto.EncryptedRTPSocket;
import memetic.crypto.RTPCrypt;

/**
 * An implementation of RTPConnector based on UDP sockets.
 *
 * @author Andrew G D Rowley
 * @version 1-1-alpha2
 */
public class RTPSocketAdapter implements RTPConnector {

    private static final String PORT_SEP = "/";

    // The size of the empty datagram for port opening
    private static final int EMPTY_DATAGRAM_SIZE = 100;

    // The data socket
    private EncryptedRTPSocket dataSock;

    // The control socket
    private EncryptedRTPSocket ctrlSock;

    // The address to send data to
    private InetAddress addr;

    // The port to send / receive data
    private int port;

    // The ttl to send with
    private int ttl;

    //  The port to send  data
    private int sendport;

    // The current encryption
    private RTPCrypt crypter = null;

    // The local address to bind to
    private InetAddress localAddress = null;

    // The input stream of the data socket
    private SimpleSocketInputStream dataInStrm = null;

    // The input stream of the control socket
    private SimpleSocketInputStream ctrlInStrm = null;

    // The output stream of the data socket
    private SocketOutputStream dataOutStrm = null;

    // The output stream of the control socket
    private SocketOutputStream ctrlOutStrm = null;
    
    /**
     * Creates a new RTPSocketAdaptor with TTL 1
     *
     * @param addr
     *            The address of the socket
     * @param port
     *            The port of the socket (data)
     * @throws IOException
     */
    public RTPSocketAdapter(InetAddress addr, int port) throws IOException {
        this(addr, port, 1);
    }

    /**
     * Creates a new RTPSocketAdaptor
     *
     * @param addr
     *            The address of the socket
     * @param port
     *            The port of the socket
     * @param ttl
     *            The ttl for sending data
     * @throws IOException
     */
    public RTPSocketAdapter(InetAddress addr, int port,
            int ttl)
            throws IOException {
        this(InetAddress.getLocalHost(), addr, port, ttl);
    }

    /**
     * Creates a new RTPSocketAdaptor
     *
     * @param laddr
     *            The local address to bind to
     * @param addr
     *            The address of the socket
     * @param port
     *            The port of the socket
     * @param ttl
     *            The ttl for sending data
     */
    public RTPSocketAdapter(InetAddress laddr, InetAddress addr, int port,
            int ttl) {

        // Set the address and port
        this.addr = addr;
        this.port = port;
        this.sendport = port;
        this.ttl = ttl;
        this.localAddress = laddr;
    }

    public void addForwardAddress(InetSocketAddress forwardAddress)
            throws IOException {
        getDataInputStream();
        getControlInputStream();
        dataInStrm.addForwardAddress(forwardAddress);
        InetSocketAddress rtcpForwardAddress = new InetSocketAddress(
                forwardAddress.getAddress(), forwardAddress.getPort() + 1);
        ctrlInStrm.addForwardAddress(rtcpForwardAddress);
    }

    /**
     * Gets the address of the socket
     * @return The address of the socket
     */
    public InetSocketAddress getAddress() {
        return new InetSocketAddress(addr, sendport);
    }

    /**
     * Gets the TTL
     * @return the TTL
     */
    public int getTTL() {
        return ttl;
    }

    public void setSendPort(int port){
        this.sendport=port;

    }

    // Creates the sockets
    private void createSockets() throws IOException {

        if (dataSock == null) {

            System.err.println("Starting Listening to "
                    + addr.getHostAddress() + PORT_SEP + port + PORT_SEP + ttl + " " + localAddress);

            // If the address is multicast, create multicast sockets and joing
            // groups etc
            if (addr.isMulticastAddress()) {
                dataSock = new EncryptedRTPSocket(null, port);
                ctrlSock = new EncryptedRTPSocket(null, port + 1);
                ((MulticastSocket) dataSock).setInterface(localAddress);
                ((MulticastSocket) dataSock).joinGroup(addr);
                ((MulticastSocket) dataSock).setTimeToLive(ttl);
                ((MulticastSocket) dataSock).setLoopbackMode(false);
                ((MulticastSocket) ctrlSock).setInterface(localAddress);
                ((MulticastSocket) ctrlSock).joinGroup(addr);
                ((MulticastSocket) ctrlSock).setTimeToLive(ttl);
                ((MulticastSocket) ctrlSock).setLoopbackMode(false);
            } else {

                // If the address is unicast, create unicast sockets
                dataSock = new EncryptedRTPSocket(null, port);
                ctrlSock = new EncryptedRTPSocket(null, port + 1);
            }

            dataSock.setEncryption(crypter);
            ctrlSock.setEncryption(crypter);

            // Send out data to open the correct ports
            dataSock.send(new DatagramPacket(new byte[EMPTY_DATAGRAM_SIZE],
                    EMPTY_DATAGRAM_SIZE, addr, port));
            ctrlSock.send(new DatagramPacket(new byte[EMPTY_DATAGRAM_SIZE],
                    EMPTY_DATAGRAM_SIZE, addr, port + 1));
        }
    }

    /**
     * Sets the encryption on the socket
     *
     * @param crypter
     *            The encrypter/decrypter
     */
    public void setEncryption(RTPCrypt crypter) {
        this.crypter = crypter;
        if (dataSock != null) {
            dataSock.setEncryption(crypter);
            ctrlSock.setEncryption(crypter);
        }
    }

    /**
     * Returns an input stream to receive the RTP data.
     * @throws IOException
     */
    public PushSourceStream getDataInputStream() throws IOException {
        if (dataInStrm == null) {
            createSockets();
            dataInStrm = new SimpleSocketInputStream(dataSock);
            dataInStrm.start();
        }
        return dataInStrm;
    }

    /**
     * Returns an output stream to send the RTP data.
     * @throws IOException
     */
    public OutputDataStream getDataOutputStream() throws IOException {
        if (dataOutStrm == null) {
            createSockets();
            dataOutStrm = new SocketOutputStream(dataSock, addr, sendport);
        }
        return dataOutStrm;
    }

    /**
     * Returns an input stream to receive the RTCP data.
     * @throws IOException
     */
    public PushSourceStream getControlInputStream() throws IOException {
        if (ctrlInStrm == null) {
            createSockets();
            ctrlInStrm = new SimpleSocketInputStream(ctrlSock);
            ctrlInStrm.start();
        }
        return ctrlInStrm;
    }

    /**
     * Returns an output stream to send the RTCP data.
     * @throws IOException
     */
    public OutputDataStream getControlOutputStream() throws IOException {
        if (ctrlOutStrm == null) {
            createSockets();
            ctrlOutStrm = new SocketOutputStream(ctrlSock, addr, sendport + 1);
        }
        return ctrlOutStrm;
    }

    /**
     * Close all the RTP, RTCP streams.
     */
    public void close() {
        if (dataInStrm != null) {
            dataInStrm.kill();
            dataInStrm = null;
        }
        if (ctrlInStrm != null) {
            ctrlInStrm.kill();
            ctrlInStrm = null;
        }
        if (dataOutStrm != null) {
            dataOutStrm = null;
        }
        if (ctrlOutStrm != null) {
            ctrlOutStrm = null;
        }
        if (dataSock != null) {
            dataSock.close();
            dataSock = null;
        }
        if (ctrlSock != null) {
            ctrlSock.close();
            ctrlSock = null;
        }
    }

    /**
     * Set the receive buffer size of the RTP data channel. This is only a hint
     * to the implementation. The actual implementation may not be able to do
     * anything to this.
     */
    public void setReceiveBufferSize(int size) throws IOException {
        dataSock.setReceiveBufferSize(size);
    }

    /**
     * Get the receive buffer size set on the RTP data channel. Return -1 if the
     * receive buffer size is not applicable for the implementation.
     */
    public int getReceiveBufferSize() {
        try {
            return dataSock.getReceiveBufferSize();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Set the send buffer size of the RTP data channel. This is only a hint to
     * the implementation. The actual implementation may not be able to do
     * anything to this.
     */
    public void setSendBufferSize(int size) throws IOException {
        dataSock.setSendBufferSize(size);
    }

    /**
     * Get the send buffer size set on the RTP data channel. Return -1 if the
     * send buffer size is not applicable for the implementation.
     */
    public int getSendBufferSize() {
        try {
            return dataSock.getSendBufferSize();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Return the RTCP bandwidth fraction. This value is used to initialize the
     * RTPManager. Check RTPManager for more detauls. Return -1 to use the
     * default values.
     */
    public double getRTCPBandwidthFraction() {
        return -1;
    }

    /**
     * Return the RTCP sender bandwidth fraction. This value is used to
     * initialize the RTPManager. Check RTPManager for more detauls. Return -1
     * to use the default values.
     */
    public double getRTCPSenderBandwidthFraction() {
        return -1;
    }

	public void setRtpPacketSink(RTPPacketSink rtpPacketSink) {
		dataInStrm.setRtpPacketSink(rtpPacketSink);
		
	}

	public void setRtcpPacketSink(RTCPPacketSink rtcpPacketSink) {
		ctrlInStrm.setRtcpPacketSink(rtcpPacketSink);
	}
}
