import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.plaf.basic.BasicBorders.RadioButtonBorder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Handlers {
    static class RootHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String requestMethod = exchange.getRequestMethod();
                switch (requestMethod) {
                    case "GET":
                        // Get the requested file name from the request URI
                        String requestedFile = exchange.getRequestURI().getPath().substring(1);
                        // Determine the MIME type of the file based on its extension
                        String mimeType;

                        if (requestedFile.endsWith(".html") || requestedFile.endsWith(".htm")) {
                            mimeType = "text/html";
                        } else if (requestedFile.endsWith(".txt")) {
                            mimeType = "text/plain";
                        } else if (requestedFile.endsWith("") || requestedFile.endsWith(".jpg")) {
                            mimeType = "image/jpeg";
                        }else {
                            // Unsupported file type
                            exchange.sendResponseHeaders(404, -1);
                            exchange.close();
                            return;
                        }
                        
                        // Open the requested file and set the response headers
                        File file;
                        if (requestedFile == "") {
                            mimeType = "image/jpeg";
                            file = new File("src/index.html");
                        } else {
                            file = new File(requestedFile);
                        }
                        if (file.exists() && !file.isDirectory()) {
                            long fileLength = file.length();
                            exchange.sendResponseHeaders(200, fileLength);
                            exchange.getResponseHeaders().set("Content-Type", mimeType);
                            exchange.getResponseHeaders().set("Content-Length", String.valueOf(fileLength));
                             
                            // Write the file to the response body
                            OutputStream os = exchange.getResponseBody();
                            FileInputStream fis = new FileInputStream(file);
                            byte[] buffer = new byte[100000];
                            int count = 0;
                            while ((count = fis.read(buffer)) >= 0) {
                                os.write(buffer, 0, count);
                            }
                            fis.close();
                            os.close();
                        } else {
                            // File not found
                            exchange.sendResponseHeaders(404, -1);
                            exchange.close();
                        }
                        break;

                    case "POST":
                        // handle POST request
                        break;
                    case "DELETE":
                        // handle DELETE request
                        break;
                    default:
                        // handle unsupported request
                        break;
                }
            } catch(Exception e) {
                throw new UnsupportedOperationException("Unimplemented method 'handle'");
            }
        }
        
    }
}

