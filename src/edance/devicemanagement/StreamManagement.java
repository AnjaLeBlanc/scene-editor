package edance.devicemanagement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.media.Format;
import javax.media.control.BufferControl;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.rtp.Participant;
import javax.media.rtp.RTPControl;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.RemoteListener;
import javax.media.rtp.SendStream;
import javax.media.rtp.SendStreamListener;
import javax.media.rtp.SessionListener;
import javax.media.rtp.event.ApplicationEvent;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.LocalPayloadChangeEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.NewSendStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.ReceiverReportEvent;
import javax.media.rtp.event.RemoteEvent;
import javax.media.rtp.event.RemotePayloadChangeEvent;
import javax.media.rtp.event.SendStreamEvent;
import javax.media.rtp.event.SenderReportEvent;
import javax.media.rtp.event.SessionEvent;
import javax.media.rtp.event.StreamMappedEvent;
import javax.media.rtp.event.TimeoutEvent;
import javax.media.rtp.rtcp.Report;
import javax.media.rtp.rtcp.SourceDescription;
import javax.swing.JOptionPane;

import net.crew_vre.codec.controls.KeyFrameForceControl;

import rtpReceiver.RecordingStreamsManager;
import rtpReceiver.TimeUpdateListener;

import com.memetic.media.rtp.RTPSocketAdapter;

import memetic.crypto.AESCrypt;
import memetic.crypto.DESCrypt;
import memetic.crypto.RTPCrypt;

public class StreamManagement implements SessionListener, ReceiveStreamListener,
RemoteListener, SendStreamListener {

	//	 The buffer control class
    private static final String BUFFER_CONTROL_CLASS =
        "javax.media.control.BufferControl";
    
    // The class containing the RTP control
    private static final String RTP_CONTROL_CLASS =
        "javax.media.rtp.RTPControl";
    
	private static Vector<InetSocketAddress> forwardAddresses = new Vector<InetSocketAddress>();
	//	 The Recieving sockets
    private static Vector<RTPSocketAdapter> sockets = new Vector<RTPSocketAdapter>();

    // The current buffer time
    private long currentBufferTime = 0;

    // The current buffer length
    private long currentBufferLength = 0;

    // The receiving RTP Managers
    private static Vector<RTPManager> managers = new Vector<RTPManager>();

    // A hashmap of formats to add to the managers
    private static HashMap<Integer, Format> formats = new HashMap<Integer, Format>();
    
    // A map of streams that are being sent locally
    private HashMap<Long, Boolean> localStreamMap = new HashMap<Long, Boolean>();
    
//    // A map of cname to number of times participant cname appears
//    private HashMap<String,Integer> personCount = new HashMap<String,Integer>();

    // A map of cName to Vector of ssrc
    private HashMap<String, Vector<Long>> ssrcMap = new HashMap<String,Vector<Long>>();
    
    // A map of ssrc to cName for video streams
    private HashMap<Long, String> remoteVideo = new HashMap<Long, String>();

    // A map of ssrc to cName for audio streams
    private HashMap<Long, String> remoteAudio = new HashMap<Long, String>();
    
    // A vector of time update listeners
    private Vector<TimeUpdateListener> timeUpdateListeners = new Vector<TimeUpdateListener>();

    // should the layout be followed from remote master?
    private boolean receiveScreenLayout=false;
    
    private Vector<RemoteDataSource> remoteDataSources = new Vector<RemoteDataSource>();
 
    private static final int NO_TTL_ADDRESS_PARTS = 2;
    
    private static final int ENC_KEY_PART = 4;

    private static final int ENC_TYPE_PART = 3;

    private static final int WITH_ENCRYPTION_ADDRESS_PARTS = 5;

    private static final int TTL_PART = 2;

    private static final int PORT_PART = 1;

    private static final int ADDRESS_PART = 0;

    private static final int WITH_TTL_ADDRESS_PARTS = 3;

    private static final int DEFAULT_TTL = 127;
    
    private static final String RECORDING = "/recording";
    
    private File sessionFile=null;
    
    private Vector<RecordingStreamsManager> recordingStreamsManagers =
    	new Vector<RecordingStreamsManager>();
    
    private Vector<KeyFrameForceControl> keyFrameForce = 
    	new Vector<KeyFrameForceControl>();


    public void update(SessionEvent sessionevent) {
		// TODO Auto-generated method stub
//    	System.out.println("session event " + sessionevent);
	}

    @SuppressWarnings("unchecked")
	public void update(ReceiveStreamEvent evt) {
//        System.out.println("ReceiveStreamEvent " + evt.toString());

        // Get the participant if there is one
        Participant participant = evt.getParticipant();

        // Get the stream if there is one
        ReceiveStream stream = evt.getReceiveStream();

        // If the participant changed their payload, update it
        if (evt instanceof RemotePayloadChangeEvent) {
            evt = new NewReceiveStreamEvent(evt.getSessionManager(), stream);
        }

        // If a new participant has joined us, display them
        if (evt instanceof NewReceiveStreamEvent) {
            boolean isLocal = (localStreamMap.get(stream.getSSRC()) != null);

            System.err.println("Stream " + stream.getSSRC() + " isLocal = " + isLocal);

            // Get the source of the new stream
            DataSource ds = stream.getDataSource();

            // Find out the formats.
            RTPControl ctl = (RTPControl) ds
                    .getControl(RTP_CONTROL_CLASS);
            Format format = null;
            if (ctl != null) {
            	if (ctl.getFormat() instanceof AudioFormat && !isLocal) {   
            		remoteAudio.put(stream.getSSRC(), new Long(stream.getSSRC()).toString());
            	}
            	if (ctl.getFormat() instanceof VideoFormat && !isLocal) {   
            		remoteVideo.put(stream.getSSRC(), new Long(stream.getSSRC()).toString());
            	}
            	
            	Vector<Long> map = (Vector<Long>) ssrcMap.get(new Long(stream.getSSRC()).toString());
                if (map == null) {
                    map = new Vector<Long>();
                    map.add(stream.getSSRC());
                    ssrcMap.put(new Long(stream.getSSRC()).toString(), map);
                }            	
            	
                format = ctl.getFormat();
                if (!isLocal /*|| (format instanceof AudioFormat)*/) {
                    addStream(ds, format, stream.getSSRC());
                } 
            }
        } else if (evt instanceof StreamMappedEvent) {

            // If there is a stream and a data source
            if ((stream != null) && (stream.getDataSource() != null)) {
            	System.out.println("stream mapped event " + participant.getSourceDescription().size());
                // Get the data source format
                DataSource ds = stream.getDataSource();
                RTPControl ctl = (RTPControl) ds
                        .getControl(RTP_CONTROL_CLASS);

                // Get the description
                Vector<SourceDescription> descriptions = participant.getSourceDescription();
                String streamName = getSDES(SourceDescription.SOURCE_DESC_NAME, 
                		descriptions, participant.getCNAME());

                while (ssrcMap.get(streamName)!=null){
                	Vector<Long> ssrc=ssrcMap.get(streamName);
                	if(ssrc.size()==0 || ssrc.firstElement()==stream.getSSRC()){
                		return;
                	}
                	streamName=streamName.concat("_");
                }
                
                // If this is a video stream, set the video description
                if (ctl.getFormat() instanceof VideoFormat && 
                		this.localStreamMap.containsKey(stream.getSSRC()) == false) {
                	
                	if(remoteVideo.containsKey(stream.getSSRC()) && 
                			remoteVideo.get(stream.getSSRC()).compareTo(streamName) != 0){
                	
                	
                	if (remoteVideo.containsKey(stream.getSSRC())) {
                		remoteVideo.remove(stream.getSSRC());
                	} 
                	System.out.println("got video stream " + streamName + " " + stream.getSSRC());
                	remoteVideo.put(stream.getSSRC(), streamName);
                	
                	
                	}
                }

                // If this is an audio stream, set the audio description
                if (ctl.getFormat() instanceof AudioFormat &&
                		this.localStreamMap.containsKey(stream.getSSRC()) == false) {                	
                	if (remoteAudio.containsKey(stream.getSSRC())) {
                		remoteAudio.remove(stream.getSSRC());
                	} 
                	System.out.println("got audio stream " + streamName);
                	remoteAudio.put(stream.getSSRC(), streamName);
                }

                // Add the ssrc to the map
                Vector<Long> map = (Vector<Long>) ssrcMap.get(streamName);
                if (map == null) {
                    map = new Vector<Long>();
                }
                if (!map.contains(new Long(stream.getSSRC()))) {
                    map.add(new Long(stream.getSSRC()));
                    ssrcMap.put(streamName, map);
                }
            }
        } else if ((evt instanceof ByeEvent) || (evt instanceof TimeoutEvent)) {

            // If a stream has stopped
            if (stream != null) {
                removeStream(stream.getSSRC());
                remoteVideo.remove(stream.getSSRC());
                remoteAudio.remove(stream.getSSRC());
                if (participant != null) {
                    Vector<Long> map = ssrcMap.get(participant.getCNAME());
                    if (map != null) {
                        map.remove(new Long(stream.getSSRC()));
                    }
                }
                for (int i=0; i < remoteDataSources.size(); i++) {
                	if(remoteDataSources.get(i).getSsrc()==stream.getSSRC()){
                		remoteDataSources.remove(i);
                	}
                }
            }
        } else if (evt instanceof ApplicationEvent) {
            ApplicationEvent app = (ApplicationEvent) evt;
//            System.err.println("Received App packet " + app.getAppString() + "  " + app.getAppSubType());
            //if(app.getAppString()!=null && app.getAppString().compareTo(ArenaAPPPacket.APP_PACKET_TYPE)==0){
                if (app.getAppSubType() == 0) {
                    DataInputStream input =
                        new DataInputStream(
                                new ByteArrayInputStream(app.getAppData()));
                    try {
                        long time = input.readInt();
                        for (int i = 0; i < timeUpdateListeners.size(); i++) {
                            TimeUpdateListener listener =
                                (TimeUpdateListener) timeUpdateListeners.get(i);
                            System.err.println("Sending update");
                            listener.update(time);
                            System.err.println("Time Update "
                                    + time + " Sent");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            else if(app.getAppSubType()==1 && receiveScreenLayout==true){
//                    DataInputStream input =
//                        new DataInputStream(
//                                new ByteArrayInputStream(app.getAppData()));
//                    try {
//                        long ssrc = input.readInt();
//                        int x = input.readInt();
//                        int y = input.readInt();
//                        int height = input.readInt();
//                        int width = input.readInt();
//                        int transparency = input.readInt();
//                        int transparencyBorder = input.readInt();
//                        byte flags= input.readByte();
//                        boolean displayed=false;
//                        boolean border=false;
//                        System.out.println("flags " + flags + " " + (byte)(flags&(byte)0x01)
//                                + " " + (byte)(flags&(byte)0x02));
//                        if((flags&(byte)0x01)==1){
//                            displayed=true;
//                        }
//                        if((flags&(byte)0x02)==2){
//                            border=true;
//                        }
//                        Vector<VideoWindowState> windowLayout=new Vector<VideoWindowState>();
//                        if(displayed){
//                            windowLayout.add(new VideoWindowState(ssrc,x,y,height,width,
//                                transparency, transparencyBorder, border));
//                        }else {
//                            windowLayout.add(new VideoWindowState(ssrc));
//                        }
//                        this.setCurrentVideoLayout(windowLayout);
//
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }
//	        }
        }
    }

	private void removeStream(long ssrc) {
		// TODO Auto-generated method stub
		
	}

	private void addStream(DataSource ds, Format format, long ssrc) {
		RemoteDataSource rds = new RemoteDataSource(ds, format, ssrc);
		remoteDataSources.add(rds);
	}
	
    @SuppressWarnings("unchecked")
	public synchronized void update(RemoteEvent evt) {
        // If a sender report has been received ...
        if ((evt instanceof SenderReportEvent)
                || (evt instanceof ReceiverReportEvent)) {

            // Get the report
            Report report = null;
            if (evt instanceof SenderReportEvent) {
                report = ((SenderReportEvent) evt).getReport();
            } else {
                report = ((ReceiverReportEvent) evt).getReport();
            }

            // If the participant has been linked ...
            Participant part = report.getParticipant();

            if (part != null) {

                // Get the descriptions of the stream
                Vector<SourceDescription> descriptions = report.getSourceDescription();

                // Get the ssrc of the stream
                long ssrc = report.getSSRC();


                String streamName = "r:" + getSDES(SourceDescription.SOURCE_DESC_NAME, 
                		descriptions, part.getCNAME());

                while (ssrcMap.get(streamName)!=null){
                	Vector<Long> ssrc2=ssrcMap.get(streamName);
                	if(ssrc2.size() ==  0) {
                		return;
                	}
                	if(ssrc2.firstElement()==ssrc){
                		return;
                	}
                	streamName=streamName.concat("_");
                }
                if(streamName.contains("local:")){
                	streamName=streamName.replaceAll("local:", "");
                }
                
                // If this is a video stream, set the video description
                if (this.localStreamMap.containsKey(ssrc) == false) {
                	if (remoteVideo.containsKey(ssrc) && remoteVideo.get(ssrc).compareTo(streamName) != 0) {
                		remoteVideo.remove(ssrc);
                		System.out.println("got video stream " + streamName + " " + ssrc);
                    	remoteVideo.put(ssrc, streamName);
                	} else if (remoteAudio.containsKey(ssrc)) {
                		remoteAudio.remove(ssrc);
                		System.out.println("got audio stream " + streamName);
                    	remoteAudio.put(ssrc, streamName);
                	}
                	
                }

                // Add the ssrc to the map
                Vector<Long> map = (Vector<Long>) ssrcMap.get(streamName);
                if (map == null) {
                    map = new Vector<Long>();
                }
                if (!map.contains(new Long(ssrc))) {
                    map.add(new Long(ssrc));
                    ssrcMap.put(streamName, map);
                }
            }
        }
    }

	public void update(SendStreamEvent evt) {
        // Do Nothing
        System.out.println("SendStream event ReceivePanel " + evt.toString());

        // Get the stream if there is one
        SendStream stream = evt.getSendStream();

        // If the participant changed their payload, update it
        if (evt instanceof LocalPayloadChangeEvent) {
            evt = new NewSendStreamEvent(evt.getSessionManager(), stream);
        }

        // If a new particapant has joined us, display them
        if (evt instanceof NewSendStreamEvent) {
            System.err.println("Local send stream " + stream.getSSRC());
            if (localStreamMap.get(new Long(stream.getSSRC())) == null) {
                localStreamMap.put(stream.getSSRC(), true);
            }
        }
	}

    /**
     * Add an address to listen to streams on
     * @param addr The address
     * @param port The port
     * @param ttl The ttl
     * @param encryption The encryption or null for none
     * @throws IOException
     *
     */
    public void addListenAddress(InetAddress addr, int port, int ttl,
            RTPCrypt encryption)
            throws IOException {
    	System.out.println("addListenAddress");
        RTPSocketAdapter socket = new RTPSocketAdapter(addr, port, ttl);
        for (int i = 0; i < forwardAddresses.size(); i++) {
            socket.addForwardAddress(forwardAddresses.get(i));
        }
        RTPManager manager = RTPManager.newInstance();
        socket.setEncryption(encryption);
        sockets.add(socket);
        manager.addSessionListener(this);
        manager.addReceiveStreamListener(this);
        manager.addRemoteListener(this);
		
        Iterator<Integer> iterator = formats.keySet().iterator();
        while (iterator.hasNext()) {
            Integer rtpMap = (Integer) iterator.next();
            manager.addFormat((Format) formats.get(rtpMap), rtpMap
                    .intValue());
        }
        BufferControl buffer =
            (BufferControl) manager.getControl(BUFFER_CONTROL_CLASS);
        if (buffer != null) {
            buffer.setBufferLength(currentBufferLength);
            buffer.setMinimumThreshold(currentBufferTime);
        }
        managers.add(manager);
    }
    
    public void addListenAddress(String address) throws IOException{
    	String[] parts = address.split("/");
    	if (parts.length >= NO_TTL_ADDRESS_PARTS) {
    		InetAddress addr = InetAddress.getByName(parts[ADDRESS_PART]);
    		int port = Integer.valueOf(parts[PORT_PART]).intValue();
    		int ttl = DEFAULT_TTL;
    		if (parts.length >= WITH_TTL_ADDRESS_PARTS) {
    			ttl = Integer.valueOf(parts[TTL_PART]).intValue();
    		}
    		RTPCrypt crypt = null;
    		String encType = null;
    		String encKey = null;
    		if (parts.length >= WITH_ENCRYPTION_ADDRESS_PARTS) {
    			encType = parts[ENC_TYPE_PART];
    			encKey = parts[ENC_KEY_PART];
    			if (encType.equals(AESCrypt.TYPE)) {
                    crypt = new RTPCrypt(new AESCrypt(encKey));
                } else if (encType.equals(DESCrypt.TYPE)) {
                    crypt = new RTPCrypt(new DESCrypt(encKey));
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Unrecognised Encryption Algorithm: "
                            + encType);
                    System.err.println("Unrecognised Encryption "
                            + "Algorithm");
                    System.exit(0);
                }
    		}
    		addListenAddress(addr, port, ttl, crypt);
    		
    	}
    }

    /**
     * Sets the buffering of the receiver
     * @param time The time length of the receiver
     */
    public void setBufferTime(long time) {
        this.currentBufferTime = time;
        for (int i = 0; i < managers.size(); i++) {
            RTPManager manager = (RTPManager) managers.get(i);
            BufferControl buffer =
                (BufferControl) manager.getControl(BUFFER_CONTROL_CLASS);
            if (buffer != null) {
                buffer.setMinimumThreshold(time);
            }
        }
    }

    /**
     * Maps the given format to the given RTP number
     *
     * @param rtpMap
     *            The number to map to
     * @param format
     *            The format to map to
     */
    public static void mapFormat(int rtpMap, Format format) {
        formats.put(new Integer(rtpMap), format);
        for (int i = 0; i < managers.size(); i++) {
            RTPManager manager = (RTPManager) managers.get(i);
            manager.addFormat(format, rtpMap);
        }
    }

	public RTPSocketAdapter getRTPSocket() {
		return sockets.firstElement();
	}
	
	public RTPSocketAdapter getRTPSocketAudio() {
		return sockets.get(1);
	}
	
	public void startLisening() {
		for (int i=0; i< managers.size(); i++) {
			startManager(i);
		}
	}
	
	private void startManager(int i) {
		RTPManager manager = managers.get(i);
		RTPSocketAdapter socket = sockets.get(i);
		manager.initialize(socket);
		
	}

	public Collection<String> getRemoteVideoList() {
		return remoteVideo.values();
	}
	
	private class RemoteDataSource {
		private DataSource ds;
		private Format format;
		private long ssrc;
		
		public RemoteDataSource(DataSource ds, Format format, long ssrc){
			this.ds =  ds;
			this.format = format;
			this.ssrc = ssrc;
		}

		public DataSource getDs() {
			return ds;
		}

		public Format getFormat() {
			return format;
		}

		public long getSsrc() {
			return ssrc;
		}
		
	}

	public DataSource getDataSource(String selectedValue) {
		Long ssrc;
		for (int j = 0; j< ssrcMap.get(selectedValue).size(); j++){
			ssrc = ssrcMap.get(selectedValue).get(j);
			if(ssrc == null) {
				continue;
			}
			for (int i=0; i < remoteDataSources.size(); i++) {
				if(remoteDataSources.get(i).getSsrc()==ssrc){
					return remoteDataSources.get(i).getDs();
				}
			}
		}
		return null;
	}

	public Format getFormat(String selectedValue) {
		Long ssrc;
		for (int j = 0; j< ssrcMap.get(selectedValue).size(); j++){
			ssrc = ssrcMap.get(selectedValue).get(j);
			if(ssrc == null) {
				continue;
			}
			for (int i=0; i < remoteDataSources.size(); i++) {
				if(remoteDataSources.get(i).getSsrc()==ssrc){
					return remoteDataSources.get(i).getFormat();
				}
			}
		}
		return null;
	}

	public long getSSRC(String selectedValue) {
		return ssrcMap.get(selectedValue).firstElement();
	}

	public Collection<String> getRemoteAudioList() {
		return remoteAudio.values();
	}
	
    /**
     * Extracts a specific type of description for a stream from a list of
     * descriptions
     *
     * @param type
     *            The SDES type to search for
     * @param descriptions
     *            The descriptions to search
     * @param def
     *            The default to return if not found
     * @return The sdes item requested
     */
    private String getSDES(int type, Vector<SourceDescription> descriptions, String def) {
        for (int i = 0; i < descriptions.size(); i++) {
            SourceDescription d = descriptions.get(i);
//            System.out.println("sdes " + d.getType() + " " + type + " " + d.getDescription());
            if (d.getType() == type) {
                return d.getDescription();
            }
        }
        return (def);
    }
    
    public void startRecording(File currDir) {
        try {
            File dir = new File(currDir.getCanonicalPath().concat(RECORDING));
            Integer count=1;
            while(dir.exists()){
                dir=new File(currDir.getCanonicalPath().concat(RECORDING).concat(count.toString()));
                count++;
            }
            dir.mkdirs();
            sessionFile= new File(dir.getCanonicalPath().concat(File.separator).concat("session.txt"));
//            if(!sessionFile.exists()){
                PrintWriter out
                   = new PrintWriter(new BufferedWriter(new FileWriter(sessionFile)));
                try{
                    out.println("<"+System.currentTimeMillis() + " - ");
                    out.flush();
                }
                finally {
                    out.close();
                }

//            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try {
        	for(int j = 0; j< sockets.size(); j++){
        		RecordingStreamsManager recordingStreamsManager = new RecordingStreamsManager(
	                    sessionFile.getCanonicalPath(), sockets.get(j));
	            for (int i = 0; i < keyFrameForce.size(); i++) { 
	                KeyFrameForceControl force = keyFrameForce.get(i);
	                if (force != null) {
	                	System.err.println("Key frame forced");
	                	force.nextFrameKey();
	                }
	            }
	            recordingStreamsManagers.add(recordingStreamsManager);
        	}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void stopRecording() {
    	for(int i = 0; i< recordingStreamsManagers.size(); i++){
    		stopRecording(recordingStreamsManagers.get(i));
    	}
    }
    
    private void stopRecording(RecordingStreamsManager recordingStreamsManager) {
        System.err.println("start shutting down");
        recordingStreamsManager.stopRecording();
        StringBuffer contents = new StringBuffer();
        try{
            BufferedReader input =  new BufferedReader(new FileReader(sessionFile));
            try{
                String line = null;
                line = input.readLine();
                if(line.matches("<\\d+ - \\d+>")){
                    System.err.println("append to session");
                    line=line.replaceFirst("- \\d+>", "- "+recordingStreamsManager.getRecordStop()+">");
                }
                else if(line.matches("<\\d+ - ")){
                    System.err.println("finish session");
                    line="<" + recordingStreamsManager.getRecordStart() + " - " + recordingStreamsManager.getRecordStop() + ">";
                }
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
                while (( line = input.readLine()) != null){
                      contents.append(line);
                      contents.append(System.getProperty("line.separator"));
                    }
            }
            finally {
                input.close();
            }
            BufferedWriter output = new BufferedWriter(new FileWriter(sessionFile));
            try{
                output.write( contents.toString());
                output.flush();
            }
            finally {
                output.close();
            }

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
	
}
