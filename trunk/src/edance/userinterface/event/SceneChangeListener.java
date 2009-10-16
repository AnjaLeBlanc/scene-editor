package edance.userinterface.event;

import edance.sceeneditor.Location;
import edance.sceeneditor.Scene;

public interface SceneChangeListener {
	public void currentSceneChanged(Location location, Scene scene);
	public void moveScene(Location location, Scene scene, int position);
}
