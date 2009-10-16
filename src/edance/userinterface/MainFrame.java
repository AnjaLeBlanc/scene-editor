package edance.userinterface;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import edance.userinterface.event.ToolbarListener;

/**
 * Central frame of the edance user interface
 * @author zzptmba
 *
 */
public class MainFrame extends JFrame {
	private static final String TITLE = "eDance Scene Editor";
	private JSplitPane splitPane;
	private Toolbar menuBar;
	private DesignPanel designPanel;
	private ScenePanel scenePanel;
	
	private static final long serialVersionUID = 1L;
	public MainFrame(){
		super(TITLE);
			
		TimeScrollPanel timeScrollPanel= new TimeScrollPanel();
		scenePanel = new ScenePanel();
		designPanel = new DesignPanel();
		menuBar = new Toolbar();
		JScrollPane sceneScrollPane = new JScrollPane(scenePanel, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scenePanel.setParentCompoent(sceneScrollPane);
		//Create a split pane with the two scroll panes in it.
		JSplitPane topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
									designPanel, sceneScrollPane);
		topSplitPane.setDividerLocation(950);
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
		                           topSplitPane, timeScrollPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(750);

//		Provide minimum sizes for the two components in the split pane
		Dimension minimumSize = new Dimension(100, 300);
		topSplitPane.setMinimumSize(minimumSize);
		timeScrollPanel.setMinimumSize(new Dimension(100,50));
		designPanel.setMinimumSize(new Dimension(400,300));
		//designPanel.setPreferredSize(new Dimension(800,600));
		//designPanel.setSize(new Dimension(800,600));
		this.setLayout(new BorderLayout());
		this.add(menuBar, BorderLayout.PAGE_START);
		this.add(splitPane, BorderLayout.CENTER);
		
		this.addToolbarListener(designPanel);

	}
	
	public void addToolbarListener(ToolbarListener tl) {
		menuBar.addToolbarListener(tl);
	}
	
	public DesignPanel getDesignPanel() {
		return designPanel;
	}

	public Toolbar getToolbar() {
		return menuBar;
	}

	public ScenePanel getScenePanel() {
		return this.scenePanel;
	}
}
