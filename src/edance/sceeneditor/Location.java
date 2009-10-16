package edance.sceeneditor;

import java.util.Vector;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edance.devicemanagement.StreamManagement;
import edance.userinterface.DesignPanel;

public class Location extends Object{

	public static enum ASPECT_RATIO {AR4TO3,
		AR9TO5};
		
	private ASPECT_RATIO aspect_ratio = ASPECT_RATIO.AR4TO3;
	private String name;
	private Vector<Scene> scenes=new Vector<Scene>();
	private Vector<ChangeListener> listeners =new Vector<ChangeListener>();
	private Scene currentScene;
	private StreamManagement sm;
	
	public Location(String location, StreamManagement sm) {
		name=location;	
		this.sm = sm;
		newScene(sm);
	}

	public Location(Node node, StreamManagement sm) {
		System.out.println("new Location from node");
		NamedNodeMap nnm = node.getAttributes();
		for(int i = 0; i < nnm.getLength(); i++){
			Node n2 = nnm.item(i);
			System.out.println("node " + n2.getNodeName());
			if (n2.getNodeName().trim().compareTo("NAME") == 0){
				name=n2.getNodeValue();
			} else if (n2.getNodeName().trim().compareTo("ASPCET_RATIO") == 0){
				aspect_ratio=ASPECT_RATIO.valueOf(n2.getNodeValue());
			}
			
		}
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++){
			System.out.println("location child nodes " + i);
			if(nl.item(i).getNodeName().trim().compareTo("SCENE") == 0) {
				scenes.add(new Scene(nl.item(i), sm));
			}
		}
		currentScene=scenes.firstElement();
		triggerChange();
	}

	public void setAspectRatio(ASPECT_RATIO aspectRatio) {
		aspect_ratio=aspectRatio;
		triggerChange();
	}
	
	public ASPECT_RATIO getAspectRatio() {
		return aspect_ratio;
	}
	
	public void setLocation(String location) {
		name=location;
		triggerChange();
	}
	
	public String getLocationName() {
		return name;
	}
	
	public Scene getCurrentScene() {
		System.out.println("current scene " + scenes.indexOf(currentScene));
		return currentScene;
	}
	
	public boolean setCurrentScene(Scene scene) {
		if(scenes.contains(scene)) {
			currentScene = scene;
			return true;
		}
		return false;
	}
	
	public Scene nextScene() {
		currentScene= scenes.elementAt(scenes.indexOf(currentScene)+1);
		return currentScene;
	}
	
	public Scene newScene(StreamManagement sm) {
		Scene newScene=new Scene(sm);
		if(currentScene==null) {
			scenes.add(newScene);
		}
		else {
			scenes.add(scenes.indexOf(currentScene)+1,newScene);
			currentScene.stopPlay();
		}
		currentScene=newScene;
		triggerChange();
		return newScene;
	}
	
	public void moveScene(Scene scene, int position) {
		int oldpos=scenes.indexOf(scene);
		if(oldpos<position) {
			position--;
		}
		if(scenes.remove(scene)==true) {
			scenes.add(position, scene);
		}
		triggerChange();
	}

	public void stopTransmitting() {
		for(int i=0;i<scenes.size();i++) {
			scenes.get(i).stopTransmitting();
		}
		
	}

	public void showNextScene() {
		if(scenes.indexOf(currentScene)<(scenes.size() - 1)){
			currentScene.stopPlay();
			System.out.println("nextscene " +(scenes.indexOf(currentScene) + 1));
			currentScene=scenes.get((scenes.indexOf(currentScene) + 1));
			currentScene.startPlay();
		} else {
			System.out.println("Location: last scene already displayed");
		}
	}

	public void showPreviousScene() {
		if(scenes.indexOf(currentScene)>0){
			currentScene.stopPlay();
			System.out.println("previousscene " +(scenes.indexOf(currentScene) - 1));
			currentScene=scenes.get((scenes.indexOf(currentScene) - 1));
			currentScene.startPlay();
		} else {
			System.out.println("Location: first scene already displayed");
		}
	}

	public Element save(Document xmldoc) {
		Element e = xmldoc.createElementNS(null, "LOCATION");
		e.setAttributeNS(null, "NAME", this.name);
		e.setAttributeNS(null, "ASPCET_RATIO", aspect_ratio.toString());
		for (int i = 0; i < scenes.size(); i++) {
			Element child = scenes.get(i).save(xmldoc);
			if(child != null){
				e.appendChild(child);
			}
		}
		return e;
	}
	

	public void attachStreams(DesignPanel dp) {
		for (int i = scenes.size()-1; i >= 0; i--) {
			currentScene.stopPlay();
			this.currentScene = scenes.get(i);
			dp.setScene(this.currentScene);
//			dp.repaint();
			currentScene.attachAllMedia();
//			dp.repaint();
			
		}
		currentScene = scenes.firstElement();
		currentScene.startPlay();
		dp.setScene(this.currentScene);
		dp.repaint();
	}

	public Vector<Scene> getScenes() {
		return scenes;
	}

	public void addChangeListener(ChangeListener changeListener) {
		if(listeners.contains(changeListener) == false){
			listeners.add(changeListener);
		}
	}
	
	public void removeChangeListener(ChangeListener changeListener) {
		listeners.remove(changeListener);
	}
	
	private void triggerChange() {
		ChangeEvent event = new ChangeEvent(this);
		for (int i=0; i<listeners.size(); i++) {
			listeners.get(i).stateChanged(event);
		}
	}

	public void deleteCurrentScene() {
		 currentScene.stopPlay();
		 int  old = scenes.indexOf(currentScene);
		 scenes.remove(currentScene);
		 //next scene if available
		 System.out.println("old " + old + " scene.size " + scenes.size());
		 if(scenes.size() > old) {
			 currentScene=scenes.get(old);
		 } else if (scenes.size()>0) {
			currentScene=scenes.get(old - 1);
		 } else {
			 currentScene = null;
			 newScene(sm);
		 }
		 currentScene.startPlay();
		 triggerChange();
	}

	public void removeAllScenes() {
		currentScene.stopPlay();
		scenes.removeAllElements();
		currentScene = null;
		newScene(sm);
		currentScene.stopPlay();
		triggerChange();
	}

}
