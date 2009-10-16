/*
 * @(#)RTPTransmissionStats.java
 * Created: 08-Dec-2005
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

import javax.media.rtp.TransmissionStats;

/**
 * Represents statistics for transmission
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public class RTPTransmissionStats implements TransmissionStats {
    
    // The number of bytes transmitted
    private int bytesTransmitted = 0;
    
    // The number of packets transmitted
    private int pduTransmitted = 0;

    /**
     * 
     * @see javax.media.rtp.TransmissionStats#getPDUTransmitted()
     */
    public int getPDUTransmitted() {
        return pduTransmitted;
    }

    /**
     * 
     * @see javax.media.rtp.TransmissionStats#getBytesTransmitted()
     */
    public int getBytesTransmitted() {
        return bytesTransmitted;
    }

    /**
     * 
     * @see javax.media.rtp.TransmissionStats#getRTCPSent()
     */
    public int getRTCPSent() {
        return 0;
    }
    
    /**
     * Adds a packet to the number transmitted
     */
    public void addPDUTransmitted() {
        pduTransmitted++;
    }
    
    /**
     * Adds some bytes to the number transmitted
     * @param bytes The number of bytes to add
     */
    public void addBytesTransmitted(int bytes) {
        bytesTransmitted += bytes;
    }

}
