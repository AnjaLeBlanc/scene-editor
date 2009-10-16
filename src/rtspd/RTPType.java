/*
 * @(#)RTPType.java
 * Created: 05-Aug-2005
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

/**
 * Represents the RDF for an RTP Type
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RTPType {

    /**
     * The RTPType type
     */
    public static final String TYPE = NetworkEndpoint.MEMETIC + "RTP-Type";

    /**
     * The id field
     */
    public static final String ID = NetworkEndpoint.MEMETIC + "has-rtp-type";

    /**
     * The media field
     */
    public static final String MEDIA = NetworkEndpoint.MEMETIC + "has-media";

    /**
     * The encoding field
     */
    public static final String ENCODING = NetworkEndpoint.MEMETIC 
        + "has-encoding";

    /**
     * The clock field
     */
    public static final String CLOCK = NetworkEndpoint.MEMETIC + "has-clock";

    /**
     * The channels field
     */
    public static final String CHANNELS = NetworkEndpoint.MEMETIC 
        + "has-channels";

    /**
     * The RTP Audio Type
     */
    public static final String AUDIO = "audio";

    /**
     * The RTP Video Type
     */
    public static final String VIDEO = "video";

    /**
     * G723 Audio
     */
    public static final String G723 = "G723";

    /**
     * PCMU Audio
     */
    public static final String PCMU = "PCMU";

    /**
     * GSM Audio
     */
    public static final String GSM = "GSM";

    /**
     * DVI4 Audio
     */
    public static final String DVI4 = "DVI4";

    /**
     * LPC Audio
     */
    public static final String LPC = "LPC";

    /**
     * PCMA Audio
     */
    public static final String PCMA = "PCMA";

    /**
     * G722 Audio
     */
    public static final String G722 = "G722";

    /**
     * L16 Audio
     */
    public static final String L16 = "L16";

    /**
     * QCELP Audio
     */
    public static final String QCELP = "QCELP";

    /**
     * MPEG Audio
     */
    public static final String MPA = "MPA";

    /**
     * CN Audio
     */
    public static final String CN = "CN";

    /**
     * G728 Audio
     */
    public static final String G728 = "G728";

    /**
     * G729 Audio
     */
    public static final String G729 = "G729";

    /**
     * CelB Video
     */
    public static final String CEL_B = "CelB";

    /**
     * JPEG Video
     */
    public static final String JPEG = "JPEG";

    /**
     * NV Video
     */
    public static final String NV = "nv";

    /**
     * H261 Video
     */
    public static final String H261 = "H261";

    /**
     * MPV Video
     */
    public static final String MPV = "MPV";

    /**
     * MP2T Video
     */
    public static final String MP2T = "MP2T";

    /**
     * H.263 Video
     */
    public static final String H263 = "H263";
    
    /**
     * MPEG-4 Video
     */
    public static final String MPEG4 = "MPEG4";
    
    /**
     * H264 Video
     */
    public static final String H264 = "H264";

    /**
     * RAT Audio
     */
    public static final String RAT_AG_AUDIO = "RAT-L16-16K-MONO";

    /**
     * INSORS Audio
     */
    public static final String INSORS_AG_AUDIO = "InSORS-L16-16K-MONO";

    /**
     * Clock rate of 8000Hz
     */
    public static final int CLOCK_8KHZ = 8000;

    /**
     * Clock rate of 16000Hz
     */
    public static final int CLOCK_16KHZ = 16000;

    /**
     * Clock rate of 44100Hz
     */
    public static final int CLOCK_44_1KHZ = 44100;

    /**
     * Default Video Clock Rate
     */
    public static final int CLOCK_VIDEO = 90000;

    /**
     * Clock rate of 11025Hz
     */
    public static final int CLOCK_11_025KHZ = 11025;

    /**
     * Clock rate of 22050Hz
     */
    public static final int CLOCK_22_05KHZ = 22050;

    /**
     * Mono Channels
     */
    public static final int MONO = 1;

    /**
     * Stereo Channels
     */
    public static final int STEREO = 2;

    /**
     * PCMU Audio Type
     */
    public static final int RTP_PCMU = 0;

    /**
     * GSM Audio Type
     */
    public static final int RTP_GSM = 3;

    /**
     * G723 Audio Type
     */
    public static final int RTP_G723 = 4;

    /**
     * DVI4 Audio Type at 8Khz
     */
    public static final int RTP_DVI8 = 5;

    /**
     * DVI4 Audio Type at 16Khz
     */
    public static final int RTP_DVI16 = 6;

    /**
     * LPC Audio Type
     */
    public static final int RTP_LPC = 7;

    /**
     * PCMA Audio Type
     */
    public static final int RTP_PCMA = 8;

    /**
     * G722 Audio Type
     */
    public static final int RTP_G722 = 9;

    /**
     * L16 Stereo Audio Type
     */
    public static final int RTP_L16_STEREO = 10;

    /**
     * L16 Mono Audio Type
     */
    public static final int RTP_L16_MONO = 11;

    /**
     * QCELP Audio Type
     */
    public static final int RTP_QCELP = 12;

    /**
     * CN Audio Type
     */
    public static final int RTP_CN = 13;

    /**
     * MPEG Audio Type
     */
    public static final int RTP_MPA = 14;

    /**
     * G728 Audio Type
     */
    public static final int RTP_G728 = 15;

    /**
     * DVI4 Audio Type at 11.025Khz
     */
    public static final int RTP_DVI11 = 16;

    /**
     * DVI4 Audio Type at 22.05Khz
     */
    public static final int RTP_DVI_22 = 17;

    /**
     * G729 Audio Type
     */
    public static final int RTP_G729 = 18;

    /**
     * CEL-B Video Type
     */
    public static final int RTP_CELB = 25;

    /**
     * Motion JPEG Video Type
     */
    public static final int RTP_MJPEG = 26;

    /**
     * NV Video Type
     */
    public static final int RTP_NV = 28;

    /**
     * H.261 Video Type
     */
    public static final int RTP_H261 = 31;

    /**
     * MPV Video Type
     */
    public static final int RTP_MPV = 32;

    /**
     * MP2T Video Type
     */
    public static final int RTP_MP2T = 33;

    /**
     * H263 Video Type
     */
    public static final int RTP_H263 = 34;
    
    /**
     * MPEG4 Video Type
     */
    public static final int RTP_MPEG4 = 45;
    
    /**
     * MPEG4-XVID Video Type
     */
    public static final int RTP_MPEG4_XVID = 46;
    
    /**
     * H.264-X264 Video Type
     */
    public static final int RTP_H264_X264 = 47;

    /**
     * RTP Type Dynamic Number 112
     */
    public static final int RTP_DYNAMIC_112 = 112;

    /**
     * RTP Type Dynamic Number 84
     */
    public static final int RTP_DYNAMIC_84 = 84;

    /**
     * The event namespace postfix
     */ 
    public static final String NAMESPACE_EVENT = "Event_";

    /**
     * The RTPType namespace postfix
     */ 
    public static final String NAMESPACE_RTPTYPE = "RTPType";
}
