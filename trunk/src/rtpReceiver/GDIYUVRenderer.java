/*
 * @(#)GDIRenderer.java	1.28 03/04/23
 *
 * Copyright (c) 1996-2002 Sun Microsystems, Inc.  All rights reserved.
 */

package rtpReceiver;

import javax.media.*;
import javax.media.format.*;
import javax.media.renderer.VideoRenderer;
import java.awt.*;
import com.sun.media.*;

/**
 * A renderer using GDI that takes YUV
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class GDIYUVRenderer implements VideoRenderer {

    private static boolean available = false;

    static {
        try {
            JMFSecurityManager.loadLibrary("libgdi");
            available = true;
        } catch (Exception e) {
            // Does Nothing
        } catch (UnsatisfiedLinkError ule) {
            // Does Nothing
        }
    }

    private int handle = 0;

    private int blitter = 0;

    private Component component = new Canvas();

    private YUVFormat inputFormat = null;

    private synchronized native boolean gdiInitialize();
    private synchronized native boolean gdiDraw(Object data,
                        int srcWidth, int srcHeight,
                        int dstWidth, int dstHeight,
                        int windowHandle,
                        int yOffset, int uOffset, int vOffset,
                        int yStride, int uvStride);
    private synchronized native boolean gdiFree();

    /**
     * Creates a new GDIYUVRenderer
     */
    public GDIYUVRenderer() {
        if (available) {
            if (!gdiInitialize()) {
                System.err.println("GDIRenderer. gdiInitialize() failed");
                available = false;
            }
        }
    }

    /**
     *
     * @see javax.media.PlugIn#open()
     */
    public void open() throws ResourceUnavailableException {
        if (!available) {
            throw new ResourceUnavailableException("GDI not available !!!");
        }
        handle = 0;
        if (blitter == 0) {
            gdiInitialize();
        }
        if (blitter == 0) {
            throw new ResourceUnavailableException("GDIRenderer couldn't open");
        }
    }

    /**
     *
     * @see javax.media.PlugIn#reset()
     */
    public synchronized void reset() {
        handle = 0;
    }

    /**
     *
     * @see javax.media.PlugIn#close()
     */
    public void close() {
        if (available) {
            gdiFree();
        }
    }

    /**
     * Set the data input format.
     * @return false if the format is not supported.
     */
    public Format setInputFormat(Format format) {
        if (!available) {
            return null;
        }
        if (format instanceof YUVFormat) {
            YUVFormat yuv = (YUVFormat) format;
            if (yuv.getYuvType() == YUVFormat.YUV_420) {
                inputFormat = yuv;
                //component.setSize(inputFormat.getSize());
                component.setPreferredSize(inputFormat.getSize());
                return format;
            }
        }
        return null;
    }

    /**
     *
     * @see javax.media.Renderer#process(javax.media.Buffer)
     */
    public int process(Buffer buffer) {
        if (!available || component == null)
            return BUFFER_PROCESSED_OK;
        if (handle == 0)
            handle = com.sun.media.util.WindowUtil.getWindowHandle(component);
        if (handle == 0) {
            return BUFFER_PROCESSED_OK;
        }
        if (!buffer.getFormat().equals(inputFormat)) {
            if (setInputFormat(buffer.getFormat()) == null) {
                return BUFFER_PROCESSED_FAILED;
            }
        }

        byte[] data = (byte []) buffer.getData();

        if (data == null) {
            return BUFFER_PROCESSED_OK;
        }
        Dimension outSize = component.getSize();
        Dimension inSize = inputFormat.getSize();
        if (outSize.width > 0 && outSize.height > 0) {
            int returned = (gdiDraw(data, inSize.width, inSize.height,
                    outSize.width, outSize.height, handle,
                    inputFormat.getOffsetY(), inputFormat.getOffsetU(),
                    inputFormat.getOffsetV(),
                    inputFormat.getStrideY(), inputFormat.getStrideUV()))?
            BUFFER_PROCESSED_OK : BUFFER_PROCESSED_FAILED;
            return returned;
        }
        return BUFFER_PROCESSED_OK;
    }

    /**
     *
     * @see javax.media.renderer.VideoRenderer#getBounds()
     */
    public Rectangle getBounds() {
        return component.getBounds();
    }

    /**
     *
     * @see javax.media.renderer.VideoRenderer#getComponent()
     */
    public Component getComponent() {
        return component;
    }

    /**
     *
     * @see javax.media.renderer.VideoRenderer#setBounds(java.awt.Rectangle)
     */
    public void setBounds(Rectangle r) {
        component.setBounds(r);
    }

    /**
     *
     * @see javax.media.renderer.VideoRenderer#setComponent(java.awt.Component)
     */
    public boolean setComponent(Component comp) {
        return false;
    }

    /**
     *
     * @see javax.media.Renderer#getSupportedInputFormats()
     */
    public Format[] getSupportedInputFormats() {
        return new Format[]{new YUVFormat(null, Format.NOT_SPECIFIED,
                Format.byteArray, Format.NOT_SPECIFIED,
                YUVFormat.YUV_420, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED, Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED)};
    }

    /**
     *
     * @see javax.media.Renderer#start()
     */
    public void start() {
        // Does Nothing
    }

    /**
     *
     * @see javax.media.Renderer#stop()
     */
    public void stop() {
        // Does Nothing
    }

    /**
     *
     * @see javax.media.PlugIn#getName()
     */
    public String getName() {
        return "GDIYUVRenderer";
    }

    /**
     *
     * @see javax.media.Controls#getControl(java.lang.String)
     */
    public Object getControl(String className) {
        return null;
    }

    /**
     *
     * @see javax.media.Controls#getControls()
     */
    public Object[] getControls() {
        return new Object[0];
    }
}
