/******************************************************************************
 *                                                                            *
 *  (c) Copyright 2006 Verizon Communications USA and The Open University UK  *
 *                                                                            *
 *          This program code may not be used or distributed except in        *
 *                accordance with the license published at                    *
 *      http://www.CompendiumInstitute.org/download/sourcecode/license.htm    *
 *                                                                            *
 ******************************************************************************/

package uploader;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Vector;

/**
 * Uploads a file using HTTP
 * @author Andrew G D Rowley (University of Manchester) / Michelle Bachler
 * @version 1.0
 */
public class HttpFileUploadOutputStream extends OutputStream {

    // The size of the buffer
    private static final int BUFFER_SIZE = 8096;

    // The name item
    private static final String ITEM_NAME = "name=";

    // The space
    private static final String SPACE = " ";

    // An inverted comma
    private static final String INVERTED_COMMA = "\"";

    // The content-disposition header for form-data
    private static final String CONTENT_DISPOSITION_FORM_DATA =
        "Content-Disposition: form-data;";

    // The end of a line marker
    private static final String EOL = "\r\n";

    // The boundary marker
    private static final String BOUNDARY_MARKER = "--";

    // The amount by which to multiply math.random numbers by
    private static final int RANDOM_MULTIPLIER = 1000;

    // The length of the boundary string
    private static final int BOUNDARY_LENGTH = 8;

    // The number of parts to each item in a url
    private static final int PARTS_PER_ITEM = 2;

    // The connection to the http server
    private HttpURLConnection connection = null;

    // The output stream of the connection
    private DataOutputStream output = null;

    // The query variables
    private HashMap<String,String> queryVars = new HashMap<String,String>();

    // The query variable names in order
    private Vector<String> queryNames = new Vector<String>();

    // The boundary
    private String boundary = "";

    // The response
    private String response = null;

    /**
     * Creates a new UploadFile
     * @param url The url to upload to
     * @param fieldname The fieldname to give the file
     * @param filename The name to give the file
     * @param username The user uploading the file
     * @param password The password of the user
     * @throws IOException
     */
    public HttpFileUploadOutputStream(URL url, String fieldname, 
            String filename, String username, 
            String password) throws IOException {

        if (!url.getProtocol().equals("http")) {
            throw new MalformedURLException("URL is not a http URL");
        }
        String query = url.getQuery();
        if (query != null) {
            String[] items = query.split("&");
            for (int i = 0; i < items.length; i++) {
                String[] item = items[i].split("=", PARTS_PER_ITEM);
                if (item.length == PARTS_PER_ITEM) {
                    queryVars.put(item[0], item[1]);
                } else {
                    queryVars.put(item[0], "");
                }
                queryNames.add(item[0]);
            }
        }

        // Generate a boundary
        byte b[] = new byte[BOUNDARY_LENGTH];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) (((Math.random() * RANDOM_MULTIPLIER)
                    % ('z' - 'a')) + 'a');
        }
        boundary = new String(b);

        // Connect to the server
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Content-Type",
                "multipart/form-data;boundary=" + boundary);
        if ((username != null) && (password != null)) {
            String userpass = new String(Base64.base64encode(
                        new String(username + ":" + password).getBytes(
                                "UTF-8")));
            connection.setRequestProperty("Authorization",
                    "Basic " + userpass);
        }
        connection.connect();
        output = new DataOutputStream(connection.getOutputStream());

        // Output the query variables
        for (int i = 0; i < queryNames.size(); i++) {
            String name = (String) queryNames.get(i);
            String value = (String) queryVars.get(name);
            output.writeBytes(BOUNDARY_MARKER + boundary + EOL);
            output.writeBytes(CONTENT_DISPOSITION_FORM_DATA
                    + SPACE + ITEM_NAME + INVERTED_COMMA + name
                    + INVERTED_COMMA + EOL);
            output.writeBytes(EOL);
            output.writeBytes(value + EOL);
        }

        // Start the file
        output.writeBytes(BOUNDARY_MARKER + boundary + EOL);
        output.writeBytes(CONTENT_DISPOSITION_FORM_DATA
                + SPACE + ITEM_NAME + INVERTED_COMMA + fieldname
                + INVERTED_COMMA + ";" + SPACE + "filename="
                + INVERTED_COMMA + filename + INVERTED_COMMA + EOL);
        output.writeBytes(EOL);
    }

    /**
    * Return a random number between the given integers.
    * @param l The low number
    * @param h The high number
    * @return int the random number
    */
    public int rand(int l, int h)   {
        int n = h - l + 1;
        int i = (int) (Math.random() * RANDOM_MULTIPLIER) % n;
        if (i < 0) {
            i = -i;
        }
        return l + i;
    }

    /**
     * Write the given int to the output Stream.
     * @param b the int to write.
     * @throws IOException
     */
    public void write(int b) throws IOException {
        output.write(b);
    }

    /**
     * Close the output stream.
     * @throws IOException
     */
    public void close() throws IOException {
        output.writeBytes(EOL);
        output.writeBytes(BOUNDARY_MARKER + boundary + BOUNDARY_MARKER + EOL);
        output.close();
    }

    /**
     * Returns the first line of the response
     * @return The response string
     * @throws IOException
     */
    public String getResponse() throws IOException {
        if (response == null) {
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String line = input.readLine();

            while ((line != null) && line.equals("")) {
                line = input.readLine();
            }

            if (line == null) {
                line = "";
            }
            response = line;
            input.close();
        }
        return response;
    }

    /**
     * Uploads to the server from a file
     * @param filename The name of the file to upload
     * @return The string returned from the upload to display to users
     * @throws IOException
     */
    public String uploadFromFile(String filename) throws IOException {

        FileInputStream input = new FileInputStream(filename);
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = 0;
        while ((bytesRead = input.read(buffer)) != -1) {
            write(buffer, 0, bytesRead);
        }
        close();

        return getResponse();
    }
}
