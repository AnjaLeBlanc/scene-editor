package edance.userinterface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.protocol.DataSource;
import javax.swing.JPanel;

import edance.sceeneditor.AudioFile;
import edance.sceeneditor.LiveAudio;
import edance.sceeneditor.LiveVideo;
import edance.sceeneditor.MediaObjects;
import edance.sceeneditor.PrerecordedVideo;
import edance.sceeneditor.Scene;
import edance.sceeneditor.Location.ASPECT_RATIO;
import edance.userinterface.Toolbar.ToolbarButtons;
import edance.userinterface.event.StreamListener;
import edance.userinterface.event.ToolbarListener;
import effects.CloneEffect;

public class DesignPanel extends JPanel implements ToolbarListener,
		ComponentListener, MouseListener, MouseMotionListener, KeyListener,
		StreamListener {

	private static final long serialVersionUID = 1L;

	private int oldX, oldY;

	private int bx0, by0, bx1, by1;

	private boolean bvis = false;

	private int mode;

	private Vector<MediaObjects> currCopy;

	private static final double MULTIPLICATION_RATIO1 = 1.3333;
	private static final double MULTIPLICATION_RATIO2 = 1.5556;
	private double[] ratios = {MULTIPLICATION_RATIO1,  MULTIPLICATION_RATIO2};

	protected static final int[][] DISPLAY_SIZE = {{1024, 768}, {1280, 768}};
	private static final Dimension DEFAULT_SIZE = new Dimension(400, 300);
	
	private boolean playing=false;


	private int currRatio = 0;

	private enum State {
		SELECT, ADD_PRERECORDED_VIDEO, ADD_PRERECORDED_AUDIO, ADD_LIVE_VIDEO, ADD_LIVE_AUDIO, ADD_TIMESHIFTED_VIDEO, ADD_PICTURE
	}

	private State curState = State.SELECT;

	private Scene scene;

	private JPanel designArea;

	private GraphicsConfiguration defaultConfiguration;

	public DesignPanel() {
		this.addComponentListener(this);
		this.setLayout(new BorderLayout());
		this.setFocusable(true);
		designArea = new JPanel();
		designArea.setBackground(Color.BLACK);
		designArea.setMinimumSize(DEFAULT_SIZE);
		//designArea.addComponentListener(this);
		designArea.addMouseListener(this);
		designArea.addMouseMotionListener(this);
		this.addKeyListener(this);
		this.add(designArea, BorderLayout.CENTER);
	}

	public void paint(Graphics g) {
		super.paint(g);
		scene.paint(g);
		if (bvis) {
			g.setColor(Color.red);
			g.drawLine(bx0, by0, bx1, by0);
			g.drawLine(bx1, by0, bx1, by1);
			g.drawLine(bx1, by1, bx0, by1);
			g.drawLine(bx0, by1, bx0, by0);
		}
	}

	public void setScene(Scene scene) {
		this.scene = scene;
		System.out.println("setScene");
		scene.scale((double) DISPLAY_SIZE[currRatio][0]
				/ (double) designArea.getSize().width, (double) designArea
				.getSize().width
				/ (double) DISPLAY_SIZE[currRatio][0]);
		this.scene.addAddStreamListener(this);
	}

	public void toolbarButtonPressed(ToolbarButtons toolbarButton) {
		if (toolbarButton == ToolbarButtons.PRERECORDED_VIDEO) {
			curState = State.ADD_PRERECORDED_VIDEO;
		} else if (toolbarButton == ToolbarButtons.PRERECORDED_AUDIO) {
			curState = State.ADD_PRERECORDED_AUDIO;
		} else if (toolbarButton == ToolbarButtons.LIVE_VIDEO) {
			curState = State.ADD_LIVE_VIDEO;
		} else if (toolbarButton == ToolbarButtons.AUDIO) {
			curState = State.ADD_LIVE_AUDIO;
		} else if (toolbarButton == ToolbarButtons.PICTURE) {
			curState = State.ADD_PICTURE;
		} else if (toolbarButton == ToolbarButtons.TIMESHIFTED_LIVE_VIDEO) {
			curState = State.ADD_TIMESHIFTED_VIDEO;
		}
	}

	/*
	 * ComponentListener implementation - keep aspect ratio constant
	 * (non-Javadoc)
	 *
	 * @see java.awt.event.ComponentListener#componentHidden(java.awt.event.ComponentEvent)
	 */
	public void componentHidden(ComponentEvent arg0) {
	}

	public void componentMoved(ComponentEvent arg0) {
	}

	public void componentResized(ComponentEvent e) {
		System.out.println("DesignPanel resize");
		Dimension size = this.getSize();
		if (size.height * ratios[0] < size.width) {
			designArea.setSize(new Dimension(
					(int) (size.height * ratios[currRatio]), size.height));
		} else {
			designArea.setSize(new Dimension(size.width,
					(int) (size.width / ratios[currRatio])));
		}
		scene.scale((double) DISPLAY_SIZE[currRatio][0]
				/ (double) designArea.getSize().width, (double) designArea
				.getSize().width
				/ (double) DISPLAY_SIZE[currRatio][0]);
	}

	public void componentShown(ComponentEvent arg0) {
	}

	public void setAspcetRatio(ASPECT_RATIO ratio) {
		currRatio = ratio.ordinal();
		System.out.println("ordinal " + ratio.ordinal());
		this.componentResized(new ComponentEvent(this,
				ComponentEvent.COMPONENT_RESIZED));
	}

	/*
	 * MouseListener Events
	 */
	public void mouseClicked(MouseEvent e) {
		if (curState == State.ADD_LIVE_VIDEO) {
			LiveVideo lv = new LiveVideo(this.getMousePosition().x, this
					.getMousePosition().y);
			scene.addMediaObject(lv);
			lv.paint(this.getGraphics());
			curState = State.SELECT;
		} else if (curState == State.ADD_PRERECORDED_VIDEO) {
			PrerecordedVideo prv = new PrerecordedVideo(
					this.getMousePosition().x, this.getMousePosition().y);
			scene.addMediaObject(prv);
			prv.paint(this.getGraphics());
			curState = State.SELECT;
		} else if (curState == State.ADD_PRERECORDED_AUDIO) {
			AudioFile audioFile = new AudioFile(this.getMousePosition().x, 
					this.getMousePosition().y);
			scene.addMediaObject(audioFile);
			audioFile.paint(this.getGraphics());
			curState = State.SELECT;
		} else if (curState == State.ADD_LIVE_AUDIO) {
			LiveAudio audio = new LiveAudio(this.getMousePosition().x, 
					this.getMousePosition().y);
			scene.addMediaObject(audio);
			audio.paint(this.getGraphics());
			curState = State.SELECT;
		}
		
	}

	public void mouseEntered(MouseEvent e) {
		this.requestFocusInWindow();
	}

	public void mouseExited(MouseEvent e) {

	}

	public void mousePressed(MouseEvent e) {

		if (e.getButton() == MouseEvent.BUTTON1) {
			Point mousePoint = this.getMousePosition();
			if (mousePoint == null) {
				return;
			}
			int xs = mousePoint.x;
			int ys = mousePoint.y;
			boolean returnvalue = false;
			mode = 0;
			if (curState == State.SELECT) {
				scene.setSelectedStates1();
				returnvalue = scene.selectpointCond(xs, ys);
				if (!returnvalue) {
					returnvalue = scene.selectObject(e.isShiftDown(), xs, ys);
					mode = 1;
				}
				if (!returnvalue) {
					if (!e.isShiftDown()) {
						scene.setSelectedStates0();
					}
					bvis = true;
					bx0 = xs;
					bx1 = xs;
					by0 = ys;
					by1 = ys;
					mode = 1;
				}
				oldX = mousePoint.x;
				oldY = mousePoint.y;
				paint(this.getGraphics());
			}
			return;
		}
		if (e.getButton() == MouseEvent.BUTTON3) {
			if (scene.showMediaPopupMenu(e)) {
				paint(this.getGraphics());
			}
		}

	}

	public void mouseReleased(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			Point mousePoint = this.getMousePosition();
			if (mousePoint == null) {
				return;
			}
			if (bvis) {
				bx1 = mousePoint.x;
				by1 = mousePoint.y;
				int xmin = (bx1 < bx0) ? bx1 : bx0;
				int ymin = (by1 < by0) ? by1 : by0;
				int xmax = (bx1 > bx0) ? bx1 : bx0;
				int ymax = (by1 > by0) ? by1 : by0;
				scene.selectrect(xmin, ymin, xmax, ymax);
				bvis = false;
				repaint();
			} else {
				if (mode == 0) {
					scene.movepoint(mousePoint.x - oldX, mousePoint.y
							- oldY);
				} else {
//					System.out.println("mouse release move object");
					scene.moveobj(mousePoint.x - oldX, mousePoint.y
							- oldY);
				}
				if (curState != State.SELECT) {
					scene.setSelectedStates0();
				}
				paint(this.getGraphics());
			}
			oldX = mousePoint.x;
			oldY = mousePoint.y;
		}
	}

	public void mouseDragged(MouseEvent e) {
		Point mousePoint = this.getMousePosition();
		if (mousePoint == null) {
			return;
		}
		if (bvis) {
			bx1 = mousePoint.x;
			by1 = mousePoint.y;
		} else {
			if (mode == 0) {
				scene.movepoint(mousePoint.x - oldX, mousePoint.y
						- oldY);
			} else {
//				System.out.println("move object");
				scene.moveobj(mousePoint.x - oldX, mousePoint.y
						- oldY);
			}
		}
		oldX = mousePoint.x;
		oldY = mousePoint.y;
		paint(this.getGraphics());
	}

	public void mouseMoved(MouseEvent arg0) {
	}

	/*
	 * KeyListener impemented functions (non-Javadoc)
	 *
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent e) {
		boolean returnvalue = scene.sentKeyToSelected(e.getKeyCode(), e
				.isControlDown());
		if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
			currCopy = scene.copySelObj();
			scene.setSelectedStates0();
			returnvalue = true;
		} else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) {
			scene.pastSelObj(currCopy);
			returnvalue = true;
		} else if (e.getKeyCode() == KeyEvent.VK_T) {
			scene.moveSelTop();
			returnvalue = true;
		} else if (e.getKeyCode() == KeyEvent.VK_B) {
			scene.moveSelBottom();
			returnvalue = true;
		} else if (e.getKeyCode() == KeyEvent.VK_U) {
			scene.moveSelUp();
			returnvalue = true;
		} else if (e.getKeyCode() == KeyEvent.VK_P) {
			scene.moveSelDown();
			returnvalue = true;
		}
		if (returnvalue) {
			paint(this.getGraphics());
			System.out.println("paint");
		}
	}

	public void keyReleased(KeyEvent e) {

	}

	public void keyTyped(KeyEvent e) {

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edance.userinterface.event.StreamListener#addStream(effects.CloneEffect,
	 *      javax.media.Format, long, int)
	 */
	public void addStream(DataSource cloneEffect, Format format, int position) {
		System.out.println("add stream " + scene.getMediaObjectCount() + " " + position);
		scene.addStream(cloneEffect, format, position);
		Component comp = scene.getPreviewComponents(position);
		if (comp == null) {
			return;
		}
		this.add(comp);
		this.setComponentZOrder(comp,
				(scene.getMediaObjectCount() - position - 1));
		comp.addMouseListener(this);
		comp.addMouseMotionListener(this);
		comp.addKeyListener(this);
		System.out.println("add stream " + position);
		if(playing == true) {
			scene.startFullScreenMedia(defaultConfiguration, position);
		}
	}

	public void zOrderChanged(Vector<MediaObjects> mediaObjects) {
		for (int i = 0; i < mediaObjects.size(); i++) {
			MediaObjects m = mediaObjects.get(i);
			if (m instanceof LiveVideo || m instanceof PrerecordedVideo) {
				this.setComponentZOrder(m.getPreviewComponent(), mediaObjects
						.size()
						- i - 1);
			}
		}
	}

	/**
	 * callback function StreamListener
	 */
	public void addVideo(CloneEffect cloneEffect, Format format, long ssrc,
			int position) {
		scene.addVideo(cloneEffect, format, ssrc, position);
		Component comp = scene.getPreviewComponents(position);
		this.add(comp);
		this.setComponentZOrder(comp,
				(scene.getMediaObjectCount() - position - 1));
		comp.addMouseListener(this);
		comp.addMouseMotionListener(this);
		comp.addKeyListener(this);
	}


	public void removeMedia(int position) {
		Component comp = scene.getPreviewComponents(position);
		if (comp != null) {
			this.remove(comp);
			comp.removeMouseListener(this);
			comp.removeKeyListener(this);
		}
	}

	public void stopCurrScene() {
		if	(playing) {
			scene.stopFullScreen();
			playing = false;
		}
	}

	public void playCurrScene(GraphicsConfiguration defaultConfiguration) {
		this.defaultConfiguration = defaultConfiguration;
		if	(!playing){
			scene.startFullScreen(defaultConfiguration);
			playing = true;
		}
	}

	@Override
	public void addStream(MediaLocator locator, long ssrc, int position) {
		scene.addStream(locator, ssrc, position);
	}
}
