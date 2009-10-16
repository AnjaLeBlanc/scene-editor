/*
 * @(#)ReusablePacketQueue.java
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

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A queue of packets that can be reused thus avoiding memory outages
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class ReusablePacketQueue {
    
    // The log file
    private static Log logger = LogFactory.getLog(
            ReusablePacketQueue.class.getName());

    // The maximum number of datagram packets
    private static final int MAX_PACKET = 1000000;
    
    // The packets to use for the queue
    private static final ReusablePacketQueue PACKETS = 
        new ReusablePacketQueue(MAX_PACKET);
    
    // The list of reusable packets
    private LinkedList<ReusablePacket> reusable = new LinkedList<ReusablePacket>();
    
    // The number of free packets
    private int free = 0;
    
    // The number of packets created
    private int created = 0;
    
    // The number of packets in the queue
    private int queued = 0;
    
    /**
     * Creates a new ReusablePacketQueue
     * @param size The initial size of the queue
     */
    private ReusablePacketQueue(int size) {
        this.free = size;
    }
    
    /**
     * Gets the instance of the queue
     * @return The queue
     */
    public static ReusablePacketQueue getInstance() {
        return PACKETS;
    }
    
    /**
     * Releases the give packet to be reused
     * @param packet The packet to release
     */
    public void releasePacket(ReusablePacket packet) {
        synchronized (reusable) {
            reusable.addLast(packet);
            queued++;
            free++;
            /*logger.debug("ReleasePacket: Free = " + free + ", created = " 
                    + created + ", queued = " + queued); */
            reusable.notifyAll();
        }
    }
    
    /**
     * Releases a packet that has been finalized
     */
    public void releasePacket() {
        synchronized (reusable) {
            if (free < MAX_PACKET) {
                free++;
                reusable.notifyAll();
            }
        }
    }
    
    /**
     * Gets a free packet (may have to wait for one to become available)
     * @return A packet that has not been used
     */
    public ReusablePacket getFreePacket() {
        ReusablePacket packet = null;
        synchronized (reusable) {
            /*logger.debug("GetFreePacket: Free = " + free + ", created = " 
                    + created + ", queued = " + queued); */
            while (free == 0) {
                try {
                    logger.debug("Waiting for packet to become free");
                    reusable.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
            
            free--;
            if (reusable.isEmpty()) {
                packet = new ReusablePacket(this);
                created++;
            } else {
                packet = (ReusablePacket) reusable.removeFirst();
                queued--;
            }
        }
        packet.use();
        return packet;
    }
}
