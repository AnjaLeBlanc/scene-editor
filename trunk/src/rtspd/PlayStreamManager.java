/*
 * @(#)PlayStreamManager.java
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

/**
 * Manages a play stream
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class PlayStreamManager {

    // The source to manage
    private StreamSource streamSrc = null;

    /**
     * Creates a new PlayStreamManager
     * 
     * @param db
     *            The database containing details of the source
     * @param request
     *            The request to setup the playback
     * @param synch
     *            The object to synchronize playback
     * @param netTrans
     *            The object to transmit data
     * @param sFilePath The path where recordings are stored
     * @param session The session being played from
     * @param streamId The id of the stream to play
     * @throws RTSPResponse 
     */
//    public PlayStreamManager(Database db, RTSPSetupRequest request,
//            PlayStreamSynchronizer synch, PlaybackNetworkTransport netTrans,
//            String sFilePath, Session session, String streamId) 
//            throws RTSPResponse {
//        streamSrc = new StreamSource(sFilePath);
//        streamSrc.setup(db, request, synch, netTrans, session, streamId);
//    }
    public PlayStreamManager(RTSPSetupRequest request,
            PlayStreamSynchronizer synch, PlaybackNetworkTransport netTrans,
            String sFilePath, Session session, String streamId) 
            throws RTSPResponse {
        streamSrc = new StreamSource(sFilePath);
        streamSrc.setup(request, synch, netTrans, session, streamId);
    }
    /**
     * Performs operations to speed up playback start
     */
    public void prefetch() {
        streamSrc.readIndexFile();
    }

    /**
     * Starts playback of a stream
     * 
     * @param request
     *            The request to start the playback
     */
    public void play(RTSPPlayRequest request) {
        
        // Does Nothing
    }

    /**
     * Stops the playback of a stream
     */
    public void teardown() {
        streamSrc.teardown();
    }
}