/*
 * @(#)Reactor.java
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
import java.net.DatagramSocket;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handles input from a datagram socket
 *
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class Reactor extends Thread {

    // The log file
    private static Log logger = LogFactory.getLog(Reactor.class.getName());

    // The socket to receive input from
    private DatagramSocket socket = null;

    // The handler of incoming traffic
    private ReactorHandler handler = null;

    /**
     * Creates a new Reactor
     *
     * @param socket
     *            The socket for listen to packets on
     * @param handler
     *            The handler to handle the packets
     */
    public Reactor(DatagramSocket socket, ReactorHandler handler) {
        this.socket = socket;
        this.handler = handler;
        System.out.println("Reactor " + socket.getLocalPort() + " handler " + handler.toString());
    }

    /**
     * The thread function. Gets and handles packets from the socket
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {
    	System.out.println("Reactor run " + socket.getLocalPort() + " isClosed " + socket.isClosed());
        while ((socket != null) && (!socket.isClosed())) {
            ReusablePacket packet = null;
            try {
                packet = ReusablePacketQueue.getInstance().getFreePacket();
//                System.out.println("before socket receive");
                socket.receive(packet.getPacket());
//                System.out.println("got packet " + packet);
                handler.handleInput(packet);
            } catch (SocketException e) {
                logger.debug("Socket Closed, Reactor finising");
                System.err.println("Socket Closed, Reactor finising");
                if (packet != null) {
                    packet.release();
                }
            } catch (IOException e) {
                logger.error("Error", e);
                System.err.println("Error " + e);
                if (packet != null) {
                    packet.release();
                }
            }
        }
    }
}