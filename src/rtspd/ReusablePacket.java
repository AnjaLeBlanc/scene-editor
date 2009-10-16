/*
 * @(#)ReusablePacket.java
 * Created: 03-Nov-2006
 * Version: 1.0
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

import java.net.DatagramPacket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A packet that can be reused
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class ReusablePacket {

    // The log file
    private static Log logger = LogFactory.getLog(
            ReusablePacketQueue.class.getName());
    
    // The length of the buffer
    private static final int BUFFER_LENGTH = 8096;
    
    // The packet
    private final DatagramPacket packet = new DatagramPacket(
            new byte[BUFFER_LENGTH], BUFFER_LENGTH);
    
    // The queue of data
    private ReusablePacketQueue queue = null;
    
    // True if reused already
    private boolean released = true;
    
    // The throwable when the object was last used
    private Throwable throwable = null;
    
    /**
     * Creates a new Reusable Packet
     * @param queue The queue
     */
    public ReusablePacket(ReusablePacketQueue queue) {
        this.queue = queue;
    }
    
    /**
     * Starts using the packet
     */
    public void use() {
        if (released) {
            released = false;
            throwable = new Throwable();
        } else {
            logger.warn("Using packet already in use");
            logger.warn("", throwable);
            throwable = new Throwable();
        }
    }
    
    /**
     * Releases the packet back to be reused
     */
    public void release() {
        if (!released) {
            packet.setData(packet.getData(), 0, BUFFER_LENGTH);
            queue.releasePacket(this);
            released = true;
        }
    }
    
    /**
     * Returns the packet from the queue
     * @return the packet
     */
    public DatagramPacket getPacket() {
        if (released) {
            logger.warn("Using datagram that has been released");
        }
        return packet;
    }
    
    /**
     * 
     * @see java.lang.Object#finalize()
     */
    public void finalize() {
        queue.releasePacket();
    }
}
