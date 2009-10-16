package edance.userinterface;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//import java.net.URL;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import common.JarExtractor;

import edance.userinterface.event.ToggleButton;
import edance.userinterface.event.ToolbarListener;

public class Toolbar extends JToolBar implements ActionListener, ToggleButton {

	private static final long serialVersionUID = 1L;
	
	public enum ToolbarButtons {
		OTHER,
		NEW_PROJECT, 
		OPEN_PROJECT, 
		SAVE_PROJECT, 
		UPLOAD_PROJECT,
		NEW_SCENE,
		DELETE_SCENE,
		UNDO,
		REDO,
		PRERECORDED_VIDEO,
		LIVE_VIDEO,
		TIMESHIFTED_LIVE_VIDEO,
		PICTURE,
		AUDIO,
		PRERECORDED_AUDIO,
		MASK_ON,
		MASK_OFF,
		RECTANGULAR_MASK,
		CIRCULAR_MASK,
		FREEFORM_MASK,
		ASPECT_RATIO_4TO3,
		ASPECT_RATIO_9TO5,
		PLAY,
		STOP_PLAY,
		TIMED_PLAY,
		STOP_TIMED_PLAY,
		RECORD,
		STOP_RECORD
	};
	
	//EventListener
	private Vector<ToolbarListener> toolbarListeners = new Vector<ToolbarListener>();
	
	//Buttons of the scene toolbar
	private static String NEW="New Project";
	private static String newString= "images/page_white.png";	
//	private static URL newURL=Toolbar.class.getResource("../../images/page_white.png");
	private static String OPEN="Open Project";
	private static String openString= "images/folder_page_white.png";
//	private static URL openURL=Toolbar.class.getResource("../../images/folder_page_white.png");
	private static String SAVE="Save Project";
	private static String saveString= "images/disk.png";
	private static String UPLOAD="Upload Project";
	private static String uploadString= "images/db_comit.png";
//	private static URL saveURL=Toolbar.class.getResource("../../images/disk.png");
	private static String NEXT="New Scene";
	private static String nextURLString= "images/action_add.png";
//	private static URL nextURL=Toolbar.class.getResource("../../images/action_add.png");
	private static String DELETE="Delete Scene";
	private static String deleteString= "images/action_remove.png";
//	private static URL deleteURL=Toolbar.class.getResource("../../images/action_remove.png");
	private static String UNDO="Undo Change";
	private static String undoString= "images/arrow_left.png";
//	private static URL undoURL=Toolbar.class.getResource("../../images/arrow_left.png");
	private static String REDO="Redo Change";
	private static String redoString= "images/arrow_right.png";
//	private static URL redoURL=Toolbar.class.getResource("../../images/arrow_right.png");
	
	private JButton newScene;
	private JButton openScene;
	private JButton saveScene;
	private JButton uploadScene;
	private JButton nextScene;
	private JButton deleteScene;
	private JButton undoChange;
	private JButton redoChange;
	
	//Buttons of the media toolbar
	private static String AUDIO="Live Audio";
	private static String audioString= "images/microphone16x16.png";
//	private static URL audioURL=Toolbar.class.getResource("../../images/microphone16x16.png");
	private static String PRERECORDED_AUDIO="Prerecorded Audio";
	private static String prerecordedAudioString= "images/musiccd.png";
//	private static URL prerecordedaudioURL=Toolbar.class.getResource("../../images/musiccd.png");
	private static String PRERECORDED="Prerecorded Local Video";
	private static String prerecordedString= "images/tape30x16.png";
//	private static URL prerecordedURL=Toolbar.class.getResource("../../images/tape30x16.png");
	private static String LIVE="Live Video";
	private static String liveString= "images/camcorder20x16.png";
//	private static URL liveURL=Toolbar.class.getResource("../../images/camcorder20x16.png");
	private static String TIMESHIFTED="Replay Live Video Timeshifted";
	private static String timeshiftedString= "images/camcorderreplay20x16.png";
//	private static URL remoteURL=Toolbar.class.getResource("../../images/camcorderreplay20x16.png");
	private static String PICTURE="Picture";
	private static String pictureString= "images/photoA17x16.png";
//	private static URL pictureURL=Toolbar.class.getResource("../../images/photoA17x16.png");
	
	private JButton audio;
	private JButton prerecordedAudio;
	private JButton prerecorded;
	private JButton live;
	private JButton remote;
	private JButton picture;
	
	//Buttons for the masks toolbar
	private static String MASK="Mask";
	private static String maskString = "images/mask30x16.png";
//	private static URL maskURL=Toolbar.class.getResource("../../images/mask30x16.png");
	private static String SQUARE="Rectangular Mask";
	private static String squareString = "images/square.png";
//	private static URL squareURL=Toolbar.class.getResource("../../images/square.png");
	private static String CIRCLE="Circular Mask";
	private static String circleString = "images/circle16.png";
//	private static URL circleURL=Toolbar.class.getResource("../../images/circle16.png");
	private static String FREEFORM="Freeform Mask";
	private static String freeformString = "images/star.png";
//	private static URL freeformURL=Toolbar.class.getResource("../../images/star.png");

	private JButton mask;
	private JButton square;
	private JButton circle;
	private JButton freeform;
	
	//Buttons for the operations toolbar
	private static String RATIO="Screen Aspect Ratio 16:9";
	private static String RATIO2="Screen Aspect Ratio 4:3";
	private static String ratioString= "images/aspectratio28x16B.png";
//	private static URL ratioURL=Toolbar.class.getResource("../../images/aspectratio28x16B.png");
	private static String PLAY="Run Scene";
	private static String playString= "images/resultset_next.png";
//	private static URL playURL=Toolbar.class.getResource("../../images/resultset_next.png");
	private static String STOP_PLAY="Stop Running the Scene";
	private static String stopPlayString= "images/stop16x16.png";
//	private static URL stopPlayURL=Toolbar.class.getResource("../../images/stop16x16.png");
	private static String PLAYTIMED="Run Scene Timed";
	private static String playTimedString= "images/timeplay.png";
//	private static URL playtimedURL=Toolbar.class.getResource("../../images/timeplay.png");
	private static String STOP_PLAYTIMED="Stop Running the (timed) Scene";
	private static String stopPlayTimedString= "images/stoptimeplay.png";
//	private static URL stopPlaytimedURL=Toolbar.class.getResource("../../images/stoptimeplay.png");
	private static String REC="Record Run";
	private static String recString= "images/rec16x16.png";
//	private static URL recURL=Toolbar.class.getResource("../../images/rec16x16.png");
	private static String STOPREC="Stop Recording";
	private static String stopRecString= "images/stoprec16x16.png";
//	private static URL stoprecURL=Toolbar.class.getResource("../../images/stoprec16x16.png");
	
	private JButton ratio;
	private JButton play;
	private JButton playtimed;
	private JButton rec;
	
//	private JarExtractor je = new JarExtractor(Toolbar.class.getProtectionDomain().getCodeSource().getLocation().getFile());
	private JarExtractor je = new JarExtractor("edance.jar");
	
	public Toolbar (){
		super();
		this.setFloatable(false);
		this.add(scene());
		this.add(media());
		this.add(masks());
		this.add(operation());
	}
	
	private JToolBar scene(){
		JToolBar scene= new JToolBar();
		newScene= new JButton();
		if (je.hasImageData(newString)) {                      //image found
			newScene.setIcon(new ImageIcon(je.getImageData(newString), NEW));
			newScene.setToolTipText(NEW);
			newScene.setActionCommand(NEW);
	    } else {                                     //no image found
	    	newScene.setText(NEW);
	        System.err.println("Resource not found!");
	    }
		newScene.addActionListener(this);
		scene.add(newScene);
		
		openScene= new JButton();
		if (je.hasImageData(openString)) {                      //image found
			openScene.setIcon(new ImageIcon(je.getImageData(openString), OPEN));
			openScene.setToolTipText(OPEN);
			openScene.setActionCommand(OPEN);
	    } else {                                     //no image found
	    	openScene.setText(OPEN);
	        System.err.println("Resource not found!");
	    }
		openScene.addActionListener(this);
		scene.add(openScene);
		
		saveScene= new JButton();
		if (je.hasImageData(saveString)) {                      //image found
			saveScene.setIcon(new ImageIcon(je.getImageData(saveString), SAVE));
			saveScene.setToolTipText(SAVE);
			saveScene.setActionCommand(SAVE);
	    } else {                                     //no image found
	    	saveScene.setText(SAVE);
	        System.err.println("Resource not found!");
	    }
		saveScene.addActionListener(this);
		scene.add(saveScene);
		
		uploadScene= new JButton();
		if (je.hasImageData(uploadString)) {                      //image found
			uploadScene.setIcon(new ImageIcon(je.getImageData(uploadString), UPLOAD));
			uploadScene.setToolTipText(UPLOAD);
			uploadScene.setActionCommand(UPLOAD);
	    } else {                                     //no image found
	    	uploadScene.setText(UPLOAD);
	        System.err.println("Resource not found!");
	    }
		uploadScene.addActionListener(this);
		scene.add(uploadScene);
		
		scene.addSeparator();
		
		nextScene= new JButton();
		if (je.hasImageData(nextURLString)) {                      //image found
			nextScene.setIcon(new ImageIcon(je.getImageData(nextURLString), NEXT));
			nextScene.setToolTipText(NEXT);
			nextScene.setActionCommand(NEXT);
	    } else {                                     //no image found
	    	nextScene.setText(NEXT);
	        System.err.println("Resource not found!");
	    }
		nextScene.addActionListener(this);
		scene.add(nextScene);
		
		deleteScene= new JButton();
		if (je.hasImageData(deleteString)) {                      //image found
			deleteScene.setIcon(new ImageIcon(je.getImageData(deleteString), DELETE));
			deleteScene.setToolTipText(DELETE);
			deleteScene.setActionCommand(DELETE);
	    } else {                                     //no image found
	    	deleteScene.setText(DELETE);
	        System.err.println("Resource not found!");
	    }
		deleteScene.addActionListener(this);
		scene.add(deleteScene);
		
		scene.addSeparator();
		
		undoChange= new JButton();
		if (je.hasImageData(undoString)) {                      //image found
			undoChange.setIcon(new ImageIcon(je.getImageData(undoString), UNDO));
			undoChange.setToolTipText(UNDO);
			undoChange.setActionCommand(UNDO);
	    } else {                                     //no image found
	    	undoChange.setText(UNDO);
	        System.err.println("Resource not found!");
	    }
		undoChange.addActionListener(this);
		scene.add(undoChange);
		
		redoChange= new JButton();
		if (je.hasImageData(redoString)) {                      //image found
			redoChange.setIcon(new ImageIcon(je.getImageData(redoString), REDO));
			redoChange.setToolTipText(REDO);
			redoChange.setActionCommand(REDO);
	    } else {                                     //no image found
	    	redoChange.setText(REDO);
	        System.err.println("Resource not found!");
	    }
		redoChange.addActionListener(this);
		scene.add(redoChange);

		return scene;
	}
	
	private JToolBar media(){
		JToolBar media = new JToolBar();
		
		prerecorded= new JButton();
		if (je.hasImageData(prerecordedString)) {                      //image found
			prerecorded.setIcon(new ImageIcon(je.getImageData(prerecordedString), PRERECORDED));
			prerecorded.setToolTipText(PRERECORDED);
			prerecorded.setActionCommand(PRERECORDED);
	    } else {                                     //no image found
	    	prerecorded.setText(PRERECORDED);
	        System.err.println("Resource not found!");
	    }
		prerecorded.addActionListener(this);
		media.add(prerecorded);
		
		live= new JButton();
		if (je.hasImageData(liveString)) {                      //image found
			live.setIcon(new ImageIcon(je.getImageData(liveString), LIVE));
			live.setToolTipText(LIVE);
			live.setActionCommand(LIVE);
	    } else {                                     //no image found
	    	live.setText(LIVE);
	        System.err.println("Resource not found!");
	    }
		live.addActionListener(this);
		media.add(live);
		
		remote= new JButton();
		if (je.hasImageData(timeshiftedString)) {                      //image found
			remote.setIcon(new ImageIcon(je.getImageData(timeshiftedString), TIMESHIFTED));
			remote.setToolTipText(TIMESHIFTED);
			remote.setActionCommand(TIMESHIFTED);
	    } else {                                     //no image found
	    	remote.setText(TIMESHIFTED);
	        System.err.println("Resource not found!");
	    }
		remote.addActionListener(this);
		media.add(remote);
		
		picture= new JButton();
		if (je.hasImageData(pictureString)) {                      //image found
			picture.setIcon(new ImageIcon(je.getImageData(pictureString), PICTURE));
			picture.setToolTipText(PICTURE);
			picture.setActionCommand(PICTURE);
	    } else {                                     //no image found
	    	picture.setText(PICTURE);
	        System.err.println("Resource not found!");
	    }
		picture.addActionListener(this);
		media.add(picture);
		
		media.addSeparator();
		
		audio= new JButton();
		if (je.hasImageData(audioString)) {                      //image found
			audio.setIcon(new ImageIcon(je.getImageData(audioString), AUDIO));
			audio.setToolTipText(AUDIO);
			audio.setActionCommand(AUDIO);
	    } else {                                     //no image found
	    	audio.setText(AUDIO);
	        System.err.println("Resource not found!");
	    }
		audio.addActionListener(this);
		media.add(audio);
		
		prerecordedAudio= new JButton();
		if (je.hasImageData(prerecordedAudioString)) {                      //image found
			prerecordedAudio.setIcon(new ImageIcon(je.getImageData(prerecordedAudioString), 
					PRERECORDED_AUDIO));
			prerecordedAudio.setToolTipText(PRERECORDED_AUDIO);
			prerecordedAudio.setActionCommand(PRERECORDED_AUDIO);
	    } else {                                     //no image found
	    	prerecordedAudio.setText(PRERECORDED_AUDIO);
	        System.err.println("Resource not found!");
	    }
		prerecordedAudio.addActionListener(this);
		media.add(prerecordedAudio);
		
		return media;
	}

	private JToolBar masks(){
		JToolBar masks= new JToolBar();
		
		mask= new JButton();
		if (je.hasImageData(maskString)) {                      //image found
			mask.setIcon(new ImageIcon(je.getImageData(maskString), MASK));
			mask.setToolTipText(MASK);
			mask.setActionCommand(MASK);
	    } else {                                     //no image found
	    	mask.setText(MASK);
	        System.err.println("Resource not found!");
	    }
		mask.addActionListener(this);
		masks.add(mask);
		
		square= new JButton();
		if (je.hasImageData(squareString)) {                      //image found
			square.setIcon(new ImageIcon(je.getImageData(squareString), SQUARE));
			square.setToolTipText(SQUARE);
			square.setActionCommand(SQUARE);
	    } else {                                     //no image found
	    	square.setText(SQUARE);
	        System.err.println("Resource not found!");
	    }
		square.addActionListener(this);
		masks.add(square);
		
		circle= new JButton();
		if (je.hasImageData(circleString)) {                      //image found
			circle.setIcon(new ImageIcon(je.getImageData(circleString), CIRCLE));
			circle.setToolTipText(CIRCLE);
			circle.setActionCommand(CIRCLE);
	    } else {                                     //no image found
	    	circle.setText(CIRCLE);
	        System.err.println("Resource not found!");
	    }
		circle.addActionListener(this);
		masks.add(circle);

		freeform= new JButton();
		if (je.hasImageData(freeformString)) {                      //image found
			freeform.setIcon(new ImageIcon(je.getImageData(freeformString), FREEFORM));
			freeform.setToolTipText(FREEFORM);
			freeform.setActionCommand(FREEFORM);
	    } else {                                     //no image found
	    	freeform.setText(FREEFORM);
	        System.err.println("Resource not found!");
	    }
		freeform.addActionListener(this);
		masks.add(freeform);
		
		return masks;
	}

	private JToolBar operation(){
		JToolBar operation = new JToolBar();
		ratio= new JButton();
		
		if (je.hasImageData(ratioString)) {                      //image found
			ratio.setIcon(new ImageIcon(je.getImageData(ratioString), RATIO));
			ratio.setToolTipText(RATIO);
			ratio.setActionCommand(RATIO);
	    } else {                                     //no image found
	    	ratio.setText(RATIO);
	        System.err.println("Resource not found!");
	    }
		ratio.addActionListener(this);
		operation.add(ratio);
		
		play= new JButton();
		if (je.hasImageData(playString)) {                      //image found
			play.setIcon(new ImageIcon(je.getImageData(playString), PLAY));
			play.setToolTipText(PLAY);
			play.setActionCommand(PLAY);
	    } else {                                     //no image found
	    	play.setText(PLAY);
	        System.err.println("Resource not found!");
	    }
		play.addActionListener(this);
		operation.add(play);
		
		playtimed= new JButton();
		if (je.hasImageData(playTimedString)) {                      //image found
			playtimed.setIcon(new ImageIcon(je.getImageData(playTimedString), PLAYTIMED));
			playtimed.setToolTipText(PLAYTIMED);
			playtimed.setActionCommand(PLAYTIMED);
	    } else {                                     //no image found
	    	playtimed.setText(PLAYTIMED);
	        System.err.println("Resource not found!");
	    }
		playtimed.addActionListener(this);
		operation.add(playtimed);
		
		rec= new JButton();
		if (je.hasImageData(recString)) {                      //image found
			rec.setIcon(new ImageIcon(je.getImageData(recString), REC));
			rec.setToolTipText(REC);
			rec.setActionCommand(REC);
	    } else {                                     //no image found
	    	rec.setText(REC);
	        System.err.println("Resource not found!");
	    }
		rec.addActionListener(this);
		operation.add(rec);
		return operation;
	}
	
	/**
	 * register listener to receive events of pressed buttons;
	 * @param toolbarListener the listener to receive events
	 */
	public void addToolbarListener(ToolbarListener toolbarListener) {
		toolbarListeners.add(toolbarListener);
	}
	
	/**
	 * remove listener to stop receiveing events
	 * @param toolbarListener the listener to be removed
	 */
	public void removeToolbarListener(ToolbarListener toolbarListener) {
		toolbarListeners.remove(toolbarListener);
	}
	
	public void actionPerformed(ActionEvent event) {
		ToolbarButtons tb=ToolbarButtons.OTHER;
		if(((JButton)event.getSource()).getActionCommand().compareTo(NEW)==0){
			tb=ToolbarButtons.NEW_PROJECT;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(OPEN)==0){
			tb=ToolbarButtons.OPEN_PROJECT;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(SAVE)==0){
			tb=ToolbarButtons.SAVE_PROJECT;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(UPLOAD)==0){
			tb=ToolbarButtons.UPLOAD_PROJECT;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(NEXT)==0){
			tb=ToolbarButtons.NEW_SCENE;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(DELETE)==0){
			tb=ToolbarButtons.DELETE_SCENE;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(UNDO)==0){
			tb=ToolbarButtons.UNDO;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(REDO)==0){
			tb=ToolbarButtons.REDO;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(AUDIO)==0){
			tb=ToolbarButtons.AUDIO;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(PRERECORDED_AUDIO)==0){
			tb=ToolbarButtons.PRERECORDED_AUDIO;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(PRERECORDED)==0){
			tb=ToolbarButtons.PRERECORDED_VIDEO;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(LIVE)==0){
			tb=ToolbarButtons.LIVE_VIDEO;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(TIMESHIFTED)==0){
			tb=ToolbarButtons.TIMESHIFTED_LIVE_VIDEO;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(PICTURE)==0){
			tb=ToolbarButtons.PICTURE;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(MASK)==0){
			tb=ToolbarButtons.MASK_ON;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(SQUARE)==0){
			tb=ToolbarButtons.RECTANGULAR_MASK;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(CIRCLE)==0){
			tb=ToolbarButtons.CIRCULAR_MASK;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(FREEFORM)==0){
			tb=ToolbarButtons.FREEFORM_MASK;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(RATIO2)==0){
			tb=ToolbarButtons.ASPECT_RATIO_4TO3;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(RATIO)==0){
			tb=ToolbarButtons.ASPECT_RATIO_9TO5;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(STOP_PLAY)==0){
			tb=ToolbarButtons.STOP_PLAY;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(PLAY)==0){
			tb=ToolbarButtons.PLAY;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(PLAYTIMED)==0){
			tb=ToolbarButtons.TIMED_PLAY;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(STOP_PLAYTIMED)==0){
			tb=ToolbarButtons.STOP_TIMED_PLAY;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(STOPREC)==0){
			tb=ToolbarButtons.STOP_RECORD;
		}
		else if(((JButton)event.getSource()).getActionCommand().compareTo(REC)==0){
			tb=ToolbarButtons.RECORD;
		}
		for(int i=0; i<toolbarListeners.size();i++) {
			toolbarListeners.get(i).toolbarButtonPressed(tb);
		}
//		toggleButton(tb);
		if(tb==ToolbarButtons.OTHER) {
			System.err.println("Button " + ((JButton)event.getSource()).getActionCommand() +
					"not in ToolbarButtons enumeration!!!");
		}
		
	}

	public void toggleButton(ToolbarButtons toolbarButton) {
		System.out.println("toggle called" + toolbarButton.name());
		if (toolbarButton==ToolbarButtons.PLAY) {
			play.setIcon(new ImageIcon(je.getImageData(stopPlayString), STOP_PLAY));
			play.setToolTipText(STOP_PLAY);
			play.setActionCommand(STOP_PLAY);
		}
		else if (toolbarButton==ToolbarButtons.STOP_PLAY) {
			play.setIcon(new ImageIcon(je.getImageData(playString), PLAY));
			play.setToolTipText(PLAY);
			play.setActionCommand(PLAY);
		}
		else if (toolbarButton==ToolbarButtons.TIMED_PLAY) {
			playtimed.setIcon(new ImageIcon(je.getImageData(stopPlayTimedString), STOP_PLAYTIMED));
			playtimed.setToolTipText(STOP_PLAYTIMED);
			playtimed.setActionCommand(STOP_PLAYTIMED);
		}
		else if (toolbarButton==ToolbarButtons.STOP_TIMED_PLAY) {
			playtimed.setIcon(new ImageIcon(je.getImageData(playTimedString), PLAYTIMED));
			playtimed.setToolTipText(PLAYTIMED);
			playtimed.setActionCommand(PLAYTIMED);
		}
		else if (toolbarButton==ToolbarButtons.RECORD) {
			rec.setIcon(new ImageIcon(je.getImageData(stopRecString), STOPREC));
			rec.setToolTipText(STOPREC);
			rec.setActionCommand(STOPREC);
		}
		else if (toolbarButton==ToolbarButtons.STOP_RECORD) {
			rec.setIcon(new ImageIcon(je.getImageData(recString), REC));
			rec.setToolTipText(REC);
			rec.setActionCommand(REC);
		}
		else if (toolbarButton==ToolbarButtons.ASPECT_RATIO_4TO3) {
			ratio.setIcon(new ImageIcon(je.getImageData(ratioString), RATIO));
			ratio.setToolTipText(RATIO);
			ratio.setActionCommand(RATIO);
		}
		else if (toolbarButton==ToolbarButtons.ASPECT_RATIO_9TO5) {
			ratio.setIcon(new ImageIcon(je.getImageData(ratioString), RATIO2));
			ratio.setToolTipText(RATIO2);
			ratio.setActionCommand(RATIO2);
		}
	}
}
