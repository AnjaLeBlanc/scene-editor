/*
 * Copyright (c) 2008, University of Manchester All rights reserved.
 * See LICENCE in root directory of source code for details of the license.
 */

package edance.userinterface;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.media.Effect;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.PlugInManager;
import javax.media.protocol.PushBufferStream;
import javax.media.protocol.recorded.DataSource;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.crew_vre.media.Misc;

import rtpReceiver.RGBRenderer;


/**
 * A Panel for playing recorded video
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class RecordedVideoPanel extends JPanel {


	private static final long serialVersionUID = 1L;

	private RGBRenderer renderer = new RGBRenderer(new Effect[]{});

    private Component component = null;

    private DataSource dataSource = null;

    /**
     * Creates a new Recorded Video Panel
     * @param filename The name of the file to play
     * @throws NoDataSourceException
     * @throws IOException
     */
    public RecordedVideoPanel(String filename)
            throws NoDataSourceException, IOException {
        this(filename, 0, 1.0);
    }

    /**
     * Creates a new Recorded Video Panel
     * @param filename The name of the file to play
     * @param seek The position to start from in milliseconds
     * @throws NoDataSourceException
     * @throws IOException
     */
    public RecordedVideoPanel(String filename, long seek)
            throws NoDataSourceException, IOException {
        this(filename, seek, 1.0);
    }

    /**
     * Creates a new Recorded Video Panel
     * @param filename The name of the file to play
     * @param seek The position to start from in milliseconds
     * @param scale The speed at which to play (1.0 = normal)
     * @throws NoDataSourceException
     * @throws IOException
     */
    public RecordedVideoPanel(String filename, long seek, double scale)
            throws NoDataSourceException, IOException {
        MediaLocator locator = new MediaLocator("recorded://" + filename
                + "?seek=" + seek + "&scale=" + scale);
        System.out.println("locator " + locator);
        dataSource = (DataSource) Manager.createDataSource(locator);
        dataSource.connect();
        PushBufferStream[] streams = dataSource.getStreams();
        renderer.setDataSource(dataSource, 0);
        renderer.setInputFormat(streams[0].getFormat());
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        component = renderer.getComponent();
        add(component);
    }

    /**
     *
     * @see java.awt.Component#setSize(java.awt.Dimension)
     */
    public void setSize(int width, int height) {
        super.setSize(width, height);
        component.setPreferredSize(new Dimension(width, height));
        component.setSize(width, height);
        component.setBounds(0, 0, width, height);
    }

    /**
     *
     * @see java.awt.Component#setBounds(int, int, int, int)
     */
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        component.setPreferredSize(new Dimension(width, height));
        component.setSize(width, height);
        component.setBounds(0, 0, width, height);
    }

    /**
     * Starts the panel
     */
    public void play() {
        renderer.start();
    }

    /**
     * Stops the panel
     */
    public void stop() {
        renderer.stop();
    }

    /**
     * Seeks to a new time
     * @param seek The time to seek to
     */
    public void seek(long seek) {
        dataSource.seek(seek, 1.0);
    }

    /**
     * The main method
     * @param args None
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
//        Misc.addCodec("codecs.h261.H261Encoder");
//        Misc.addCodec("codecs.h261.H261ASEncoder");
//        Misc.addCodec("codecs.h261.H261ASDecoder");
        Misc.addCodec("codecs.rgb.RGB2432Converter");
        Misc.addCodec("codecs.rgb.YUV420RGB32Converter");
        
        Misc.addCodec("net.crew_vre.codec.h261.H261Decoder");
        Misc.addCodec("net.crew_vre.codec.h261.H261ASDecoder");
        Misc.addCodec("net.crew_vre.codec.h261.H261ASEncoder");
        
        PlugInManager.removePlugIn(
        		"com.sun.media.codec.video.h261Decoder", 
        		PlugInManager.CODEC);
        
        PlugInManager.removePlugIn(
                "com.sun.media.codec.video.vcm.NativeDecoder",
                PlugInManager.CODEC);
        PlugInManager.removePlugIn(
                "com.sun.media.codec.video.vcm.NativeEncoder",
                PlugInManager.CODEC);
        PlugInManager.removePlugIn(
                "com.sun.media.codec.video.colorspace.YUVToRGB",
                PlugInManager.CODEC);
        RecordedVideoPanel panel = new RecordedVideoPanel("D:/Anja/edance_newdesign/edance/test/teststream");
        JFrame frame = new JFrame("RecordedVideoPanel");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));
        frame.getContentPane().add(panel);
        panel.setSize(new Dimension(352, 288));
        frame.pack();
        frame.setVisible(true);
        panel.play();
        Thread.sleep(10000);
        System.err.println("Seeking");
        panel.seek(50000);
    }
}
