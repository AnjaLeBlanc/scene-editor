package edance.sceeneditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;

import javax.media.Effect;
import javax.media.Format;
import javax.media.protocol.DataSource;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import common.JarExtractor;

import rtpReceiver.RGBRenderer;

public class LiveVideo extends MediaObjects {

	// The multiplier for the large size
	private static final int LARGE_SIZE_MULTIPLIER = 2;

	// The divider for the small size
	private static final int SMALL_SIZE_DIVIDER = 2;

	// The default width of the video
	private static final int DEFAULT_WIDTH = 352;

	// The offset of the key pressed
	private static final int KEY_OFFSET = 5;

	// Border around which the point is still considered as belonging to the point
	private static final int BORDERSPACE = 4;
	// Size of the square around the points
	private static final int SQUARE = 3;

	// TransparencyValue
	private int transparency = 0;

	private RGBRenderer renderer = null;

	private JFrame video = null;

	@Override
	public java.lang.Object clone() throws CloneNotSupportedException {
		LiveVideo clone = (LiveVideo) super.clone();
		clone.defaultSize = this.defaultSize.clone();
		clone.currSize = this.currSize.clone();
		clone.woff = this.woff;
		clone.hoff = this.hoff;
		clone.renderer = null;
		clone.video = null;
		return clone;
	}

	// The width to remove for key '4'
	private static final int NEGATIVE_MULTIPLIER_WIDTH = 59;

	// The width to add for key '6'
	private static final int POSITIVE_MULTIPLIER_WIDTH = 118;

	// The height to remove for key '4'
	private static final int NEGATIVE_MULTIPLIER_HEIGHT = 48;

	// The width to add for key '6'
	private static final int POSITIVE_MULTIPLIER_HEIGHT = 96;

	// The width offset of the panel
	private int woff = 0;

	// The height offset of the panel
	private int hoff = 0;

	private static final int SIZE_X = 352;
	private static final int SIZE_Y = 288;
	private int[] defaultSize = {SIZE_X, SIZE_Y};

	private int[] currSize = defaultSize.clone();

	private boolean visible = true;

	private Rectangle bounds;

//	private JarExtractor je = new JarExtractor(LiveVideo.class.getProtectionDomain().getCodeSource().getLocation().getFile());
	private JarExtractor je = new JarExtractor("edance.jar");

	public LiveVideo() {
		super();
		this.spacehoderImage = new ImageIcon(je.getImageData("images/camcorder20x16.png")).getImage();
	}
	
	public LiveVideo(Node node){
		super();
		this.spacehoderImage = new ImageIcon(je.getImageData("images/camcorder20x16.png")).getImage();
		
		NamedNodeMap nnm = node.getAttributes();
		if(nnm != null){
			for(int i = 0; i < nnm.getLength(); i++){
				Node n2 = nnm.item(i);
				if (n2.getNodeName().trim().compareTo("SOURCE") == 0) {
					this.setSource(n2.getNodeValue());
				} else if (n2.getNodeName().trim().compareTo("TRANSPARENCY") == 0) {
					this.transparency =Integer.valueOf(n2.getNodeValue());
					System.out.println("transparency " +transparency);
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
			} else if(nl.item(i).getNodeName().trim().compareTo("SIZE") == 0) {
				nnm = nl.item(i).getAttributes();
				if(nnm != null){
					for(int j = 0; j < nnm.getLength(); j++){
						Node n2 = nnm.item(j);
						if (n2.getNodeName().trim().compareTo("WIDTH") == 0) {
							this.currSize[0] = Integer.valueOf(n2.getNodeValue());
						} else if (n2.getNodeName().trim().compareTo("HIGHT") == 0) {
							this.currSize[1] = Integer.valueOf(n2.getNodeValue());
						}
					}
				}
			}
		}
	}

	public LiveVideo(int x, int y) {
		super();
		this.positionX = (int) ((double) x * MediaObjects.scaleToReal);
		this.positionY = (int) ((double) y * MediaObjects.scaleToReal);
		this.spacehoderImage = new ImageIcon(je.getImageData("images/camcorder20x16.png")).getImage();
		// System.out.println("x,y " + x +" "+ y);
		// System.out.println("scale " + MediaObjects.scaleToReal);
	}

	public boolean setKey(int key, boolean ctrl) {
		boolean returnvalue = false;
		if (ctrl) {
			return false;
		}
		Dimension size = new Dimension(defaultSize[0], defaultSize[1]);

		// Calculate the default height and width
		int width = DEFAULT_WIDTH;
		int height = (int) ((DEFAULT_WIDTH * size.getHeight()) / size
				.getWidth());

		// Resize the window depending on the key
		// Size of transmitting stream
		if ((key == 'D') || (key == 'd')) {
			setSize((int) size.getWidth() + woff, (int) size.getHeight()
					+ hoff);
			returnvalue = true;
		}

		// Small size
		if ((key == 'S') || (key == 's')) {
			setSize((width / SMALL_SIZE_DIVIDER) + woff,
					(height / SMALL_SIZE_DIVIDER) + hoff);
			returnvalue = true;
		}

		// Medium size
		if ((key == 'M') || (key == 'm')) {
			setSize((width) + woff, (height) + hoff);
			returnvalue = true;
		}

		// Large size
		if ((key == 'L') || (key == 'l')) {
			setSize((width * LARGE_SIZE_MULTIPLIER) + woff,
					(height * LARGE_SIZE_MULTIPLIER) + hoff);
			returnvalue = true;
		}

		// Size between very small and very large (2 = small, 5 = medium, 8 =
		// large)
		if ((key >= '1') && (key <= '9')) {
			int value = (key - '0') - KEY_OFFSET;
			int wdiff = value < 0 ? NEGATIVE_MULTIPLIER_WIDTH
					: POSITIVE_MULTIPLIER_WIDTH;
			int hdiff = value < 0 ? NEGATIVE_MULTIPLIER_HEIGHT
					: POSITIVE_MULTIPLIER_HEIGHT;
			width = (width + (wdiff * value));
			height = (height + (hdiff * value));
			setSize(width + woff, height + hoff);
			returnvalue = true;
		}
		return returnvalue;
	}

	public void setSize(int width, int height) {
		// System.out.println("setSize " + currSize);
		currSize[0] = width;
		currSize[1] = height;
		changePreviewRenderer();
	}

	private void changePreviewRenderer() {
		if (renderer != null) {
			/*
			 * renderer.getPreviewRenderer().setBounds(new Rectangle(
			 * (int)(this.positionX*MediaObjects.scaleToDesign+1),
			 * (int)(this.positionY*MediaObjects.scaleToDesign+1),
			 * (int)((this.positionX+currSize[0])*MediaObjects.scaleToDesign),
			 * (int)((this.positionY+currSize[1])*MediaObjects.scaleToDesign)));
			 */
			Component comp = renderer.getComponent();
			comp.setPreferredSize(new Dimension(currSize[0], currSize[1]));
			comp.setSize(new Dimension(currSize[0], currSize[1]));

			comp = renderer.getPreviewRenderer().getComponent();
//			if(renderer.getPreviewRenderer()  instanceof RGBRenderer){
//				((RGBRenderer)renderer.getPreviewRenderer()).setTransparency(1-getTransparency());
//			}
			comp.setPreferredSize(new Dimension((int) (currSize[0]
					* MediaObjects.scaleToDesign - 1), (int) (currSize[1]
					* MediaObjects.scaleToDesign - 1)));
			comp.setSize((int) (currSize[0] * MediaObjects.scaleToDesign - 1),
					(int) (currSize[1] * MediaObjects.scaleToDesign - 1));
			comp.setLocation(
					(int) (this.positionX * MediaObjects.scaleToDesign + 1),
					(int) (this.positionY * MediaObjects.scaleToDesign + 1));
		}
		if (video != null) {
			video.pack();
			video.setLocation(this.positionX + bounds.x , this.positionY + bounds.y);
		}
	}

	public void createRenderer(Effect[] effect, Format format, DataSource ds,
			int track) {
		try {
			renderer = new RGBRenderer(
					new Effect[] {/* transEffect, softEdgesEffect */});
			if (renderer.setInputFormat(format) == null) {
				System.err.println("no renderer found");
			}
			renderer.setDataSource(ds, track);
			renderer.setTransparency(1-getTransparency());
		} catch (Exception e) {
			e.printStackTrace();
			renderer = null;
		}

		renderer.getComponent().setVisible(false);
		renderer.start();
		changePreviewRenderer();

	}

	public void paintPreview(Graphics g, int offset) {
		int x = (int) (this.positionX * MediaObjects.scaleToPreview);
		int y = (int) (this.positionY * MediaObjects.scaleToPreview + offset);
		int w = (int) (currSize[0] * MediaObjects.scaleToPreview);
		int h = (int) (currSize[1] * MediaObjects.scaleToPreview);

		g.setColor(Color.gray);
		g.fillRect(x, y, w, h);
		g.setColor(Color.darkGray);
		g.drawRect(x, y, w, h);

		g.drawImage(this.spacehoderImage, (int) (x + 1),
				(int) (y), this.spacehoderImage.getWidth(null), this.spacehoderImage
						.getHeight(null), null);
		super.paintPreview(g, offset);
	}
	
	public void paint(Graphics g) {
		changePreviewRenderer();
		int x = (int) (this.positionX * MediaObjects.scaleToDesign);
		int y = (int) (this.positionY * MediaObjects.scaleToDesign);
		int w = (int) (currSize[0] * MediaObjects.scaleToDesign);
		int h = (int) (currSize[1] * MediaObjects.scaleToDesign);

		if (visible == false) {
			g.setColor(Color.black);
			g.fillRect(x, y, w + 1, h + 1);
			return;
		}

		if (renderer == null) {
			g.setColor(Color.gray);
			g.fillRect(x, y, w, h);
			g.setColor(Color.darkGray);
			g.drawRect(x, y, w, h);

			g.drawImage(this.spacehoderImage, x + 1, y,
					this.spacehoderImage.getWidth(null), this.spacehoderImage
							.getHeight(null), null);
		}
		if (select != POINTS.NONE) {
			g.setColor(Color.red);
			g.drawRect(x, y, w, h);
			g.fillRect(x - SQUARE, y - SQUARE, SQUARE * 2, SQUARE * 2);
			g.fillRect(x + w - 2, y - SQUARE, SQUARE * 2, SQUARE * 2);
			g.fillRect(x - SQUARE, y + h - 2, SQUARE * 2, SQUARE * 2);
			g.fillRect(x + w - 2, y + h - 2, SQUARE * 2, SQUARE * 2);
		}
		super.paint(g);
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
		x = (int) (x * MediaObjects.scaleToReal);
		y = (int) (y * MediaObjects.scaleToReal);
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
		xx0 = (int) (xx0 * MediaObjects.scaleToReal);
		yy0 = (int) (yy0 * MediaObjects.scaleToReal);
		xx1 = (int) (xx1 * MediaObjects.scaleToReal);
		yy1 = (int) (yy1 * MediaObjects.scaleToReal);
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
		changePreviewRenderer();
	}

	public void movepoint(int x, int y) {
		// System.out.println("movePoint by x" + x + " y " + y);
		if (select == POINTS.POINT4 || select == POINTS.POINT2) {
			currSize[0] += x;
		}
		if (select == POINTS.POINT3 || select == POINTS.POINT1) {
			currSize[0] -= x;
			this.positionX += x;
		}
		if (select == POINTS.POINT4 || select == POINTS.POINT3) {
			currSize[1] += y;
		}
		if (select == POINTS.POINT2 || select == POINTS.POINT1) {
			currSize[1] -= y;
			this.positionY += y;
		}
		changePreviewRenderer();
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

	public Component getPreviewComponent() {
		if (renderer != null) {
			return renderer.getPreviewRenderer().getComponent();
		} else {
			return null;
		}
	}

	public Component getComponent() {
		if (renderer != null) {
			renderer.getComponent().setVisible(true);
			return renderer.getComponent();
		} else {
			return null;
		}
	}

	public void stop() {
		this.stopRun();
		if (renderer != null) {
			renderer.getPreviewRenderer().getComponent().setVisible(false);
			renderer.stop();
			renderer.close();
		}
	}

	public void setVisible(boolean b) {
		this.visible = b;
		if (renderer != null) {
			renderer.getPreviewRenderer().getComponent().setVisible(b);
		}
		if (b && video != null) {
			this.run(null, bounds);
		} else if (!b && video != null) {
			this.stopRun();
		}


	}

	public void setTransparency(int value) {
		this.transparency = value;
		if (video != null) {
			com.sun.jna.examples.WindowUtils.setWindowAlpha(video,
					(float) (1.0f - this.getTransparency()));
		}
		if(renderer == null) {
			return;
		}
		renderer.setTransparency(1-getTransparency());
	}

	public int getTransparencyValue() {
		return this.transparency;
	}
	
	private float getTransparency() {
		return (float) (this.transparency / 100f);
	}

	public void run(GraphicsConfiguration gc, Rectangle bounds) {
		this.bounds = bounds;
		
		if(this.getComponent() == null) {
			return;
		}
		
		if (video == null) {
			video = new JFrame(gc);
			video.setUndecorated(true);
			video.setTitle("video");
			video.add(this.getComponent());
		}
		video.setLocation(this.positionX + bounds.x , this.positionY + bounds.y);
		video.pack();
		com.sun.jna.examples.WindowUtils.setWindowAlpha(video, (float) (1.0f - this.getTransparency()));
		video.setVisible(true);
	}

	public void stopRun() {
		if (video != null) {
			video.setVisible(false);
			video.dispose();
		}
		video = null;
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
		Element size = xmldoc.createElementNS(null, "SIZE");
		size.setAttribute("WIDTH", String.valueOf(this.currSize[0]));
		size.setAttribute("HIGHT", String.valueOf(this.currSize[1]));
		e.appendChild(size);
		e.setAttribute("TRANSPARENCY", String.valueOf(this.transparency));
		return e;
	}

	public void setPreviewComponentNull() {
		this.renderer=null;
	}
}
