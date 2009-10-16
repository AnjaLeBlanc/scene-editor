package edance.sceeneditor;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.purl.sword.base.Collection;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.Workspace;


import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;

import edance.userinterface.MetaDataDialog;
import edu.harvard.hul.ois.mets.Agent;
import edu.harvard.hul.ois.mets.Div;
import edu.harvard.hul.ois.mets.DmdSec;
import edu.harvard.hul.ois.mets.FLocat;
import edu.harvard.hul.ois.mets.FileGrp;
import edu.harvard.hul.ois.mets.FileSec;
import edu.harvard.hul.ois.mets.Fptr;
import edu.harvard.hul.ois.mets.Loctype;
import edu.harvard.hul.ois.mets.MdWrap;
import edu.harvard.hul.ois.mets.Mdtype;
import edu.harvard.hul.ois.mets.Mets;
import edu.harvard.hul.ois.mets.MetsHdr;
import edu.harvard.hul.ois.mets.Name;
import edu.harvard.hul.ois.mets.Note;
import edu.harvard.hul.ois.mets.Role;
import edu.harvard.hul.ois.mets.StructMap;
import edu.harvard.hul.ois.mets.Type;
import edu.harvard.hul.ois.mets.XmlData;
import edu.harvard.hul.ois.mets.helper.MetsValidator;
import edu.harvard.hul.ois.mets.helper.MetsWriter;
import edu.harvard.hul.ois.mets.helper.PCData;
import edu.harvard.hul.ois.mets.helper.PreformedXML;

import sword_client.Client;
import sword_client.SWORDClientException;
import sword_client.PostMessage;

public class SwordUploadSession {
	//	 The slash character
    private static final String SLASH = System.getProperty("file.separator");
    // The length of the buffer used for file reading
    private static final int BUFFER_LENGTH = 2048;
	private static final String EXPORT_EXTENSION = ".zip";
	private Client sword;
	private File dir;
//	private String href;
//	private String username;
//	private char[] password;
	private boolean isSwordServer=false;
	private ServiceDocument serviceDocu=null;
	private String filename;
	private static final String streamsDir="recording";
	
	private static Logger log = Logger.getLogger(SwordUploadSession.class);
	
	public SwordUploadSession(File file, String href, String username, char[] password) {
		sword = new Client();
		this.dir=file;
//		this.href=href;
//		this.username=username;
//		this.password=password;
		try {
			initialiseServer(href,username,new String(password));
			System.out.println("url: " + href.concat("/servicedocument"));
			serviceDocu = sword.getServiceDocument(href.concat("/servicedocument"));
			log.info(serviceDocu.toString());
		} catch (MalformedURLException e) {
			this.isSwordServer=false;
			e.printStackTrace();
			log.error(e.toString());
		} catch (SWORDClientException e) {
			this.isSwordServer=false;
			e.printStackTrace();
			log.error(e.toString());
		}
		this.isSwordServer=true;
	}

	public boolean isSwordServer() {
		return this.isSwordServer;
	}
	
	private void initialiseServer(String location, String username, String password)
		throws MalformedURLException
	{
		URL url = new URL(location);
		int port = url.getPort();
		if( port == -1 ) 
		{
			port = 80;
		}

		sword.setServer(url.getHost(), port);

		if (username != null && username.length() > 0 && 
            password != null && password.length() > 0 )
		{
			log.info("Setting the username/password: " + username + " "
					+ password);
			sword.setCredentials(username, password);
		}
		else
		{
			sword.clearCredentials();
		}
	}
	
//	@SuppressWarnings("unchecked")
//	Iknow this is working try something new
//	private void writeMETS(ZipOutputStream output, MetaData mde, Vector<String> streams){
//		try {
//		    Mets mets = new Mets ();
//		    mets.setID("sword-mets");
//		    mets.setOBJID ("sword-mets-obj");
//		    mets.setLABEL ("SWORD Item");
//		    mets.setSchema("mets", "http://www.loc.gov/standards/mets/mets.xsd");
//		    mets.setPROFILE ("DSpace METS SIP Profile 1.0");
//
//		      MetsHdr metsHdr = new MetsHdr ();
//		      metsHdr.setCREATEDATE  (new Date ());
//		      metsHdr.setRECORDSTATUS ("Production");
//
//		    Agent agent = new Agent ();
//			agent.setROLE (Role.PRESERVATION);
//			agent.setTYPE (Type.OTHER);
//			agent.setOTHERTYPE ("depositingAgent");
//
//			  Name name = new Name ();
//			  name.getContent ().add (new PCData ("eDance application"));
//			agent.getContent ().add (name);
//
//			  Note note = new Note ();
//			  note.getContent ().add (new PCData ("Depositing on behalf " +
//							      "of " + mde.speaker));
//			agent.getContent ().add (note);
//		      metsHdr.getContent ().add (agent);
//
//		    mets.getContent ().add (metsHdr);
//
//		    /*
//		     * Descriptive meta data section
//		     * This is the area there we have to put in information like
//		     * Camera positions number of sites ....
//		     * 
//		     * Define our own Schema??!!
//		     */
//		      DmdSec dmdSec = new DmdSec ();
//		      dmdSec.setID("sword-mets-dmd-1");
//
//		        MdWrap mdWrap = new MdWrap ();
//			mdWrap.setMIMETYPE ("text/xml");
//			mdWrap.setMDTYPE (Mdtype.OTHER);
//			mdWrap.setOTHERMDTYPE("EPDCX");
//
//			  XmlData xmlData = new XmlData ();
////			 
//			  PreformedXML preformed = new PreformedXML(
//					  "\n\t<epdcx:descriptionSet\n"+
//					  "\t	xmlns:epdcx=\"http://purl.org/eprint/epdcx/2006-11-16/\"\n"+
//					  "\t	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"+
//					  "\t	xsi:schemaLocation=\"http://purl.org/eprint/epdcx/2006-11-16/ http://purl.org/eprint/epdcx/xsd/2006-11-16/epdcx.xsd\">\n"+
//
//					  "\t<epdcx:description\n" +
//					  "\t	epdcx:resourceId=\"sword-mets-epdcx-1\">\n" +
//					  "\t	<epdcx:statement\n" +
//					  "\t		epdcx:propertyURI=\"http://purl.org/dc/elements/1.1/type\"\n" +
//					  "\t		epdcx:valueURI=\"http://purl.org/eprint/entityType/ScholarlyWork\" />\n" +
//					  "\t	<epdcx:statement\n" +
//					  "\t		epdcx:propertyURI=\"http://purl.org/dc/elements/1.1/title\">\n" +
//					  "\t		<epdcx:valueString> " +
//					  "\t			" + mde.title +
//					  "\t		</epdcx:valueString>" +
//					  "\t	</epdcx:statement>" +
//					  "\t	<epdcx:statement\n" +
//					  "\t		epdcx:propertyURI=\"http://purl.org/dc/elements/1.1/creator\">\n" +
//					  "\t		<epdcx:valueString> " +
//					  "\t			" + mde.speaker +
//					  "\t		</epdcx:valueString>" +
//					  "\t	</epdcx:statement>" +
//					  "\t	<epdcx:statement\n" +
//					  "\t		epdcx:propertyURI=\"http://purl.org/dc/elements/1.1/description\">\n" +
//					  "\t		<epdcx:valueString> " +
//					  "\t			Speaker: " + mde.speaker +"\n" +
//					  "\t			Title: " + mde.title +"\n" +
//					  "\t			Conference: " + mde.conference +"\n" +
//					  "\t			Session: " + mde.session +"\n" +
//					  "\t			Venue: " + mde.venue +"\n" +
//					  "\t		</epdcx:valueString>" +
//					  "\t	</epdcx:statement>" +
//					  "\t	<epdcx:statement\n" +
//					  "\t		epdcx:propertyURI=\"http://purl.org/dc/elements/1.1/rights\">\n" +
//					  "\t		<epdcx:valueString> " +
//					  "\t			All rights remain with the author\n" +
//					  "\t		</epdcx:valueString>" +
//					  "\t	</epdcx:statement>" +
//					  "\t</epdcx:description>"+
//					  "\t</epdcx:descriptionSet>"
//			  
//			  
//			  
//			  
//			  );
//
//			  xmlData.getContent ().add (preformed);
//			  
//			mdWrap.getContent ().add (xmlData);
//		      dmdSec.getContent ().add (mdWrap);
//		    mets.getContent ().add (dmdSec);
//
//		    /*
//		     * Administrative Meta Data
//		     * 
//		     * Don't know whether we need this
//		     */
//		      AmdSec amdSec = new AmdSec ();
//
//		        SourceMD sourceMD = new SourceMD ();
////			sourceMD.setID ("254933-source");
//		      sourceMD.setID ("sword-mets-source-1");
//
//			  mdWrap = new MdWrap ();
//			  mdWrap.setMIMETYPE ("text/xml");
////			  mdWrap.setMDTYPE(Mdtype.DC);
//			  mdWrap.setMDTYPE (Mdtype.OTHER);
//			  mdWrap.setOTHERMDTYPE("EPDCX");
//			
//			    xmlData = new XmlData ();
//			    xmlData.setSchema ("dc",
//					       "http://purl.org/dc/elements/1.1/");
//			    /* Use the PreformedXML element */
//			      PreformedXML preformed2 = new PreformedXML("" +
//			    		  "<dc:title>Birds</dc:title>\n"+
//			      		"<dc:source>" +
//								   "Bulletin of the " +
//			      "the Museum of Comparative Zoology at Harvard College " +
//						   "68(7): 311-381. 1928</dc:source>");
//			    xmlData.getContent ().add (preformed2);
//			    /* Use the Any element */
//			    /*
//			      any = new Any ("dc:source");
//			      any.getContent ().add (new PCData ("Bulletin of the " +
//				"Museum of Comparative Zoology at Harvard College " +
//				"68(7): 311-381. 1928"));
//			    xmlData.getContent ().add (any);
//			    */
//			  mdWrap.getContent ().add (xmlData);
//			sourceMD.getContent ().add (mdWrap);
//		      amdSec.getContent ().add (sourceMD);
////		    mets.getContent ().add (amdSec);
//
//		    /*
//		     * structural map
//		     * 'heart' of any mods file???
//		     * same number of divs as files 
//		     */
//		    StructMap structMap = new StructMap ();
//		      structMap.setTYPE ("LOGICAL");
//		      structMap.setLABEL("structure");
//		      structMap.setID("sword-mets-struct-1");
//		    
//		    /*
//		     * getting finally to the files of this upload
//		     * definitly need this
//		     */
//		      FileSec fileSec = new FileSec ();
//
//		        FileGrp fileGrp = new FileGrp ();
//		        fileGrp.setUSE("CONTENT");
//
//			    Div div2 = new Div ();
//			    div2.setTYPE ("media stream");
//			    div2.setDMDID("sword-mets-dmd-1");
//			    structMap.getContent ().add (div2);
//		        
//	          for(int i=0; i<streams.size();i++){
//	        	  edu.harvard.hul.ois.mets.File file = new edu.harvard.hul.ois.mets.File ();
//	        	  file.setID("stream"+streams.get(i));
//	        	  file.setMIMETYPE("RTP");
//	        	  file.setSIZE(new File(streams.get(i)).getTotalSpace());
//	        	  FLocat flocat=new FLocat();
//	        	  flocat.setLOCTYPE(Loctype.URL);
//	        	  flocat.setXlinkHref(streams.get(i));
//	        	  file.getContent().add(flocat);
////	        	  Stream stream= new Stream();
////	        	  stream.setStreamType("RTP");
////	        	  file.getContent().add(stream);
//	        	  
//	        	  fileGrp.getContent ().add (file);
//	        	  
//	        	  Div div = new Div ();
//	  			  div.setTYPE ("File");
//	  			  div.setID("sword-mets-file-"+i);
//	  			  div.setDMDID("sword-mets-dmd-1");
//	  			  Fptr fptr= new Fptr();
//	  			  fptr.setFILEID(file.getID(),file);
//	  			  div.getContent().add(fptr);
//	  			  div2.getContent ().add (div);
//	          }
//		        
//		      fileSec.getContent ().add (fileGrp);
//		      
//		    mets.getContent ().add (fileSec);
//		    
//
//		    
//		    mets.getContent ().add (structMap);
//
//		  mets.validate (new MetsValidator ());
//		  ZipEntry entry = new ZipEntry("mets.xml");
//		  output.putNextEntry(entry);
//		  mets.write (new MetsWriter (output));
//		  output.closeEntry();
//		  mets.write (new MetsWriter (System.out));
//		}
//		catch (Exception e) {
//		    e.printStackTrace ();
//		}
//	}
	
	@SuppressWarnings("unchecked")
	private void writeMETS(ZipOutputStream output, MetaData mde, Vector<String> streams){
		try {
		    Mets mets = new Mets ();
		    mets.setID("sword-mets");
		    mets.setOBJID ("sword-mets-obj");
		    mets.setLABEL ("SWORD Item");
		    mets.setSchema("mets", "http://www.loc.gov/standards/mets/mets.xsd");
		    mets.setSchema("edance", "http://grace.rcs.manchester.ac.uk/edance/edance.xsd");
		    mets.setSchema("mods", "http://www.loc.gov/mods/v3",
		    		"http://www.loc.gov/standards/mods/v3/mods-3-0.xsd");
		    mets.setPROFILE ("DSpace METS SIP Profile 1.0");

		      MetsHdr metsHdr = new MetsHdr ();
		      metsHdr.setCREATEDATE  (new Date ());
		      metsHdr.setRECORDSTATUS ("Production");

		    Agent agent = new Agent ();
			agent.setROLE (Role.CUSTODIAN);
			agent.setTYPE (Type.ORGANIZATION);

			  Name name = new Name ();
			  name.getContent ().add (new PCData ("eDance application"));
			agent.getContent ().add (name);

			  Note note = new Note ();
			  note.getContent ().add (new PCData ("Depositing on behalf " +
							      "of " + mde.speaker));
			agent.getContent ().add (note);
		      metsHdr.getContent ().add (agent);

		    mets.getContent ().add (metsHdr);

		    /*
		     * Descriptive meta data section
		     * This is the area there we have to put in information like
		     * Camera positions number of sites ....
		     * 
		     * Define our own Schema??!!
		     */
		      DmdSec dmdSec = new DmdSec ();
		      dmdSec.setID("sword-mets-dmd-1");

		        MdWrap mdWrap = new MdWrap ();
			mdWrap.setMIMETYPE ("text/xml");
			mdWrap.setMDTYPE (Mdtype.MODS);

			  XmlData xmlData = new XmlData ();
			  
			  String xml = "<mods:name>\n" + 
			  " <mods:role>\n"+
			  "  <mods:roleTerm type=\"text\">author</mods:roleTerm>\n"+
			  "  </mods:role>\n"+
			  "  <mods:namePart>"+ mde.speaker+"</mods:namePart>\n"+
			  "</mods:name>\n"+
			  "<mods:titleInfo>\n"+
			  " <mods:title>"+mde.title+"</mods:title>\n"+
			  "</mods:titleInfo>\n"+
			  "<mods:abstract>An description of the piece</mods:abstract>\n"+
			  "<edance:choreographer>" + mde.speaker + "</edance:choreographer>\n";
			  
			  for(int i=0; i<mde.getOtherMetadataLength(); i++){
				  xml=xml.concat("<"+mde.getOtherMetadataKey(i)+">"+ mde.getOtherMetadata(i)+"</"+mde.getOtherMetadataKey(i)+">\n");
			  }
//			  "<edance:performer>" + mde.getOtherMetadata("edance.performer") + "</edance:performer>\n"
//			  "<mods:name>\n" + 
//			  " <mods:role>\n"+
//			  "  <mods:roleTerm type=\"text\">choreographer</mods:roleTerm>\n"+
//			  "  </mods:role>\n"+
//			  "  <mods:namePart>" + mde.speaker+ "</mods:namePart>\n"+
//			  "</mods:name>\n"// +
//			  "<mods:camera>sony camcorder</mods:camera>\n"

//			 
			  PreformedXML preformed = new PreformedXML(xml);

			  xmlData.getContent ().add (preformed);
			  
			mdWrap.getContent ().add (xmlData);
		      dmdSec.getContent ().add (mdWrap);
		    mets.getContent ().add (dmdSec);

		    /*
		     * structural map
		     * 'heart' of any mods file???
		     * same number of divs as files 
		     */
		    StructMap structMap = new StructMap ();
		      structMap.setTYPE ("LOGICAL");
		      structMap.setLABEL("structure");
		      structMap.setID("sword-mets-struct-1");
		    
		    /*
		     * getting finally to the files of this upload
		     * definitly need this
		     */
		      FileSec fileSec = new FileSec ();

		        FileGrp fileGrp = new FileGrp ();
		        fileGrp.setUSE("CONTENT");

			    Div div2 = new Div ();
			    div2.setTYPE ("media stream");
			    div2.setDMDID("sword-mets-dmd-1");
			    structMap.getContent ().add (div2);
		        
	          for(int i=0; i<streams.size();i++){
	        	  edu.harvard.hul.ois.mets.File file = new edu.harvard.hul.ois.mets.File ();
	        	  file.setID("stream"+streams.get(i));
	        	  file.setMIMETYPE("RTP");
	        	  file.setSIZE(new File(streams.get(i)).getTotalSpace());
	        	  FLocat flocat=new FLocat();
	        	  flocat.setLOCTYPE(Loctype.URL);
	        	  flocat.setXlinkHref(streams.get(i));
	        	  file.getContent().add(flocat);
//	        	  Stream stream= new Stream();
//	        	  stream.setStreamType("RTP");
//	        	  file.getContent().add(stream);
	        	  
	        	  fileGrp.getContent ().add (file);
	        	  
	        	  Div div = new Div ();
	  			  div.setTYPE ("File");
	  			  div.setID("sword-mets-file-"+i);
	  			  div.setDMDID("sword-mets-dmd-1");
	  			  Fptr fptr= new Fptr();
	  			  fptr.setFILEID(file.getID(),file);
	  			  div.getContent().add(fptr);
	  			  div2.getContent ().add (div);
	          }
		        
		      fileSec.getContent ().add (fileGrp);
		      
		    mets.getContent ().add (fileSec);
		    

		    
		    mets.getContent ().add (structMap);

		  mets.validate (new MetsValidator ());
		  ZipEntry entry = new ZipEntry("mets.xml");
		  output.putNextEntry(entry);
		  mets.write (new MetsWriter (output));
		  output.closeEntry();
		  mets.write (new MetsWriter (System.out));
		}
		catch (Exception e) {
		    e.printStackTrace ();
		}
	}

	public void exportSession() {
		try {
			produceZip();
		} catch (Exception e) {
			e.printStackTrace();
		}		
		uploadZip();
	}

	private void uploadZip() {
		PostMessage message = new PostMessage(); 
		message.setFilepath(this.filename);
		System.out.println("service Docu" + serviceDocu + " services " + serviceDocu.getService());
		
		Iterator<Workspace> wsi=serviceDocu.getService().getWorkspaces();
		HashMap<String,String>collection=new HashMap<String,String>();
		while(wsi.hasNext()){
			Workspace ws = wsi.next();
			List<Collection>cl=ws.getCollections();
			for(int i=0; i<cl.size();i++){
				Collection c= cl.get(i);
				collection.put(c.getTitle(),c.getLocation());
			}
		}
		String selection= (String) JOptionPane.showInputDialog(null, 
				"Choose a collection", "Collection", JOptionPane.PLAIN_MESSAGE,
				null, collection.keySet().toArray(new String[0]),collection.keySet().iterator().next());
//		message.setDestination(serviceDocu.getService().getWorkspaces().next().collectionIterator().next().getLocation());
		if(selection==null){
			return;
		}
		message.setDestination(collection.get(selection));
		message.setFiletype("application/zip");
		message.setXpackaging("http://purl.org/net/sword-types/METSDSpaceSIP");
		message.setUseMD5(false);
		message.setVerbose(true);
		message.setNoOp(false);
		message.setFormatNamespace(null);
		
		try {
			processPost(message);
		} catch (SWORDClientException e) {
			e.printStackTrace();
		}
		
	}

	private void produceZip() throws Exception {
        // Get the session information
    	
        // Open a zip file
    	MetaData mde = readMetaData();
    	if(mde==null){
    		throw new Exception("no MetaDataFile");
    	}
    	String filename=mde.title.replaceAll(" ", "_");
    	filename=filename.replaceAll("\\W", "");
    	OutputStream output=new FileOutputStream(this.dir.getCanonicalPath()
    			.concat("/"+filename+EXPORT_EXTENSION));
    	this.filename=this.dir.getCanonicalPath().concat("/"+filename+EXPORT_EXTENSION);
    	
        ZipOutputStream out = new ZipOutputStream(output);
        Vector<String> streams = readStreamSsrcs();
//        Vector files = session.getFiles();
//        Model model = session.getSessionCompendium(username, password, true);

        // Write each of the streams
        Vector<String> correctedStreams=new Vector<String>();
        for (int i = 0; i < streams.size(); i++) {
                exportStream(streams.get(i), out, correctedStreams);
        }
        writeMETS(out,mde, correctedStreams);

        // Write live Anotations from Session (jabber xml)
//        metadata.setTime(System.currentTimeMillis());
//        out.putNextEntry(metadata);
//        uout = new UnclosableOutputStream(out);
//        model.write(uout);
//        out.closeEntry();

        // Write out the Compendium uploads
//        for (int i = 0; i < files.size(); i++) {
//            HashMap file = (HashMap) files.get(i);
//            String filename = (String) file.get(UploadedFile.NAME);
//            long filetime = Long.parseLong((String) file.get(
//                    UploadedFile.UPLOADTIME));
//            File data = new File(getCaptureLocation() + SLASH + session.getId()
//                    + SLASH + filename);
//            BufferedInputStream input =
//                new BufferedInputStream(new FileInputStream(data));
//            byte[] buffer = new byte[BUFFER_SIZE];
//            int bytesRead = 0;
//            ZipEntry dataEntry = new ZipEntry(filename);
//            String fileuploader = (String) file.get(UploadedFile.UPLOADER);
//            String[] name = (String[]) users.get(fileuploader);
//
//            dataEntry.setComment(name[0] + NAME_SEP + name[1]);
//            dataEntry.setTime(filetime);
//            out.putNextEntry(dataEntry);
//            while ((bytesRead = input.read(buffer)) != -1) {
//                out.write(buffer, 0, bytesRead);
//            }
//            out.closeEntry();
//        }

        // Close the zip
        out.close();
	}
		
	private MetaData readMetaData() {
	    // The length of the buffer used for file reading
		File file=null, file2;
		try {
			file2 = new File(this.dir.getCanonicalPath().concat("/metadata.txt"));

			int i=1;
			while(file2.exists()){
				file=file2;
				file2 = new File(this.dir.getCanonicalPath().concat("/metadata.txt"+i));
				i++;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		if(file == null || !file.exists()){
			MetaData metaData=new MetaData();
			//TODO have the real dialog here
			metaData.setTitle(JOptionPane.showInputDialog("Please input the title of the upload"));
			return metaData;
		} 
			
		MetaData metadata=new MetaData();
		Model model = ModelFactory.createDefaultModel();
		Model modelread;
		try {
			modelread = model.read(new FileInputStream(file),"");
			modelread.write(System.out);
			ResIterator iterator = modelread.listSubjects();
			while(iterator.hasNext()){
				Resource resource = modelread.getResource(((ResourceImpl)iterator.next()).getURI());
				
				metadata.setTitle(resource.getProperty(
						modelread.getProperty(MetaDataDialog.TITLE)).getObject().toString());
				metadata.setSpeaker(resource.getProperty(
						modelread.getProperty(MetaDataDialog.SPEAKER)).getObject().toString());
				metadata.setVenue(resource.getProperty(
						modelread.getProperty(MetaDataDialog.VENUE)).getObject().toString());
				metadata.setSession(resource.getProperty(
						modelread.getProperty(MetaDataDialog.SESSION)).getObject().toString());
				metadata.setConference(resource.getProperty(
						modelread.getProperty(MetaDataDialog.CONFERENCE)).getObject().toString());
				XSDDateTime d= (XSDDateTime) resource.getProperty(modelread.getProperty(MetaDataDialog.STARTDATETIME)).getLiteral().getValue();
				metadata.setStartDateTime(d.asCalendar());
				d= (XSDDateTime) resource.getProperty(modelread.getProperty(MetaDataDialog.STOPDATETIME)).getLiteral().getValue();
				metadata.setStopDateTime(d.asCalendar());
				for(int i = 0; i<MetaDataDialog.additionalFields.length; i++){
					if(resource.getProperty(modelread.getProperty(MetaDataDialog.additionalFields[i][2]))!=null){
						metadata.setOtherMetadata(MetaDataDialog.additionalFields[i][1], 
								resource.getProperty(modelread.getProperty(
										MetaDataDialog.additionalFields[i][2])).getObject().toString());
					}
				}
				return metadata;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private Vector<String> readStreamSsrcs() {
		Vector<String> ssrcs= new Vector<String>();
		File file;
		try {
			file = new File(this.dir.getCanonicalPath().concat("/session.txt"));
			if(file.exists()){
				ssrcs.addAll(readStreamsFromDir(file));
			} else {
				String addon="";
				int count=0;
				while(new File(this.dir.getCanonicalPath().concat(SLASH+streamsDir+addon)).exists()){
					file = new File(this.dir.getCanonicalPath().concat(SLASH+
							streamsDir+addon+"/session.txt"));
					if(file.exists()){
						ssrcs.addAll(readStreamsFromDir(file));
					}
					count++;
					addon=new Integer(count).toString();
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
        return ssrcs;
	}
	
	private Vector<String> readStreamsFromDir(File file){
		Vector<String> ssrcs= new Vector<String>();
		try{
	        BufferedReader input =  new BufferedReader(new FileReader(file));
	        try{
	            String line = null;
	            while((line=input.readLine())!=null){
	                if(line.trim().matches("<\\d+ - \\d+>")){
	                	//do nothing
	                }
	                else if(line.trim().matches("<\\d+ -")){
	                	//do nothing
	                }
	                else{
	                    String[] sessionKeys=line.split(" ");
	                    for(int i=0; i<sessionKeys.length; i++){
	                        ssrcs.add(file.getParent()+SLASH+sessionKeys[i]);
	                    }
	                }
	            }
	        }
	        finally {
	            input.close();
	        }
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		return ssrcs;
	}
	
    // Exports a stream to an output stream
    private void exportStream(String streamId,
            ZipOutputStream out, Vector<String> correctedStreams) throws IOException {
        int error=0;
    	ZipEntry entry = new ZipEntry(streamId.substring(streamId.lastIndexOf(SLASH)+1));
        File data = new File(streamId);
        BufferedInputStream instream = new BufferedInputStream(
                new FileInputStream(data));
        byte[] indata = new byte[BUFFER_LENGTH];
        int bytesRead = instream.read(indata);

        entry.setTime(data.lastModified()); 
        while(error==0){
	        try {
				out.putNextEntry(entry);
				correctedStreams.add(entry.getName());
				error=1;
			} catch (java.util.zip.ZipException ze) {
				String newName=entry.getName();
				Long newNumber=Long.valueOf(newName);
				newNumber++;
				entry = new ZipEntry(newNumber.toString());
			}
        }
        while (bytesRead > 0) {
            out.write(indata, 0, bytesRead);
            bytesRead = instream.read(indata);
        }
        instream.close();
        out.closeEntry();
    }
    
	/**
	 * Process the post response. The message contains the list of arguments 
	 * for the post. The method will then print out the details of the 
	 * response. 
	 * 
	 * @parma message The post options. 
	 *  
	 * @exception SWORDClientException if there is an error accessing the 
	 *                                 post response. 
	 */
	protected void processPost(PostMessage message)
	throws SWORDClientException
	{
		
		DepositResponse response = sword.postFile(message);
		

		System.out.println("The status is: " + sword.getStatus());
		
		if( response != null)
		{
			log.debug("message is: " + response.marshall());
			
			// iterate over the data and output it 
			SWORDEntry entry = response.getEntry(); 
			

			System.out.println("Id: " + entry.getId());
			org.w3.atom.Title title = entry.getTitle(); 
			if( title != null ) 
			{
				System.out.print("Title: " + title.getContent() + " type: " ); 
				if( title.getType() != null )
				{
					System.out.println(title.getType().toString());
				}
				else
				{
					System.out.println("Not specified.");
				}
			}

			// process the authors
			Iterator<org.w3.atom.Author> authors = entry.getAuthors();
			while( authors.hasNext() )
			{
			   org.w3.atom.Author author = authors.next();
			   System.out.println("Author - " + author.toString() ); 
			}
			
			Iterator<String> categories = entry.getCategories();
			while( categories.hasNext() )
			{
			   System.out.println("Category: " + categories.next()); 
			}
			
			Iterator<org.w3.atom.Contributor> contributors = entry.getContributors();
			while( contributors.hasNext() )
			{
			   org.w3.atom.Contributor contributor = contributors.next(); 
			   System.out.println("Contributor - " + contributor.toString());
			}
			
			Iterator<org.w3.atom.Link> links = entry.getLinks();
			while( links.hasNext() )
			{
			   org.w3.atom.Link link = links.next();
			   System.out.println(link.toString());
			}

			System.out.println( "Published: " + entry.getPublished());
			
			org.w3.atom.Content content = entry.getContent();
			if( content != null ) 
			{
			   System.out.println(content.toString());
			}
			else
			{
			   System.out.println("There is no content element.");
			}
			
			org.w3.atom.Rights right = entry.getRights();
			if( right != null ) 
         {
			   
            System.out.println(right.toString());
         }
         else
         {
            System.out.println("There is no right element.");
         }
			
			org.w3.atom.Source source = entry.getSource();
			if( source != null ) 
         {
            org.w3.atom.Generator generator = source.getGenerator();
            if( generator != null ) 
            {
               System.out.println(generator.toString());
            }
            else
            {
               System.out.println("The generator is not specified");
            }
         }
         else
         {
            System.out.println("There is no source element.");
         }
			org.w3.atom.Summary summary = entry.getSummary();
			if( summary != null ) 
         {
            
            System.out.println(summary.toString());
         }
         else
         {
            System.out.println("There is no summary element.");
         }
			
			System.out.println("description: " + entry.getContent().toString());
			System.out.println("Update: " + entry.getUpdated() );
			System.out.println("Published: " + entry.getPublished());
			System.out.println("Verbose Description: " + entry.getVerboseDescription());
			System.out.println("Treatment: " + entry.getTreatment());
			System.out.println("Format Namespace: " + entry.getFormatNamespace());

			if( entry.isNoOpSet() )
			{
				System.out.println("NoOp: " + entry.isNoOp());
			}
		}
		else
		{
			System.out.println("No valid Entry document was received from the server");
		}	
	}
	
}
