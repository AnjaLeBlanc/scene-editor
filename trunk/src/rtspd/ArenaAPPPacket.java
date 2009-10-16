/*
 * @(#)ArenaAPPPacket.java
 * Created: 06-Apr-2006
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An RTCP APP Packet Sent by Arena for timing information
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class ArenaAPPPacket {

    /**
     * The length of the packet in bytes
     */
    public static final int LENGTH = 24;
    
    // The log file
    private static Log logger = 
        LogFactory.getLog(ArenaAPPPacket.class.getName());
    
    // The length of the app packet in words - 1
    private static final int PACKET_LENGTH = 3;
    
    // The position of the first byte of the time in the packet
    private static final int TIME_POS_1 = 20;
    
    // The position of the second byte of the time in the packet
    private static final int TIME_POS_2 = 21;
    
    // The position of the third byte of the time in the packet
    private static final int TIME_POS_3 = 22;
    
    // The position of the fourth byte of the time in the packet
    private static final int TIME_POS_4 = 23;
    
    // The type of the APP packet (must be 4 chars according to RTCP spec)
    public static final String APP_PACKET_TYPE = "ARNA";
    
    // The RTCP packet to send
    private byte[] rtcpPacket = new byte[0];
    
    /**
     * Creates a new ArenaAPPPacket
     */
    public ArenaAPPPacket() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        
        // The RR packet
        try {
            RTCPHeader rrHeader = 
                new RTCPHeader(false, 0, RTCPHeader.PT_RR, 1, 0);
            RTCPHeader appHeader = 
                new RTCPHeader(false, 0, RTCPHeader.PT_APP, 
                        PACKET_LENGTH, 0);
            
            output.write(rrHeader.getBytes());
            
            // Write the APP packet header
            output.write(appHeader.getBytes());
            output.write(new String(APP_PACKET_TYPE).getBytes("UTF-8"));
            output.writeInt(0);
            
            // Create the packet
            output.close();
            bytes.close();
            rtcpPacket = bytes.toByteArray();
        } catch (IOException e) {
            logger.debug("Error", e);
        }
    }
    
    /**
     * Sets the time to be sent with the APP Packet
     * @param time The time in milliseconds
     */
    public void setTime(long time) {
        rtcpPacket[TIME_POS_1] = (byte) 
            ((time >> RTCPHeader.INT1_TO_BYTE_SHIFT) & RTCPHeader.INT_TO_BYTE);
        rtcpPacket[TIME_POS_2] = (byte) 
            ((time >> RTCPHeader.INT2_TO_BYTE_SHIFT) & RTCPHeader.INT_TO_BYTE);
        rtcpPacket[TIME_POS_3] = (byte) 
            ((time >> RTCPHeader.INT3_TO_BYTE_SHIFT) & RTCPHeader.INT_TO_BYTE);
        rtcpPacket[TIME_POS_4] = (byte)
            ((time >> RTCPHeader.INT4_TO_BYTE_SHIFT) & RTCPHeader.INT_TO_BYTE);
    }
    
    /**
     * Returns the APP packet to be sent
     * @return A Byte Array
     */
    public byte[] getBytes() {
        return rtcpPacket;
    }
}
