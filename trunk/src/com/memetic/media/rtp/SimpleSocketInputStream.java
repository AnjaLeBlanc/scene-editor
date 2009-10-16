/*
 * @(#)SocketInputStream.java
 * Created: 26-Oct-2005
 * Version: TODO
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
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Vector;

import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.SourceTransferHandler;

import rtspd.RTCPPacketSink;
import rtspd.RTPPacketSink;
import rtspd.ReusablePacket;
import rtspd.ReusablePacketQueue;

/**
 * A DatagramSocket Input Stream Adapter
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public class SimpleSocketInputStream extends Thread
        implements PushSourceStream {

    private static final int DEFAULT_MTU = 2048;

    // The datagram socket
    private DatagramSocket socket = null;

    // True if the receiving is finished
    private boolean done = false;

    // The transfer handler
    private SourceTransferHandler handler = null;

    // True if data has been read
    private boolean dataRead = false;

    private Vector<InetSocketAddress> forwardAddresses =
        new Vector<InetSocketAddress>();
    
    private RTPPacketSink rtpPacketSink = null;
    private RTCPPacketSink rtcpPacketSink = null;
    

    /**
     * Creates a new SocketInputStream
     * @param socket The socket to handle
     */
    public SimpleSocketInputStream(DatagramSocket socket) {
        this.socket = socket;
    }

    /**
     * Adds an address to forward to
     * @param forwardAddress The address to forward to
     */
    public void addForwardAddress(InetSocketAddress forwardAddress) {
        this.forwardAddresses.add(forwardAddress);
    }

    /**
     *
     * @see javax.media.protocol.PushSourceStream#read(byte[], int, int)
     */
    public int read(byte[] buffer, int offset, int length) {
        //helper.end();
        try {
            DatagramPacket packet = new DatagramPacket(buffer, offset, length);
            socket.receive(packet);
            ReusablePacket packetCopy= ReusablePacketQueue.getInstance().getFreePacket();
            DatagramPacket copyDG=packetCopy.getPacket();
            copyDG.setSocketAddress(packet.getSocketAddress());
            byte[] data = copyDG.getData();
            System.arraycopy(buffer, 0, data, 0, length);
            copyDG.setData(data, packet.getOffset(), packet.getLength());
            
            InetSocketAddress orig = (InetSocketAddress)
                packet.getSocketAddress();
//            for (int i = 0; i < forwardAddresses.size(); i++) {
//                packet.setSocketAddress(forwardAddresses.get(i));
//                socket.send(packet);
//            }
//            packet.setSocketAddress(orig);
            if(orig.getPort()%2==1){
            	if(rtcpPacketSink!=null){
            		rtcpPacketSink.handleRTCPPacket(packetCopy);
            	}
            } else {
            	if(rtpPacketSink!=null){
            		rtpPacketSink.handleRTPPacket(packetCopy);
            	}
            }
            synchronized (this) {
                dataRead = true;
                notify();
            }
            return packet.getLength();
        } catch (IOException e) {
            synchronized (this) {
                notify();
            }
            return 0;
        }
    }

    /**
     *
     * @see javax.media.protocol.PushSourceStream#getMinimumTransferSize()
     */
    public int getMinimumTransferSize() {

        // There is currently no way to get the MTU in Java, so return a
        // suitable large value
        return DEFAULT_MTU;
    }

    /**
     *
     * @see javax.media.protocol.PushSourceStream#setTransferHandler(
     *     javax.media.protocol.SourceTransferHandler)
     */
    public synchronized void setTransferHandler(
            SourceTransferHandler transferHandler) {
        this.handler = transferHandler;
        if (handler != null) {
            dataRead = true;
            notify();
        }
    }

    /**
     *
     * @see javax.media.protocol.SourceStream#getContentDescriptor()
     */
    public ContentDescriptor getContentDescriptor() {
        return null;
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
        return done;
    }

    /**
     *
     * @see javax.media.Controls#getControls()
     */
    public Object[] getControls() {
        return new Object[0];
    }

    /**
     *
     * @see javax.media.Controls#getControl(java.lang.String)
     */
    public Object getControl(String controlType) {
        return null;
    }

    /**
     *
     * @see java.lang.Thread#start()
     */
    public void start() {
        super.start();
        if (handler != null) {
            synchronized (this) {
                dataRead = true;
                notify();
            }
        }
    }

    /**
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while (!done) {
            synchronized (this) {
                while (!dataRead && !done) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // Do Nothing
                    }
                }
                dataRead = false;
                if (!done && (handler != null)) {
                    handler.transferData(this);
                }
            }
        }
    }

    /**
     * Stops the socket
     */
    public void kill() {
        done = true;
        socket.close();
    }

	public void setRtpPacketSink(RTPPacketSink rtpPacketSink) {
		this.rtpPacketSink = rtpPacketSink;
	}

	public void setRtcpPacketSink(RTCPPacketSink rtcpPacketSink) {
		this.rtcpPacketSink = rtcpPacketSink;
	}
}
