package edance.sceeneditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.IOException;

import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.NoPlayerException;
import javax.media.Processor;
import javax.media.protocol.DataSource;
import javax.swing.ImageIcon;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import common.JarExtractor;

public class LiveAudio extends MediaObjects implements ControllerListener {

	// Border around which the point is still considered as belonging to the point
	private static final int BORDERSPACE = 4;
	// Size of the square around the points
	private static final int SQUARE = 3;
	// An object to allow locking
	private Integer stateLock = new Integer(0);
	private static final String CONTROLLER_ERROR = "Controller would not configure";
	private Processor player;
	private boolean processorFailed = false;

	@Override
	public java.lang.Object clone() throws CloneNotSupportedException {
		LiveAudio clone = (LiveAudio) super.clone();
		clone.defaultSize = this.defaultSize.clone();
		clone.currSize = this.currSize.clone();
		clone.woff = this.woff;
		clone.hoff = this.hoff;
		return clone;
	}

	// The width offset of the panel
	private int woff = 0;

	// The height offset of the panel
	private int hoff = 0;

	private static final int SIZE_X = 30;
	private static final int SIZE_Y = 30;
	private int[] defaultSize = {SIZE_X, SIZE_Y};

	private int[] currSize = defaultSize.clone();
	private boolean visible = true;
//	private JarExtractor je = new JarExtractor(LiveAudio.class.getProtectionDomain().getCodeSource().getLocation().getFile());
	private JarExtractor je = new JarExtractor("edance.jar");
	public LiveAudio() {
		super();
		this.spacehoderImage = new ImageIcon(je.getImageData("images/microphone16x16.png")).getImage();
	}
	
	public LiveAudio(Node node){
		super();
		this.spacehoderImage = new ImageIcon(je.getImageData("images/microphone16x16.png")).getImage();
		
		NamedNodeMap nnm = node.getAttributes();
		if(nnm != null){
			for(int i = 0; i < nnm.getLength(); i++){
				Node n2 = nnm.item(i);
				if (n2.getNodeName().trim().compareTo("SOURCE") == 0) {
					this.setSource(n2.getNodeValue());
				}
			}
		}
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++){
//			System.out.println("childnodes " + nl.item(i).toString());
			if(nl.item(i).getNodeName().trim().compareTo("POSITION") == 0) {
				nnm = nl.item(i).getAttributes();
				if(nnm != null){
					for(int j = 0; j < nnm.getLength(); j++){
						Node n2 = nnm.item(j);
						if (n2.getNodeName().trim().compareTo("X") == 0) {
							this.positionX = Integer.valueOf(n2.getNodeValue());
						} else if (n2.getNodeName().trim().compareTo("Y") == 0) {
							this.positionY = Integer.valueOf(n2.getNodeValue());
						} else if (n2.getNodeName().trim().compareTo("Z") == 0) {
							this.positionZ = Integer.valueOf(n2.getNodeValue());
						}
					}
				}
			}
		}
	}

	public LiveAudio(int x, int y) {
		super();
		this.positionX = (int) ((double) x);
		this.positionY = (int) ((double) y);
		this.spacehoderImage = new ImageIcon(je.getImageData("images/microphone16x16.png")).getImage();
	}

	public boolean setKey(int key, boolean ctrl) {
		boolean returnvalue = false;
		if (ctrl) {
			return false;
		}
		return returnvalue;
	}

	public void paint(Graphics g) {
		int x = (int) this.positionX;
		int y = (int) this.positionY;
		int w = (int) currSize[0];
		int h = (int) currSize[1];
		if (!this.visible) {
			g.setColor(Color.black);
			g.fillRect(x, y, w + 1, h + 1);
			return;
		}

		g.setColor(Color.gray);
		g.fillRect(x, y, w, h);
		g.setColor(Color.darkGray);
		g.drawRect(x, y, w, h);

		g.drawImage(this.spacehoderImage, (int) (this.positionX + 3),
				(int) (this.positionY + 2), this.spacehoderImage.getWidth(null),
				this.spacehoderImage.getHeight(null), null);

		if (select != POINTS.NONE) {
			g.setColor(Color.red);
			g.drawRect(x, y, w, h);
			g.fillRect(x - SQUARE, y - SQUARE, SQUARE * 2, SQUARE * 2);
			g.fillRect(x + w - 2, y - SQUARE, SQUARE * 2, SQUARE * 2);
			g.fillRect(x - SQUARE, y + h - 2, SQUARE * 2, SQUARE * 2);
			g.fillRect(x + w - 2, y + h - 2, SQUARE * 2, SQUARE * 2);
		}
	}

	public boolean selectpoint(int x, int y) {
		x = (int) Math.round(x * MediaObjects.scaleToReal);
		y = (int) Math.round(y * MediaObjects.scaleToReal);
		int x0 = this.positionX;
		int x1 = this.positionX + currSize[0];
		int y0 = this.positionY;
		int y1 = this.positionY + currSize[1];
		if (x - x0 < BORDERSPACE && x - x0 > -BORDERSPACE && y - y0
				< BORDERSPACE && y - y0 > -BORDERSPACE) {
			// System.out.println("select 2");
			select = POINTS.POINT1;
			return (true);
		}
		if (x - x1 < BORDERSPACE && x - x1 > -BORDERSPACE && y - y0
				< BORDERSPACE && y - y0 > -BORDERSPACE) {
			// System.out.println("select 3");
			select = POINTS.POINT2;
			return (true);
		}
		if (x - x0 < BORDERSPACE && x - x0 > -BORDERSPACE && y - y1
				< BORDERSPACE && y - y1 > -BORDERSPACE) {
			// System.out.println("select 4");
			select = POINTS.POINT3;
			return (true);
		} else if (x - x1 < BORDERSPACE && x - x1 > -BORDERSPACE && y - y1
				< BORDERSPACE && y - y1 > -BORDERSPACE) {
			// System.out.println("select 5");
			select = POINTS.POINT4;
			return (true);
		}
		return (false);
	}

	public boolean selectobj(int x, int y) {
		int x0 = this.positionX;
		int x1 = this.positionX + currSize[0];
		int y0 = this.positionY;
		int y1 = this.positionY + currSize[1];
		if (x >= x0 && x <= x1 && y >= y0 && y <= y1) {
			select = POINTS.ALL_POINTS;
			return (true);
		}
		return (false);
	}

	public boolean selectrect(int xx0, int yy0, int xx1, int yy1) {
		int x0 = this.positionX;
		int x1 = this.positionX + currSize[0];
		int y0 = this.positionY;
		int y1 = this.positionY + currSize[1];

		if (xx0 < x0 && xx1 > x1 && yy0 < y0 && yy1 > y1) {
			select = POINTS.ALL_POINTS;
			return (true);
		}
		return (false);
	}

	public void moveobj(int x, int y) {
		// System.out.println("moveObj by x" + x + " y " + y);
		this.positionX += x;
		this.positionY += y;
	}

	public void movepoint(int x, int y) {
	}

	public Dimension getSize() {
		return new Dimension(currSize[0], currSize[1]);
	}

	public MediaObjects getObject(int x, int y) {
		int x0 = this.positionX;
		int x1 = this.positionX + currSize[0];
		int y0 = this.positionY;
		int y1 = this.positionY + currSize[1];
		if ((x - x0 < BORDERSPACE && x - x0 > -BORDERSPACE && y - y0 < BORDERSPACE && y - y0 > -BORDERSPACE)
				|| (x - x1 < BORDERSPACE && x - x1 > -BORDERSPACE && y - y0 < BORDERSPACE && y - y0 > -BORDERSPACE)
				|| (x - x0 < BORDERSPACE && x - x0 > -BORDERSPACE && y - y1 < BORDERSPACE && y - y1 > -BORDERSPACE)
				|| (x - x1 < BORDERSPACE && x - x1 > -BORDERSPACE && y - y1 < BORDERSPACE && y - y1 > -BORDERSPACE)) {
			return this;
		}
		return null;
	}


	public void stop() {
		if (player != null) {
			player.stop();
		}

	}

	public void setVisible(boolean b) {
		this.visible = b;
		if(player == null){
			return;
		}
		if (b) {
			player.start();
		} else {
			player.stop();
		}

	}

	public void startReceivingAudio(DataSource datasource) throws IOException, NoPlayerException {

		System.out.println("start playing audio");
		player = javax.media.Manager.createProcessor(datasource);
		player.addControllerListener(this);
		player.configure();
		processorFailed = false;
		while (!processorFailed && player.getState() < Processor.Configured) {
			synchronized (stateLock) {
				try {
					stateLock.wait();
				} catch (InterruptedException e) {
					//Do Nothing
				}
			}
		}
		if (processorFailed) {
			throw new NoPlayerException(CONTROLLER_ERROR);
		}
		player.setContentDescriptor(null);
		player.realize();
		processorFailed = false;
		while (!processorFailed && player.getState() < Processor.Realized) {
			synchronized (stateLock) {
				try {
					stateLock.wait();
				} catch (InterruptedException e) {
					//Do Nothing
				}
			}
		}
		if (processorFailed) {
			throw new NoPlayerException(CONTROLLER_ERROR);
		}
		player.start();
		System.out.println("should hear something now");
	}

	@Override
	public void controllerUpdate(ControllerEvent ce) {
		// If there was an error during configure or
		// realize, the processor will be closed
		if (ce instanceof ControllerClosedEvent) {
			processorFailed = true;
		}

		// All controller events, send a notification
		// to the waiting thread in waitForState method.
		synchronized (stateLock) {
			stateLock.notifyAll();
		}
	}
	
	public Element save(Document xmldoc) {
		Element e = xmldoc.createElementNS(null, "MEDIA_OBJECT");
		e.setAttribute("NAME", this.getClass().getName());
		e.setAttribute("SOURCE", this.getSource());
		Element pos = xmldoc.createElementNS(null, "POSITION");
		pos.setAttribute("X", String.valueOf(this.positionX));
		pos.setAttribute("Y", String.valueOf(this.positionY));
		pos.setAttribute("Z", String.valueOf(this.positionZ));
		e.appendChild(pos);
		return e;
	}

}

