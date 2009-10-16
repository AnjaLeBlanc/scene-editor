/*
 * @(#)PlayStreamSynchronizer.java
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

import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.Headers;

/**
 * Plays streams so that they are synchronized
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class PlayStreamSynchronizer {
    
    // The response to send when everthing is OK
    private static final String OK_RESPONSE = "OK";

    // The amount by which to delay the start of play
    private static final int START_DELAY = 1000;
    
    // The amount of difference between times before a new APP packet is sent
    private static final int MIN_TIME_DIFF = 1000;

    // The log file for the database
    private static Log logger = 
        LogFactory.getLog(PlayStreamSynchronizer.class.getName());

    // The RTCP Packet
    private ArenaAPPPacket rtcpPacket = new ArenaAPPPacket();

    // The list of sources this synchronizer is sending
    private Vector<StreamSource> sourceList = new Vector<StreamSource>();

    // The smallest time value
    private long minTime = 0;

    // The stream with the smallest time value
    private StreamSource minSource = null;

    // The id of the session
    private int sessionId = 0;

    // True if the smallest time value has not been set
    private boolean minTimeNotSet = true;
    
    // A map of tranport -> vector of sources using the transport
    private HashMap<PlaybackNetworkTransport,Vector<StreamSource>> transportMap = 
    	new HashMap<PlaybackNetworkTransport,Vector<StreamSource>>();
    
    // The current time being sent
    private long currentTime = -MIN_TIME_DIFF;
    
    private Vector<TimeUpdateListener> updateTimes=new Vector<TimeUpdateListener>();
    

    /**
     * Creates a new PlayStreamSynchronizer
     * 
     * @param id
     *            The id of the session
     */
    public PlayStreamSynchronizer(int id) {
        sessionId = id;
        
        
    }

    /**
     * Registers a new stream to be played
     * 
     * @param src
     *            The stream to be played
     * @param time
     *            The time that the stream is to be played at
     */
    public void registerStream(StreamSource src, long time) {
        logger.debug("Play_Stream_Synchronizer::registerStream: "
                + "Stream_Source is registering\n");
        Vector<StreamSource> srcList = transportMap.get(src.getTransport());

        if (minTimeNotSet) {
            minTime = time;
            minSource = src;
            minTimeNotSet = false;
        } else if (time < minTime) {
            minTime = time;
            minSource = src;
        }
        
        if (srcList == null) {
            srcList = new Vector<StreamSource>();
        }
        srcList.add(src);
        transportMap.put(src.getTransport(), srcList);

        sourceList.add(src);
    }

    /**
     * Stops a stream from being sent
     * 
     * @param src
     *            The stream to stop
     */
    public void unregisterStream(StreamSource src) {
        Vector<StreamSource> srcList =  transportMap.get(src.getTransport());
        logger.debug("Play_Stream_Synchronizer::unregisterStream: "
            + "a Stream_Source is dropping it's registration");
        sourceList.remove(src);
        if (srcList != null) {
            srcList.remove(src);
            if (srcList.size() > 0) {
                transportMap.put(src.getTransport(), srcList);
            } else {
                transportMap.remove(src.getTransport());
            }
        }
        if (src == minSource) {
            minSource = null;
            minTime = -1;
            minTimeNotSet = true;
            for (int i = 0; i < sourceList.size(); i++) {
                StreamSource source = (StreamSource) sourceList.get(i);
                if (minTimeNotSet) {
                    minTime = source.getStartTime();
                    minSource = source;
                    minTimeNotSet = false;
                } else if (source.getStartTime() < minTime) {
                    minTime = source.getStartTime();
                    minSource = source;
                }
            }
        }
    }

    /**
     * Plays all the registered streams
     * 
     * @param playRequest
     *            The request to play the streams
     */
    public void play(RTSPPlayRequest playRequest) {
        RTSPRequestPacket request = playRequest.getRequestPacket();
        RTSPResponse resp = 
            new RTSPResponse(Headers.RTSP_OK, OK_RESPONSE, request);

        // Give the streams some seconds to get ready
        long startingTime = System.currentTimeMillis() + START_DELAY;
        logger.debug("Play_Stream_Synchronizer::play:  Got a play request");
        currentTime = playRequest.getStartingOffset();

        // Start the streams
        for (int i = 0; i < sourceList.size(); i++) {
            ((StreamSource) sourceList.get(i)).play(playRequest, minTime,
                    startingTime);
        }

        // Wait until the first packet is sent before responding
        if (minSource != null) {
            minSource.waitForFirstPacket();
        }

        // Send a response to the play request
        resp.setHeader(Headers.RTSP_SESSION, String.valueOf(sessionId));
        resp.send();
    }
    
    public void play(client.rtsp.RTSPPlayRequest playRequest) {
        // Give the streams some seconds to get ready
        long startingTime = System.currentTimeMillis() + START_DELAY;
        logger.debug("Play_Stream_Synchronizer::play:  Got a play request");
        currentTime = playRequest.getStartingOffset();

        // Start the streams
        for (int i = 0; i < sourceList.size(); i++) {
            ((StreamSource) sourceList.get(i)).play(playRequest, minTime,
                    startingTime);
        }

        // Wait until the first packet is sent before responding
        if (minSource != null) {
            minSource.waitForFirstPacket();
        }
    }

    /**
     * @param request
     */
    public void pause(RTSPPauseRequest request) {
        long offset = request.getOffset();
        if (offset == 0) {
            offset = -1;
        }

        // Pause the streams
        for (int i = 0; i < sourceList.size(); i++) {
            ((StreamSource) sourceList.get(i)).pause(offset);
        }
    }
    
    /**
     * Sets the current time being sent out by the synchronizer
     * @param time The time in milliseconds to set
     */
    public void setCurrentTime(long time) {
        if ((Math.abs(time - currentTime)) > MIN_TIME_DIFF) {
            Iterator<PlaybackNetworkTransport> iterator = transportMap.keySet().iterator();
            currentTime = time;
            rtcpPacket.setTime(time);
            while (iterator.hasNext()) {
                PlaybackNetworkTransport netTrans = 
                    iterator.next();
                netTrans.playRtcpPacket(
                        new DatagramPacket(rtcpPacket.getBytes(), 
                                ArenaAPPPacket.LENGTH));
            }
            for(int i=0;i<updateTimes.size();i++){
            	updateTimes.get(i).updateTime(time);
            }
        }
    }
    
    public void addTimeUpdateListener(TimeUpdateListener listener){
    	updateTimes.add(listener);
    }
    
    public void removeTimeUpdateListener(TimeUpdateListener listener){
    	updateTimes.remove(listener);
    }
}