package edance.userinterface;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TransparencyDialog extends JDialog implements 
	PropertyChangeListener{

	private static final long serialVersionUID = 1L;
	    private JSlider slider = new JSlider(0,100,0);;

	    private JOptionPane optionPane;

	    private String btnString1 = "OK";

	    /** Creates the reusable dialog. */
	    public TransparencyDialog(Frame aFrame) {
	        super(aFrame, true);

	        setTitle("Set Transparency");
	        
//	        slider.createStandardLabels(50);
	        slider.setPaintLabels(true);

	        String msgString1 = "Set the transparency";
	        Object[] array = {msgString1, slider};

	        Object[] options = {btnString1};

	        //Create the JOptionPane.
	        optionPane = new JOptionPane(array,
	                                    JOptionPane.QUESTION_MESSAGE,
	                                    JOptionPane.OK_OPTION,
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
	                slider.requestFocusInWindow();
	            }
	        });

	        //Register an event handler that reacts to option pane state changes.
	        optionPane.addPropertyChangeListener(this);
	        this.pack();
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

	            clearAndHide();
	        }
	    }

	    /** This method clears the dialog and hides it. */
	    public void clearAndHide() {
	        setVisible(false);
	    }
	    
	    public void registerChangeListener(ChangeListener l) {
	    	slider.addChangeListener(l);
	    }
	    
	    public void unregisterChangeListener(ChangeListener l) {
	    	slider.removeChangeListener(l);
	    }

		public void setTransparencyValue(int transparencyValue) {
			slider.setValue(transparencyValue);
		}

}
