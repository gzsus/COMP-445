import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class httpfs {

    // ----------------------------------------------------------------------
    // ------------------------------ Variables -----------------------------
    // ----------------------------------------------------------------------

    // HttpStatusCode
    private enum HttpStatusCode {
        OK("200", "OK"),
        NOTFOUND("404", "Not Found"),
        CREATED("201", "Created"),
        BADREQUEST("400", "Bad Request"),
        FORBIDDEN("403", "Forbidden");

        private final String code;
        private final String msg;

        HttpStatusCode(String code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return msg;
        }
    }

    // HashMap of Header Fields
    public static HashMap<String, String> headerFields;

    // Server Utilities
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    // Server Settings
    private static String version = "HTTP/1.0";
    private static String body = "";
    private static String httpCode = "";
    private static String httpMessage = "";
    private static String response = "";
    private static String absolutePath = "";


    // ----------------------------------------------------------------------
    // ------------------------------ Methods -------------------------------
    // ----------------------------------------------------------------------

    // start TCP connection and send the response to the client
    public void start(boolean verbose_flag, int port_number, String directory) throws Exception {

        // The server (httpfs) needs to have a 'unique' ip address and port number. This 'unique' (ip,port) pair allows for a communication to be established from the client to the server.
        serverSocket = new ServerSocket(port_number);

        while(true){
            System.out.println("\n\tServer is listening ...");
            Socket clientSocket = serverSocket.accept(); // accept the connection when client requests the socket
            clientSocket.setKeepAlive(true);

            // Data input from client
            Scanner in = new Scanner(clientSocket.getInputStream());
            // Data output to client
            DataOutputStream out = new DataOutputStream( clientSocket.getOutputStream() );

            // Request received by HTTPc
            String request = "";
            // Knowing line number to split the file name from line #1
            int lineNumber = 0;

            // Variables for request
            boolean request_doc = false;
            String method = "";
            String fileName = "";
            String path = "";

            // RUN SERVER
            while (in.hasNextLine()) {
                // -------- START OF REQUEST
                request = in.nextLine();
                lineNumber++; // Increase Line #

                // Print request to server
                if (verbose_flag && !request.isBlank())
                    System.out.println(lineNumber + "#: " + request); // <!---- checks line # on console

                // if its the first line, grab the filename
                if(lineNumber == 1){
                    method = request.split(" ")[0].toLowerCase(); // GET||POST
                    path = request.split(" ")[1]; // requested directory or file

                    if ( path.endsWith(".txt") )
                        request_doc = true; // flag to check if request for document or directory

                    absolutePath = request.split(" ")[1].substring(1); // requested directory

                }

                // ---------- END OF REQUEST
                if(request.equals("")){
                // ---------- START OF RESPONSE
                    File file = new File(absolutePath);

                    /*          --- Get Cases ---
                        Directories:  "/" or "/dir" or "/dir/dir"
                            Empty request:
                                Show all files and directories in absolute path or error
                            Specific directory with no file:
                                Show all files in specific directory or error
                        Files: "/File.txt" or "/dir/File.txt" or "/dir/dir/File.txt"
                            File with no path:
                                Send back file or error
                            File with path:
                                Send back file or error
                     */


                    //Check if the file exists
                    if(file.exists() || absolutePath.equals("")){

                        // sudo chmod 666 hello.txt - can read
                        if(file.canRead()) {
                            if ( method.equals("get") && request_doc)
                                // transform the file to a string
                                body = file_to_string(file);
                            if ( method.equals("get") && !request_doc)
                                // transform the file to a string
                                body = get_files(file);

                            // 1. 200 OK file exists and its readable

                            // status code
                            String httpCode = HttpStatusCode.OK.getCode();
                            String httpMessage = HttpStatusCode.OK.getMessage();

                            // Prepare header
                            headerFields = new HashMap<String, String>();
                            headerFields.put("User-Agent", "Concordia");
                            headerFields.put("Content-Type", "text/html");
                            headerFields.put("Content-Length", Integer.toString(body.length()));

                            // prepare response
                            response = responseString(version, httpCode, httpMessage, headerFields, body);

                            // Prepare response
                            out.writeBytes(response);
                            out.flush();
                            out.close(); // close stream to finish it off
                            break;

                        // sudo chmod 600 hello.txt - cannot read
                        }else{
                            // 3. 403 Forbidden file is not readable

                            body = "";

                            // status code
                            String httpCode = HttpStatusCode.FORBIDDEN.getCode();
                            String httpMessage = HttpStatusCode.FORBIDDEN.getMessage();

                            // Prepare header
                            headerFields = new HashMap<String, String>();
                            headerFields.put("User-Agent", "Concordia");

                            // prepare response
                            response = responseString(version, httpCode, httpMessage, headerFields, body);

                            // Prepare response
                            out.writeBytes(response);
                            out.flush();
                            out.close(); // close stream to finish it off
                            break;
                        }

                    // 2. 404 Not Found file doesn't exist
                    }else{

                        body = "";

                        // status code
                        String httpCode = HttpStatusCode.NOTFOUND.getCode();
                        String httpMessage = HttpStatusCode.NOTFOUND.getMessage();

                        // Prepare header
                        headerFields = new HashMap<String, String>();
                        headerFields.put("User-Agent", "Concordia");

                        // prepare response
                        response = responseString(version, httpCode, httpMessage, headerFields, body);

                        // Prepare response
                        out.writeBytes(response);
                        out.flush();
                        out.close(); // close stream to finish it off
                        break;
                    }

                // ---------- END OF RESPONSE
                }
            }
        }

    }

    // Returns the inline data to be sent from the input arguments or an empty string
    public static String file_to_string(File file) throws FileNotFoundException {
        // Data to be returned
        String data = "";

        // Create scanner for it
        Scanner myReader = new Scanner(file);

        // Read the file
        while (myReader.hasNextLine()) {
            data +=  myReader.nextLine()+"\n";
        }
        myReader.close();
        return data;
    }

    // get all files in directory
    public static String get_files(File folder) {
        String filesNames = "";

        try {
            File[] listOfFiles = folder.listFiles();

            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    filesNames += "\t" + listOfFiles[i].getName();
                } else if (listOfFiles[i].isDirectory()) {
                    filesNames += "\tFolder:" + listOfFiles[i].getName();
                }

            }
        }
        catch (Exception e) { filesNames = "\tBad Request"; }
        if( filesNames.equals("") )
            filesNames = "Empty directory";
        return filesNames;
    }

    // Creates the response string based on the arguments
    private static String responseString(String version, String statusCode, String phrase, HashMap<String, String> headerLine, String body){
        // https://github.com/Ra-Ni/COMP-445-LAB-2/blob/master/img/HTTPResp.png
        String sp = " ";
        String cr = "\r";
        String lf_ = "\n";
        String httpResponse = "";

        // Status Line
        httpResponse += version + sp + statusCode + sp + phrase + cr + lf_ ;

        // Header Line
        for (Map.Entry<String, String> entry : headerLine.entrySet()) { // loop through the header lines
            String headerFieldName = entry.getKey();
            String value = entry.getValue();

            httpResponse += headerFieldName + ":" + value + cr + lf_ ;

        }

        // add the last cr and lf to use body
        httpResponse += cr + lf_;

        // Entity body
        httpResponse += body;

        return httpResponse;
    }


    // ----------------------------------------------------------------------
    // ------------------------------- ARGS ---------------------------------
    // ----------------------------------------------------------------------

    // Get help and verbose flags
    private static boolean[] get_flags(String[] arguments){
        boolean flags[] = {false, false}; // The default is false for verbose and
        for (String s : arguments) {
            if (s.toLowerCase().equals("-h") || s.toLowerCase().equals("help"))
                flags[0] = true;
            if (s.toLowerCase().equals("-v"))
                flags[1] = true;
        }
        return flags;
    }

    // Retrieve the port from the command line arguments if given
    private static int get_port(String[] arguments){
        int port = 8080; // The default port is 9999
        for (int i=0; i<arguments.length;i++)
            if (arguments[i].toLowerCase().equals("-p")){
                try {
                    port = Integer.parseInt(arguments[i + 1]);
                    if (port < 1)
                        throw new Exception();
                }
                catch (Exception e){ port = 8080; }
            };
        return port;
    }

    // Get directory specified in command line arguments if given
    private static String get_directory(String[] arguments) {
        String directory = "/";
        for (int i=0; i<arguments.length;i++)
            if (arguments[i].toLowerCase().equals("-d")) {
                try {
                    directory = arguments[i + 1];
                }
                catch (Exception e){ directory = "/"; }
            }
        return directory;
    }



    // ----------------------------------------------------------------------
    // ------------------------------- MAIN ---------------------------------
    // ----------------------------------------------------------------------

    public static void main( String[] args )throws Exception {
        boolean [] flags = get_flags(args);

        boolean help_flag = flags[0];
        if (help_flag){
            System.out.println("\tUsage: httpfs [-v] [-p PORT] [-d PATH-TO-DIR]\n");
            System.exit(0);
        }

        boolean verbose_flag = flags[1];
        int port_number = get_port(args);
        String directory = get_directory(args);

        httpfs server = new httpfs();
        server.start( verbose_flag, port_number, directory );

    }
}
