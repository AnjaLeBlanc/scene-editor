package edance.userinterface;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import edance.sceeneditor.MediaObjects;
import edance.userinterface.event.MediaPopupMenuListener;

public class MediaPopupMenu extends JPopupMenu implements ActionListener {

	private static final long serialVersionUID = 1L;

	private final JMenuItem moveToFront = new JMenuItem("Move to front");
	private final JMenuItem moveToBack = new JMenuItem("Move to back");
	private final JMenuItem moveForward = new JMenuItem("Move forward");
	private final JMenuItem moveBackward = new JMenuItem("Move backward");
	private final JMenuItem attachMedia = new JMenuItem("Attach Media");
	private final JMenuItem attachVideo = new JMenuItem("Attach Video");
	private final JMenuItem playVideo = new JMenuItem("Play Video");
	private final JMenuItem stopVideo = new JMenuItem("Stop Video");
	private final JMenuItem setVideoOffset = new JMenuItem("Set Time Offset");
	private final JMenuItem setTransparency = new JMenuItem("Set Transparency");
	private final JMenuItem removeCamera = new JMenuItem("Remove Camera");
	private final JMenuItem attachAudioFile = new JMenuItem("Attach Audio");
	private final JMenuItem playAudio = new JMenuItem("Play Audio");
	private final JMenuItem stopAudio = new JMenuItem("Stop Audio");
	private final JMenuItem setAudioOffset = new JMenuItem("Set Audio Offset");

	private Vector<MediaPopupMenuListener> listeners = new Vector<MediaPopupMenuListener>();

	public enum MediaPopupMenuState {
		NO_LIVE_VIDEO,
		LIVE_VIDEO_PLAYING,
		NO_PRERECORDED_VIDEO,
		PRERECORDED_VIDEO_STOPPED,
		PRERECORDED_VIDEO_PLAYING,
		NO_LIVE_AUDIO,
		NO_PRERECORDED_AUDIO,
		PRERECORDED_AUDIO_PLAYING,
		PRERECORDED_AUDIO_STOPED
		
	}

	public enum MediaPopupMenuFunction {
		NONE,
		MOVE_TO_FRONT,
		MOVE_TO_BACK,
		MOVE_FORWARD,
		MOVE_BACKWARD,
		ATTACH_MEDIA,
		ATTACH_VIDEO,
		PLAY_VIDEO,
		STOP_VIDEO,
		SET_VIDEO_OFFSET,
		SET_TRANSPARENCY,
		REMOVE_CAMERA,
		ATTACH_AUDIO,
		PLAY_AUDIO,
		STOP_AUDIO,
		SET_AUDIO_OFFSET
	}

	private MediaObjects media;

	public MediaPopupMenu() {
		super();
		moveToFront.addActionListener(this);
		moveToBack.addActionListener(this);
		moveForward.addActionListener(this);
		moveBackward.addActionListener(this);
		attachMedia.addActionListener(this);
		attachVideo.addActionListener(this);
		playVideo.addActionListener(this);
		stopVideo.addActionListener(this);
		setVideoOffset.addActionListener(this);
		setTransparency.addActionListener(this);
		removeCamera.addActionListener(this);
		attachAudioFile.addActionListener(this);
		playAudio.addActionListener(this);
		stopAudio.addActionListener(this);
		setAudioOffset.addActionListener(this);
		this.add(moveToFront);
		this.add(moveToBack);
		this.add(moveForward);
		this.add(moveBackward);
		this.addSeparator();
	}

	public void addMenuPopupMenuListener(MediaPopupMenuListener listener) {
		listeners.add(listener);
	}

	public void removeMenuPopupMenuListener(MediaPopupMenuListener listener) {
		listeners.remove(listener);
	}

	public void actionPerformed(ActionEvent e) {
		MediaPopupMenuFunction func = MediaPopupMenuFunction.NONE;
		if (e.getActionCommand().compareTo(moveToFront.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.MOVE_TO_FRONT;
		} else if (e.getActionCommand().compareTo(moveToBack.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.MOVE_TO_BACK;
		} else if (e.getActionCommand().compareTo(moveBackward.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.MOVE_BACKWARD;
		} else if (e.getActionCommand().compareTo(moveForward.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.MOVE_FORWARD;
		} else if (e.getActionCommand().compareTo(attachMedia.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.ATTACH_MEDIA;
		} else if (e.getActionCommand().compareTo(attachVideo.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.ATTACH_VIDEO;
		} else if (e.getActionCommand().compareTo(playVideo.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.PLAY_VIDEO;
		} else if (e.getActionCommand().compareTo(stopVideo.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.STOP_VIDEO;
		} else if (e.getActionCommand().compareTo(setVideoOffset.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.SET_VIDEO_OFFSET;
		} else if (e.getActionCommand().compareTo(setTransparency.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.SET_TRANSPARENCY;
		} else if (e.getActionCommand().compareTo(removeCamera.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.REMOVE_CAMERA;
		} else if (e.getActionCommand().compareTo(attachAudioFile.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.ATTACH_AUDIO;
		} else if (e.getActionCommand().compareTo(playAudio.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.PLAY_AUDIO;
		} else if (e.getActionCommand().compareTo(stopAudio.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.STOP_AUDIO;
		} else if (e.getActionCommand().compareTo(setAudioOffset.getActionCommand()) == 0) {
			func = MediaPopupMenuFunction.SET_AUDIO_OFFSET;
		}
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).functionSelected(media, func);
		}
	}

	public void showPopupMenu(MouseEvent e, MediaPopupMenuState type) {
		System.out.println("show PopupMenu of type " + type);
		this.remove(attachMedia);
		this.remove(attachVideo);
		this.remove(playVideo);
		this.remove(stopVideo);
		this.remove(setVideoOffset);
		this.remove(attachAudioFile);
		this.remove(playAudio);
		this.remove(stopVideo);
		this.remove(setAudioOffset);
		this.remove(setTransparency);
		this.remove(removeCamera);
		if (type == MediaPopupMenuState.NO_LIVE_VIDEO) {
			this.add(attachMedia);
			this.show(e.getComponent(), e.getX(), e.getY());
		} else if (type == MediaPopupMenuState.LIVE_VIDEO_PLAYING) {
			this.add(setTransparency);
			this.add(removeCamera);
			this.add(attachMedia);
			this.show(e.getComponent(), e.getX(), e.getY());
		} else if (type == MediaPopupMenuState.NO_PRERECORDED_VIDEO) {
			this.add(attachVideo);
			this.show(e.getComponent(), e.getX(), e.getY());
		} else if (type == MediaPopupMenuState.PRERECORDED_VIDEO_STOPPED) {
			this.add(attachVideo);
			this.add(setVideoOffset);
			this.add(setTransparency);
			this.add(playVideo);
			this.show(e.getComponent(), e.getX(), e.getY());
		} else if (type == MediaPopupMenuState.PRERECORDED_VIDEO_PLAYING) {
			this.add(setVideoOffset);
			this.add(setTransparency);
			this.add(stopVideo);
			this.show(e.getComponent(), e.getX(), e.getY());
		} else if (type == MediaPopupMenuState.NO_LIVE_AUDIO) {
			this.add(attachMedia);
			this.show(e.getComponent(), e.getX(), e.getY());
		} else if (type == MediaPopupMenuState.NO_PRERECORDED_AUDIO) {
			this.add(attachAudioFile);
			this.show(e.getComponent(), e.getX(), e.getY());
		} else if (type == MediaPopupMenuState.PRERECORDED_AUDIO_STOPED) {
			this.add(attachAudioFile);
			this.add(setAudioOffset);
			this.add(playAudio);
			this.show(e.getComponent(), e.getX(), e.getY());
		} else if (type == MediaPopupMenuState.PRERECORDED_AUDIO_PLAYING) {
			this.add(setAudioOffset);
			this.add(stopAudio);
			this.show(e.getComponent(), e.getX(), e.getY());
		}
		
	}

}
