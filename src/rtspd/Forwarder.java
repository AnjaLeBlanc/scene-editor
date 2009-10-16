/*
 * @(#)Forwarder.java
 * Created: 07-Jul-2005
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
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Forwards packets onto users
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class Forwarder extends Thread {

    // The time to wait
    private static final int MAX_WAIT_TIME = 1000;

    // The log file for the database
    private static Log logger = LogFactory.getLog(Forwarder.class.getName());

    // People to forward the datagrams on to
    private Vector<InetSocketAddress> forwards = new Vector<InetSocketAddress>();

    // A queue of packets to be forwarded
    private LinkedList<DatagramPacket> queue = new LinkedList<DatagramPacket>();

    // A datagram socket to send requests on
    private DatagramSocket socket = null;

    // True if the forwarder is finished with
    private boolean done = false;

    /**
     * Creates a new forwarder
     * 
     * @param socket
     *            The socket to send out packets on
     */
    public Forwarder(DatagramSocket socket) {
        this.socket = socket;
    }

    /**
     * Add a person to forward to
     * 
     * @param address
     *            The address to forward to
     */
    public void addListener(InetSocketAddress address) {
        synchronized (forwards) {
            forwards.add(address);
            forwards.notifyAll();
        }
    }

    /**
     * Adds a packet to the queue of packets to be processed
     * 
     * @param packet
     *            The packet to be processed
     */
    public void addPacket(DatagramPacket packet) {
        if (forwards.size() > 0) {
            synchronized (queue) {
                queue.addLast(packet);
                queue.notifyAll();
            }
        }
    }

    // Returns the next packet in the queue
    private DatagramPacket getNextPacket() {
        synchronized (queue) {
            while (!done && queue.isEmpty()) {
                try {
                    queue.wait(MAX_WAIT_TIME);
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
            if (!queue.isEmpty()) {
                return (DatagramPacket) queue.removeFirst();
            }
            return null;
        }
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while (!done) {
            DatagramPacket packet = null;
            synchronized (forwards) {
                while (!done && forwards.isEmpty()) {
                    try {
                        forwards.wait(MAX_WAIT_TIME);
                    } catch (InterruptedException e) {
                        // Do Nothing
                    }
                }
            }
            packet = getNextPacket();
            if (packet != null) {
                for (int i = 0; i < forwards.size(); i++) {
                    InetSocketAddress address = (InetSocketAddress) forwards
                            .get(i);
                    packet.setSocketAddress(address);
                    try {
                        socket.send(packet);
                    } catch (IOException e) {
                        logger.error("Error", e);
                    }
                }
            }
        }
    }

    /**
     * Stops the forwarder
     */
    public void end() {
        done = true;
        socket.close();
    }
}
