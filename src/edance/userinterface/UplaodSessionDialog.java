package edance.userinterface;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import edance.sceeneditor.SwordUploadSession;
import edance.sceeneditor.UploadSession;

public class UplaodSessionDialog extends JDialog implements ActionListener {


    /**
     * The default Border Spacing
     */
    public static final int BORDER_WIDTH = 5;

    /**
     * The default Componenet Spacing
     */
    public static final int SPACING = 5;
	
	private static final long serialVersionUID = 1L;
	private static final String CHANGE_DIR_TEXT = "Change Directory";
	private static final String EXPORT_TEXT = "Export";
	private static final String CANCEL_TEXT = "Cancel";
	private JTextField currDir = null;
	private JLabel serverUrlLabel = new JLabel("URL to Media Server");
	private JTextField serverUrl = new JTextField("");
	private JLabel userNameLabel = new JLabel("Username on Server");
//	private JTextField username = new JTextField("crew");
	private JTextField username = new JTextField("");
	private JLabel passwordLabel = new JLabel("Password");
	private JPasswordField password = new JPasswordField("");
	JPanel mainPanel = new JPanel();
    private JButton changeButton = new JButton(CHANGE_DIR_TEXT);
    private JButton exportButton = new JButton(EXPORT_TEXT);
    private JButton cancelButton = new JButton(CANCEL_TEXT);
//  The progress bar
    private JProgressBar progress = new JProgressBar(0, 100);

	public UplaodSessionDialog(String dir) {
		super((Frame)null, "Upload Recording Dialog");
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setPreferredSize(new Dimension(400,180));
		this.setLocationRelativeTo(null);
		mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.Y_AXIS));
		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
		content.add(new JLabel("Recording Directory: "));
        currDir = new JTextField();
        currDir.setAlignmentX(Component.CENTER_ALIGNMENT);
        currDir.setText(dir);
        currDir.setEditable(false);
        changeButton.addActionListener(this);
        exportButton.addActionListener(this);
        cancelButton.addActionListener(this);
        content.add(currDir);
        content.add(Box
                .createRigidArea(new Dimension(SPACING, 0)));
        content.add(changeButton);
        content.setBorder(BorderFactory.createEmptyBorder(BORDER_WIDTH,
                BORDER_WIDTH, BORDER_WIDTH,
                BORDER_WIDTH));
        mainPanel.add(content);
        
//      Add the fields for server settings
        JPanel serverPanel = new JPanel();
        serverPanel.setLayout(new GridLayout(0,2,5,5));
        serverPanel.setBorder(BorderFactory.createEmptyBorder(BORDER_WIDTH,
                BORDER_WIDTH, BORDER_WIDTH,
                BORDER_WIDTH));
        serverUrlLabel.setPreferredSize(new Dimension(50,20));
        userNameLabel.setPreferredSize(new Dimension(50,20));
        passwordLabel.setPreferredSize(new Dimension(50,20));
        serverUrl.setSize(new Dimension(50,20));
        username.setPreferredSize(new Dimension(50,20));
        password.setPreferredSize(new Dimension(50,20));
        serverPanel.add(serverUrlLabel);
        serverPanel.add(serverUrl);
        serverPanel.add(userNameLabel);
        serverPanel.add(username);
        serverPanel.add(passwordLabel);
        serverPanel.add(password);
        mainPanel.add(serverPanel);
        
//      Add the buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        exportButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(exportButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(
                SPACING, 0)));
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(cancelButton);
        buttonPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(BORDER_WIDTH,
                BORDER_WIDTH, BORDER_WIDTH,
                BORDER_WIDTH));
        mainPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(buttonPanel);
        this.add(mainPanel);
        this.pack();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(CANCEL_TEXT)) {
            this.dispose();
        } else if (e.getActionCommand().equals(CHANGE_DIR_TEXT)){

        	JFileChooser chooser = new JFileChooser();
        	chooser.setCurrentDirectory(new File(currDir.getText()));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = chooser.showOpenDialog(chooser);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    currDir.setText(file.getCanonicalPath());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        } else if(e.getActionCommand().equals(EXPORT_TEXT)) {
        	mainPanel.add(progress,BorderLayout.SOUTH);
        	SwordUploadSession sus = new SwordUploadSession(new File(currDir.getText()), serverUrl.getText(),
        			username.getText(),password.getPassword());
        	if(sus.isSwordServer()){
        		try {
        			sus.exportSession();
        		}
        		finally {
    				this.dispose();
    			}
        		return;
        	}
        	UploadSession us = new UploadSession(new File(currDir.getText()), serverUrl.getText(),
        			username.getText(),password.getPassword());
        	try {
        		us.setProgressBar(progress);
				us.exportSession();
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(this, "The following error has occured: \n "
						+e1.getMessage(), "Error during Export", JOptionPane.ERROR_MESSAGE);
				e1.printStackTrace();
			} finally {
				this.dispose();
			}
        }
		
	}

}
