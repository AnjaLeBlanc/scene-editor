package edance.userinterface;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import edance.userinterface.event.ScenePopupMenuListener;

public class ScenePopupMenu extends JPopupMenu implements ActionListener {

	private static final long serialVersionUID = 1L;
	private final JMenuItem changeName = new JMenuItem("changeName");
	private Vector<ScenePopupMenuListener> listeners = new Vector<ScenePopupMenuListener>();
	
	public ScenePopupMenu(){
		super();
		changeName.addActionListener(this);
		this.add(changeName);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==changeName){
			for(ScenePopupMenuListener listener : listeners){
				listener.changeNameSelected();
			}
		}
		
	}
	
	public void addScenePopupMenuListener(ScenePopupMenuListener listener) {
		listeners.add(listener);
	}

	public void removeScenePopupMenuListener(ScenePopupMenuListener listener) {
		listeners.remove(listener);
	}

}
