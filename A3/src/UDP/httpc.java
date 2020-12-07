package UDP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.InetAddress;
import java.io.PrintWriter;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Scanner;
import java.util.Set;


import static java.nio.channels.SelectionKey.OP_READ;


public class httpc {


    // Display appropriate help usage according to the input.
    public static void show_help(String argument){
        // This function will be called when the commands are unknown or the help command is given as arg
        if ( argument.equals("get") ) {
            // Show help get
            System.out.println("\thttpc help get\n"+
                    "\tusage: httpc get [-v] [-h key:value] \"URL\"\n"+
                    "\tGet executes a HTTP GET request for a given URL.\n"+
                    "\t-v Prints the detail of the response such as protocol, status, and headers.\n"+
                    "\t-h key:value Associates headers to HTTP Request with the format 'key:value'.\n");
        }

        else if ( argument.equals("post") ) {
            // Show help post
            System.out.println("\thttpc help post\n"+
                    "\tusage: \n\t\thttpc post [-v] [-h key:value] [-d inline-data] [-f file] \"URL\"\n"+
                    "\tPost executes a HTTP POST request for a given URL with inline data or from file.\n" +
                    "\t\t-v Prints the detail of the response such as protocol, status, and headers.\n" +
                    "\t\t-h key:value Associates headers to HTTP Request with the format 'key:value'.\n" +
                    "\t\t-d string Associates an inline data to the body HTTP POST request.\n" +
                    "\t\t-f file Associates the content of a file to the body HTTP POST request.\n" +
                    "\t\tEither [-d] or [-f] can be used but not both.\n");
        }
        else {
            // Show help
            System.out.println("\thttpc is a curl-like application but supports HTTP protocol only.\n" +
                    "\tUsage:\n" +
                    "\t\thttpc command [arguments]\n" +
                    "\tThe commands are:\n" +
                    "\t\tget executes a HTTP GET request and prints the response.\n" +
                    "\t\tpost executes a HTTP POST request and prints the response.\n" +
                    "\t\thelp prints this screen.\n\n" +
                    "\tUse \"httpc help [command]\" for more information about a command.\n");
        }
    }


    // Check if input has verbose
    public static boolean has_verbose(String[] input){
        // Valid inputs:
        // httpc (get|post) [-v] (-h "k:v")* [-d inline-data] [-f file] URL
        for (String s : input){
            if (s.toLowerCase().equals("-v"))
                return true;
        }
        return false;
    }


    // Returns the headers to be sent from the input arguments or an empty string
    public static String get_headers(String[] input){
        String h = "";
        // Valid inputs:
        // httpc (get|post) [-v] (-h "k:v")* [-d inline-data] [-f file] URL
        for (int i=0; i<(input.length-1); i++){
            if (input[i].toLowerCase().equals("-h")) {
                String header = input[i + 1];
                if ( header.contains(":")  )
                    h += input[i + 1] + "\r\n";
            }
        }
        return h;
    }


    // Returns the inline data to be sent from the input arguments or an empty string
    public static String get_data(String[] input) throws FileNotFoundException {
        // Valid inputs:
        // httpc (get|post) [-v] (-h "k:v")* [-d inline-data] [-f file] URL
        String d = "";

        for (int i=0; i<(input.length-1); i++){
            if (input[i].toLowerCase().equals("-d"))
                return input[i + 1];
            else if ( input[i].toLowerCase().equals("-f") )
                return file_to_string(input[i + 1]);
        }
        return "";
    }


    // Returns the inline data to be sent from the input arguments or an empty string
    public static String file_to_string(String file_name) throws FileNotFoundException {
        // Data to be returned
        String data = "";

        // Create file object and scanner for it
        File myObj = new File(file_name);
        Scanner myReader = new Scanner(myObj);

        // Read the file
        while (myReader.hasNextLine()) {
            data +=  myReader.nextLine()+"\n";
        }
        myReader.close();
        return data;
    }


    // Get the host, path and request from a given url in string format
    public static String[] get_components(String url){

        String host, path, request;

        int indexOfPath = url.indexOf('/',7);
        //int indexOfReq = url.lastIndexOf('/');
        int indexOfHost = 0;

        if( url.toLowerCase().startsWith("http://www.") )
            indexOfHost = 11;
        else if ( url.toLowerCase().startsWith("http://") )
            indexOfHost = 7;

        host = url.substring(indexOfHost);
        if ( indexOfPath > -1 ) {
            host = url.substring(indexOfHost,indexOfPath);
            path = url.substring( indexOfPath );
            request = url.substring( url.lastIndexOf('/') );
        }
        else
            path = request = "/";

        // host: anything between 'http://www.' and '/'
        // path: /Dir..[/Request]

        return new String[]{ host, path, request };
    }


    // Make sure arguments are valid
    public static boolean validate_args(String[] arguments){
        boolean get_flag = false;
        boolean d_flag = false;
        boolean f_flag = false;
        for (String s : arguments) {
            if (s.toLowerCase().equals("-f"))
                f_flag = true;
            if (s.toLowerCase().equals("-d"))
                d_flag = true;
        }
        if ( get_flag && (f_flag || d_flag) ) {
            show_help("get");
            return false;
        }
        else if ( f_flag && d_flag ) {
            show_help("post");
            return false;
        }
        return true;
    }


    // Create an HTTP request from an array of arguments
    public static void request(String[] arguments) throws IOException {

        // Validate the arguments
        // httpc get -v -h k:value 'url'
        if ( !validate_args(arguments) )
            System.exit(0);

        // In arguments locate the target host, and the request
        String method = arguments[0].toUpperCase();   // GET or POST
        String url = arguments[arguments.length-1];   // url is always last argument
        boolean verbose_flag = has_verbose(arguments);

        // Split the URL in host and request components
        String[] components = get_components(url);
        String host = components[0];
        //String path = components[1];
        String request = components[1];

        int port = 8007;
        if (host.contains(":")) {
            try {
                port = Integer.parseInt(host.split(":")[1]);
                if (port < 1)
                    throw new Exception();
            }
            catch (Exception e){ port = 8007; }
        }


        if (host.equals("")) {
            System.out.println("\tInvalid Host given\n");
            System.exit(0);
        }
        else if (host.startsWith("http://www.localhost") || host.startsWith("www.localhost") || host.startsWith("localhost")){
            host = null;
        }


        ////    New
        // get headers if present
        String header = get_headers(arguments);
        String body = "";

        // get body if method is post
        if(method.equals("POST")){
            body = get_data(arguments);
            header += "Content-Length:"+body.length()+"\r\n";
        }

        /* * * * * * * * * * * * * * * * *  Standard * * * * * * * * * * * * * * * * * * * * * *
         *   (GET|POST) + " " + URL + " HTTP/1.0\r\n"+ [header(s)+"\r\n"] +  "" + "\r\n" + [body]
         * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

        // Prepare and send the request
        String payload = method+" "+request+" "+"HTTP/1.0\r\n"+header+"\r\n"+body;

        SocketAddress r_address = new InetSocketAddress("localhost", 3000);
        InetSocketAddress s_address = new InetSocketAddress("localhost", port);

        System.out.println("Payload: "+payload+"\n");
        Packet p = new Packet(0,0,s_address.getAddress(),s_address.getPort(),payload.getBytes());
        System.out.println("Sent: "+p+"\n");
        runUDPclient(r_address,s_address,p);
    }

//        // Get the IP of the given site
//        InetAddress web = InetAddress.getByName(host);
//
//        // Connect to that IP on port 80
//        Socket sock = new Socket( web, port );
//
//        // Create the stream for communication
//        PrintWriter out = new PrintWriter(sock.getOutputStream());
//        Scanner in = new Scanner(sock.getInputStream());
//
//        // get headers if present
//        String header = get_headers(arguments);
//        String body = "";
//
//        // get body if method is post
//        if(method.equals("POST")){
//            body = get_data(arguments);
//            header += "Content-Length:"+body.length()+"\r\n";
//        }
//
//        /* * * * * * * * * * * * * * * * *  Standard * * * * * * * * * * * * * * * * * * * * * *
//         *   (GET|POST) + " " + URL + " HTTP/1.0\r\n"+ [header(s)+"\r\n"] +  "" + "\r\n" + [body]
//         * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
//
//        // Prepare and send the request
//        String statement = method+" "+request+" "+"HTTP/1.0\r\n"+header+"\r\n"+body;
//        out.write( statement );
//        out.flush();
//
//
//        boolean header_done=false;
//        // Receive answer from server
//        while (in.hasNextLine()) {
//            String line = in.nextLine();
//            // If verbose was not requested
//            if (!verbose_flag) {    // this part reads the header of the response from the server
//                // set flag after reading the header
//                if (line.isBlank()) { header_done = true; continue; }
//                // pint header after flag is set
//                if (header_done) { System.out.println("\t"+line); }
//            }
//            else // this part reads the content of the response from the server
//                System.out.println("\t"+line);
//        }
//        System.out.println();
//
//        // close all the streams and sockets
//        out.close();
//        in.close();
//        sock.close();



    ////    NEW
    //  Send a given Packet using UDP
    public static Packet runUDPclient(SocketAddress router, InetSocketAddress server, Packet p){
        try( DatagramChannel channel = DatagramChannel.open() ){

            channel.send(p.to_buffer(), router);

            // Receive packet within timeout.
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
            selector.select(10000);

            Set<SelectionKey> keys = selector.selectedKeys();
            if(keys.isEmpty()){
                return null;
            }

            // Get a single response.
            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_SIZE);
            SocketAddress route = channel.receive(buf);
            buf.flip();

            Packet received;

            if (buf.limit() < UDP.Packet.MIN_SIZE || buf.limit() > Packet.MAX_SIZE) {
                throw new IOException("Invalid packet length");
            }
            else{
                int typ = Byte.toUnsignedInt(buf.get());
                long seq = Integer.toUnsignedLong(buf.getInt());
                byte[] host = new byte[]{ buf.get(), buf.get(), buf.get(), buf.get() };
                InetAddress addr = InetAddress.getByAddress(host);
                int port = Short.toUnsignedInt(buf.getShort());
                byte[] payld = new byte[buf.remaining()];

                received = new Packet(typ, seq,addr,port,payld);
            }

//            System.out.println(received);

            keys.clear();
            return received;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void main(String[] args) {
        // Space out message
        System.out.println();

        // Any valid input Should have between 2 and 7 strings
        if ( args.length >= 2 && args.length <= 7 ){
            if ( args[0].toLowerCase().equals("help") ) {
                show_help(args[1].toLowerCase());
                System.exit(0);
            }
            else{   // If no help command is given

                if ( args[0].toLowerCase().equals("get") || args[0].toLowerCase().equals("post") ) {

                    try {
                        request(args);
                    }
                    catch (FileNotFoundException e) {  System.out.println("\tProblem opening the file."); }
                    catch (Exception e) {
                        System.out.println("Unknown error occurred.");
                        e.printStackTrace();
                    }

                }
                else{
                    show_help("");
                    System.exit(0);
                }
            }
        }
        else {
            show_help("");
            System.exit(0);
        }

    }

}


/* * * * * * * * * * * * * * * * * * * * * * * * * *
 * Possible cases:
 *
 * ----- show specific help -----
 *   httpc help
 *   httpc help get
 *   httpc help post
 *
 *
 * ----- get commands -----
 *   httpc get 'url'
 *   httpc get -v 'url'
 *   httpc get -h k:value 'url'
 *   httpc get -v -h k:value 'url'
 *
 *
 * ----- post commands -----
 *   httpc post 'url'
 *   httpc post -v 'url'
 *   httpc post -h k:value 'url'
 *
 *   httpc post -d inline-data 'url'
 *   httpc post -v -d inline-data 'url'
 *   httpc post -h k:value -d inline-data 'url'
 *   httpc post -v -h k:value -d inline-data 'url'
 *
 *   httpc post -f file 'url'
 *   httpc post -v -f file 'url'
 *   httpc post -h k:value -f file 'url'
 *   httpc post -v -h k:value -f filename 'url'
 *
 * * * * * * * * * * * * * * * * * * * * * * * * */
