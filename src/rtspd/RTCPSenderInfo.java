/*
 * @(#)RTCPSenderInfo.java
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Represents the sender info part of the RTCP SR Packet
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTCPSenderInfo {

    /** 
     * The number of bytes in an SR packet (without the header)
     */
    public static final int SIZE = 20;
    
    // The mask to get the MSB
    private static final long MSB_MASK = 0x80000000L;

    // The maximum value of the fraction part of the time
    private static final long MAX_FRACTION = 0x100000000L;

    // The number of ms in a second
    private static final double MS_PER_SEC = 1000D;

    // baseline NTP time if bit-0=0 -> 7-Feb-2036 @ 06:28:16 UTC
    private static final long MSB_0_BASE_TIME = 2085978496000L;

    // baseline NTP time if bit-0=1 -> 1-Jan-1900 @ 01:00:00 UTC
    private static final long MSB_1_BASE_TIME = -2208988800000L;

    // The Most Significant word of the timestamp
    private long ntpTimestampMSW = 0;

    // The Least Significant word of the timestamp
    private long ntpTimestampLSW = 0;

    // The RTP timestamp
    private long rtpTimestamp = 0;

    // The packet count
    private long packetCount = 0;

    // The octet count
    private long octetCount = 0;

    /**
     * Parses an RTCP SR packet
     * 
     * @param rtcpPacket
     *            The data of the RTCP packet
     * @throws IOException
     */
    public RTCPSenderInfo(byte[] rtcpPacket) throws IOException {
        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(
                rtcpPacket));
        ntpTimestampMSW = stream.readInt() & RTPHeader.UINT_TO_LONG_CONVERT;
        ntpTimestampLSW = stream.readInt() & RTPHeader.UINT_TO_LONG_CONVERT;
        rtpTimestamp = stream.readInt() & RTPHeader.UINT_TO_LONG_CONVERT;
        packetCount = stream.readInt() & RTPHeader.UINT_TO_LONG_CONVERT;
        octetCount = stream.readInt() & RTPHeader.UINT_TO_LONG_CONVERT;
    }

    /**
     * Returns the timestamp of the information
     * 
     * @return the ntp timestamp in milliseconds
     */
    public long getTimestamp() {
        long seconds = ntpTimestampMSW;
        long fraction = ntpTimestampLSW;

        // Use round-off on fractional part to preserve going to lower precision
        fraction = Math.round(MS_PER_SEC * fraction / MAX_FRACTION);

        /*
         * If the most significant bit (MSB) on the seconds field is set we use
         * a different time base. The following text is a quote from RFC-2030
         * (SNTP v4):
         * 
         * If bit 0 is set, the UTC time is in the range 1968-2036 and UTC time
         * is reckoned from 0h 0m 0s UTC on 1 January 1900. If bit 0 is not set,
         * the time is in the range 2036-2104 and UTC time is reckoned from 6h
         * 28m 16s UTC on 7 February 2036.
         */
        long msb = seconds & MSB_MASK;
        if (msb == 0) {
            
            // use base: 7-Feb-2036 @ 06:28:16 UTC
            return MSB_0_BASE_TIME + (seconds * (int) MS_PER_SEC) + fraction;
        }
        
        // use base: 1-Jan-1900 @ 01:00:00 UTC
        return MSB_1_BASE_TIME + (seconds *  (int) MS_PER_SEC) + fraction;
    }

    /**
     * Returns the timestamp value in seconds
     * 
     * @return the timestamp in seconds
     */
    public double getNtpTimestampSecs() {
        return getTimestamp() / MS_PER_SEC;
    }

    /**
     * Returns the timestamp most significant word
     * 
     * @return the MSW of the ntp timestamp
     */
    public long getNtpTimestampMSW() {
        return ntpTimestampMSW;
    }

    /**
     * Returns the timestamp least significant word
     * 
     * @return the LSW of the ntp timestamp
     */
    public long getNtpTimestampLSW() {
        return ntpTimestampLSW;
    }

    /**
     * Returns the rtp timestamp
     * 
     * @return the rtp timestamp
     */
    public long getRtpTimestamp() {
        return rtpTimestamp;
    }

    /**
     * Returns the octet count
     * 
     * @return the octet count
     */
    public long getOctetCount() {
        return octetCount;
    }

    /**
     * Returns the packet count
     * 
     * @return the packet count
     */
    public long getPacketCount() {
        return packetCount;
    }
}