package edance.userinterface.event;

import edance.userinterface.Toolbar.ToolbarButtons;

/**
 * Trigger to toggle a button in a userinterface
 * @author zzptmba
 *
 */
public interface ToggleButton {
	/**
	 * Toggle a button in the UI
	 * @param tulbarButton the button to be toggled
	 */
	public void toggleButton(ToolbarButtons tulbarButton);
}
