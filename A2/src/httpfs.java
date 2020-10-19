import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.PrintWriter;
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
        FORBIDDEN("403", "Forbidden"),
        ;

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

    // HashMap of header fields
    public static HashMap<String, String> headerFields;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    // ----------------------------------------------------------------------
    // ------------------------------ Methods -------------------------------
    // ----------------------------------------------------------------------

    public void start(int port) throws Exception {

        // The server (httpfs) needs to have a 'unique' ip address and port number. This 'unique' (ip,port) pair allows for a communication to be established from the client to the server.
        serverSocket = new ServerSocket(9999);


        while(true){
            Socket clientSocket = serverSocket.accept(); // accept the connection when client requests the socket
            clientSocket.setKeepAlive(true);

            // Data from client
            Scanner in = new Scanner(clientSocket.getInputStream());
            // Data to client
            DataOutputStream out = new DataOutputStream( clientSocket.getOutputStream() );

            // body
            String body = "Hello World!";

            // header fields
            headerFields = new HashMap<String, String>();
            headerFields.put("Content-Type", "text/html");
            headerFields.put("Content-Length", Integer.toString(body.length()));

            // version
            String version = "HTTP/1.0";

            // status code
            String httpCode = HttpStatusCode.OK.getCode();
            String httpMessage = HttpStatusCode.OK.getMessage();

            // Formatting HTTP Response:
            String response = responseString(version, httpCode, httpMessage, headerFields, body);

            String line = "";
            while (in.hasNextLine()) {
                line = in.nextLine();
                System.out.println(line);

                if(line.equals("")){
                    // Prepare response
                    out.writeBytes(response);
                    out.flush();
                    out.close(); // close stream to finish it off
                    break;
                }
            }
        }

    }

    public void stop() throws Exception {
//        in.close();
//        out.close();
//        clientSocket.close();
//        serverSocket.close();
    }

    private static String responseString(String version, String statusCode, String phrase, HashMap<String, String> headerLine, String body){
        // https://github.com/Ra-Ni/COMP-445-LAB-2/blob/master/img/HTTPResp.png
        String sp = " ";
        String cr = "\r";
        String if_ = "\n";
        String httpResponse = "";

        // Status Line
        httpResponse += version + sp + statusCode + sp + phrase + cr + if_ ;

        // Header Line
        for (Map.Entry<String, String> entry : headerLine.entrySet()) { // loop through the header lines
            String headerFieldName = entry.getKey();
            String value = entry.getValue();

            httpResponse += headerFieldName + ":" + value + cr + if_ ;

        }

        // add the last cr and if to use body
        httpResponse += cr + if_;

        // Entity body
        httpResponse += body;

        return httpResponse;
    }

    private String handleRequest(String request){
        // Split request by space into an array
        String[] requestArr = request.split("\\s+");

        // Go throught the content of the request
        for (String item : requestArr) {
            System.out.println(item);
        }
        return "";
    }


    // ----------------------------------------------------------------------
    // ------------------------------- MAIN ---------------------------------
    // ----------------------------------------------------------------------

    public static void main(String[] args )throws Exception {

        httpfs server=new httpfs();
        server.start(9999);


    }
}
