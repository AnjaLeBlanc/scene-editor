package net.sf.fmj.media;

import java.awt.Component;
import java.awt.Rectangle;

import javax.media.renderer.VideoRenderer;

/**
 * Abstract implementation of VideoRenderer, useful for subclassing.
 * @author Ken Larson
 *
 */
public abstract class AbstractVideoRenderer extends AbstractRenderer implements VideoRenderer
{

	private Rectangle bounds = null;
	
	public Rectangle getBounds()
	{
		return bounds;
	}

	public abstract Component getComponent();

	public void setBounds(Rectangle rect)
	{	this.bounds = rect;
	}

	public boolean setComponent(Component comp)
	{
		// default implementation does not allow changing of component.
		return false;
	}
	


}
