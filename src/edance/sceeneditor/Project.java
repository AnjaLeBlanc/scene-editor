package edance.sceeneditor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edance.devicemanagement.StreamManagement;
import edance.userinterface.DesignPanel;

public class Project extends Object{
	private File saveFile = null;
	private File saveDir = null;
	private Vector<Location> locations;
	private Location currLocation;
	private StreamManagement sm = null;
	
	public Project(StreamManagement sm) {
		this.sm = sm;
		locations=new Vector<Location>();
		Location l= new Location("Location 1", sm);
		this.addLocation(l);
		currLocation=l;
		changed=true;
		
	}
		
	public void save() throws IOException {
		JFileChooser chooser;
		if(saveFile==null) {
			chooser= new JFileChooser();
		} else if(saveDir!=null){
			chooser = new JFileChooser(saveDir);
		} else {
			chooser= new JFileChooser(saveFile);
		}
	    FileNameExtensionFilter filter = new FileNameExtensionFilter(
	        "(*.ecf) e-dance config file", "ecf");
	    chooser.setFileFilter(filter);
	    chooser.setDialogTitle("Save project");
	    chooser.setApproveButtonText("Save");
	    chooser.setApproveButtonToolTipText("press to save using file name provided");
	    int returnVal = chooser.showOpenDialog(null);
	    if(returnVal == JFileChooser.APPROVE_OPTION) {
	            saveFile=chooser.getSelectedFile();
	            if(saveFile.getName().endsWith(".ecf")==false){
	            	saveFile=new File(saveFile.getCanonicalPath().concat(".ecf"));
	            }
	            saveDir=saveFile.getParentFile();
	    }
	    else {
	    	return;
	    }
		PrintWriter pw= new PrintWriter(new BufferedWriter(new FileWriter(saveFile)));

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			DOMImplementation impl = builder.getDOMImplementation();
			Document xmldoc = impl.createDocument(null, "EDANCE", null);
			Element root = xmldoc.getDocumentElement();
			for(int i=0;i<locations.size();i++) {
				Element e = locations.get(i).save(xmldoc);
				if (e != null) {
					root.appendChild(e);
				}
			}
			DOMSource domSource = new DOMSource(xmldoc);
			StreamResult streamResult = new StreamResult(pw);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer serializer = tf.newTransformer();
			serializer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
//			serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "edance.dtd");
			serializer.setOutputProperty(OutputKeys.INDENT, "yes");
			serializer.transform(domSource, streamResult);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			pw.close();
			return;
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
			pw.close();
		} catch (TransformerException e) {
			e.printStackTrace();
			pw.close();
		}
		
		pw.flush();
		pw.close();
		changed=false;
	}
	
	public void load() {
		JFileChooser chooser;
		if(saveFile==null) {
			chooser= new JFileChooser();
		}
		else {
			chooser= new JFileChooser(saveFile);
		}
	    FileNameExtensionFilter filter = new FileNameExtensionFilter(
	        "(*.ecf) e-dance config file", "ecf");
	    chooser.setFileFilter(filter);
	    int returnVal = chooser.showOpenDialog(null);
	    if(returnVal == JFileChooser.APPROVE_OPTION) {
	    		locations.clear();
	    		currLocation =  null;
	            saveFile=chooser.getSelectedFile();
	            loadFile(saveFile);
	    }
	    else {
	    	return;
	    }
	    
		changed=false;
	}
	
	public void attachStreams(DesignPanel dp) {
		for (int i =0; i < locations.size(); i++){
			locations.get(i).attachStreams(dp);
		}
	}

	private void loadFile(File saveFile2) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(saveFile2);
			Element element = doc.getDocumentElement();
			NodeList nodeList = element.getElementsByTagName("LOCATION");
			for (int i = 0; i < nodeList.getLength(); i++){
				Node node = nodeList.item(i); 
				currLocation = new Location(node, sm);
				locations.add(currLocation);
			}
			currLocation = locations.firstElement();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addLocation(Location location) {
		locations.add(location);
		changed=true;
	}

	public Location getCurrLocation() {
		return currLocation;
	}
	
	public Vector<Location> getLocations() {
		return locations;
	}


	public void stopTransmitting() {
		for(int i=0;i<locations.size();i++) {
			locations.get(i).stopTransmitting();
		}
	}

	public Scene createNewLocalScene(StreamManagement sm) {
		changed=true;
		return locations.firstElement().newScene(sm);
	}

	public void showNextScene() {
		locations.firstElement().showNextScene();
		
	}

	public void showPreviousScene() {
		locations.firstElement().showPreviousScene();
	}

	/**
	 * returns the directory into which the project should be saved
	 * @param fileChooser true  - displays the FileChooserDialog always; 
	 * 					  false - displays FileChooserDialog only if not set already
	 * @return the the selected save directory
	 */
	public File getSaveDir(boolean fileChooser) {
		if (saveDir == null || fileChooser == true){
			JFileChooser chooser;
			chooser = new JFileChooser();
			chooser.setSelectedFile(saveDir);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = chooser.showOpenDialog(null);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
		            saveDir=chooser.getSelectedFile();
		    }
		    else {
		    	return null;
		    }
		}
		return saveDir;
	}

	public boolean requireSave() {
		return changed;
	}

	public void setCurrLocation(Location location) {
		if(locations.contains(location)){
			this.currLocation = location;
		}
	}

	public void removeAllScenes() {
		for(Location location : locations){
			location.removeAllScenes();
		}
		
	}
}
