/*
 * @(#)GUIStatus.java
 * Created: 14-Nov-2005
 * Version: 2-0-alpha
 * Copyright (c) 2005-2006, University of Manchester All rights reserved. 
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials 
 * provided with the distribution. Neither the name of the University of 
 * Manchester nor the names of its contributors may be used to endorse or 
 * promote products derived from this software without specific prior written
 * permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package client.rtsp;

import java.awt.Component;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;


/**
 * A GUI status information window
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class GUIStatus implements StatusInformation {
    
    // The size of spaces in the GUI
    private static final int SPACE_SIZE = 5;

    // The height of the gui
    private static final int HEIGHT = 100;

    // The width of the gui
    private static final int WIDTH = 300;
    
    // The percentage for completion
    private static final int COMPLETE = 100;

    // The dialog box to show
    private JDialog dialog = null;
    
    // The status text
    private JLabel status = new JLabel("");
    
    // The progress bar
    private JProgressBar progress = new JProgressBar(0, COMPLETE);
    
    /**
     * Creates a GUIStatus object
     * @param parent The parent object
     * @param title The title of the status window
     */
    public GUIStatus(Component parent, String title) {
        Frame frame = JOptionPane.getFrameForComponent(parent);
        JPanel content = new JPanel();
        dialog = new JDialog(frame, false);
        dialog.setTitle(title);
        dialog.setSize(WIDTH, HEIGHT);
        dialog.setLocationRelativeTo(parent);
        
        dialog.getContentPane().add(content);
        content.setBorder(BorderFactory.createEmptyBorder(SPACE_SIZE, 
                SPACE_SIZE, SPACE_SIZE, SPACE_SIZE));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        progress.setAlignmentX(Component.CENTER_ALIGNMENT);
        status.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        content.add(Box.createVerticalGlue());
        content.add(progress);
        content.add(Box.createVerticalStrut(SPACE_SIZE));
        content.add(status);
        content.add(Box.createVerticalGlue());
    }

    /**
     * 
     * @see client.rtsp.StatusInformation#setStatusText(java.lang.String)
     */
    public void setStatusText(String text) {
        status.setText(text);
        if (!dialog.isVisible()) {
            dialog.setVisible(true);
        }
    }

    /**
     * 
     * @see client.rtsp.StatusInformation#setPercentComplete(int)
     */
    public void setPercentComplete(int percent) {
        progress.setValue(percent);
        if (percent == COMPLETE) {
            dialog.setVisible(false);
        }
    }
    
    /**
     * Sets the dialog always on top status
     * @param alwaysOnTop
     */
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        dialog.setAlwaysOnTop(alwaysOnTop);
    }

}
