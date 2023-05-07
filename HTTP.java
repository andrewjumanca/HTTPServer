import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class HTTP {
    private static final String BASE_DIRECTORY = "test_files";

    //Mime type just descripes how the file should be processed
    private static final Map<String, String> MIME_TYPES = Map.of(
            "html", "text/html",
            "txt", "text/plain",
            "jpg", "image/jpeg",
            "png", "image/png"
    );


    private static void sendSuccessResponse(PrintWriter headerOut, int statusCode, String statusText) {
        headerOut.println("HTTP/1.1" + " " + statusCode + " " + statusText);
        headerOut.println("Content-Type: text/plain");
        headerOut.println("Content-Length: 0");
        headerOut.println("Connection: close"); 
        headerOut.println();
        headerOut.flush();
    }
    
    //creating a concise way to sendStatus if there is any issues
    private static void sendStatusError(PrintWriter headerOut, int statusCode, String statusText) {
            headerOut.println("HTTP/1.1 " + statusCode + " " + statusText);
            headerOut.println("Content-Type: text/plain");
            headerOut.println("Content-Length: " + statusText.length());
            headerOut.println();
            headerOut.println(statusText);
            headerOut.flush();     

        //Example: 
        // HTTP/1.1 404 Not Found
        // Content-Type: text/html
        // Content-Length: 0

    }

    //getting the file extension to determine the MIME
    private static String getFileExtension(File file) {
        String filename = file.getName();
        int dotIndex = filename.lastIndexOf('.'); //lastIndexOf get the last occurance of character if not found returns -1
        if (dotIndex == -1) {
            return "";
        } else {
            return filename.substring(dotIndex + 1);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = 8080;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port: " + port);
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    handleRequest(socket);
                } catch (IOException e) {
                    System.err.println("Error handling request: " + e);
                }
            }
        }
    }

    private static void handleRequest(Socket socket) throws IOException {

        try{ 
                //We set up a bufferReader to read text ad the StreamReader is to convert those bytes to readab;e characters
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            
            //This is like the envlope of a letter, this allows to give detials about the message
            PrintWriter headerOut = new PrintWriter(out, true); //printWriter convient way to send text data to client

            //Example of request line: "GET /index.html HTTP/1.1"
            String requestLine = in.readLine();
            if (requestLine == null) {
                return;
            }

            //Allows us to spilt the requst line and then set the file path
            StringTokenizer tokenizer = new StringTokenizer(requestLine);
            String method = tokenizer.nextToken();
            String path = tokenizer.nextToken();

            String filePath = BASE_DIRECTORY + path;
            File file = new File(filePath);

            //Switch method is something new I am trying, it is basically similar to if elif else condtional
            //if we don't use break for the switch it will continue to drop to various cases
            switch (method) {
                case "GET":
                    handleGet(file, headerOut, out);
                    break;
                case "POST":
                    handlePost(file, in, headerOut);
                    break;
                case "PUT":
                    handlePut(file, in, headerOut);
                    break;
                case "DELETE":
                    handleDelete(file, headerOut);
                    break;
                case "OPTIONS":
                    handleOptions(file, headerOut);
                    break;
                case "HEAD":
                    handleHead(file, headerOut);
                    break;
                default:
                    sendStatusError(headerOut, 405, "Method Not Allowed");
                    break;
            }

        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    // Implement the different request methods below
    private static void handleGet(File file, PrintWriter headerOut, BufferedWriter out) throws IOException {
        if (!file.exists()) {
            sendStatusError(headerOut, 404, "Not Found");
            return;
        }
        
        if (!file.isFile()) {
            sendStatusError(headerOut, 400, "Bad Request");
            return;
        }

        String extension = getFileExtension(file);

        //checks if the meme type is in the map, and if not then will defualt to other option
        String contentType = MIME_TYPES.getOrDefault(extension, "application/octet-stream"); //default is just handing it as binary data 
        

        //Client will see the header first then recieve the file
        headerOut.println("HTTP/1.1 200 OK");
        headerOut.println("Content-Type: " + contentType);
        headerOut.println("Content-Length: " + file.length());

        //marks end of the header
        headerOut.println();

        //send the headerOut
        headerOut.flush();
        
        //we have to read the file that we want to send back to the client

        //Look into the use of bufferedREader vs InputStream. 
        // Currently I know that this is reading characters instead of bytes
        //Then BufferWriter is writing characters instead of bytes
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        while ((line = reader.readLine()) != null) {
            out.write(line);
            out.newLine();
            out.flush();
        }

        reader.close();
    }


    //We tend to override whatever exists previously 
    private static void handlePost(File file, BufferedReader in, PrintWriter headerOut) throws IOException {
        
        //adding this avoided my handing but it tells us when the line ends.
        String line;
        int contentLength = -1;
        while (!(line = in.readLine()).equals("")) {
            if (line.startsWith("Content-Length: ")) {
                contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
            }
        }
    
        if (contentLength == -1) {
            sendStatusError(headerOut, 400, "Bad Request");
            return;
        }
        
        if (!file.exists()) {
            sendStatusError(headerOut, 404, "Not Found");
            return;
        }

        if (!file.isFile()) {
            sendStatusError(headerOut, 400, "Bad Request");
            return;
        }

        String extension = getFileExtension(file);
        if (!extension.equals("txt")) {
            sendStatusError(headerOut, 400, "Bad Request");
            return;
        }

        //allows me to read based on the contentLength so that I don't have it reading 
        // indefiniently. Previously I was using a while loop that would get stuck. 
        // It pretty much did not exit while loop because I was not setting it to the end of the
        // content, I feel this is way is cleaner and more accurate then fixing my 
        //previous code to have while (in.readline != " ")

        //we know that we are reading the content as we read the header before. In.read tracks the current position 
        // it is at in the input stream. 
        char[] content = new char[contentLength];
        in.read(content, 0, contentLength);

        FileWriter fileWriter = new FileWriter(file, true);
        fileWriter.write(content);
        fileWriter.close();

        sendSuccessResponse(headerOut, 200, "OK");
        // return; 
    }

    private static void handlePut(File file, BufferedReader in, PrintWriter headerOut) throws IOException {
        
        // Read headers first until the blank line and then once it hits blank line
        // we can then after this while loop look at the body indicated by the space. 
        String line;
        int contentLength = -1;
        while (!(line = in.readLine()).equals("")) {
            if (line.startsWith("Content-Length: ")) {
                contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
            }
        }
    
        if (contentLength == -1) {
            sendStatusError(headerOut, 400, "Bad Request");
            return;
        }
    
        // Creates new file
        if (file.exists()) {
            file.delete();
        }
    
        String extension = getFileExtension(file);
        if (!extension.equals("txt")) {
            sendStatusError(headerOut, 400, "Bad Request");
            return;
        }
    
        // Read the content
        char[] content = new char[contentLength];
        in.read(content, 0, contentLength);
    
        // Write the content to the file
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(content);
        fileWriter.close();
    
        sendSuccessResponse(headerOut, 200, "OK");
    }

    private static void handleDelete(File file, PrintWriter headerOut) throws IOException {
        if (!file.exists()) {
            sendStatusError(headerOut, 404, "Not Found");
            return;
        }

        //this will execute the delete --> if it didnt execute enters if contional
        //else we return status OK 
        if (!file.delete()) {
            sendStatusError(headerOut, 500, "Internal Server Error");
            return;
        }

        sendStatusError(headerOut, 200, "OK");
        return;
    }

    //When asking for options the server will respond with showing its Allow header field
    private static void handleOptions(File file, PrintWriter headerOut) throws IOException {
        headerOut.println("HTTP/1.1 200 OK");
        headerOut.println("Allow: GET, POST(only .txt files), PUT(only .txt files), DELETE, OPTIONS, HEAD");
        headerOut.println();
        headerOut.flush();
    }

    //we generally need to throw IOException when we are dealing with inputstreams and output streams
    private static void handleHead(File file, PrintWriter headerOut) throws IOException {
        if(!file.exists()){
            sendStatusError(headerOut, 404, "Not Found");
            return;
        }
        if (!file.isFile()){
            sendStatusError(headerOut,400,"Bad Request");
            return; 
        }

        String extension = getFileExtension(file);
        String contentType = MIME_TYPES.getOrDefault(extension, "application/octet-stream");

        headerOut.println("HTTP/1.1 " + 200 + " OK");
        headerOut.println("Content-Type: " + contentType);
        headerOut.println("Content-Length: " + file.length());
        headerOut.println();
        headerOut.flush();
        //Example: 
        // HTTP/1.1 404 Not Found
        // Content-Type: text/html
        // Content-Length: 0
    }

   
}