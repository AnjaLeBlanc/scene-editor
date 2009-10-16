package edance.userinterface;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edance.sceeneditor.Location;
import edance.sceeneditor.Project;
import edance.sceeneditor.Scene;
import edance.userinterface.ScenePreviewPanel.INSERT;
import edance.userinterface.event.SceneChangeListener;
import edance.userinterface.event.ScenePopupMenuListener;

public class ScenePanel extends JPanel implements ChangeListener, MouseListener, ScenePopupMenuListener {
	
	private static final long serialVersionUID = 1L;
	private Component parent = null;
	private Vector<ScenePreviewPanel> sceneList = new Vector<ScenePreviewPanel>();
	private Vector<SceneChangeListener> sceneChangeListeners = new Vector<SceneChangeListener>();
	private ScenePopupMenu menu = new ScenePopupMenu();
	private Scene moveScene = null;
	private Scene currentScene = null;
	private ScenePreviewPanel moveTo = null;
	
	private Project currentProject;
	
	public ScenePanel(){
		this.setLayout(new SpringLayout());
		menu.addScenePopupMenuListener(this);
	}

	public void setProject(Project currentProject) {
		this.currentProject = currentProject;
		this.rebuildUI();
		if(parent!=null){
			parent.validate();
		}
	}

	private void rebuildUI() {
		System.out.println("rebuildUI called");
		for (ScenePreviewPanel previews : sceneList) {
			previews.removeMouseListener(this);
		}
		sceneList.removeAllElements();
		this.removeAll();
		Vector<Location> locations = currentProject.getLocations();
		for (Location location : locations) {
			location.addChangeListener(this);
			Vector<Scene> scenes = location.getScenes();
			for (Scene scene : scenes) {
				sceneList.add(new ScenePreviewPanel(location, scene));
			}
			
		}
		for (ScenePreviewPanel scene : sceneList) {
			if(parent != null){
				scene.addParent(parent);
				scene.addMouseListener(this);
			}
			this.add(scene);
		}
		this.add(Box.createGlue());
		SpringUtilities.makeCompactGrid(this,
                this.getComponentCount(), 1,
                2, 6, 2, 6);
	}

	@Override
	public void stateChanged(ChangeEvent event) {
		if(event.getSource() instanceof Location){
			rebuildUI();
		}
		if(parent != null){
			parent.validate();
		}
	}

	public void setParentCompoent(Component parent) {
		this.parent = parent;
		
	}

	public void setCurrentScene(Scene s) {
		for(ScenePreviewPanel scenes : sceneList) {
			scenes.setCurrent(s);
		}
	}

	public void mouseClicked(MouseEvent e) {
		Object obj = e.getSource();
		if(obj instanceof ScenePreviewPanel) {
			for (SceneChangeListener listener : sceneChangeListeners){
				listener.currentSceneChanged(((ScenePreviewPanel)obj).getSceneLocation(),
						((ScenePreviewPanel)obj).getScene());
			}
		}
	}

	public void mouseEntered(MouseEvent e) {
		if(moveScene != null){
			Object obj = e.getSource();
			if(obj instanceof ScenePreviewPanel) {
				moveTo = (ScenePreviewPanel)obj;
			}
		}
	}

	public void mouseExited(MouseEvent e) {
		// nothing to do
		
	}

	public void mousePressed(MouseEvent e) {
		Object obj = e.getSource();
		if(obj instanceof ScenePreviewPanel) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				moveScene = ((ScenePreviewPanel)obj).getScene();
			}
			else if (e.getButton() == MouseEvent.BUTTON3) {
				currentScene = ((ScenePreviewPanel)obj).getScene();
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	public void mouseReleased(MouseEvent e) {
		if(moveTo == null) {
			return;
		}
		Object obj = e.getSource();
		if(obj instanceof ScenePreviewPanel) {
			
			INSERT movepos = moveTo.getMovePosition();
			int position = moveTo.getSceneLocation().getScenes().indexOf(moveTo.getScene());
			if(movepos == INSERT.AFTER){
				position++;
			}
			
			
			if(moveTo.getScene() != moveScene) {
				for (SceneChangeListener listener : sceneChangeListeners){
					System.out.println("call listener");
					listener.moveScene(((ScenePreviewPanel)obj).getSceneLocation(), moveScene,position);
				}
				
			}
			moveScene = null;
			moveTo = null;
		}
		
	}

	public void addSceneChangeListener(SceneChangeListener sceneChangeListener) {
		this.sceneChangeListeners.add(sceneChangeListener);
	}

	@Override
	public void changeNameSelected() {
		currentScene.setSceneName(JOptionPane.showInputDialog("Please input a name for this scene")); 
	}
	
}