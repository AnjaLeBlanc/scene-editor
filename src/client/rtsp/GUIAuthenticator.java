/*
 * @(#)GUIAuthenticator.java
 * Created: 15-Jun-2005
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
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/**
 * Gets user authentication information
 * 
 * @author Andrew G D Rowley
 * @version 2-0-alpha
 */
public class GUIAuthenticator implements Authenticator {

    // The prompt to display to get a username
    private static final String USERNAME_PROMPT = "Enter Username:";

    // The title to display for authentication windows
    private static final String WINDOW_TITLE = "Authentication";

    // The prompt to display when the password is incorrect
    private static final String INCORRECT_PASSWORD_PROMPT = 
        "Incorrect username or password.  Retry?";

    // The prompt to display to get a password
    private static final String PASSWORD_PROMPT = "Enter Password:";

    // The character to display for password input
    private static final char PASSWORD_CHAR = '*';

    // The length of the password field
    private static final int PASSWORD_LENGTH = 10;

    // The size of dialog spaces
    private static final int SPACE_SIZE = 5;

    // The client username
    private String username = "";

    // True if the username and password have been asked for
    private boolean gotAuth = false;

    // The Password pane
    private JPanel passwordPanel = null;

    // The Password field
    private JPasswordField passwordField = null;

    // The parent component
    private Component parent = null;

    /**
     * Creates a new GUI Authenticator
     * 
     * @param parent
     *            The Parent Component
     */
    public GUIAuthenticator(Component parent) {
        this.parent = parent;

        // Setup a password box
        passwordPanel = new JPanel();
        passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.Y_AXIS));
        passwordField = new JPasswordField(PASSWORD_LENGTH);
        passwordField.setEchoChar(PASSWORD_CHAR);
        passwordPanel.add(new JLabel(PASSWORD_PROMPT));
        passwordPanel.add(Box.createRigidArea(
                new Dimension(SPACE_SIZE, SPACE_SIZE)));
        passwordPanel.add(passwordField);
    }

    /**
     * @see client.rtsp.Authenticator#requestAuthParameters(boolean)
     */
    public boolean requestAuthParameters(boolean newRequest) {
        if (!newRequest) {
            gotAuth = false;
            if (JOptionPane.showConfirmDialog(parent,
                    INCORRECT_PASSWORD_PROMPT,
                    WINDOW_TITLE, JOptionPane.YES_NO_OPTION)
                        == JOptionPane.NO_OPTION) {
                return false;
            }
        }

        if (!gotAuth || !newRequest) {
            int value = 0;
            JOptionPane password = new JOptionPane(passwordPanel,
                    JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            JDialog dialog = password.createDialog(parent, WINDOW_TITLE);
            username = JOptionPane.showInputDialog(parent, USERNAME_PROMPT, 
                    WINDOW_TITLE, JOptionPane.QUESTION_MESSAGE);
            if (username == null) {
                return false;
            }
            passwordField.setText("");
            
            dialog.setVisible(true);
            if (password.getValue() == null) {
                return false;
            }

            value = ((Integer) password.getValue()).intValue();
            if (value != JOptionPane.OK_OPTION) {
                return false;
            }
            gotAuth = true;
        }

        return true;
    }

    /**
     * @see client.rtsp.Authenticator#getUsername()
     */
    public String getUsername() {
        return username;
    }

    /**
     * @see client.rtsp.Authenticator#getPassword()
     */
    public String getPassword() {
        return new String(passwordField.getPassword());
    }

}
