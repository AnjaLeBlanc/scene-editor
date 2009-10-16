package edance;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.media.PlugInManager;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.swing.JFrame;

import net.crew_vre.media.Misc;

import edance.devicemanagement.StreamManagement;
import edance.sceeneditor.EdanceFunctionality;
import edance.userinterface.MainFrame;

import memetic.crypto.RTPCrypt;

public class Edance {
	private static JFrame app = null;

	private MainFrame mainPanel;

	private EdanceFunctionality functionality;
	private StreamManagement streamManagement;

	// The AG audio format
	private static final AudioFormat AG_AUDIO_FORMAT = new AudioFormat(
			AudioFormat.LINEAR, 16000, 16, 1, AudioFormat.BIG_ENDIAN,
			AudioFormat.SIGNED);

	// The AG audio RTP number
	private static final int AG_AUDIO_RTP = 112;

	// The InSORS audio format
	private static final AudioFormat INSORS_AUDIO_FORMAT = new AudioFormat(
			AudioFormat.ULAW, 16000, 16, 1, AudioFormat.LITTLE_ENDIAN,
			AudioFormat.SIGNED);

	// The InSORS audio RTP number
	private static final int INSORS_AUDIO_RTP = 84;

	/**
	 * @param args
	 *        none
	 */
	public static void main(String[] args) {
		String video = null;
		String audio = null;
		if (args.length >= 1) {
			video = args[0];
		}
		if (args.length >= 2) {
			audio = args[1];
		}
		final Edance edance = new Edance(video, audio);
        // Make the application quit when the window is closed
        app = edance.getUserInterface();
        app.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                edance.stop();
                System.exit(0);
            }
        });

		// Add the venue client applet to the frame and display it
		app.setSize(800, 600);
		app.setExtendedState(JFrame.MAXIMIZED_BOTH);
		app.setLocationRelativeTo(null);

		app.setVisible(true);
	}

	/**
	 * Constructor
	 *
	 */
	public Edance(String video, String audio) {
		this.setUpStreamManagement(video, audio);
		mainPanel = new MainFrame();
		functionality = new EdanceFunctionality(mainPanel, streamManagement);
		mainPanel.addToolbarListener(functionality);

	}

	private void setUpStreamManagement(String video, String audio) {
		// Add inSORS format
		StreamManagement.mapFormat(INSORS_AUDIO_RTP, INSORS_AUDIO_FORMAT);

		// Add RAT format
		StreamManagement.mapFormat(AG_AUDIO_RTP, AG_AUDIO_FORMAT);

		StreamManagement.mapFormat(77, new VideoFormat("h261as/rtp"));
		StreamManagement.mapFormat(84, new AudioFormat("ULAW/rtp", 16000, 8, 1, 
				AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED));

		
		
		try {
//			 	Misc.addCodec("codecs.rgb.RGB2432Converter");
		        Misc.addCodec("net.crew_vre.codec.colourspace.YUV420RGB32Converter");
		        
		        Misc.addCodec("net.crew_vre.codec.h261.H261Decoder");
		        Misc.addCodec("net.crew_vre.codec.h261.H261ASDecoder");
		        Misc.addCodec("net.crew_vre.codec.h261.H261ASEncoder");
		        
		        PlugInManager.removePlugIn(
		        		"com.sun.media.codec.video.h261Decoder", 
		        		PlugInManager.CODEC);
		        
		        PlugInManager.removePlugIn(
		                "com.sun.media.codec.video.vcm.NativeDecoder",
		                PlugInManager.CODEC);
		        PlugInManager.removePlugIn(
		                "com.sun.media.codec.video.vcm.NativeEncoder",
		                PlugInManager.CODEC);
		        PlugInManager.removePlugIn(
		                "com.sun.media.codec.video.colorspace.YUVToRGB",
		                PlugInManager.CODEC);
		        
		        
		        //16 bit audio
				PlugInManager.removePlugIn(
						"com.sun.media.codec.audio.ulaw.Packetizer",
						PlugInManager.CODEC);
				PlugInManager.removePlugIn(
						"com.sun.media.codec.audio.rc.RateCvrt",
						PlugInManager.CODEC);
				PlugInManager.removePlugIn(
						"com.sun.media.codec.audio.rc.RCModule",
						PlugInManager.CODEC);
				Misc.addCodec("net.crew_vre.codec.ulaw.Packetizer");
				Misc.addCodec("net.crew_vre.codec.linear.Packetizer");
				Misc.addCodec("net.crew_vre.codec.linear.RateConverter");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		InetAddress addr = null;
		InetAddress addrAudio = null;
		try {
			addr = InetAddress.getByName("233.33.100.10");
			addrAudio =  InetAddress.getByName("233.33.100.16");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
        int port = 57006;
        int portAudio = 57004;
        int ttl = 1;
        RTPCrypt encryption = null;

        try {
        	streamManagement = new StreamManagement();
        	if (video != null) {
        		streamManagement.addListenAddress(video);
        	} else {
        		streamManagement.addListenAddress(addr, port, ttl,
					encryption);
        	}
        	if (audio != null) {
        		streamManagement.addListenAddress(audio);
        	} else {
        		streamManagement.addListenAddress(addrAudio, portAudio, 127,
					encryption);
        	}
			streamManagement.startLisening();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private JFrame getUserInterface() {
		return mainPanel;
	}

	protected void stop() {
		functionality.stopTransmitting();
	}

}
