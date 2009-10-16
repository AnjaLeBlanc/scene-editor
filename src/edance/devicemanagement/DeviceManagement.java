package edance.devicemanagement;

import java.awt.Dimension;
import java.io.IOException;
//import java.security.AccessController;
//import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.ControllerEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.PackageManager;
import javax.media.PlugInManager;
import javax.media.Time;
import javax.media.format.AudioFormat;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.Codec;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerListener;
import javax.media.Processor;
import javax.media.control.FormatControl;
import javax.media.control.FrameRateControl;
import javax.media.control.QualityControl;
import javax.media.control.TrackControl;
import javax.media.format.YUVFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.protocol.SourceCloneable;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.rtcp.SourceDescription;
import javax.swing.JOptionPane;

import net.crew_vre.media.Misc;

import com.lti.civil.CaptureException;
import com.lti.civil.CaptureSystem;
import com.lti.civil.CaptureSystemFactory;
import com.lti.civil.DefaultCaptureSystemFactorySingleton;
import com.memetic.media.rtp.RTPSocketAdapter;
import com.sun.media.DisabledSecurity;
import com.sun.media.JMFSecurityManager;

import edance.userinterface.event.StreamListener;
import effects.CloneEffect;

import rtspd.TimeUpdateListener;

public class DeviceManagement implements ControllerListener,  TimeUpdateListener {

//	private static final int MINIMUM_CAPTURE_DEVICES_BEFORE_DETECT = 1;

	// The text to use for a controller error
	private static final String CONTROLLER_ERROR_TEXT = "Controller would not configure";

	// The locator prefix for civil devices
	private static final String CIVIL_LOCATOR = "civil:";

	private static final String ERROR_TEXT = "Error: ";

	// The send RTP Manager
	private RTPManager sendManager = null;

	private Vector<RTPManager> sendManagers = new Vector<RTPManager>();

	private static Vector<String> locators = new Vector<String>();

	private static HashMap<String, Processor> devicemap = new HashMap<String, Processor>();

//	// The send RTP Stream
//	private SendStream sendStream = null;

	private Vector<SendStream> sendStreams = new Vector<SendStream>();

	// The send processor
	private Processor sendProcessor = null;

	private Vector<Processor> sendProcessors = new Vector<Processor>();

	private static HashMap<String, DataSource> availableDatasources = new HashMap<String, DataSource>();

	private static HashMap<Long, javax.media.protocol.recorded.DataSource> videoDataSources
		= new HashMap<Long, javax.media.protocol.recorded.DataSource>();

	private StreamManagement streamManagement;

	private FormatItem[] formats = new FormatItem[] {
			new FormatItem(new VideoFormat("h261as/rtp"), new YUVFormat(
					YUVFormat.YUV_422)),
			new FormatItem(new VideoFormat(VideoFormat.JPEG_RTP),
					new YUVFormat(YUVFormat.YUV_422)),
			new FormatItem(new VideoFormat(VideoFormat.H261_RTP),
					new YUVFormat(YUVFormat.YUV_422)), };

	private boolean processorFailed = false;

	// An object to allow locking
	private Integer stateLock = new Integer(0);

	// event Listeners
	private Vector<StreamListener> streamListeners = new Vector<StreamListener>();

	public DeviceManagement(StreamManagement sm) {
		this.streamManagement = sm;
		try {
			Vector<String> codecs = new Vector<String>();
//			codecs.add("codecs.h261.H261Encoder");
//			codecs.add("codecs.h261.H261ASEncoder");
//			codecs.add("codecs.h261.H261ASDecoder");
			codecs.add("net.crew_vre.codec.h261.H261Decoder");
			codecs.add("net.crew_vre.codec.h261.H261ASDecoder");
			codecs.add("net.crew_vre.codec.h261.H261ASEncoder");
//			codecs.add("codecs.rgb.RGB2432Converter");
			for (int i = 0; i < codecs.size(); i++) {
				String name = codecs.get(i);
				Class< ? > classDef = Class.forName(name);
				Codec codec = (Codec) classDef.newInstance();
				PlugInManager.addPlugIn(name, codec.getSupportedInputFormats(),
						codec.getSupportedOutputFormats(null),
						PlugInManager.CODEC);
			}

			/*
			 * PlugInManager.removePlugIn(
			 * "com.sun.media.renderer.video.JPEGRenderer",
			 * PlugInManager.RENDERER);
			 */
			/*
			 * PlugInManager.removePlugIn(
			 * "com.sun.media.codec.video.jpeg.NativeDecoder",
			 * PlugInManager.CODEC);
			 */
			/*
			 * PlugInManager.removePlugIn(
			 * "com.sun.media.codec.video.jpeg.NativeEncoder",
			 * PlugInManager.CODEC);
			 */
			/*
			 * PlugInManager.removePlugIn(
			 * "com.sun.media.codec.video.jpeg.DePacketizer",
			 * PlugInManager.CODEC);
			 */
			PlugInManager.removePlugIn(
					"com.sun.media.codec.video.h261.NativeDecoder",
					PlugInManager.CODEC);

			PlugInManager.removePlugIn(
					"com.sun.media.codec.video.vcm.NativeDecoder",
					PlugInManager.CODEC);
			PlugInManager.removePlugIn(
					"com.sun.media.codec.video.vcm.NativeEncoder",
					PlugInManager.CODEC);
			PlugInManager.removePlugIn(
					"com.sun.media.codec.video.colorspace.JavaRGBConverter",
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




			// PlugInManager.removePlugIn(
			// "codecs.h261Encoder.H261Encoder",
			// PlugInManager.CODEC);
			/*
			 * PlugInManager.removePlugIn(
			 * "com.sun.media.codec.video.h261.NativeDecoder",
			 * PlugInManager.CODEC);
			 */
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, ERROR_TEXT + e.getMessage());
			System.exit(0);
		}
	}

	/**
	 * Detects capture devices for use
	 */
	@SuppressWarnings("unchecked")
	public void detectCaptureDevices() {
		registerDataSource();

		// Sound and VFW devices
		DisabledSecurity.security = null;
		JMFSecurityManager.disableSecurityFeatures();
		/*if (CaptureDeviceManager.getDeviceList(null).size() < MINIMUM_CAPTURE_DEVICES_BEFORE_DETECT) {
			AccessController.doPrivileged(new PrivilegedAction<Object>() {
				public Object run() {
					Class< ? > sound = null;
                    Class< ? > directAudio = null;
                    try {
                        directAudio = Class.forName("DirectSoundAuto");
                        if (directAudio != null) {
                            try {
                                directAudio.newInstance();
                            } catch (Error e) {
                                directAudio = null;
                                e.printStackTrace();
                            }
                            System.err.println("found DirectSoundAuto");
                        }

                        sound = Class.forName("JavaSoundAuto");
                        if (sound != null) {
                            try {
                                sound.newInstance();
                            } catch (Error e) {
                                sound = null;
                                e.printStackTrace();
                            }
                            System.err.println("found JavaSoundAuto");
                        }
					} catch (Exception e) {
						e.printStackTrace();
					}
					return null;
				}
			});
		} */

		CaptureDeviceInfo localAudio = new CaptureDeviceInfo("Local Audio",
				new MediaLocator("javasound://44100/16/1"),
				new Format[] {new AudioFormat(AudioFormat.LINEAR)});
		CaptureDeviceManager.addDevice(localAudio);

		// Civil devices
		CaptureSystemFactory factory = DefaultCaptureSystemFactorySingleton
				.instance();

		//remove disconnected devices
		Vector<CaptureDeviceInfo> devices = CaptureDeviceManager.getDeviceList(new RGBFormat());
		for(int i= devices.size()-1; i >= 0; i--){
//			System.out.println("device " + i + " " + devices.get(i));
			try {
				Manager.createDataSource(devices.get(i).getLocator()).connect();
			} catch (Exception e) {
				boolean ret = locators.remove(devices.get(i).getLocator().toString());
				System.out.println("remove " + ret + " "+ devices.get(i).getName() + " / " + devices.get(i).getLocator());
				CaptureDeviceManager.removeDevice(devices.get(i));
			}
		}

		try {
			CaptureSystem system = factory.createCaptureSystem();
			system.init();
			List<com.lti.civil.CaptureDeviceInfo> list = system.getCaptureDeviceInfoList();
			for (int i = 0; i < list.size(); ++i) {
				com.lti.civil.CaptureDeviceInfo civilInfo = list.get(i);
				/*TODO correct that*/
				CaptureDeviceInfo jmfInfo = new CaptureDeviceInfo("local:"+civilInfo
						.getDescription(), new MediaLocator(CIVIL_LOCATOR
						+ civilInfo.getDeviceID()),
						new Format[] {new RGBFormat()});
				boolean found = false;
				System.out.println("no locators " + locators.size());
				for(int j=0 ;j < locators.size(); j++){
					if(locators.get(j).compareTo(jmfInfo.getLocator().toString()) == 0) {
						System.out.println("found: " + jmfInfo.getLocator().toString());
						found = true;
					}
				}
				if(found == false) {
					locators.add(jmfInfo.getLocator().toString());
					if (CaptureDeviceManager.getDevice(jmfInfo.getName()) == null) {
						CaptureDeviceManager.addDevice(jmfInfo);
					} else {
						String capturename = "local:"+civilInfo.getDescription();
						while (CaptureDeviceManager.getDevice(capturename) != null) {
							capturename=capturename.concat("_");
						}
						jmfInfo = new CaptureDeviceInfo(capturename, new MediaLocator(CIVIL_LOCATOR
								+ civilInfo.getDeviceID()),
								new Format[] {new RGBFormat()});
						CaptureDeviceManager.addDevice(jmfInfo);
					}
					if(availableDatasources.get(jmfInfo.getName()) != null){
						try {
							availableDatasources.get(jmfInfo.getName()).stop();
							availableDatasources.get(jmfInfo.getName()).disconnect();
							availableDatasources.remove(jmfInfo.getName());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					System.out.println("not found: " + jmfInfo.getLocator().toString() + " " + jmfInfo.getName());
				}
//				System.out.println("Device: " + jmfInfo.getName());
//				System.out.println("locator " + jmfInfo.getLocator());
			}
		} catch (CaptureException e) {
			e.printStackTrace();
		}

	}

	@SuppressWarnings("unchecked")
	private void registerDataSource() {

		// get registered prefixes
		Vector<String> prefixes = PackageManager.getProtocolPrefixList();

		// create new prefix
		String newPrefix = "net.sf.fmj";

		// Go through existing prefixes and if the new one isn't already there,
		// then add it
		boolean protocolFound = false;
		for (Enumeration e = prefixes.elements(); e.hasMoreElements();) {
			String s = (String) e.nextElement();
			System.out.println("protocol " + s);
			if (s.equals(newPrefix)) {
				protocolFound = true;
			}
		}
		if (!protocolFound) {
			System.out.println("protocol not found - registering");
			prefixes.addElement(newPrefix);
			PackageManager.setProtocolPrefixList(prefixes);
			PackageManager.commitProtocolPrefixList();
		}
	}

	@SuppressWarnings("unchecked")
	public Vector<String> getVideoDeviceList() {
		Vector<String> video = new Vector<String>();
		Vector<CaptureDeviceInfo> devices = CaptureDeviceManager
				.getDeviceList(null);
		for (int i = 0; i < devices.size(); i++) {
			CaptureDeviceInfo cdi = devices.get(i);
			Format[] formats = cdi.getFormats();
			for (int j = 0; (j < formats.length); j++) {
				if (formats[j] instanceof VideoFormat) {
					video.add(cdi.getName());
					break;
				}
			}
		}
		return video;
	}

	@SuppressWarnings("unchecked")
	public Vector<String> getAudioDeviceList() {
		Vector<String> audio = new Vector<String>();
		Vector<CaptureDeviceInfo> devices = CaptureDeviceManager
				.getDeviceList(null);
		for (int i = 0; i < devices.size(); i++) {
			CaptureDeviceInfo cdi = devices.get(i);
			Format[] formats = cdi.getFormats();
			for (int j = 0; (j < formats.length); j++) {
				if (formats[j] instanceof AudioFormat) {
					audio.add(cdi.getName());
					break;
				}
			}
		}
		return audio;
	}

	/**
	 * Starts the device
	 */
	public synchronized void startDevice(String device,
			RTPSocketAdapter recvSocket, int number) throws Exception {
		if (device == null) {
			return;
		}
		CaptureDeviceInfo cdi = CaptureDeviceManager.getDevice(device);
		MediaLocator locator = cdi.getLocator();
		SendStream sendStream = null;
		if (locator == null) {
			JOptionPane
					.showMessageDialog(null, "Selected Device was not found");
			return;
		}

		// DataSource ds = Manager.createDataSource(locator);
		System.out
				.println("availableDatasources " + availableDatasources.size());
		DataSource ds2 = availableDatasources.get(device);
		DataSource ds; // =Manager.createCloneableDataSource(availableDatasources.get(device));
		if (ds2 == null) {
			ds2 = Manager.createDataSource(locator);
			ds = Manager.createCloneableDataSource(ds2);
			availableDatasources.put(device, ds);
		} else {
			System.out.println("have controls? " + ds2.getControls().length);
			ds = ((SourceCloneable) ds2).createClone();
			// if we have a data source we are already sending it - don't do it again
			for (int i = 0; i < streamListeners.size(); i++) {
				streamListeners.get(i).addStream(ds, ((PushBufferDataSource) ds)
						.getStreams()[0].getFormat(), number);
			}
			return;
		}
		CloneEffect cloneEffect = new CloneEffect();

		PushBufferStream[] datastreams = ((PushBufferDataSource) ds)
				.getStreams();
		float frameRate = 30.0f;
		FormatControl formatControl = (FormatControl) ds
				.getControl(FormatControl.class.getName());
		if (formatControl != null) {
			Format format = formatControl.getFormat();
			if (format instanceof RGBFormat) {
				RGBFormat rgb = (RGBFormat) format;
				format = new RGBFormat(rgb.getSize(), rgb.getMaxDataLength(),
						rgb.getDataType(), frameRate, rgb.getBitsPerPixel(),
						rgb.getRedMask(), rgb.getGreenMask(),
						rgb.getBlueMask(), rgb.getPixelStride(), rgb
								.getLineStride(), rgb.getFlipped(), rgb
								.getEndian());
				formatControl.setFormat(format);
			} else if (format instanceof YUVFormat) {
				YUVFormat yuv = (YUVFormat) format;
				format = new YUVFormat(yuv.getSize(), yuv.getMaxDataLength(),
						yuv.getDataType(), frameRate, yuv.getYuvType(), yuv
								.getStrideY(), yuv.getStrideUV(), yuv
								.getOffsetY(), yuv.getOffsetU(), yuv
								.getOffsetV());
				formatControl.setFormat(format);
			}
		} else {
			FrameRateControl frameRateControl = (FrameRateControl) ds
					.getControl(FrameRateControl.class.getName());
			if (frameRateControl != null) {
				frameRateControl.setFrameRate(frameRate);
			}
		}

		Format origFormat = datastreams[0].getFormat();
		System.err.println("Orig format = " + datastreams[0].getFormat());

		// Setup the sending element and add the datasource
		SendOnlyRTPSocketAdapter socket = new SendOnlyRTPSocketAdapter(
				recvSocket);
		sendManager = RTPManager.newInstance();
		sendManager.addFormat(new VideoFormat("h261as/rtp"), 77);
		sendManager.addFormat(new AudioFormat("ULAW/rtp", 16000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED), 84);
		sendManager.addSessionListener(streamManagement);
		sendManager.addReceiveStreamListener(streamManagement);
		sendManager.addRemoteListener(streamManagement);
		sendManager.addSendStreamListener(streamManagement);
		sendManager.initialize(socket);

		if (origFormat instanceof VideoFormat) {

			FormatItem format = formats[0];
			System.out.println("format " + format);
			Format setFormat = format.outputFormat;

			// Configure the processor
			sendProcessor = javax.media.Manager.createProcessor(ds);
			sendProcessor.addControllerListener(this);
			sendProcessor.configure();
			System.err.println("configure processor");
			processorFailed = false;
			while (!processorFailed
					&& (sendProcessor.getState() < Processor.Configured)) {
				synchronized (stateLock) {
					stateLock.wait();
				}
			}
			if (processorFailed) {
				throw new Exception(CONTROLLER_ERROR_TEXT);
			}

			// Set to send in RTP
			ContentDescriptor cd = new ContentDescriptor(
					ContentDescriptor.RAW_RTP);
			sendProcessor.setContentDescriptor(cd);

			// Set the format of the transmission to the selected value
			TrackControl[] tracks = sendProcessor.getTrackControls();
			for (int i = 0; i < tracks.length; i++) {
				if (tracks[i].isEnabled()) {
					origFormat = datastreams[0].getFormat();

					// set codec chain -- add the change effect
					if (datastreams[0].getFormat() instanceof VideoFormat) {
						if (format.equals(VideoFormat.JPEG_RTP)) {
							VideoFormat dataFormat = (VideoFormat) datastreams[0]
									.getFormat();
							Dimension size = new Dimension(dataFormat.getSize());
							float scale = 1f;
							size.width *= scale;
							size.height *= scale;
							setFormat = new VideoFormat(format.outputFormat
									.getEncoding(), size,
									VideoFormat.NOT_SPECIFIED,
									VideoFormat.byteArray,
									VideoFormat.NOT_SPECIFIED);
						}
						System.err.println("VideoFormat " + setFormat);
						if (tracks[i].setFormat(setFormat) == null) {
							throw new Exception("Format unsupported by track "
									+ i);
						}
					}
					if (datastreams[0].getFormat() instanceof AudioFormat) {
						setFormat = new AudioFormat(/*AudioFormat.LINEAR*/
								"ULAW/rtp", 16000, 8, 1, AudioFormat.LITTLE_ENDIAN,
								AudioFormat.SIGNED);

						if (tracks[i].setFormat(setFormat) == null) {
							throw new Exception("Format unsupported by track "
									+ i);
						}
					}

					try {
						tracks[i].setCodecChain(new Codec[] {cloneEffect});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			System.err.println("realize processor 2");

			// Realise the processor
			sendProcessor.realize();
			processorFailed = false;
			while (!processorFailed
					&& (sendProcessor.getState() < Processor.Realized)) {
				synchronized (stateLock) {
					stateLock.wait();
				}
			}
			if (processorFailed) {
				throw new Exception(CONTROLLER_ERROR_TEXT);
			}

			// Set the quality dependant on the slider
			QualityControl quality = (QualityControl) sendProcessor
					.getControl("javax.media.control.QualityControl");
			if (quality != null) {
				quality.setQuality(0.85f);
			}

			// Get the output of the processor
			/*
			 * SimpleProcessor processor = new SimpleProcessor(origFormat,
			 * setFormat);
			 */
			PushBufferDataSource data = /* processor.getDataOutput(ds, 0); */
			(PushBufferDataSource) sendProcessor.getDataOutput();

			sendManagers.add(sendManager);
			sendStream = sendManager.createSendStream(data, 0);
			String cname = sendStream.getParticipant().getCNAME();
			sendStream.setSourceDescription(new SourceDescription[] {
					new SourceDescription(SourceDescription.SOURCE_DESC_CNAME,
							cname, 1, false),
					new SourceDescription(SourceDescription.SOURCE_DESC_NAME,
							device, 3, false)});

			// Start sending
			sendStream.start();
			sendStreams.add(sendStream);

			// Start the processor
			sendProcessor.start();
			sendProcessors.add(sendProcessor);
			devicemap.put(device, sendProcessor);
			// processor.start(ds, 0);
			for (int i = 0; i < streamListeners.size(); i++) {
				streamListeners.get(i).addStream(cloneEffect, origFormat,
						number);
			}
		} else {

			// Configure the processor
			sendProcessor = javax.media.Manager.createProcessor(ds);
			sendProcessor.addControllerListener(this);
			sendProcessor.configure();
			System.err.println("configure processor");
			processorFailed = false;
			while (!processorFailed
					&& (sendProcessor.getState() < Processor.Configured)) {
				synchronized (stateLock) {
					stateLock.wait();
				}
			}
			if (processorFailed) {
				throw new Exception(CONTROLLER_ERROR_TEXT);
			}

			// Set to send in RTP
			ContentDescriptor cd = new ContentDescriptor(
					ContentDescriptor.RAW_RTP);
			sendProcessor.setContentDescriptor(cd);

			// Set the format of the transmission to the selected value
			TrackControl[] tracks = sendProcessor.getTrackControls();
			for (int i = 0; i < tracks.length; i++) {
				if (tracks[i].isEnabled()) {
					origFormat = datastreams[0].getFormat();

					// set codec chain -- add the change effect
					if (datastreams[0].getFormat() instanceof AudioFormat) {
//						Format setFormat = new AudioFormat(AudioFormat.ULAW_RTP);
						Format setFormat = new AudioFormat(/*AudioFormat.LINEAR*/"ULAW/rtp", 16000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);

						setFormat = tracks[i].setFormat(setFormat);
						System.err.println("Format set to " + setFormat);
						if (setFormat == null) {
							throw new Exception("Format unsupported by track "
									+ i);
						}
					}
				}
			}

			System.err.println("realize processor 2");

			// Realise the processor
			sendProcessor.realize();
			processorFailed = false;
			while (!processorFailed
					&& (sendProcessor.getState() < Processor.Realized)) {
				synchronized (stateLock) {
					stateLock.wait();
				}
			}
			if (processorFailed) {
				throw new Exception(CONTROLLER_ERROR_TEXT);
			}

			DataSource data = sendProcessor.getDataOutput();

			// Setup the sending element and add the datasource
			sendManagers.add(sendManager);
			System.err.println("init socket end LR");
			sendStream = sendManager.createSendStream(data, 0);

			// Start sending
			System.err.println("start processor 3");
			sendStream.start();
			sendStreams.add(sendStream);

			// Start the processor
			sendProcessor.start();
			sendProcessors.add(sendProcessor);
			devicemap.put(device, sendProcessor);

			System.err.println("end start processor");
		}
	}

	/**
	 * Starts the device
	 */
	public synchronized void startVideo(String filename,
			RTPSocketAdapter recvSocket, long seek, double scale, int number)
			throws Exception {

		System.out.println("startVideo called " + filename);

		if (filename == null || filename.length() == 0) {
			return;
		}
		MediaLocator locator = new MediaLocator("recorded://" + filename
				+ "?seek=" + seek + "&scale=" + scale);

		if (locator == null) {
			JOptionPane
					.showMessageDialog(null, "Selected Video was not found");
			return;
		}
		SendStream sendStream = null;
		// DataSource ds = Manager.createDataSource(locator);
		System.out
				.println("availableDatasources" + availableDatasources.size());
		javax.media.protocol.recorded.DataSource ds;
		ds = (javax.media.protocol.recorded.DataSource) Manager.createDataSource(locator);
		System.out.println("ds's " + ds);
		ds.connect();
		CloneEffect cloneEffect = new CloneEffect();

		PushBufferStream[] datastreams = ((PushBufferDataSource) ds)
				.getStreams();
		float frameRate = 30.0f;
		FormatControl formatControl = (FormatControl) ds
				.getControl(FormatControl.class.getName());
		if (formatControl != null) {
			Format format = formatControl.getFormat();
			if (format instanceof RGBFormat) {
				RGBFormat rgb = (RGBFormat) format;
				format = new RGBFormat(rgb.getSize(), rgb.getMaxDataLength(),
						rgb.getDataType(), frameRate, rgb.getBitsPerPixel(),
						rgb.getRedMask(), rgb.getGreenMask(),
						rgb.getBlueMask(), rgb.getPixelStride(), rgb
								.getLineStride(), rgb.getFlipped(), rgb
								.getEndian());
				formatControl.setFormat(format);
			} else if (format instanceof YUVFormat) {
				YUVFormat yuv = (YUVFormat) format;
				format = new YUVFormat(yuv.getSize(), yuv.getMaxDataLength(),
						yuv.getDataType(), frameRate, yuv.getYuvType(), yuv
								.getStrideY(), yuv.getStrideUV(), yuv
								.getOffsetY(), yuv.getOffsetU(), yuv
								.getOffsetV());
				formatControl.setFormat(format);
			}
		} else {
			FrameRateControl frameRateControl = (FrameRateControl) ds
					.getControl(FrameRateControl.class.getName());
			if (frameRateControl != null) {
				frameRateControl.setFrameRate(frameRate);
			}
		}

		Format origFormat = datastreams[0].getFormat();
		System.err.println("Orig format = " + datastreams[0].getFormat());

		// Setup the sending element and add the datasource
		SendOnlyRTPSocketAdapter socket = new SendOnlyRTPSocketAdapter(
				recvSocket);
		sendManager = RTPManager.newInstance();
		sendManager.addFormat(new VideoFormat("h261as/rtp"), 77);
		sendManager.addFormat(new AudioFormat("ULAW/rtp", 16000, 8, 1,
				AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED),84);

		sendManager.addSessionListener(streamManagement);
		sendManager.addReceiveStreamListener(streamManagement);
		sendManager.addRemoteListener(streamManagement);
		sendManager.addSendStreamListener(streamManagement);
		sendManager.initialize(socket);

		if (origFormat instanceof VideoFormat) {

			FormatItem format = formats[0];
			System.out.println("format " + format);
			Format setFormat = format.outputFormat;

			// Configure the processor
			sendProcessor = javax.media.Manager.createProcessor(ds);
			sendProcessor.addControllerListener(this);
			sendProcessor.configure();
			System.err.println("configure processor");
			processorFailed = false;
			while (!processorFailed
					&& (sendProcessor.getState() < Processor.Configured)) {
				synchronized (stateLock) {
					stateLock.wait();
				}
			}
			if (processorFailed) {
				throw new Exception(CONTROLLER_ERROR_TEXT);
			}

			// Set to send in RTP
			ContentDescriptor cd = new ContentDescriptor(
					ContentDescriptor.RAW_RTP);
			sendProcessor.setContentDescriptor(cd);

			// Set the format of the transmission to the selected value
			TrackControl[] tracks = sendProcessor.getTrackControls();
			for (int i = 0; i < tracks.length; i++) {
				if (tracks[i].isEnabled()) {
					origFormat = datastreams[0].getFormat();

					// set codec chain -- add the change effect
					if (datastreams[0].getFormat() instanceof VideoFormat) {
						if (format.equals(VideoFormat.JPEG_RTP)) {
							VideoFormat dataFormat = (VideoFormat) datastreams[0]
									.getFormat();
							Dimension size = new Dimension(dataFormat.getSize());
//							float scale = 1f;
							size.width *= scale;
							size.height *= scale;
							setFormat = new VideoFormat(format.outputFormat
									.getEncoding(), size,
									VideoFormat.NOT_SPECIFIED,
									VideoFormat.byteArray,
									VideoFormat.NOT_SPECIFIED);
						}
						System.err.println("VideoFormat " + setFormat);
						if (tracks[i].setFormat(setFormat) == null) {
							throw new Exception("Format unsupported by track "
									+ i);
						}
					}
					if (datastreams[0].getFormat() instanceof AudioFormat) {
//						setFormat = new AudioFormat(AudioFormat.LINEAR);
						setFormat = new AudioFormat(/*AudioFormat.LINEAR*/"ULAW/rtp", 16000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);

						if (tracks[i].setFormat(setFormat) == null) {
							throw new Exception("Format unsupported by track "
									+ i);
						}
					}

					try {
						tracks[i].setCodecChain(new Codec[] {cloneEffect});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			System.err.println("realize processor 2");

			// Realise the processor
			sendProcessor.realize();
			processorFailed = false;
			while (!processorFailed
					&& (sendProcessor.getState() < Processor.Realized)) {
				synchronized (stateLock) {
					stateLock.wait();
				}
			}
			if (processorFailed) {
				throw new Exception(CONTROLLER_ERROR_TEXT);
			}

			// Set the quality dependant on the slider
			QualityControl quality = (QualityControl) sendProcessor
					.getControl("javax.media.control.QualityControl");
			if (quality != null) {
				quality.setQuality(0.85f);
			}

			// Get the output of the processor
			/*
			 * SimpleProcessor processor = new SimpleProcessor(origFormat,
			 * setFormat);
			 */
			PushBufferDataSource data = /* processor.getDataOutput(ds, 0); */
			(PushBufferDataSource) sendProcessor.getDataOutput();

			sendManagers.add(sendManager);
			sendStream = sendManager.createSendStream(data, 0);
			String cname = sendStream.getParticipant().getCNAME();
			sendStream.setSourceDescription(new SourceDescription[] {
					new SourceDescription(SourceDescription.SOURCE_DESC_CNAME,
							cname, 1, false),
					new SourceDescription(SourceDescription.SOURCE_DESC_NAME,
							filename.substring(filename.lastIndexOf('/')), 3, false)});

			// Start sending
			sendStream.start();
//			sendStream.start();
//			Thread.sleep(50000);
//			sendStream.stop();
			sendStreams.add(sendStream);

			// Start the processor
			sendProcessor.start();
			sendProcessors.add(sendProcessor);
			// processor.start(ds, 0);
			for (int i = 0; i < streamListeners.size(); i++) {
				streamListeners.get(i).addVideo(cloneEffect, origFormat,
						sendStream.getSSRC(), number);
			}
			Thread.sleep(5000);
			sendStream.stop();
			videoDataSources.put(sendStream.getSSRC(), ds);
		} else {

			// Configure the processor
			sendProcessor = javax.media.Manager.createProcessor(ds);
			sendProcessor.addControllerListener(this);
			sendProcessor.configure();
			System.err.println("configure processor");
			processorFailed = false;
			while (!processorFailed
					&& (sendProcessor.getState() < Processor.Configured)) {
				synchronized (stateLock) {
					stateLock.wait();
				}
			}
			if (processorFailed) {
				throw new Exception(CONTROLLER_ERROR_TEXT);
			}

			// Set to send in RTP
			ContentDescriptor cd = new ContentDescriptor(
					ContentDescriptor.RAW_RTP);
			sendProcessor.setContentDescriptor(cd);

			// Set the format of the transmission to the selected value
			TrackControl[] tracks = sendProcessor.getTrackControls();
			for (int i = 0; i < tracks.length; i++) {
				if (tracks[i].isEnabled()) {
					origFormat = datastreams[0].getFormat();

					// set codec chain -- add the change effect
					if (datastreams[0].getFormat() instanceof AudioFormat) {
//						Format setFormat = new AudioFormat(AudioFormat.ULAW_RTP);
						Format setFormat = new AudioFormat(/*AudioFormat.LINEAR*/"ULAW/rtp", 16000, 8, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);

						setFormat = tracks[i].setFormat(setFormat);
						System.err.println("Format set to " + setFormat);
						if (setFormat == null) {
							throw new Exception("Format unsupported by track "
									+ i);
						}
					}
				}
			}

			System.err.println("realize processor 2");

			// Realise the processor
			sendProcessor.realize();
			processorFailed = false;
			while (!processorFailed
					&& (sendProcessor.getState() < Processor.Realized)) {
				synchronized (stateLock) {
					stateLock.wait();
				}
			}
			if (processorFailed) {
				throw new Exception(CONTROLLER_ERROR_TEXT);
			}

			DataSource data = sendProcessor.getDataOutput();

			// Setup the sending element and add the datasource
			sendManagers.add(sendManager);
			System.err.println("init socket end LR");
			sendStream = sendManager.createSendStream(data, 0);

			// Start sending
			System.err.println("start processor 3");
			sendStream.start();
			sendStreams.add(sendStream);

			// Start the processor
			sendProcessor.start();
			sendProcessors.add(sendProcessor);

			System.err.println("end start processor");
		}
	}

	public void playVideo(long ssrc, long seek, double scale) throws IOException {
		System.out.println("playVideo called " + ssrc + "seek " + seek);
		for (int i = 0; i < sendStreams.size(); i++) {
			if (sendStreams.get(i).getSSRC() == ssrc) {
				SendStream sendStream = sendStreams.get(i);
				javax.media.protocol.recorded.DataSource dataSource
					= videoDataSources.get(ssrc);
				if (dataSource != null) {
					dataSource.seek(seek, scale);
				}
				sendStream.start();
				return;
			}
		}
	}

	public void playAudio(long ssrc, double seek) throws IOException {
		System.out.println("playAudio called " + ssrc + "seek " + seek);
		for (int i = 0; i < sendStreams.size(); i++) {
			if (sendStreams.get(i).getSSRC() == ssrc) {
				sendProcessors.get(i).setMediaTime(new Time(seek));
				return;
			}
		}
	}

	public void stopVideo(long ssrc) throws IOException {
		for (int i = 0; i < sendStreams.size(); i++) {
			if (sendStreams.get(i).getSSRC() == ssrc) {
				sendStreams.get(i).stop();
				return;
			}
		}
	}


//    /**
//     * Start replay a session
//     *
//     * @return True if the screen starts transmitting ok
//     */
//    public synchronized boolean loadLocalStream(String file,
//			RTPSocketAdapter recvSocket, int number) {
//
//    	PlaybackNetworkTransport netTransUsed = new PlaybackNetworkTransport(new NetworkEndpoint(
//                recvSocket.getAddress().getHostName(),
//                recvSocket.getAddress().getPort(), recvSocket.getTTL()), null,
//                sessionId);
//    	netTransUsed.setSendPort(recvSocket.getAddress().getPort());
//        synch = new PlayStreamSynchronizer(sessionId);
//        synch.addTimeUpdateListener(this);
//        if (netTransUsed == null) {
//            return false;
//        }
//        try {
//            session = new LocalSession(new URL("http",
//            		netTransUsed.getEndpoint().getHost(),
//                    netTransUsed.getEndpoint().getPort(), "").toString());
//        } catch (MalformedURLException e1) {
//            e1.printStackTrace();
//            return false;
//        }
//
//        try {
//        	System.out.println("file " + file);
//			session.addStream(file.substring(file.lastIndexOf('/')));
//			StreamSource ss = new StreamSource(file.substring(0, file.lastIndexOf('/')));
//			streamSource.add(ss);
//			ss.setup(new RTSPSetupRequest(), synch, netTransUsed, session, file.substring(file.lastIndexOf('/')));
//			synch.registerStream(ss, System.currentTimeMillis() + 1000);
//			for (int i = 0; i < streamListeners.size(); i++) {
//				streamListeners.get(i).addVideo(null, ss.getTransport(). sessionId , number);
//			}
//			client.rtsp.RTSPPlayRequest req2 = new client.rtsp.RTSPPlayRequest(
//					netTransUsed.getDestination(), session.getId(), 1.0);
//			synch.play(req2);
////			for (int i = 0; i < streamListeners.size(); i++) {
////				streamListeners.get(i).addVideo(null, ss.getTransport(). sessionId , number);
////			}
//		} catch (MalformedURLException e) {
//			e.printStackTrace();
//			return false;
//		} catch (RTSPResponse e) {
//			e.printStackTrace();
//			return false;
//		}
//        return true;
//
//    }
//

	/**
	 * Stop the transmission of the screen
	 */
	public void stopTransmitting() {
		SendStream sendStream = null;
		while (sendProcessors.size() > 0) {
			sendProcessor = sendProcessors.lastElement();
			if (sendProcessor != null) {
				System.err.println("before stop processor ");
				sendProcessor.stop();
				System.err.println("before close processor ");
				sendProcessor.close();
			}
			System.err.println("before remove processor ");
			sendProcessors.remove(sendProcessor);
		}
		System.err.println("end processors");
		while (sendStreams.size() > 0) {
			try {
				sendStream = sendStreams.firstElement();
				if (sendStream != null) {
					sendStream.stop();
					sendStream.close();
				}
				sendStreams.remove(sendStream);
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}
		System.err.println("end streams");
		while (sendManagers.size() > 0) {
			sendManager = sendManagers.firstElement();
			sendManagers.remove(sendManager);
			if (sendManager != null) {
				sendManager.dispose();
			}
		}
		System.err.println("end managers");
	}

	private class FormatItem {

		private Format outputFormat;

		// private Format preferredInputFormat;

		private FormatItem(Format outputFormat, Format preferredInputFormat) {
			this.outputFormat = outputFormat;
			// this.preferredInputFormat = preferredInputFormat;
		}

		/**
		 *
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return outputFormat.getEncoding();
		}
	}

	public void streamListener(StreamListener asl) {
		streamListeners.add(asl);
	}

	public void removeStreamListener(StreamListener asl) {
		streamListeners.remove(asl);
	}

	public void controllerUpdate(ControllerEvent ce) {
		// If there was an error during configure or
		// realize, the processor will be closed
		if (ce instanceof ControllerClosedEvent) {
			processorFailed = true;
		}

		// All controller events, send a notification
		// to the waiting thread in waitForState method.
		synchronized (stateLock) {
			stateLock.notifyAll();
		}
	}

	public void removeMedia(int position) {
		for (int i = 0; i < streamListeners.size(); i++) {
			streamListeners.get(i).removeMedia(position);
		}
	}

	public void updateTime(long time) {
		// TODO Auto-generated method stub
	}

	public void startVideo(long ssrc) {
		for (int i = 0; i < sendStreams.size(); i++) {
			if (sendStreams.get(i).getSSRC() == ssrc) {
				try {
					sendStreams.get(i).start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void startRemoteDevice(String device, DataSource datasource, Format format,
			int number) {
		CloneEffect cloneEffect = new CloneEffect();
		DataSource ds2 = availableDatasources.get(device);
		DataSource ds;
		if (ds2 == null) {
			ds2 = datasource;
			ds = Manager.createCloneableDataSource(ds2);
			availableDatasources.put(device, ds);
		} else {
			ds = ((SourceCloneable) ds2).createClone();
		}
		System.err.println("ds " + ds + " datasource " + datasource);
		cloneEffect.setLocator(ds.getLocator());
		for (int i = 0; i < streamListeners.size(); i++) {
			streamListeners.get(i).addStream(ds, format,
					number);
		}
	}

	public void startAudio(String filename, RTPSocketAdapter recvSocket, int seek,
			int number) throws Exception {

		System.out.println("start Audio File");

		MediaLocator locator = new MediaLocator("file:/"+filename);

		if (locator == null) {
			JOptionPane
					.showMessageDialog(null, "Selected Video was not found");
			return;
		}
		SendStream sendStream = null;
		System.out.println("availableDatasources" + availableDatasources.size());
		// Setup the sending element and add the datasource
		SendOnlyRTPSocketAdapter socket = new SendOnlyRTPSocketAdapter(
				recvSocket);
		sendManager = RTPManager.newInstance();
		sendManager.addFormat(new AudioFormat("ULAW/rtp", 16000, 8, 1,
				AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED), 84);
		sendManager.addSessionListener(streamManagement);
		sendManager.addReceiveStreamListener(streamManagement);
		sendManager.addRemoteListener(streamManagement);
		sendManager.addSendStreamListener(streamManagement);
		sendManager.initialize(socket);


//		System.out.println("format " + origFormat.toString());
//		if (origFormat instanceof AudioFormat) {

			// Configure the processor
			sendProcessor = javax.media.Manager.createProcessor(/*ds*/locator);
			sendProcessor.addControllerListener(this);
			sendProcessor.configure();
			System.err.println("configure processor");
			processorFailed = false;
			while (!processorFailed
					&& (sendProcessor.getState() < Processor.Configured)) {
				synchronized (stateLock) {
					stateLock.wait();
				}
			}
			if (processorFailed) {
				throw new Exception(CONTROLLER_ERROR_TEXT);
			}

			// Set to send in RTP
			ContentDescriptor cd = new ContentDescriptor(
					ContentDescriptor.RAW_RTP);
			sendProcessor.setContentDescriptor(cd);

			// Set the format of the transmission to the selected value
			TrackControl[] tracks = sendProcessor.getTrackControls();
//			for (int i = 0; i < tracks.length; i++) {
//				if (tracks[i].isEnabled()) {
//					origFormat = datastreams[0].getFormat();
//
//					// set codec chain -- add the change effect
//					if (datastreams[0].getFormat() instanceof AudioFormat) {
//						Format setFormat = new AudioFormat(AudioFormat.ULAW_RTP);
//						setFormat = tracks[i].setFormat(setFormat);
//						System.err.println("Format set to " + setFormat);
//						if (setFormat == null) {
//							throw new Exception("Format unsupported by track "
//									+ i);
//						}
//					}
//				}
//			}

			boolean encodingOk = false;
			// Go through the tracks and try to program one of them to
			// output ulaw data.
			for (int i = 0; i < tracks.length; i++) {
				if (!encodingOk && tracks[i] instanceof FormatControl) {
					if (((FormatControl) tracks[i]).setFormat(/*new AudioFormat(
							AudioFormat.ULAW_RTP)*/
							new AudioFormat("ULAW/rtp", 16000, 8, 1,
									AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED))
									== null) {
						tracks[i].setEnabled(false);
					} else {
						encodingOk = true;
					}
				} else {
					// we could not set this track to ulaw, so disable it
					tracks[i].setEnabled(false);
				}
			}
			// At this

			System.err.println("realize processor 2");

			// Realise the processor
			sendProcessor.realize();
			processorFailed = false;
			while (!processorFailed
					&& (sendProcessor.getState() < Processor.Realized)) {
				synchronized (stateLock) {
					stateLock.wait();
				}
			}
			if (processorFailed) {
				throw new Exception(CONTROLLER_ERROR_TEXT);
			}

			DataSource data = sendProcessor.getDataOutput();

			// Setup the sending element and add the datasource
			sendManagers.add(sendManager);
			System.err.println("init socket end LR");
			sendStream = sendManager.createSendStream(data, 0);

			// Start sending
			System.err.println("start processor 3");
			sendStream.start();
			sendStreams.add(sendStream);

			// Start the processor
			sendProcessor.start();
			sendProcessors.add(sendProcessor);

			for (int i = 0; i < streamListeners.size(); i++) {
				streamListeners.get(i).addStream(locator, sendStream.getSSRC(),
						number);
			}

			System.err.println("end start processor");
//		}
	}

	public void stopCamera(String device) {
		Processor processor = devicemap.get(device);
		if(processor == null){
//			nothing to do;
			return;
		}
		for(int i = 0; i < sendProcessors.size(); i ++){
			if(sendProcessors.get(i).equals(processor)){
				sendProcessors.get(i).stop();
				sendProcessors.get(i).close();
				sendProcessors.remove(i);

				try {
					sendStreams.get(i).stop();
					sendStreams.get(i).close();
					sendStreams.remove(i);
				} catch (IOException e) {
					e.printStackTrace();
				}

				sendManagers.get(i).dispose();
				sendManagers.remove(i);

			}
		}
		availableDatasources.remove(device);
		devicemap.remove(device);
	}


//	public void stopAudio(int position) {
//		// TODO Auto-generated method stub
//
//	}

}
