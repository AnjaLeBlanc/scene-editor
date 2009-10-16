package edance.sceeneditor;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

//import rtspd.UnclosableOutputStream;

import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;

import edance.userinterface.MetaDataDialog;

public class UploadSession {
    /**
     * The name of the file containing Compendium metadata in the export
     */
    public static final String EXPORT_COMPENDIUM_METADATA = "compendium-data";

    /**
     * The name of the file containing session metadata in the export
     */
    public static final String EXPORT_SESSION_METADATA = "metadata";

    /**
     * The name given to a new import
     */
    public static final String NEW_SESSION_IMPORT = "new";

    /**
     * The attribute name for the session manager in the web context
     */
    public static final String SESSION_MANAGER_ATTRIBUTE = "sessionManager";

    /**
     * The file extension to give to exports
     */
    public static final String EXPORT_EXTENSION = ".arena";

    /**
     * The name of the file to hold the export details
     */
    public static final String EXPORT_SESSION_DETAILS = "arena.session";

    /**
     * The name of the field for the name of the session
     */
    public static final String EXPORT_NAME_FIELD = "name";

    /**
     * The name of the field for the description
     */
    public static final String EXPORT_DESCRIPTION_FIELD = "description";

    // The slash character
    private static final String SLASH = System.getProperty("file.separator");
    
    private static final String URL_ADDON="/doimport.jsp?session=new";

    // The length of the buffer used for file reading
    private static final int BUFFER_LENGTH = 2048;
    
    private static final String streamsDir="recording";

    // The directory of the session
    private File directory;
    
    private String filename=null;

	private JProgressBar progress;

    // The modifiers
    private static final String [] MODIFIERS = 
        new String[]{"bytes", "Kb", "Mb", "Gb"};
    
    // The maximum number of fraction digits to display
    private static final int FORMAT_MAX_FRAC_DIGITS = 2;

    // The minimum number of fraction digits to display
    private static final int FORMAT_MIN_FRAC_DIGITS = 2;

	private static final String COLON = ":";
    
    private String userpass;
	
	private URL url;
    
	public UploadSession(File dir, String serverUrl, String username, char[] password){
		directory=dir;
		try {
			url=new URL(serverUrl.concat(URL_ADDON));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		userpass=new String(common.Base64.base64encode(new String
				(username+COLON+new String(password)).getBytes()));
		
		System.out.println("url " + url);
		System.out.println("userpass " + userpass+" " +username+COLON+new String(password));
		
	}
    // Exports a stream to an output stream
    private void exportStream(String streamId,
            ZipOutputStream out) throws IOException {
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
     * Exports a session to a zip file
     *
     * @param sessionId
     *            The id of the session to export
     * @param streamIds
     *            The id of the streams to export
     * @param output
     *            The output stream to export to
     * @param username The user requesting the export
     * @param password The password of the user
     * @throws IOException
     */
    public void exportSession() throws Exception {

        // Get the session information
    	
        // Open a zip file
    	MetaData mde = readMetaData();
    	if(mde==null){
    		throw new Exception("no MetaDataFile");
    	}
    	String filename=mde.title.replaceAll(" ", "_");
    	filename=filename.replaceAll("\\W", "");
    	OutputStream output=new FileOutputStream(this.directory.getCanonicalPath()
    			.concat("/"+filename+EXPORT_EXTENSION));
    	this.filename=this.directory.getCanonicalPath().concat("/"+filename+EXPORT_EXTENSION);
    	
        ZipOutputStream out = new ZipOutputStream(output);
//        UnclosableOutputStream uout = null;

        // Write the session information
        ZipEntry entry = new ZipEntry(EXPORT_SESSION_DETAILS);
//        ZipEntry metadata = new ZipEntry(EXPORT_COMPENDIUM_METADATA);
        Properties properties = new Properties();
        Vector<String> streams = readStreamSsrcs();
//        Vector files = session.getFiles();
//        Model model = session.getSessionCompendium(username, password, true);
        entry.setTime(System.currentTimeMillis());
        out.putNextEntry(entry);
        properties.setProperty(EXPORT_NAME_FIELD, mde.title);
        properties.setProperty(EXPORT_DESCRIPTION_FIELD,
                mde.speaker+ " " +mde.session + " " + mde.conference + " " + mde.startDateTime);
        System.out.println(mde.speaker+ " " +mde.session + " " + mde.conference + " " + mde.startDateTime);
        properties.store(out, "");
        out.closeEntry();

        // Write each of the streams
        for (int i = 0; i < streams.size(); i++) {
                exportStream(streams.get(i), out);
        }

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
        
        UploadThread upload=new UploadThread();
        upload.start();
    }
	private MetaData readMetaData() {
		
		File file=null, file2;
		try {
			file2 = new File(this.directory.getCanonicalPath().concat("/metadata.txt"));

			int i=1;
			while(file2.exists()){
				file=file2;
				file2 = new File(this.directory.getCanonicalPath().concat("/metadata.txt"+i));
				i++;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		if(!file.exists()){
			MetaData metaData=new MetaData();
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
				return metadata;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
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
	
	private Vector<String> readStreamSsrcs() {
		Vector<String> ssrcs= new Vector<String>();
		File file;
		try {
			file = new File(this.directory.getCanonicalPath().concat("/session.txt"));
			if(file.exists()){
				ssrcs.addAll(readStreamsFromDir(file));
			} else {
				String addon="";
				int count=0;
				while(new File(this.directory.getCanonicalPath().concat(SLASH+streamsDir+addon)).exists()){
					file = new File(this.directory.getCanonicalPath().concat(SLASH+
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
	public void setProgressBar(JProgressBar progress) {
		this.progress=progress;	
	}
	
	private class UploadThread extends Thread {
        
        private static final int BYTES_PER_KB = 1024;

        private static final int CHUNK_SIZE = 8096;

        private static final int PARTS_PER_ITEM = 2;

        private static final int RANDOM_MULTIPLIER = 1000;

        // The size of the boundary to generate
        private static final int BOUNDARY_SIZE = 8;

        // The size of the upload buffer
        private static final int BUFFER_SIZE = 8096;

        // The size before the next modifier is used
        private static final int SIZE_CUTOFF = 1024;

        // The postfix to appear after the progress
        private static final String PROGRESS_POSTFIX = "%";

        // The separator between fields in a header
        private static final String FIELD_SEP = ";";

        // The filename field in the header
        private static final String FILENAME_FIELD = "filename=";

        // The character that separates the header items
        private static final String HEADER_SEP = " ";

        // The authorization type
        private static final String AUTH_TYPE = "Basic ";

        // The authorization header
        private static final String AUTHORIZATION_HEADER = "Authorization";

        // The boundary field
        private static final String BOUNDARY_PARAMETER = "boundary=";

        // The content type
        private static final String CONTENT_TYPE = "multipart/form-data;";

        // The content type header
        private static final String CONTENT_TYPE_HEADER = "Content-Type";

        // The connection type
        private static final String CONNECTION = "Keep-Alive";

        // The connection header
        private static final String CONNECTION_HEADER = "Connection";

        // The method used to send the data
        private static final String REQUEST_METHOD = "POST";

        // The string to put around field values
        private static final String ITEM_SURROUND = "\"";

        // The name field
        private static final String NAME_FIELD = "name=";

        // The form-data content-disposition header
        private static final String FORM_DATA_HEADER = 
            "Content-Disposition: form-data;";

        // The end of line string
        private static final String EOL = "\r\n";

        // The string to put before the boundary
        private static final String BOUNDARY_PREFIX = "--";

        // The string to put between a query variable and value
        private static final String QUERY_VAR_VAL_SPLIT = "=";

        // The string to put between query variables
        private static final String QUERY_VAR_SPLIT = "&";

		private static final long COMPLETE_PERCENT = 100;

        // The connection to the server
        private HttpURLConnection connection = null;
        
        // The boundary string
        private String boundary = null;
        
        // The data output stream
        private DataOutputStream output = null;
        
        // The query variables
        private HashMap<String,String> queryVars = new HashMap<String,String>();
        
        // The names of the variables
        private Vector<String> queryNames = new Vector<String>();
		
		private String fieldname="import";
        
        
        // Creates a new UploadThread
        private UploadThread() throws MalformedURLException {
            progress.setValue(0);
//            url=new URL("http://memetic.ag.manchester.ac.uk:5600/doimport.jsp?session=new");
            // Generate a boundary
            byte b[] = new byte[BOUNDARY_SIZE];
            for (int i = 0; i < b.length; i++) {
                b[i] = 
                    (byte) (((Math.random() * RANDOM_MULTIPLIER)
                            % ('z' - 'a')) + 'a');
            }
            boundary = new String(b);
            
            // Parse the query
            String query = url.getQuery();
            if (query != null) {
                String[] items = query.split(QUERY_VAR_SPLIT);
                for (int i = 0; i < items.length; i++) {
                    String[] item = items[i].split(QUERY_VAR_VAL_SPLIT,
                            PARTS_PER_ITEM);
                    if (item.length == PARTS_PER_ITEM) {
                        queryVars.put(item[0], item[1]);
                    } else {
                        queryVars.put(item[0], "");
                    }
                    queryNames.add(item[0]);
                }
            }
        }
        
        // Adds a field to the upload
        private void addField(String name, String value) throws IOException {
            output.writeBytes(BOUNDARY_PREFIX + boundary + EOL);
            output.writeBytes(FORM_DATA_HEADER + HEADER_SEP + NAME_FIELD 
                    + ITEM_SURROUND + name + ITEM_SURROUND + EOL);
            output.writeBytes(EOL);
            output.writeBytes(value + EOL);
        }
        
        // Starts the file upload section
        private void startFile() throws IOException {
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod(REQUEST_METHOD);
            connection.setRequestProperty(
                    CONNECTION_HEADER, CONNECTION);
            connection.setRequestProperty(CONTENT_TYPE_HEADER, 
                    CONTENT_TYPE + BOUNDARY_PARAMETER + boundary);
            connection.setChunkedStreamingMode(CHUNK_SIZE);
			if (userpass!= null) {
                connection.setRequestProperty(AUTHORIZATION_HEADER, 
                        AUTH_TYPE + userpass);
            }
            connection.connect();
            output = new DataOutputStream(connection.getOutputStream());
            
            // Output the query variables
            for (int i = 0; i < queryNames.size(); i++) {
                String name = (String) queryNames.get(i);
                String value = (String) queryVars.get(name);
                addField(name, value);
            }
            
            // Start the file
            output.writeBytes(BOUNDARY_PREFIX + boundary + EOL);
            output.writeBytes(FORM_DATA_HEADER + HEADER_SEP + NAME_FIELD
                    + ITEM_SURROUND 
                    + fieldname 
                    + ITEM_SURROUND + FIELD_SEP + HEADER_SEP + FILENAME_FIELD 
                    + ITEM_SURROUND + filename + ITEM_SURROUND + EOL);
            output.writeBytes(EOL);
        }
        
        // Ends the file upload section
        private void endFile() throws IOException {
            output.writeUTF(EOL);
            output.writeUTF(BOUNDARY_PREFIX + boundary + BOUNDARY_PREFIX + EOL);
        }
        
        /**
         * 
         * @see java.lang.Runnable#run()
         */
        public void run() {
            File file = new File(filename);
            NumberFormat format = NumberFormat.getNumberInstance();
            
            int divisions = 0;
            double fileSize = file.length();
            while ((fileSize > SIZE_CUTOFF) && (divisions < MODIFIERS.length)) {
                fileSize /= SIZE_CUTOFF;
                divisions += 1;
            }
            format.setMaximumFractionDigits(
                    FORMAT_MAX_FRAC_DIGITS);
            format.setMinimumFractionDigits(
                    FORMAT_MIN_FRAC_DIGITS);
            try {
                BufferedInputStream in = 
                    new BufferedInputStream(new FileInputStream(file));
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead = in.read(buffer);
                long totalBytes = 0;
                startFile();
                while ((bytesRead > 0) /*&& !cancelled*/) {
                    double fileSent = 0;
                    int divs = 0;
                    output.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                    progress.setValue(
                            (int) ((COMPLETE_PERCENT * totalBytes)
                                    / file.length()));
                    progress.setString(format.format(
                            (double) (COMPLETE_PERCENT * totalBytes)
                            / file.length())
                            + PROGRESS_POSTFIX);
                    fileSent = totalBytes;
                    while (fileSent > BYTES_PER_KB
                            && (divisions < MODIFIERS.length)) {
                        fileSent /= BYTES_PER_KB;
                        divs += 1;
                    }
//                    progressSize.setText(format.format(fileSent) 
//                            + MODIFIERS[divs] + " of "
//                            + format.format(fileSize) + MODIFIERS[divisions]
//                            + " sent");
                    bytesRead = in.read(buffer);
                }
        //        if (!cancelled) {
                    BufferedReader input = null;
                    String line = "";
                    String resultText = "";
                    endFile();
                    output.close();
                    input = new BufferedReader(new InputStreamReader(
                                connection.getInputStream()));
                    while ((line = input.readLine()) != null) {
                        resultText += line;
                    }
//                    resultLabel.setText(resultText);
//                } else {
//                    output.close();
//                    resultLabel.setText(ImportApplet.CANCELLED_MESSAGE);
//                }
            } catch (Exception e) {
            	e.toString();
            }
        }
    }
}
