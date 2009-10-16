/*
 * @(#)RecordListener.java
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Listens for recordings to make
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class RecordListener {

    // The log file
    private static Log logger = 
        LogFactory.getLog(RecordListener.class.getName());

    // The actual transport used to receive packets
    private RecordNetworkTransport netTrans = null;

    // Listens for RTP packets
    private RTPListener rtpListener = null;

    // Listens for RTCP packets
    private RTCPListener rtcpListener = null;

    // True if the listener has been started
    private boolean started = false;

    /**
     * Creates a new RecordListener
     * 
     * @param netTrans
     *            The transport to listen on
     * @param archiveMgr
     *            The archive manager to record to
     */
    public RecordListener(RecordNetworkTransport netTrans,
            RecordArchiveManager archiveMgr) {
        this.netTrans = netTrans;

        // Create the RTP and RTCP listeners
        logger.debug("New Record_Listener");
        rtpListener = new RTPListener(netTrans.getRtpSocket(), archiveMgr);
        System.out.println("RecordListener rtpListener " + rtpListener.toString());
        rtcpListener = new RTCPListener(netTrans.getRtcpSocket(), archiveMgr);
    }

    /**
     * Start the recording
     * 
     */
    public void record() {
        rtpListener.record();
        rtcpListener.record();
        started = true;

        logger.debug("Record_Listener::record");
    }

    /**
     * Stop the recording
     * 
     * @param transMan
     *            The transport manager from whence the transport came
     */
    public void teardown(TransportManager transMan) {
        rtpListener.stop();
        rtcpListener.stop();

        if (transMan != null) {
            transMan.closeTransport(netTrans);
            logger.debug("Record_Listener::teardown: just called "
                    + "closeTransport\n");
        }
    }

    /**
     * Returns true if recording is in progress
     * @return true if the session is recording
     */
    public boolean isRecording() {
        return started;
    }
}