package edance.sceeneditor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MediaObjects extends Object implements Cloneable {
	protected int positionX = 0;

	protected int positionY = 0;

	protected int positionZ = 0;

	protected Image spacehoderImage;

	protected POINTS select = POINTS.NONE;

	protected static double scaleToReal;

	protected static double scaleToDesign;
	
	protected static double scaleToPreview;
	
	private String source = null;

	public static enum POINTS {
		NONE, ALL_POINTS, POINT1, POINT2, POINT3, POINT4
	}

	public enum MediaObjectType {
		NONE, LIVE_VIDEO, PRERECORDED_VIDEO, LIVE_AUDIO, PRERECORDED_AUDIO, TIMESHIFTED_VIDEO, PICTURE
	}

	@Override
	protected java.lang.Object clone() throws CloneNotSupportedException {
		MediaObjects clone = (MediaObjects) super.clone();
		clone.positionX = this.positionX;
		clone.positionY = this.positionY;
		clone.positionZ = this.positionZ;
		clone.spacehoderImage = this.spacehoderImage;
		clone.select = this.select;
		return clone;
	}

	/**
	 * Resizes an image using a Graphics2D object backed by a BufferedImage.
	 *
	 * @param srcImg -
	 *            source image to scale
	 * @param w -
	 *            desired width
	 * @param h -
	 *            desired height
	 * @return - the new resized image
	 */
	protected Image getScaledImage(Image srcImg, int w, int h) {
		BufferedImage resizedImg = new BufferedImage(w, h,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = resizedImg.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(srcImg, 0, 0, w, h, null);
		g2.dispose();
		return resizedImg;
	}

	public void paint(Graphics g) {

	}
	
	public void paintPreview(Graphics g, int offset) {
	}

	public boolean selectpoint(int x, int y) {
		return false;
	}

	public boolean selectobj(int x, int y) {
		return false;
	}

	public boolean selectrect(int xx0, int yy0, int xx1, int yy1) {
		return false;
	}

	public void moveobj(int x, int y) {

	}

	public void movepoint(int x, int y) {

	}

	public boolean setKey(int key, boolean ctrl) {
		return false;
	}

	public Dimension getSize() {
		return new Dimension(0, 0);
	}

	public boolean overlaps(MediaObjects obj) {
		System.out.println("obj size " + obj.getSize().getHeight());
		System.out.println("this size " + this.getSize().getHeight());
		int xA0 = this.positionX;
		int xA1 = (int) (this.positionX + this.getSize().getWidth());
		int yA0 = this.positionY;
		int yA1 = (int) (this.positionY + this.getSize().getHeight());
		int xB0 = obj.positionX;
		int xB1 = (int) (obj.positionX + obj.getSize().getWidth());
		int yB0 = obj.positionY;
		int yB1 = (int) (obj.positionY + obj.getSize().getHeight());

		if (xA0 < xB0 && xA1 > xB0 && yA0 < yB0 && yA1 > yB0) {
			return true;
		}
		if (xA0 < xB1 && xA1 > xB1 && yA0 < yB0 && yA1 > yB0) {
			return true;
		}
		if (xA0 < xB1 && xA1 > xB1 && yA0 < yB1 && yA1 > yB1) {
			return true;
		}
		if (xA0 < xB0 && xA1 > xB0 && yA0 < yB1 && yA1 > yB1) {
			return true;
		}
		return false;
	}

	public MediaObjects getObject(int x, int y) {
		return null;
	}

	public Component getPreviewComponent() {
		return null;
	}

	/**
	 * sets the scale between viewing mode and designing mode
	 * @param scaleToReal scale factor from designing mode size to real size
	 * @param scaleToDesign scale factor from real size to designing size
	 */
	public static void scale(double scaleToReal, double scaleToDesign) {
		// System.out.println("scale " + scaleToReal + " " + scaleToDesign);
		MediaObjects.scaleToDesign = scaleToDesign;
		MediaObjects.scaleToReal = scaleToReal;
	}

	/**
	 * sets the scale between viewing mode and preview mode
	 * @param scaleToPreview scale factor from real size to preview size
	 */
	public static void scale(double scaleToPreview) {
		MediaObjects.scaleToPreview = scaleToPreview;
	}
	
	public void stop() {

	}

	public void setVisible(boolean b) {

	}

	public Component getComponent() {
		return null;
	}

	public void run(GraphicsConfiguration gc, Rectangle bounds) {
	}

	public void stopRun() {
		
	}

	public Element save(Document xmldoc) {
		return null;
	}

	public void setSource(String source) {
		this.source=source;
	}
	
	public String getSource() {
		return this.source;
	}
}
