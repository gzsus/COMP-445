import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

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

    // ----------------------------------------------------------------------
    // ------------------------------ Methods -------------------------------
    // ----------------------------------------------------------------------

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

    // ----------------------------------------------------------------------
    // ------------------------------- MAIN ---------------------------------
    // ----------------------------------------------------------------------

    public static void main(String[] args )throws Exception {
        // The server (httpfs) needs to have a 'unique' ip address and port number. This 'unique' (ip,port) pair allows for a communication to be established from the client to the server.
        ServerSocket s = new ServerSocket(9999); // unique IP Adddress
        Socket client = s.accept(); // accept the connection when client requests the socket
        System.out.println("Connection is made from Client to Server");

        // Data from client
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

        // Data to client
        PrintWriter out = new PrintWriter(client.getOutputStream());

        // body
        String body = "Hello There BIATCH!";

        // header fields
        headerFields = new HashMap<String, String>();
        headerFields.put("Content-Type", "text/html");
        headerFields.put("Content-Length", Integer.toString(body.length()));

        // version
        String version = "HTTP/1.0";

        // status code
        String httpCode = HttpStatusCode.OK.getCode();
        String httpMessage = HttpStatusCode.OK.getMessage();

        String response = responseString(version, httpCode, httpMessage, headerFields, body);

        // Prepare response -  formatting HTTP Response:
        out.print(response);

        // flush
        out.flush();

        int c;
        while((c = in.read()) != -1){
            System.out.print((char)c);
        }
    }
}
