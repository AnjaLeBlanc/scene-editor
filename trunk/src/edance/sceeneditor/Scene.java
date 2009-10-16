package edance.sceeneditor;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.Vector;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.media.Effect;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.protocol.DataSource;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edance.devicemanagement.DeviceManagement;
import edance.devicemanagement.StreamManagement;
import edance.sceeneditor.AudioFile;
import edance.sceeneditor.AudioFile.PrerecordedAudioState;
import edance.sceeneditor.MediaObjects.POINTS;
import edance.sceeneditor.PrerecordedVideo.PrerecordedVideoState;
import edance.userinterface.MediaPopupMenu;
import edance.userinterface.TimeOffsetDialog;
import edance.userinterface.TransparencyDialog;
import edance.userinterface.MediaPopupMenu.MediaPopupMenuFunction;
import edance.userinterface.MediaPopupMenu.MediaPopupMenuState;
import edance.userinterface.event.StreamListener;
import edance.userinterface.event.MediaPopupMenuListener;
import effects.CloneEffect;

public class Scene extends Object implements MediaPopupMenuListener {
	private long length = 0;
	
	private String sceneName = "";

	private Vector<MediaObjects> media = new Vector<MediaObjects>();

	private MediaPopupMenu menu = new MediaPopupMenu();

	private Graphics g;

	private DeviceManagement dm;

	private Vector<StreamListener> streamListener = new Vector<StreamListener>();

	private StreamManagement sm;
	
	private Vector<ChangeListener> listeners =new Vector<ChangeListener>();

	public Scene(StreamManagement sm) {
		this.sm = sm;
		dm =  new DeviceManagement(sm);
		menu.addMenuPopupMenuListener(this);
		menu.setLightWeightPopupEnabled(false);
	}

	public Scene(Node node, StreamManagement sm) {
		this.sm = sm;
		dm =  new DeviceManagement(sm);
		menu.addMenuPopupMenuListener(this);
		menu.setLightWeightPopupEnabled(false);
		
		NamedNodeMap nnm = node.getAttributes();
		if(nnm != null){
			for(int i = 0; i < nnm.getLength(); i++){
				Node n2 = nnm.item(i);
				System.out.println("node " + n2.getNodeName());
				if (n2.getNodeName().trim().compareTo("LENGTH") == 0){
					length = Long.valueOf(n2.getNodeValue());
				}
				if (n2.getNodeName().trim().compareTo("NAME") == 0){
					this.sceneName = n2.getNodeValue();
				}
			}
		}
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++){
			if(nl.item(i).getNodeName().trim().compareTo("MEDIA_OBJECT") == 0) {
				Node n2 = nl.item(i);
				nnm=n2.getAttributes();
				if(nnm != null){
					for(int j = 0; j < nnm.getLength(); j++){
						Node n3 = nnm.item(j);
						if (n3.getNodeName().trim().compareTo("NAME") == 0){
							try {
								media.add((MediaObjects)Class.forName(n3.getNodeValue()).
									getConstructor(Node.class).newInstance(n2));
							} catch (Exception e) {
								e.printStackTrace();
							} 
						}
					}
				}
			}
		}
		// correct possible wrong positions in loaded file - does not happen when file was written
		// with 'save' option
//		for (int i = 0; i < media.size(); i++) {
//			if(media.get(i).positionZ != i) {
//				MediaObjects obj = media.remove(i);
//				media.add(obj.positionZ, obj);
//				i --;
//			}
//		}
		triggerChange();
	}

	public void addAddStreamListener(StreamListener asl) {
		streamListener.add(asl);
		dm.streamListener(asl);
	}

	public void addMediaObject(MediaObjects md) {
		changed=true;
		media.add(md);
		md.positionZ=media.size()-1;
		triggerChange();
	}

	public void removeMediaObject(MediaObjects md) {
		media.remove(md);
		triggerChange();
	}

	public void setLenghtOfScene(long length) {
		this.length = length;
	}

	public long getLengthOfScene() {
		return length;
	}

	public void setSelectedStates1() {
		for (int i = 0; i < media.size(); i++) {
			if (media.get(i).select.ordinal() > POINTS.ALL_POINTS.ordinal()) {
				media.get(i).select = POINTS.ALL_POINTS;
			}
		}
	}

	public void setSelectedStates0() {
		for (int i = 0; i < media.size(); i++) {
			if (media.get(i).select.ordinal() >= POINTS.ALL_POINTS.ordinal()) {
				media.get(i).select = POINTS.NONE;
			}
		}
	}

	public boolean selectpointCond(int x, int y) {
		for (int i = media.size() - 1; i >= 0; i--) {
			if (media.get(i).select == POINTS.ALL_POINTS) {
				if (media.get(i).selectpoint(x, y)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean selectObject(boolean shiftdown, int x, int y) {
		for (int i = media.size() - 1; i >= 0; i--) {
			if (media.get(i).select == POINTS.ALL_POINTS) {
				if (media.get(i).selectobj(x, y)) {
					return true;
				}
			} else {
				if (media.get(i).selectobj(x, y)) {
					if (!shiftdown) {
						for (int j = 0; j < media.size(); j++) {
							if (i != j) {
								if (media.get(j).select.ordinal() >= POINTS.ALL_POINTS.ordinal()) {
									media.get(j).select = POINTS.NONE;
								}
							}
						}
					}
					return true;
				}
			}
		}
		return false;
	}

	public boolean selectrect(int xx0, int yy0, int xx1, int yy1) {
		boolean returnvalue = false;
		for (int i = 0; i < media.size(); i++) {
			returnvalue = returnvalue
					| media.get(i).selectrect(xx0, yy0, xx1, yy1);
		}
		return returnvalue;
	}

	public void paint(Graphics g) {
		this.g = g;
		for (int i = 0; i < media.size(); i++) {
			media.get(i).paint(g);
		}
	}

	public void paintPreview(Graphics g, int offset) {
		this.g = g;
		for (int i = 0; i < media.size(); i++) {
			media.get(i).paintPreview(g, offset);
		}
	}
	
	public void movepoint(int x, int y) {
		for (int i = 0; i < media.size(); i++) {
			if (media.get(i).select.ordinal() > POINTS.ALL_POINTS.ordinal()) {
				media.get(i).movepoint(x, y);
			}
		}
		triggerChange();
	}

	public void moveobj(int x, int y) {
		for (int i = 0; i < media.size(); i++) {
			if (media.get(i).select == POINTS.ALL_POINTS) {
				media.get(i).moveobj(x, y);
			}
		}
		triggerChange();
	}

	public boolean sentKeyToSelected(int keyCode, boolean ctrl) {
		boolean returnvalue = false;
		if (!ctrl && (keyCode == 'd' || keyCode == 'D'
			|| keyCode == KeyEvent.VK_DELETE)) {
			System.out.println("delet key pressed");
			for (int i = media.size() - 1; i >= 0; i--) {
				if (media.get(i).select == POINTS.ALL_POINTS) {
					media.get(i).stop();
					media.remove(i);
				}
			}
			returnvalue = true;
		} else if (!ctrl && (keyCode == KeyEvent.VK_UP)) {
			for (int i = media.size() - 1; i >= 0; i--) {
				if (media.get(i).select == POINTS.ALL_POINTS) {
					media.get(i).moveobj(0, -1);
				}
			}
			returnvalue = true;
		} else if (!ctrl && (keyCode == KeyEvent.VK_DOWN)) {
			for (int i = media.size() - 1; i >= 0; i--) {
				if (media.get(i).select == POINTS.ALL_POINTS) {
					media.get(i).moveobj(0, 1);
				}
			}
			returnvalue = true;
		} else if (!ctrl && (keyCode == KeyEvent.VK_RIGHT)) {
			for (int i = media.size() - 1; i >= 0; i--) {
				if (media.get(i).select == POINTS.ALL_POINTS) {
					media.get(i).moveobj(1, 0);
				}
			}
			returnvalue = true;
		} else if (!ctrl && (keyCode == KeyEvent.VK_LEFT)) {
			for (int i = media.size() - 1; i >= 0; i--) {
				if (media.get(i).select == POINTS.ALL_POINTS) {
					media.get(i).moveobj(-1, 0);
				}
			}
			returnvalue = true;
		} else if (ctrl && (keyCode == KeyEvent.VK_A)) {
			System.out.println("ctrl a pressed");
			for (int i = media.size() - 1; i >= 0; i--) {
				media.get(i).select = POINTS.ALL_POINTS;
			}
			returnvalue = true;
		} else {
			for (int i = 0; i < media.size(); i++) {
				if (media.get(i).select == POINTS.ALL_POINTS) {
					returnvalue = returnvalue
							| media.get(i).setKey(keyCode, ctrl);
				}
			}
		}
		if(returnvalue == true){
			triggerChange();
		}
		return returnvalue;
	}

	public Vector<MediaObjects> copySelObj() {
		Vector<MediaObjects> currSel = new Vector<MediaObjects>();
		for (int i = 0; i < media.size(); i++) {
			if (media.get(i).select == POINTS.ALL_POINTS) {
				currSel.add(media.get(i));
			}
		}
		return currSel;
	}

	public void pastSelObj(Vector<MediaObjects> obj) {
		for (int i = 0; i < obj.size(); i++) {
			try {
				MediaObjects clone = (MediaObjects) obj.get(i).clone();
				media.add(clone);
				String source = ((MediaObjects) obj.get(i)).getSource();
				attachMedia(clone, source);
				
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		triggerChange();
	}


	public void moveSelTop() {
		int j = 0;
		for (int i = 0; i < (media.size() - j); i++) {
			if (media.get(i).select == POINTS.ALL_POINTS) {
				MediaObjects obj = media.remove(i);
				media.add(obj);
				j++;
				i--;
			}
		}
		for (int i = media.size() - 1; i >= 0; i--){
			media.get(i).positionZ=i;
		}
		for (int i = 0; i < streamListener.size(); i++) {
			streamListener.get(i).zOrderChanged(media);
		}
		triggerChange();
	}

	public void moveSelBottom() {
		int j = 0;
		for (int i = media.size() - 1; i >= j; i--) {
			if (media.get(i).select == POINTS.ALL_POINTS) {
				MediaObjects obj = media.remove(i);
				media.add(0, obj);
				j++;
				i++;
			}
		}
		for (int i = media.size() - 1; i >= 0; i--){
			media.get(i).positionZ=i;
		}
		for (int i = 0; i < streamListener.size(); i++) {
			streamListener.get(i).zOrderChanged(media);
		}
		triggerChange();
	}

	public void moveSelUp() {
		Vector<MediaObjects> exclude = new Vector<MediaObjects>();
		for (int i = media.size() - 1; i >= 0; i--) {
			if (media.get(i).select == POINTS.ALL_POINTS
					&& !exclude.contains(media.get(i))) {
				for (int j = i + 1; j < media.size(); j++) {
					if (media.get(i).overlaps(media.get(j))
							&& media.get(j).select == POINTS.NONE) {
						MediaObjects obj = media.remove(i);
						media.add(j, obj);
						exclude.add(obj);
						break;
					}
				}
			}
		}
		for (int i = media.size() - 1; i >= 0; i--){
			media.get(i).positionZ=i;
		}
		for (int i = 0; i < streamListener.size(); i++) {
			streamListener.get(i).zOrderChanged(media);
		}
		triggerChange();

	}

	public void moveSelDown() {
		Vector<MediaObjects> exclude = new Vector<MediaObjects>();
		for (int i = 0; i < media.size(); i++) {
			if (media.get(i).select == POINTS.ALL_POINTS
					&& !exclude.contains(media.get(i))) {
				for (int j = i - 1; j >= 0; j--) {
					if (media.get(i).overlaps(media.get(j))
							&& media.get(j).select == POINTS.NONE) {
						MediaObjects obj = media.remove(i);
						media.add(j, obj);
						exclude.add(obj);
						break;
					}
				}
			}
		}
		for (int i = media.size() - 1; i >= 0; i--){
			media.get(i).positionZ=i;
		}
		for (int i = 0; i < streamListener.size(); i++) {
			streamListener.get(i).zOrderChanged(media);
		}
		triggerChange();
	}

	public boolean showMediaPopupMenu(MouseEvent e) {
		MediaObjects object = selectedObjects();
		if (object == null) {
			return false;
		} else if (object instanceof LiveAudio) {
			menu.showPopupMenu(e, MediaPopupMenuState.NO_LIVE_AUDIO);
			return true;
		} else if (object instanceof LiveVideo) {
			if (((LiveVideo) object).getPreviewComponent() == null) {
				menu.showPopupMenu(e, MediaPopupMenuState.NO_LIVE_VIDEO);
			} else {
				menu.showPopupMenu(e, MediaPopupMenuState.LIVE_VIDEO_PLAYING);
			}
			return true;
		} else if (object instanceof PrerecordedVideo
				&& ((PrerecordedVideo) object).getState() == PrerecordedVideoState.NO_VIDEO) {
			menu.showPopupMenu(e, MediaPopupMenuState.NO_PRERECORDED_VIDEO);
			return true;
		} else if (object instanceof PrerecordedVideo
				&& ((PrerecordedVideo) object).getState() == PrerecordedVideoState.VIDEO_STOPPED) {
			menu.showPopupMenu(e,
				MediaPopupMenuState.PRERECORDED_VIDEO_STOPPED);
			return true;
		} else if (object instanceof PrerecordedVideo
				&& ((PrerecordedVideo) object).getState() == PrerecordedVideoState.VIDEO_PLAYING) {
			menu.showPopupMenu(e,
				MediaPopupMenuState.PRERECORDED_VIDEO_PLAYING);
			return true;
		} else if (object instanceof AudioFile
				&& ((AudioFile) object).getState() == PrerecordedAudioState.NO_FILE) {
			menu.showPopupMenu(e,
				MediaPopupMenuState.NO_PRERECORDED_AUDIO);
			return true;
		} else if (object instanceof AudioFile
				&& ((AudioFile) object).getState() == PrerecordedAudioState.AUDIO_STOPPED) {
			menu.showPopupMenu(e,
				MediaPopupMenuState.PRERECORDED_AUDIO_STOPED);
			return true;
		} else if (object instanceof AudioFile
				&& ((AudioFile) object).getState() == PrerecordedAudioState.AUDIO_PLAYING) {
			menu.showPopupMenu(e,
				MediaPopupMenuState.PRERECORDED_AUDIO_PLAYING);
			return true;
		}
		return false;
	}

	private MediaObjects selectedObjects() {
		for (int i = media.size() - 1; i >= 0; i--) {
			if (media.get(i).select == POINTS.ALL_POINTS) {
				return media.get(i);
			}
		}
		return null;
	}

	public void functionSelected(MediaObjects obj, MediaPopupMenuFunction func) {
		System.out.println("function selected " + func);
		if (func == MediaPopupMenuFunction.MOVE_TO_FRONT) {
			moveSelTop();
		} else 	if (func == MediaPopupMenuFunction.MOVE_TO_BACK) {
			moveSelBottom();
		} else if (func == MediaPopupMenuFunction.MOVE_BACKWARD) {
			moveSelDown();
		} else if (func == MediaPopupMenuFunction.MOVE_FORWARD) {
			moveSelUp();
		} else if (func == MediaPopupMenuFunction.ATTACH_MEDIA) {
			attachMedia();
		} else if (func == MediaPopupMenuFunction.ATTACH_VIDEO) {
			attachVideo();
		} else if (func == MediaPopupMenuFunction.PLAY_VIDEO) {
			playVideo();
		} else if (func == MediaPopupMenuFunction.STOP_VIDEO) {
			stopVideo();
		} else if (func == MediaPopupMenuFunction.SET_VIDEO_OFFSET) {
			setVideoOffset();
		} else if (func == MediaPopupMenuFunction.SET_TRANSPARENCY) {
			setTransparency();
		} else if (func == MediaPopupMenuFunction.REMOVE_CAMERA) {
			removeCamera();
		} else if (func == MediaPopupMenuFunction.ATTACH_AUDIO) {
			attachAudio();
		} else if (func == MediaPopupMenuFunction.PLAY_AUDIO) {
			playAudio();
		} else if (func == MediaPopupMenuFunction.STOP_AUDIO) {
			stopAudio();
		} else if (func == MediaPopupMenuFunction.SET_AUDIO_OFFSET) {
			setAudioOffset();
		}
		paint(g);
	}

	private void removeCamera() {
		for (int i = 0; i < media.size(); i++) {
			if (media.get(i).select == POINTS.ALL_POINTS
					&& media.get(i) instanceof LiveVideo) {
				((LiveVideo) media.get(i)).setVisible(false);
				dm.stopCamera(((LiveVideo) media.get(i)).getSource());
				((LiveVideo) media.get(i)).setPreviewComponentNull();
				((LiveVideo) media.get(i)).setVisible(true);
				((LiveVideo) media.get(i)).paint(g);
			}
		}
	}

	private void setAudioOffset() {
		TimeOffsetDialog tod = new TimeOffsetDialog(null);
		tod.setAlwaysOnTop(true);
		tod.setVisible(true);
		for (int i = 0; i < media.size(); i++) {
			if (media.get(i).select == POINTS.ALL_POINTS
					&& media.get(i) instanceof AudioFile) {
				((AudioFile) media.get(i)).setSeek(tod.getTimeOffset());
				changed=true;
				try {
					dm.playAudio(((AudioFile) media.get(i)).getSsrc(),
							((AudioFile) media.get(i)).getSeek());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	
	private void stopAudio() {
			for (int i = 0; i < media.size(); i++) {
				if (media.get(i).select == POINTS.ALL_POINTS
						&& media.get(i) instanceof AudioFile) {
					((AudioFile)media.get(i)).stop();
				}
			}
	}

	private void playAudio() {
		for (int i = 0; i < media.size(); i++) {
			if (media.get(i).select == POINTS.ALL_POINTS
					&& media.get(i) instanceof AudioFile) {
				try {
					((AudioFile)media.get(i)).start();
					dm.playAudio(((AudioFile) media.get(i)).getSsrc(),
							((AudioFile) media.get(i)).getSeek());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void attachAudio() {
		JFileChooser chooser = new JFileChooser();
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				for (int i = 0; i < media.size(); i++) {
					if (media.get(i).select == POINTS.ALL_POINTS
							&& media.get(i) instanceof AudioFile) {
						dm.removeMedia(i);
						String filename = chooser.getSelectedFile()
								.getCanonicalPath();
						chooser.setVisible(false);
						filename = filename.replaceAll("\\\\", "/");
						
						dm.startAudio(filename, sm.getRTPSocketAudio(), 1, i);
						media.get(i).setSource(filename);
//						((AudioFile)media.get(i)).startReceivingAudio(new MediaLocator("file:/"+filename));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		changed=true;
	}

	private void setTransparency() {
		TransparencyDialog transDialog = new TransparencyDialog(null);
		transDialog.setAlwaysOnTop(true);
		for (int i = 0; i < media.size(); i++) {
			if (media.get(i).select == POINTS.ALL_POINTS
					&& media.get(i) instanceof PrerecordedVideo) {
				transDialog.setTransparencyValue(((PrerecordedVideo) media.get(i)).getTransparencyValue());
				break;
			} else if (media.get(i).select == POINTS.ALL_POINTS
					&& media.get(i) instanceof LiveVideo) {
				transDialog.setTransparencyValue(((LiveVideo) media.get(i)).getTransparencyValue());
				break;
			}
		}
		
		transDialog.registerChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				for (int i = 0; i < media.size(); i++) {
					if (media.get(i).select == POINTS.ALL_POINTS
							&& media.get(i) instanceof PrerecordedVideo) {
						((PrerecordedVideo) media.get(i)).setTransparency(
								((JSlider) e.getSource()).getValue());
					} else if (media.get(i).select == POINTS.ALL_POINTS
							&& media.get(i) instanceof LiveVideo) {
						((LiveVideo) media.get(i)).setTransparency(
								((JSlider) e.getSource()).getValue());
					}
					changed=true;
				}

			}
		});
		transDialog.setVisible(true);
	}

	private void setVideoOffset() {
		TimeOffsetDialog tod = new TimeOffsetDialog(null);
		tod.setAlwaysOnTop(true);
		tod.setVisible(true);
		for (int i = 0; i < media.size(); i++) {
			if (media.get(i).select == POINTS.ALL_POINTS
					&& media.get(i) instanceof PrerecordedVideo) {
				((PrerecordedVideo) media.get(i)).setSeek(tod.getTimeOffset());
				changed=true;
			}
		}
	}

	private void stopVideo() {
		for (int i = 0; i < media.size(); i++) {
			if (media.get(i).select == POINTS.ALL_POINTS
					&& media.get(i) instanceof PrerecordedVideo) {
				try {
					dm.stopVideo(((PrerecordedVideo) media.get(i)).getSsrc());
					((PrerecordedVideo) media.get(i)).setState(PrerecordedVideoState.VIDEO_STOPPED);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void playVideo() {
		for (int i = 0; i < media.size(); i++) {
			if (media.get(i).select == POINTS.ALL_POINTS
					&& media.get(i) instanceof PrerecordedVideo) {
				try {
					dm.playVideo(((PrerecordedVideo) media.get(i)).getSsrc(),
							((PrerecordedVideo) media.get(i)).getSeek(),
							((PrerecordedVideo) media.get(i)).getVideoscale());
				} catch (IOException e) {
					e.printStackTrace();
				}
				((PrerecordedVideo) media.get(i)).setState(PrerecordedVideoState.VIDEO_PLAYING);
			}
		}
	}

	private void attachVideo() {
		JFileChooser chooser = new JFileChooser();
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				for (int i = 0; i < media.size(); i++) {
					if (media.get(i).select == POINTS.ALL_POINTS
							&& media.get(i) instanceof PrerecordedVideo) {
						dm.removeMedia(i);
						String filename = chooser.getSelectedFile()
								.getCanonicalPath();
						chooser.setVisible(false);
						filename = filename.replaceAll("\\\\", "/");

						dm.startVideo(filename, sm.getRTPSocket(), 1, 1.0, i);
						media.get(i).setSource(filename);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		changed=true;
	}
	
	public void attachAllMedia(){
		for (int i = media.size()-1; i >= 0; i--){
			System.out.println("attachMedia " +i);
			attachMedia(i);
		}
	}

	private void attachMedia(int pos) {
		System.out.println("attachMedia " + pos);
		dm.detectCaptureDevices();
		String mediaName = media.get(pos).getSource();
		
		if (media.get(pos) instanceof LiveVideo) {
			Vector<String> devicesAll = dm.getVideoDeviceList();
			//String[] devices = (dm.getVideoDeviceList()).toArray(new String[0]);
			devicesAll.addAll(sm.getRemoteVideoList());
			if (devicesAll.contains(mediaName)){
				System.out.println("Device to load " + mediaName);
				if (sm.getRemoteVideoList().contains(mediaName)) {
					try {
						dm.removeMedia(pos);
						DataSource ds = sm.getDataSource(mediaName);
						Format format = sm.getFormat(mediaName);
						dm.startRemoteDevice(mediaName, ds, format, pos);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
						dm.removeMedia(pos);
						dm.startDevice(mediaName, sm.getRTPSocket(), pos);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} else if (media.get(pos) instanceof LiveAudio) {
			Vector<String> audioDevices = dm.getAudioDeviceList();
			audioDevices.addAll(sm.getRemoteAudioList());
			if (!audioDevices.contains(mediaName)){
				return;
			}
			
			
			if (sm.getRemoteAudioList().contains(mediaName)) {
				try {
					dm.removeMedia(pos);
					DataSource ds = sm.getDataSource(mediaName);
					Format format = sm.getFormat(mediaName);
					dm.startRemoteDevice(mediaName, ds, format, pos);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					dm.removeMedia(pos);
					dm.startDevice(mediaName, sm.getRTPSocketAudio(), pos);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (media.get(pos) instanceof PrerecordedVideo) {
			System.out.println("PrerecordedVideo");
			try {
				dm.removeMedia(pos);
				dm.startVideo(mediaName, sm.getRTPSocket(), 1, 1.0, pos);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void attachMedia(MediaObjects mediaobject, String source) {
		if (source == null) {
				return;
		}
		if (mediaobject instanceof LiveVideo) {
			System.out.println("Device copied " + source);
			if (sm.getRemoteVideoList().contains(source)) {
				try {
					DataSource ds = sm.getDataSource(source);
					Format format = sm.getFormat(source);
					dm.startRemoteDevice(source, ds, format, media.size()-1);
					mediaobject.setSource(source);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					dm.startDevice(source, sm.getRTPSocket(), media.size()-1);
					mediaobject.setSource(source);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (mediaobject instanceof LiveAudio) {

			System.out.println("Device copied " + source);

			if (sm.getRemoteAudioList().contains(source)) {
				try {
					DataSource ds = sm.getDataSource(source);
					Format format = sm.getFormat(source);
					dm.startRemoteDevice(source, ds, format, media.size()-1);
					mediaobject.setSource(source);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					dm.startDevice(source, sm.getRTPSocketAudio(), media.size()-1);
					mediaobject.setSource(source);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		changed=true;		
	}
	
	private void attachMedia() {
		dm.detectCaptureDevices();

		if (selectedObjects() instanceof LiveVideo) {
			Vector<String> devicesAll = dm.getVideoDeviceList();
			//String[] devices = (dm.getVideoDeviceList()).toArray(new String[0]);
			devicesAll.addAll(sm.getRemoteVideoList());
			String [] devices = devicesAll.toArray(new String[0]);
			if (devices.length == 0) {
				return;
			}
			String selectedValue = (String) JOptionPane.showInputDialog(null,
					"Select the video device or stream", "Input",
					JOptionPane.INFORMATION_MESSAGE, null, devices, devices[0]);
			if (selectedValue == null) {
				return;
			}

			System.out.println("Device selected " + selectedValue);
			if (sm.getRemoteVideoList().contains(selectedValue)) {
				try {
					for (int i = 0; i < media.size(); i++) {
						if (media.get(i).select == POINTS.ALL_POINTS
								&& media.get(i) instanceof LiveVideo) {
							dm.removeMedia(i);
							DataSource ds = sm.getDataSource(selectedValue);
							Format format = sm.getFormat(selectedValue);
							dm.startRemoteDevice(selectedValue, ds, format, i);
							media.get(i).setSource(selectedValue);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					for (int i = 0; i < media.size(); i++) {
						if (media.get(i).select == POINTS.ALL_POINTS
								&& media.get(i) instanceof LiveVideo) {
							dm.removeMedia(i);
							dm.startDevice(selectedValue, sm.getRTPSocket(), i);
							media.get(i).setSource(selectedValue);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (selectedObjects() instanceof LiveAudio) {
			Vector<String> audioDevices = dm.getAudioDeviceList();
			audioDevices.addAll(sm.getRemoteAudioList());
			String[] devices = audioDevices.toArray(new String[0]);
			if (devices.length == 0) {
				return;
			}
			String selectedValue = (String) JOptionPane.showInputDialog(null,
					"Select the audio device or stream", "Input",
					JOptionPane.INFORMATION_MESSAGE, null, devices, devices[0]);

			if (selectedValue == null) {
				return;
			}

			System.out.println("Device selected " + selectedValue);

			if (sm.getRemoteAudioList().contains(selectedValue)) {
				try {
					for (int i = 0; i < media.size(); i++) {
						if (media.get(i).select == POINTS.ALL_POINTS
								&& media.get(i) instanceof LiveAudio) {
							dm.removeMedia(i);
							DataSource ds = sm.getDataSource(selectedValue);
							Format format = sm.getFormat(selectedValue);
							dm.startRemoteDevice(selectedValue, ds, format, i);
							media.get(i).setSource(selectedValue);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					for (int i = 0; i < media.size(); i++) {
						if (media.get(i).select == POINTS.ALL_POINTS
								&& media.get(i) instanceof LiveAudio) {
							dm.removeMedia(i);
							dm.startDevice(selectedValue, sm.getRTPSocketAudio(), i);
							media.get(i).setSource(selectedValue);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		changed=true;
	}

	public void stopTransmitting() {
		if (dm != null){
			dm.stopTransmitting();
		}
	}

	public void addStream(DataSource ds, Format format, int pos) {
		if (media.get(pos) instanceof LiveVideo) {
			((LiveVideo) media.get(pos)).createRenderer(new Effect[] {},
					format, ds, 0);
		} else if (media.get(pos) instanceof LiveAudio) {
			try {
				((LiveAudio) media.get(pos)).startReceivingAudio(ds);
			} catch (NoPlayerException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void addStream(MediaLocator locator, long ssrc, int pos) {
		if (media.get(pos) instanceof AudioFile) {
			try {
				((AudioFile) media.get(pos)).startReceivingAudio(locator, ssrc);
			} catch (NoPlayerException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	

	public void addVideo(CloneEffect cloneEffect, Format format, long ssrc,
			int pos) {
		if (media.get(pos) instanceof PrerecordedVideo) {
			((PrerecordedVideo) media.get(pos)).createRenderer(new Effect[] {},
					format, cloneEffect, 0, ssrc);
		}
	}

	public Component getPreviewComponents(int position) {
		return media.get(position).getPreviewComponent();
	}

	public void scale(double scaleToReal, double scaleToDesign) {
		MediaObjects.scale(scaleToReal, scaleToDesign);
	}
	
	public void scale(double scaleToPreview) {
		MediaObjects.scale(scaleToPreview);
	}

	public void scaleToPreview(double scaleToPreview) {
		MediaObjects.scaleToPreview = scaleToPreview;
	}
	
	public int getMediaObjectCount() {
		return media.size();
	}

	public void stopPlay() {
		for (int i = 0; i < media.size(); i++) {
			media.get(i).setVisible(false);
			if(media.get(i) instanceof AudioFile){
//				dm.stopAudio(i);
			}
		}
	}

	public void startPlay() {
		System.out.println("size of media " + media.size());
		for (int i = 0; i < media.size(); i++) {
			media.get(i).setVisible(true);
			if(media.get(i) instanceof AudioFile){
//				dm.startAudio(i);
			}
		}
	}

	public void stopFullScreen() {
		for (int i = 0; i < media.size(); i++) {
			media.get(i).stopRun();
		}
	}
	
	public void startFullScreen(GraphicsConfiguration gc) {
		Rectangle bounds = gc.getBounds();
		for (int i = 0; i < media.size(); i++) {
			media.get(i).run(gc, bounds);
		}
	}
	
	public void startFullScreenMedia(GraphicsConfiguration gc, int position){
		media.get(position).run(gc, gc.getBounds());
	}


	public Element save(Document xmldoc) {
		Element e = xmldoc.createElementNS(null, "SCENE");
		e.setAttributeNS(null, "LENGTH", String.valueOf(this.length));
		e.setAttributeNS(null, "NAME", this.sceneName);
		for (int i = 0; i < media.size(); i++) {
			Element child = media.get(i).save(xmldoc);
			if(child != null){
				e.appendChild(child);
			}
		}
		return e;
	}

	private void triggerChange() {
		ChangeEvent event = new ChangeEvent(this);
		for (int i=0; i<listeners.size(); i++) {
			listeners.get(i).stateChanged(event);
		}
	}
	
	public void addChangeListener(ChangeListener listener) {
		if(listeners.contains(listener) == false){
			listeners.add(listener);
		}
	}
	
	public void removeChangeListener(ChangeListener listener) {
		listeners.remove(listener);
	}

	public String getSceneName() {
		return this.sceneName;
	}

	public void setSceneName(String name) {
		if(name!=null){
			this.sceneName=name;
			this.triggerChange();
		}
		
	}
}
