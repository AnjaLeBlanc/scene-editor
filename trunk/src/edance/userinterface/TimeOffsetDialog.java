package edance.userinterface;

import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TimeOffsetDialog extends JDialog 
	implements ActionListener, PropertyChangeListener{

	private static final long serialVersionUID = 1L;
		private Date typedText = null;
	    private JFormattedTextField textField;

	    private JOptionPane optionPane;

	    private String btnString1 = "OK";
	    private String btnString2 = "Cancel";

	    /**
	     * Returns entered time offset in seconds
	     */
	    public long getTimeOffset() {
	    	System.out.println("date " + typedText.toString() + " time " + typedText.getTime());
	    	//Java calculates in BST!!! -> one hour correction necessary
	        return (typedText.getTime() + 3600000);
	    }

	    /** Creates the reusable dialog. */
	    public TimeOffsetDialog(Frame aFrame) {
	        super(aFrame, true);

	        setTitle("Set Replay offset");

	        textField = new JFormattedTextField(new SimpleDateFormat("HH:mm:ss"));
	        textField.setText("00:00:00");

	        String msgString1 = "Set the time offset (HH:mm:ss)";
	        Object[] array = {msgString1, textField};

	        Object[] options = {btnString1, btnString2};

	        //Create the JOptionPane.
	        optionPane = new JOptionPane(array,
	                                    JOptionPane.QUESTION_MESSAGE,
	                                    JOptionPane.YES_NO_OPTION,
	                                    null,
	                                    options,
	                                    options[0]);

	        //Make this dialog display it.
	        setContentPane(optionPane);

	        //Handle window closing correctly.
	        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	        addWindowListener(new WindowAdapter() {
	                public void windowClosing(WindowEvent we) {
	                /*
	                 * Instead of directly closing the window,
	                 * we're going to change the JOptionPane's
	                 * value property.
	                 */
	                    optionPane.setValue(new Integer(
	                                        JOptionPane.CLOSED_OPTION));
	            }
	        });

	        //Ensure the text field always gets the first focus.
	        addComponentListener(new ComponentAdapter() {
	            public void componentShown(ComponentEvent ce) {
	                textField.requestFocusInWindow();
	            }
	        });

	        //Register an event handler that puts the text into the option pane.
	        textField.addActionListener(this);

	        //Register an event handler that reacts to option pane state changes.
	        optionPane.addPropertyChangeListener(this);
	        this.pack();
	    }

	    /** This method handles events for the text field. */
	    public void actionPerformed(ActionEvent e) {
	        optionPane.setValue(btnString1);
	    }

	    /** This method reacts to state changes in the option pane. */
	    public void propertyChange(PropertyChangeEvent e) {
	        String prop = e.getPropertyName();

	        if (isVisible()
	         && (e.getSource() == optionPane)
	         && (JOptionPane.VALUE_PROPERTY.equals(prop) ||
	             JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
	            Object value = optionPane.getValue();

	            if (value == JOptionPane.UNINITIALIZED_VALUE) {
	                //ignore reset
	                return;
	            }

	            //Reset the JOptionPane's value.
	            //If you don't do this, then if the user
	            //presses the same button next time, no
	            //property change event will be fired.
	            optionPane.setValue(
	                    JOptionPane.UNINITIALIZED_VALUE);

	            if (btnString1.equals(value)) {
                    typedText = (Date)textField.getValue();
	                System.out.println("typed Text " + typedText);
	                clearAndHide();
	                
	            } else {
	                typedText = null;
	                clearAndHide();
	            }
	        }
	    }

	    /** This method clears the dialog and hides it. */
	    public void clearAndHide() {
	        textField.setText(null);
	        setVisible(false);
	    }
}
