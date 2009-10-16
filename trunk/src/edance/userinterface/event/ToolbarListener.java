package edance.userinterface.event;

import java.util.EventListener;

import edance.userinterface.Toolbar.ToolbarButtons;

/**
 * Interface for actions occuring in the toolbar of the edance user interface
 * @author zzptmba
 *
 */
public interface ToolbarListener extends EventListener {
	/**
	 * A button in the toolbar of the edance user interface was pressed
	 * @param tulbarButton enum to specify which button was pressed
	 */
	public void toolbarButtonPressed(ToolbarButtons tulbarButton);
}
