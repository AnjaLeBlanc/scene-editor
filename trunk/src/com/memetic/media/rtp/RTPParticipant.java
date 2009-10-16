/*
 * @(#)RTPParticipant.java
 * Created: 26-Oct-2005
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

import java.util.HashMap;
import java.util.Vector;

import javax.media.rtp.Participant;
import javax.media.rtp.RTPStream;
import javax.media.rtp.rtcp.Report;
import javax.media.rtp.rtcp.SourceDescription;

/**
 * Represents an RTP participant
 * @author Andrew G D Rowley
 * @version 1-1-alpha3
 */
public class RTPParticipant implements Participant {
    
    // The length of the SDES item header
    private static final int SDES_HEADER_LENGTH = 2;

    // The streams of the participant
    private Vector<RTPStream> streams = new Vector<RTPStream>();
    
    // The RTCP reports of the participant
    private HashMap<Long,Report> rtcpReports = new HashMap<Long,Report>();
    
    // The CNAME of the particpant
    private String cName = "";
    
    // A vector of source description objects
    private HashMap<Integer,SourceDescription> sourceDescriptions = 
    	new HashMap<Integer,SourceDescription>();
    
    // True if the participant is active
    private boolean active = false;
    
    // The size of the sdes elements combined in SDES format
    private int sdesSize = 0;
    
    /**
     * Creates a new RTPParticipant
     * @param cName The CNAME of the participant
     */
    public RTPParticipant(String cName) {
        this.cName = cName;
        addSourceDescription(
                new SourceDescription(SourceDescription.SOURCE_DESC_CNAME, 
                        cName, 1, false));
        addSourceDescription(
                new SourceDescription(SourceDescription.SOURCE_DESC_NAME, 
                        cName, 1, false));
    }

    /**
     * 
     * @see javax.media.rtp.Participant#getStreams()
     */
    public Vector<RTPStream> getStreams() {
        return streams;
    }

    /**
     * 
     * @see javax.media.rtp.Participant#getReports()
     */
    public Vector<Report> getReports() {
        return new Vector<Report>(rtcpReports.values());
    }

    /**
     * 
     * @see javax.media.rtp.Participant#getCNAME()
     */
    public String getCNAME() {
        return cName;
    }

    /**
     * 
     * @see javax.media.rtp.Participant#getSourceDescription()
     */
    public Vector<SourceDescription> getSourceDescription() {
        return new Vector<SourceDescription>(sourceDescriptions.values());
    }
    
    /**
     * Returns true if the participant is active
     * @return true if the participant is active
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Sets the participant active or inactive
     */
    protected void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * Adds a stream to the participant
     */
    protected void addStream(RTPStream stream) {
        streams.add(stream);
    }
    
    protected void removeStream(RTPStream stream) {
        streams.remove(stream);
    }
    
    /**
     * Adds a source description item to the participant
     * @param sdes The SDES item to add
     */
    protected void addSourceDescription(SourceDescription sdes) {
        SourceDescription oldSdes = 
            (SourceDescription) sourceDescriptions.get(
                    new Integer(sdes.getType()));
        if (oldSdes != null) {
            sdesSize -= oldSdes.getDescription().length();
            sdesSize -= SDES_HEADER_LENGTH;
        }
        sourceDescriptions.put(new Integer(sdes.getType()), sdes);
        sdesSize += SDES_HEADER_LENGTH;
        sdesSize += sdes.getDescription().length();
    }
    
    /**
     * Returns the number of bytes of sdes that this participant requires
     * @return the size of the SDES packets
     */
    public int getSdesSize() {
        return sdesSize;
    }
    
    /**
     * Adds an RTCP Report for this participant
     * @param report The report to add
     */
	@SuppressWarnings("unchecked")
	public void addReport(Report report) {
        rtcpReports.put(new Long(report.getSSRC()), report);
        Vector<SourceDescription> sdes = report.getSourceDescription();
        for (int i = 0; i < sdes.size(); i++) {
            addSourceDescription(sdes.get(i));
        }
        
        if ((streams.size() == 0) && (report instanceof RTCPReport)) {
            ((RTCPReport) report).setSourceDescriptions(
                new Vector<SourceDescription>(sourceDescriptions.values()));
        }
    }

}
