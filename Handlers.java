import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Handlers {
    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String requestMethod = exchange.getRequestMethod();
                // Get the requested file name from the request URI
                String requestedFile = exchange.getRequestURI().getPath().substring(1);
                // Determine the MIME type of the file based on its extension
                String mimeType;
                switch (requestMethod) { 
                    
                    case "GET":
                        if (requestedFile.endsWith(".html") || requestedFile.endsWith(".htm")) {
                            mimeType = "text/html";
                        } else if (requestedFile.endsWith(".txt")) {
                            mimeType = "text/plain";
                        } else if (requestedFile.endsWith("") || requestedFile.endsWith(".jpg")) {
                            mimeType = "image/jpeg";    
                        }else {
                            // Unsupported file type
                            httpCats(exchange, 404);
                            return;
                        }
                        
                        // Open the requested file and set the response headers
                        File file;
                        if (requestedFile == "") {
                            mimeType = "image/jpeg";
                            file = new File("src/index.html");
                        } else {
                            file = new File("src/" + requestedFile);
                        }
                        if (file.exists() && !file.isDirectory()) {
                            long fileLength = file.length();
                            exchange.sendResponseHeaders(200, fileLength);
                            exchange.getResponseHeaders().set("Content-Type", mimeType);
                            exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileLength));
                             
                            // Write the file to the response body
                            OutputStream os = exchange.getResponseBody();
                            FileInputStream fis = new FileInputStream(file);
                            byte[] buffer = new byte[1024];
                            int count = 0;
                            while ((count = fis.read(buffer)) >= 0) {
                                os.write(buffer, 0, count);
                            }
                            fis.close();
                            os.close();
                        } else {
                            // File not found
                            httpCats(exchange, 404);
                        }
                        break;

                    case "POST":                    
                        file = new File("src/" + requestedFile);
                        if (!file.exists() || file.isDirectory()) {
                            // If the specified file does not exist or is a directory, return a 404 (Not Found) response
                            httpCats(exchange, 404);
                            return;
                        }
                    
                        // Get the request body
                        InputStream requestBody = exchange.getRequestBody();
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    
                        int nRead;
                        byte[] data = new byte[1024];
                        while ((nRead = requestBody.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }
                        buffer.flush();
                    
                        try (FileOutputStream fos = new FileOutputStream(file, true)) {
                            fos.write(buffer.toByteArray());
                            fos.flush();
                        }
                    
                        exchange.sendResponseHeaders(200, 0);
                        exchange.close();
                        break;
                    case "PUT":
                        // Extract the path from the HTTP request
                        String path = exchange.getRequestURI().getPath();
                                
                        // Create a file object based on the path
                        file = new File("src/" + path);

                        // Check if the file already exists
                        boolean fileExists = file.exists();

                        // Get the request body as a stream of bytes
                        requestBody = exchange.getRequestBody();

                        // Create a byte array to hold the request body
                        byte[] requestBodyBytes = requestBody.readAllBytes();

                        // Write the request body to the file
                        FileOutputStream fileOutputStream = new FileOutputStream(file, false);
                        fileOutputStream.write(requestBodyBytes);
                        fileOutputStream.close();

                        // Create the response message
                        if (fileExists) {
                            httpCats(exchange, 200);
                        } else {
                            httpCats(exchange, 201);
                        }
                        break;
                    case "DELETE":
                        if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                            URI uri = exchange.getRequestURI();
                            path = uri.getPath();
                            file = new File("src/" + path);
                            if (file.exists() && file.isFile()) {
                                if (file.delete()) {
                                    httpCats(exchange, 200);
                                } else {
                                    httpCats(exchange, 500);
                                }
                            } else {
                                httpCats(exchange, 404);
                            }
                        } else {
                            httpCats(exchange, 405);
                        }
                        break;
                    case "HEAD":
                        if (requestedFile.endsWith(".html") || requestedFile.endsWith(".htm")) {
                            mimeType = "text/html";
                        } else if (requestedFile.endsWith(".txt")) {
                            mimeType = "text/plain";
                        } else if (requestedFile.endsWith("") || requestedFile.endsWith(".jpg")) {
                            mimeType = "image/jpeg";    
                        }else {
                            // Unsupported file type
                            httpCats(exchange, 404);
                            return;
                        }
                        if (requestedFile == "") {
                            mimeType = "image/jpeg";
                            file = new File("src/index.html");
                        } else {
                            file = new File("src/" + requestedFile);
                        }
                        if (file.exists() && !file.isDirectory()) {
                            String response = "";
                            exchange.getResponseHeaders().set("Content-Type", mimeType);
                            exchange.getResponseHeaders().set("Content-Length", Integer.toString(response.length()));
                            exchange.sendResponseHeaders(200, -1); // Note: -1 for content length as no response body is sent for HEAD request
                            exchange.close();
                        } else {
                            httpCats(exchange, 404);
                        }
                        break;
                    default:
                        httpCats(exchange, 405);
                        break;
                }
            } catch(Exception e) {
                throw new UnsupportedOperationException("Unimplemented method 'handle'");
            }
        }

        public void httpCats(HttpExchange exchange, int errorCode) throws IOException {
            String ERROR = "https://github.com/andrewjumanca/HTTPServer/blob/main/src/cats/" + errorCode + ".jpg?raw=true";
            String errorHtml = "<html style=\"height: 100%;\">\n" +
              "    <head>\n" +
              "        <meta name=\"viewport\" content=\"width=device-width, minimum-scale=0.1\">\n" +
              "        <style type=\"text/css\"></style>\n" +
              "        <title>200</title>\n" +
              "    </head>\n" +
              "    <body style=\"margin: 0px; background: #0e0e0e; height: 100%\">\n" +
              "        <img style=\"display: block; margin: auto;background-color: hsl(0, 0%, 90%);transition: background-color 300ms;\" src=\"" + ERROR + "\">\n" +
              "    </body>\n" +
              "</html>\n";

            exchange.sendResponseHeaders(errorCode, errorHtml.length());
            OutputStream responseBody = exchange.getResponseBody();
            responseBody.write(errorHtml.getBytes());
            responseBody.close();
            exchange.close();
        }
    }
}

