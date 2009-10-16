package edance.sceeneditor;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import edance.devicemanagement.StreamManagement;
import edance.sceeneditor.Location.ASPECT_RATIO;
import edance.userinterface.MainFrame;
import edance.userinterface.MetaDataDialog;
import edance.userinterface.UplaodSessionDialog;
import edance.userinterface.Toolbar.ToolbarButtons;
import edance.userinterface.event.SceneChangeListener;
import edance.userinterface.event.ToolbarListener;

public class EdanceFunctionality implements ToolbarListener, KeyListener, SceneChangeListener {

	private Project currentProject;

	private MainFrame mainPanel;
	
	private StreamManagement sm;

	private boolean running = false;

	public EdanceFunctionality(StreamManagement sm) {
		this.sm = sm;
		currentProject = new Project(sm);
		mainPanel.getDesignPanel().setScene(
				currentProject.getCurrLocation().getCurrentScene());
		mainPanel.getScenePanel().setCurrentScene(
				currentProject.getCurrLocation().getCurrentScene());
		mainPanel.getScenePanel().addSceneChangeListener(this);
	}

	public EdanceFunctionality(MainFrame mainPanel, StreamManagement sm) {
		this.sm = sm;
		this.mainPanel = mainPanel;
		this.mainPanel.getDesignPanel().addKeyListener(this);
		currentProject = new Project(sm);
		mainPanel.getDesignPanel().setScene(
				currentProject.getCurrLocation().getCurrentScene());
		mainPanel.getScenePanel().setCurrentScene(
				currentProject.getCurrLocation().getCurrentScene());
		mainPanel.getScenePanel().setProject(currentProject);
		mainPanel.getScenePanel().addSceneChangeListener(this);
	}

	public void toolbarButtonPressed(ToolbarButtons toolbarButton) {
		if (toolbarButton == ToolbarButtons.NEW_PROJECT) {
			newProject();
		} else if (toolbarButton == ToolbarButtons.OPEN_PROJECT) {
			openProject();
		} else if (toolbarButton == ToolbarButtons.SAVE_PROJECT) {
			saveProject();
		} else if (toolbarButton == ToolbarButtons.UPLOAD_PROJECT) {
			uploadProject();
		} else if (toolbarButton == ToolbarButtons.NEW_SCENE) {
			newScene(sm);
		} else if (toolbarButton == ToolbarButtons.DELETE_SCENE) {
			deleteScene();
		} else if (toolbarButton == ToolbarButtons.PRERECORDED_VIDEO) {
			prerecordedVideo();
		} else if (toolbarButton == ToolbarButtons.PRERECORDED_AUDIO) {
			prerecordedAudio();
		} else if (toolbarButton == ToolbarButtons.LIVE_VIDEO) {
			liveVideo();
		} else if (toolbarButton == ToolbarButtons.AUDIO) {
			liveAudio();
		} else if (toolbarButton == ToolbarButtons.PICTURE) {
			picture();
		} else if (toolbarButton == ToolbarButtons.TIMESHIFTED_LIVE_VIDEO) {
			timeshiftedVideo();
		} else if (toolbarButton == ToolbarButtons.ASPECT_RATIO_9TO5) {
			if (mainPanel != null) {
				mainPanel.getDesignPanel().setAspcetRatio(ASPECT_RATIO.AR9TO5);
				mainPanel.getToolbar().toggleButton(toolbarButton);
				currentProject.getCurrLocation().setAspectRatio(ASPECT_RATIO.AR9TO5);
			}
		} else if (toolbarButton == ToolbarButtons.ASPECT_RATIO_4TO3) {
			if (mainPanel != null) {
				mainPanel.getDesignPanel().setAspcetRatio(ASPECT_RATIO.AR4TO3);
				mainPanel.getToolbar().toggleButton(toolbarButton);
				currentProject.getCurrLocation().setAspectRatio(ASPECT_RATIO.AR4TO3);
			}
		} else if (toolbarButton == ToolbarButtons.PLAY) {
			if (mainPanel != null) {
				run();
				mainPanel.getToolbar().toggleButton(toolbarButton);
				mainPanel.getDesignPanel().requestFocus();
			}
		} else if (toolbarButton == ToolbarButtons.STOP_PLAY) {
			if (mainPanel != null) {
				mainPanel.getDesignPanel().stopCurrScene();
				mainPanel.getToolbar().toggleButton(toolbarButton);
				this.running = false;
			}
		} else if (toolbarButton == ToolbarButtons.RECORD) {
			if (mainPanel != null) {
				File savefile = currentProject.getSaveDir(false);
				sm.startRecording(savefile);
				mainPanel.getToolbar().toggleButton(toolbarButton);
			}
		} else if (toolbarButton == ToolbarButtons.STOP_RECORD) {
			if (mainPanel != null) {
				sm.stopRecording();
				mainPanel.getToolbar().toggleButton(toolbarButton);
			}
		}
		
		
	}

	private void deleteScene() {
		if(running){
			mainPanel.getDesignPanel().stopCurrScene();
		}
		currentProject.getCurrLocation().deleteCurrentScene();

		mainPanel.getDesignPanel().setScene(
				currentProject.getCurrLocation().getCurrentScene());
		mainPanel.getScenePanel().setCurrentScene(
				currentProject.getCurrLocation().getCurrentScene());
		if(running){
			run();
		}
		mainPanel.getDesignPanel().repaint();
		mainPanel.getDesignPanel().requestFocus();
	}

	private void uploadProject() {
		File savedir = currentProject.getSaveDir(true);
		if(savedir == null) {
			return;
		}
		MetaDataDialog mdd = new MetaDataDialog(mainPanel, savedir.toString());
		int x = this.mainPanel.getLocation().x+this.mainPanel.getSize().width/2-mdd.getSize().width/2;
		int y = this.mainPanel.getLocation().y+this.mainPanel.getSize().height/2-mdd.getSize().height/2;
		mdd.setLocation(x, y);
		mdd.setVisible(true);
		boolean rv = mdd.getReturn();
		if (rv == true) {
			UplaodSessionDialog usd = new UplaodSessionDialog(savedir.toString());
			usd.setVisible(true);
		}
	}

	private void timeshiftedVideo() {
		// TODO Auto-generated method stub

	}

	private void picture() {
		// TODO Auto-generated method stub

	}

	private void liveAudio() {
		// TODO Auto-generated method stub

	}

	private void liveVideo() {
		// TODO Auto-generated method stub

	}

	private void prerecordedAudio() {
		// TODO Auto-generated method stub

	}

	private void prerecordedVideo() {
		// TODO Auto-generated method stub

	}

	private void newScene(StreamManagement sm) {
		Scene s = currentProject.createNewLocalScene(sm);
		mainPanel.getDesignPanel().setScene(s);
		mainPanel.getDesignPanel().paint(
				mainPanel.getDesignPanel().getGraphics());
		mainPanel.getScenePanel().setCurrentScene(s);
	}

	private void saveProject() {
		try {
			currentProject.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void openProject() {
		if (currentProject.requireSave()){
			int retValue = JOptionPane.showConfirmDialog(null, "Do you like to save current project?", "save", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if(retValue == JOptionPane.YES_OPTION){
				saveProject();
			}
		} 
		if(running){
			mainPanel.getDesignPanel().stopCurrScene();
		}
		currentProject.removeAllScenes();
		currentProject.load();
		System.out.println("finished load");
		currentProject.attachStreams(mainPanel.getDesignPanel());
		System.out.println("finished attachStreams");
		mainPanel.getDesignPanel().setScene(currentProject.getCurrLocation().getCurrentScene());
		mainPanel.getDesignPanel().paint(mainPanel.getDesignPanel().getGraphics());
		mainPanel.getScenePanel().setProject(currentProject);
		mainPanel.getScenePanel().setCurrentScene(
				currentProject.getCurrLocation().getCurrentScene());
		System.out.println("finished set scene");
		if(running){
			run();
		}
	}

	private void newProject() {
		System.out.println("new Project button pressed");
		// TODO Auto-generated method stub

	}

	public void stopTransmitting() {
		currentProject.stopTransmitting();
	}

	public void keyPressed(KeyEvent e) {
		System.out.println("running ? " + running);
		if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
			if(running){
				mainPanel.getDesignPanel().stopCurrScene();
			}
			currentProject.showNextScene();
			mainPanel.getDesignPanel().repaint();
			mainPanel.getDesignPanel().setScene(
					currentProject.getCurrLocation().getCurrentScene());
			mainPanel.getScenePanel().setCurrentScene(
					currentProject.getCurrLocation().getCurrentScene());
			if(running){
				run();
			}
			mainPanel.getDesignPanel().requestFocus();
		} else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
			
			if(running){
				mainPanel.getDesignPanel().stopCurrScene();
			}
			currentProject.showPreviousScene();
			mainPanel.getDesignPanel().repaint();
			mainPanel.getDesignPanel().setScene(
					currentProject.getCurrLocation().getCurrentScene());
			mainPanel.getScenePanel().setCurrentScene(
					currentProject.getCurrLocation().getCurrentScene());
			if(running){
				run();
			}
			mainPanel.getDesignPanel().requestFocus();
		}

	}

	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	private void run(){
		if (mainPanel != null) {
			GraphicsDevice thisone = mainPanel.getGraphicsConfiguration().getDevice();
			GraphicsEnvironment ge = GraphicsEnvironment.
			   getLocalGraphicsEnvironment();
			GraphicsDevice[] gs = ge.getScreenDevices();
			for (int j = 0; j < gs.length; j++) { 
			   if(thisone.equals(gs[j])){
				   System.out.println("the current monitor - don't take it");
			   } else {
			      mainPanel.getDesignPanel().playCurrScene(
			    		  gs[j].getDefaultConfiguration());
	
			      break;
			   }
		   }
			this.running=true;
		}
	}

	@Override
	public void currentSceneChanged(Location location, Scene scene) {
		if(currentProject.getCurrLocation() != location) {
			currentProject.setCurrLocation(location);
		}
		currentProject.getCurrLocation().getCurrentScene().stopPlay();
		if(running){
			mainPanel.getDesignPanel().stopCurrScene();
		}
		currentProject.getCurrLocation().setCurrentScene(scene);
		mainPanel.getDesignPanel().setScene(scene);
		mainPanel.getScenePanel().setCurrentScene(scene);
		currentProject.getCurrLocation().getCurrentScene().startPlay();
		if(running){
			run();
		}
		mainPanel.getDesignPanel().repaint();
		mainPanel.getDesignPanel().requestFocus();
	}

	@Override
	public void moveScene(Location location, Scene scene, int position) {
		location.moveScene(scene, position);
	}
	
}
