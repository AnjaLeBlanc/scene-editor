package edance.userinterface;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.text.DateFormatter;

import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;

import edance.sceeneditor.MetaData;

public class MetaDataDialog extends JDialog implements ActionListener {

	public static final String CONFERENCE = "http://www.eswc2006.org/technologies/ontology#ConferenceEvent";
	public static final String SESSION = "http://www.eswc2006.org/technologies/ontology#hasProgram";
	public static final String VENUE = "http://www.eswc2006.org/technologies/ontology#hasLocation";
	public static final String STOPDATETIME = "http://www.eswc2006.org/technologies/ontology#hasStopDateTime";
	public static final String STARTDATETIME = "http://www.eswc2006.org/technologies/ontology#hasStartDateTime";
	public static final String SPEAKER = "http://www.eswc2006.org/technologies/ontology#heldBy";
	public static final String TITLE = "http://purl.org/dc/elements/1.1/title";
	public static final String CHOREOGRAPHER = "http://grace.rcs.manchester.ac.uk/edance/edance.xsd#choreographer";
	
	private static final long serialVersionUID = 1L;
	private static final String[] fields= new String[]{"Title","Creator","Date dd/mm/yyyy", 
		"Starting time hh:mm", "Finishing Time hh:mm", "Venue", "Session", "Conference"};
	//{displayname,short namespace name, url}
	public static final String[][] additionalFields = new String[][]{{"Number of Acts","edance:acts","http://grace.rcs.manchester.ac.uk/edance/edance.xsd#noacts"},
			{"Performers","edance:performer","http://grace.rcs.manchester.ac.uk/edance/edance.xsd#performer"}, 
			{"other Locations","edance:location","http://grace.rcs.manchester.ac.uk/edance/edance.xsd#location"},
			{"Number of cameras","edance:nr-cameras","http://grace.rcs.manchester.ac.uk/edance/edance.xsd#nr-cameras"}, 
			{"Video Stream Source","edance:name-of-video","http://grace.rcs.manchester.ac.uk/edance/edance.xsd#name-of-video"}, 
			{"Name of prerecorded streams","edance:name-of-video","http://grace.rcs.manchester.ac.uk/edance/edance.xsd#name-of-video"},	
			{"Performance Type","edance:type","http://grace.rcs.manchester.ac.uk/edance/edance.xsd#type"}, 
			{"Audience Spaces","edance:audience","http://grace.rcs.manchester.ac.uk/edance/edance.xsd#audience"}, 
			{"Camera Starting Position","edance:camstart","http://grace.rcs.manchester.ac.uk/edance/edance.xsd#camstart"}, 
			{"Camera Technical Data","edance:camtech","http://grace.rcs.manchester.ac.uk/edance/edance.xsd#camtech"}, 
			{"IPR statement","dc:rights","http://purl.org/dc/elements/1.1/rights"},
			{"Comments","edance:comments","http://grace.rcs.manchester.ac.uk/edance/edance.xsd#comments"}};
	private static enum metaData {TITLE, SPEAKER, DATE, STARTTIME, STOPTIME,
		VENUE,SESSION,CONFERENCE};
	private static JTextField[] metadata = new JTextField[fields.length];
	private static JTextArea[] additionalMetaData = new JTextArea[additionalFields.length]; 
	private static JScrollPane[] amdsp = new JScrollPane[additionalFields.length];
	private JComboBox additionalMetaDataSel = new JComboBox();
	private String directory;
	private static final String metadatafile="/metadata.txt";
	private static final JButton save = new JButton("Save");
	private static final JButton cancel= new JButton("Cancel");
	private final JPanel content = new JPanel();
	private final JPanel metaDataPanel= new JPanel();
	private final JPanel buttonPanel1 = new JPanel();
	private static boolean returnvalue = false;

	public MetaDataDialog(Container parent, String dir) {
		super(JOptionPane.getFrameForComponent(parent),
                "Meta Data Capture", true);
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		this.directory=dir;
		for(int i = 0; i<additionalFields.length; i++) {
			additionalMetaDataSel.addItem(additionalFields[i][0]);
		}
		
		SpringLayout layout=new SpringLayout();
		content.setLayout(layout);
		for(int i=0; i<fields.length;i++){
			JLabel l = new JLabel(fields[i], JLabel.TRAILING);
            content.add(l);
            if(i==metaData.DATE.ordinal()){
            	metadata[i]= new JFormattedTextField(new DateFormatter(
            			new SimpleDateFormat("dd/MM/yyyy"))); 
            	((JFormattedTextField)metadata[i]).setValue(new Date());
            }
            else if(i==metaData.STARTTIME.ordinal() || i == metaData.STOPTIME.ordinal()){
            	metadata[i]= new JFormattedTextField(new DateFormatter(
            			new SimpleDateFormat("hh:mm",new DateFormatSymbols())));
            	if(i==metaData.STARTTIME.ordinal()){
            		metadata[i].setText("10:00");
            	}else{
            		metadata[i].setText("11:00");
            	}
            }
            else {
            	metadata[i] = new JTextField(10);
            }
            l.setLabelFor(metadata[i]);
            content.add(metadata[i]);
		}
		
		for(int i=0; i<additionalMetaData.length; i++){
			additionalMetaData[i] = new JTextArea(5,20);
			amdsp[i] = new JScrollPane(additionalMetaData[i]);
		}
		JPanel comboPanel = new JPanel(new BorderLayout());
		comboPanel.add(additionalMetaDataSel, BorderLayout.NORTH);
		additionalMetaDataSel.setSelectedIndex(0);
		additionalMetaDataSel.addActionListener(this);
		content.add(comboPanel);
		content.add(amdsp[0]);
		makeCompactGrid(content,content.getComponentCount()/2,2,6,6,6,6);
		save.addActionListener(this);
		cancel.addActionListener(this);
		buttonPanel1.setLayout(new FlowLayout(FlowLayout.CENTER,6,6));
		buttonPanel1.add(save);
		buttonPanel1.add(cancel);
		metaDataPanel.setLayout(new BorderLayout());
		metaDataPanel.add(content, BorderLayout.CENTER);
		File saveFile= new File(this.directory.concat(metadatafile));
		try {
			System.out.println("file " + saveFile.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(saveFile.exists())
		{
			loadFile(saveFile);
		}
		metaDataPanel.add(buttonPanel1, BorderLayout.SOUTH);
		this.getContentPane().add(metaDataPanel);
		this.doLayout();
        this.pack();
	}
		
	public void saveMetaData(File dir){
		
	}
	
    /* Used by makeCompactGrid. */
    private static SpringLayout.Constraints getConstraintsForCell(
                                                int row, int col,
                                                Container parent,
                                                int cols) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        Component c = parent.getComponent(row * cols + col);
        return layout.getConstraints(c);
    }


    /**
     * Aligns the first <code>rows</code> * <code>cols</code>
     * components of <code>parent</code> in
     * a grid. Each component in a column is as wide as the maximum
     * preferred width of the components in that column;
     * height is similarly determined for each row.
     * The parent is made just big enough to fit them all.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param initialX x location to start the grid at
     * @param initialY y location to start the grid at
     * @param xPad x padding between cells
     * @param yPad y padding between cells
     */
    public static void makeCompactGrid(Container parent,
                                       int rows, int cols,
                                       int initialX, int initialY,
                                       int xPad, int yPad) {
        SpringLayout layout;
        try {
            layout = (SpringLayout)parent.getLayout();
        } catch (ClassCastException exc) {
            System.err.println("The first argument to makeCompactGrid must use SpringLayout.");
            return;
        }

        //Align all cells in each column and make them the same width.
        Spring x = Spring.constant(initialX);
        for (int c = 0; c < cols; c++) {
            Spring width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width,
                                   getConstraintsForCell(r, c, parent, cols).
                                       getWidth());
            }
            for (int r = 0; r < rows; r++) {
                SpringLayout.Constraints constraints =
                        getConstraintsForCell(r, c, parent, cols);
                constraints.setX(x);
                constraints.setWidth(width);
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
        }

        //Align all cells in each row and make them the same height.
        Spring y = Spring.constant(initialY);
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            for (int c = 0; c < cols; c++) {
                height = Spring.max(height,
                                    getConstraintsForCell(r, c, parent, cols).
                                        getHeight());
            }
            for (int c = 0; c < cols; c++) {
                SpringLayout.Constraints constraints =
                        getConstraintsForCell(r, c, parent, cols);
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
        }

        //Set the parent's size.
        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.SOUTH, y);
        pCons.setConstraint(SpringLayout.EAST, x);
    }

	public void setDirectory(String dir) {
		this.directory=dir;
		File saveFile= new File(this.directory.concat(metadatafile));

		if(saveFile.exists())
		{
			loadFile(saveFile);
		}
		
		metaDataPanel.doLayout();
		this.pack();
	}

	public void actionPerformed(ActionEvent event) {
		System.out.println("action command " +event.getActionCommand());
		if(event.getSource() == additionalMetaDataSel){ 
			content.remove(content.getComponentCount()-1);
			content.add(amdsp[additionalMetaDataSel.getSelectedIndex()]);
			makeCompactGrid(content,content.getComponentCount()/2,2,6,6,6,6);
			content.validate();
			amdsp[additionalMetaDataSel.getSelectedIndex()].repaint();
			return;
		}
		
		if(event.getActionCommand().compareTo(cancel.getText())==0) {
			returnvalue = false;
		}
		if((event.getActionCommand().compareTo(save.getText())==0)) {
			File file = new File(this.directory.concat(metadatafile));
			if(file.exists()){
				file.delete();
			}
			saveFile(file);
			returnvalue = true;
		}			
		this.setVisible(false);
		return;
	}
	
	private void saveFile(File file){
		try {
			boolean ret=file.createNewFile();
			System.out.println("ret value " + ret + "  "  + file.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		MetaData md= new MetaData();
		md.setConference(metadata[metaData.CONFERENCE.ordinal()].getText());
		md.setSpeaker(metadata[metaData.SPEAKER.ordinal()].getText());
		md.setVenue(metadata[metaData.VENUE.ordinal()].getText());
		md.setSession(metadata[metaData.SESSION.ordinal()].getText());
		md.setTitle(metadata[metaData.TITLE.ordinal()].getText());
		for(int i = 0; i<additionalFields.length; i++){
			if(additionalMetaData[i].getText().length() > 0 ){
				md.setOtherMetadata(additionalFields[i][1], additionalMetaData[i].getText());
			}
		}
		Date d=(Date) ((JFormattedTextField)metadata[metaData.DATE.ordinal()]).getValue();
		Date s=(Date) ((JFormattedTextField)metadata[metaData.STARTTIME.ordinal()]).getValue();
		Date en=(Date) ((JFormattedTextField)metadata[metaData.STOPTIME.ordinal()]).getValue();
		Calendar c= Calendar.getInstance();
		c.setTime(d);
		Calendar cs= Calendar.getInstance();
		cs.setTime(s);
		Calendar ce= Calendar.getInstance();
		ce.setTime(en);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		c.set(Calendar.HOUR_OF_DAY, cs.get(Calendar.HOUR_OF_DAY));
		c.set(Calendar.MINUTE, cs.get(Calendar.MINUTE));
		md.setStartDateTime(c);
		Calendar c2=(Calendar) c.clone();
		c2.set(Calendar.HOUR_OF_DAY, ce.get(Calendar.HOUR_OF_DAY));
		c2.set(Calendar.MINUTE, ce.get(Calendar.MINUTE));
		md.setStopDateTime(c2);
		
		System.out.println(ce + "\n " + cs+ "\n " +c2 );
//	Old stuff, produces just standard XML not RDF XML		
//		Class[] classes= new Class[]{MetaData.class};
//		try {
//			JAXBContext jc= JAXBContext.newInstance(classes);
//			Marshaller m = jc.createMarshaller();
//			m.setProperty("jaxb.formatted.output", Boolean.TRUE);
//			try {
//				m.marshal(md, new FileOutputStream(file));
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}
//		} catchd= (XSDDateTime)  (JAXBException e) {
//			e.printStackTrace();
//		}
		Model model = ModelFactory.createDefaultModel();
		Resource resource = model.createResource("http://www.crew-vre.net/session#12043924352435");
		Property title = model.createProperty(TITLE);
		resource.addProperty(title, md.title);
		Property speaker = model.createProperty(SPEAKER);
		resource.addProperty(speaker, md.speaker);
		Literal lit = resource.getModel().createTypedLiteral(md.startDateTime);
		Property startdatetime = model.createProperty(STARTDATETIME);
		resource.addProperty(startdatetime, lit/*md.startDateTime.getTime()*/);
		lit = resource.getModel().createTypedLiteral(md.stopDateTime);
		Property stopdatetime = model.createProperty(STOPDATETIME);
		resource.addProperty(stopdatetime, lit/*md.stopDateTime.getTime()*/);
		Property venue = model.createProperty(VENUE);
		resource.addProperty(venue, md.venue);
		Property session = model.createProperty(SESSION);
		resource.addProperty(session, md.session);
		Property conference = model.createProperty(CONFERENCE);
		resource.addProperty(conference, md.conference);
		for(int i=0;i<additionalFields.length;i++){
			if(md.getOtherMetadata(additionalFields[i][1]) != null){
				Property addField = model.createProperty(additionalFields[i][2]);
				resource.addProperty(addField, md.getOtherMetadata(additionalFields[i][1]));
			}
		}
		model.write(System.out);
		try {
			model.write(new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void loadFile(File file){
		Model model = ModelFactory.createDefaultModel();
		Model modelread;
		try {
			modelread = model.read(new FileInputStream(file),"");
			modelread.write(System.out);
			ResIterator iterator = modelread.listSubjects();
			while(iterator.hasNext()){
				Resource resource = modelread.getResource(((ResourceImpl)iterator.next()).getURI());
				
				metadata[metaData.TITLE.ordinal()].setText(resource.getProperty(
						modelread.getProperty(TITLE)).getObject().toString());
				metadata[metaData.SPEAKER.ordinal()].setText(resource.getProperty(
						modelread.getProperty(SPEAKER)).getObject().toString());
				metadata[metaData.VENUE.ordinal()].setText(resource.getProperty(
						modelread.getProperty(VENUE)).getObject().toString());
				metadata[metaData.SESSION.ordinal()].setText(resource.getProperty(
						modelread.getProperty(SESSION)).getObject().toString());
				metadata[metaData.CONFERENCE.ordinal()].setText(resource.getProperty(
						modelread.getProperty(CONFERENCE)).getObject().toString());
				for(int i =0;i<additionalFields.length;i++){
					if(resource.getProperty(modelread.getProperty(additionalFields[i][2])) !=null){
						additionalMetaData[i].setText(resource.getProperty(modelread.getProperty(
								additionalFields[i][2])).getObject().toString());
					}
				}
				XSDDateTime d= (XSDDateTime) resource.getProperty(modelread.getProperty(STARTDATETIME)).getLiteral().getValue();
				((JFormattedTextField)metadata[metaData.DATE.ordinal()]).setValue(d.asCalendar().getTime());
				((JFormattedTextField)metadata[metaData.STARTTIME.ordinal()]).setValue(d.asCalendar().getTime());
				d= (XSDDateTime) resource.getProperty(modelread.getProperty(STOPDATETIME)).getLiteral().getValue();
				((JFormattedTextField)metadata[metaData.STOPTIME.ordinal()]).setValue(d.asCalendar().getTime());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	public boolean getReturn() {
		return returnvalue;
	}

}
