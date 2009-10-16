package edance.userinterface.event;

import java.util.EventListener;

import edance.sceeneditor.MediaObjects;
import edance.userinterface.MediaPopupMenu.MediaPopupMenuFunction;

public interface MediaPopupMenuListener extends EventListener {
	public void functionSelected(MediaObjects obj, MediaPopupMenuFunction func);
}
