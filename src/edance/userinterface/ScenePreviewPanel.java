package edance.userinterface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edance.sceeneditor.Location;
import edance.sceeneditor.Scene;
import edance.sceeneditor.Location.ASPECT_RATIO;

public class ScenePreviewPanel extends JPanel implements ComponentListener, ChangeListener{
	
	private static final long serialVersionUID = 1L;
	private String locationName;
	private ASPECT_RATIO aspect_ratio;
	private Scene scene;
	private Location location;
	private JPanel scenePreview = new JPanel();
	private JLabel name;
	private static final double MULTIPLICATION_RATIO1 = 1.3333;
	private static final double MULTIPLICATION_RATIO2 = 1.5556;
	private double[] ratios = {MULTIPLICATION_RATIO1,  MULTIPLICATION_RATIO2};
	
	
	protected enum INSERT  { 
		ERROR,
		BEFORE,
		AFTER
		};

	public ScenePreviewPanel(Location location, Scene scene) {
		this.aspect_ratio = location.getAspectRatio();
		this.location = location;
		this.locationName = location.getLocationName();
		this.setAlignmentX(Component.TOP_ALIGNMENT);
		this.scene = scene;
		this.setPreferredSize(new Dimension(310,(int)(310/ ratios[this.aspect_ratio.ordinal()]+40)));
		this.setMaximumSize(new Dimension(this.getSize().width,  (int)((this.getSize().width-10)*3/4) + 50));
		scene.addChangeListener(this);
		this.setLayout(new BorderLayout(5,5));
		if(this.scene.getSceneName().length()>0){
			name = new JLabel(this.locationName+" : "+this.scene.getSceneName(),JLabel.LEFT);
		} else {
			name = new JLabel(this.locationName,JLabel.LEFT);
		}
		name.setBorder(new EmptyBorder(3,5,1,3));
		this.add(name, BorderLayout.NORTH);
		Dimension size = new Dimension(310, (int)(310/ ratios[this.aspect_ratio.ordinal()]));
		System.out.println("size 2 " + size);
		scenePreview.setSize(size);
		scenePreview.setBackground(Color.BLACK);
		this.add(scenePreview, BorderLayout.CENTER);
	}

	public void componentHidden(ComponentEvent arg0) {
		// not needed
	}

	public void componentMoved(ComponentEvent arg0) {
		// not needed
		
	}

	public void componentResized(ComponentEvent event) {
		Dimension size = event.getComponent().getSize();
		System.out.println("size resize "  + size);
		this.setSize(new Dimension(size.width,(int)(size.width/ ratios[this.aspect_ratio.ordinal()]+40)));
		this.setPreferredSize(new Dimension(size.width,(int)(size.width/ ratios[this.aspect_ratio.ordinal()]+40)));
		System.out.println("size " + size);
//		scenePreview.setMinimumSize(new Dimension(size.width,
//			(int) (size.width / ratios[this.aspect_ratio.ordinal()])));
		scenePreview.setSize(new Dimension(size.width,
				(int) (size.width / ratios[this.aspect_ratio.ordinal()])));
		scenePreview.setPreferredSize(new Dimension(size.width,
				(int) (size.width / ratios[this.aspect_ratio.ordinal()])));
		System.out.println("scenePriview size "  + scenePreview.getSize());
		
		scene.scale((double) scenePreview.getSize().width/
				(double) DesignPanel.DISPLAY_SIZE[this.aspect_ratio.ordinal()][0]);
	}

	public void componentShown(ComponentEvent arg0) {
		// not needed
		
	}

	public void addParent(Component parent) {
		parent.addComponentListener(this);
	}

	@Override
	public void stateChanged(ChangeEvent event) {
		if(event.getSource() instanceof Scene){
			scenePreview.validate();
			if(this.scene.getSceneName().length()>0){
				name.setText(this.locationName+" : "+this.scene.getSceneName());
			} else {
				name.setText(this.locationName);
			}
			paint(this.getGraphics());
		}
	}
	
	public void paint(Graphics g) {
		if(g == null) {
			return;
		}
		super.paint(g);
		scene.paintPreview(g, scenePreview.getY());
	}

	protected void setCurrent(Scene s) {
		if(scene.equals(s)){
			this.setBackground(Color.LIGHT_GRAY);
		} else {
			this.setBackground(new Color(238,238,238));
		}
	}

	protected Scene getScene() {
		return scene;
	}
	
	protected Location getSceneLocation() {
		return location;
	}

	protected INSERT getMovePosition(){
		if(this.getMousePosition() == null){
			return INSERT.ERROR;
		} if(this.getComponent(0).getMousePosition() != null){
			return INSERT.BEFORE;
		} else {
			return INSERT.AFTER;
		}
		
	}
}
