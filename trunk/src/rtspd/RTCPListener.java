/*
 * @(#)RTCPListener.java
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

import java.net.DatagramSocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Listens for RTCP packets and forwards them to a handler
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTCPListener implements ReactorHandler {

    // The log
    private static Log logger = LogFactory.getLog(RTCPListener.class.getName());

    // The object to handle the data
    private RTCPPacketSink packetSink = null;

    // The thread to read the data
    private Reactor reactor = null;
    private boolean started=false;

    /**
     * Creates a new RTCPListener
     * 
     * @param socket
     *            The socket to listen to data on
     * @param packetSink
     *            The location to send packets to
     */
    public RTCPListener(DatagramSocket socket, RTCPPacketSink packetSink) {
        this.packetSink = packetSink;
        reactor = new Reactor(socket, this);
    }

    /**
     * Starts the recording
     */
    public void record() {
    	if(started==false){
    		reactor.start();
    		started=true;
    	}
    }

    /**
     * Stops the recording
     */
    public void stop() {
        
        // Does Nothing
    }

    /**
     * 
     * @see rtspd.ReactorHandler#handleInput(rtspd.ReusablePacket)
     */
    public void handleInput(ReusablePacket packet) {

        try {

            if (packetSink != null) {

                // Handle this packet
                packetSink.handleRTCPPacket(packet);
            }
        } catch (Exception e) {
            logger.error("Error", e);
        }
    }
}