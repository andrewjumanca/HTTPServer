import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.BufferedReader;

public class Handlers {
    static class RootHandler {
        public void handle(Socket socket) throws IOException {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);) {
                String curLine;
                String[] requestLineVals;
                String requestType;
                String requestedFile;

                if((curLine = in.readLine()) != null) {
                    requestLineVals = curLine.split(" ");
                    requestType = requestLineVals[0];
                    requestedFile = requestLineVals[1].substring(1);
    
                    switch(requestType) {
                        case "GET":
                            File file = new File("src" + File.separator + requestedFile);
                            if (file.exists() & !file.isDirectory()) {
                                BufferedReader reader = new BufferedReader(new FileReader(file));
                                String line;

                                out.println("HTTP/1.1 200 OK");
                                out.println("Content-Type: text/plain");
                                out.println("Content-Length: " + file.length());
                                out.println();
                                while ((line = reader.readLine()) != null) {
                                    out.println(line);
                                }
                                httpCats(out, 200);
                                reader.close();
                            } else {
                                out.println("HTTP/1.1 404 Not Found");
                                out.println("Content-Type: text/html");
                                out.println();
                                httpCats(out, 404);
                            }
                            break;
                        case "HEAD":
                            file = new File("src" + File.separator + requestedFile);
                            if (file.exists() & !file.isDirectory()) {
                                out.println("HTTP/1.1 200 OK");
                                out.println("Content-Type: text/plain");
                                out.println("Content-Length: " + file.length());
                                out.println();
                                httpCats(out, 200);
                            } else {
                                out.println("HTTP/1.1 404 Not Found");
                                out.println("Content-Type: text/html");
                                out.println();
                                httpCats(out, 404);
                            }
                            break;

                        case "POST":
                            file = new File("src" + File.separator + requestedFile);
                            if (!file.exists() & !file.isDirectory()) {
                                out.println("HTTP/1.1 404 Not Found");
                                out.println("Content-Type: text/plain");
                                out.println("Content-Length: " + file.length());
                                httpCats(out, 404);
                                out.println();
                            } else {
                                int contentLength = 0;
                                String line;
                                
                                while ((line = in.readLine()) != null && !line.isEmpty()) {
                                    if (line.startsWith("Content-Length:")) {
                                        contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
                                    }
                                }
                    
                                StringBuilder requestBody = new StringBuilder();
                                for (int i = 0; i < contentLength; i++) {
                                    requestBody.append((char)in.read());
                                }
                                
                                if (requestedFile == "") {
                                    out.println("HTTP/1.1 404 Not Found");
                                    out.println("Content-Type: text/html");
                                    out.println();
                                    httpCats(out, 400);
                                } else {
                                    if (requestedFile.substring(requestedFile.lastIndexOf(".") + 1).equals("txt")) {
                                    FileWriter fileWriter = new FileWriter(file, true);
                                    fileWriter.write(requestBody.toString());
                                    fileWriter.close();
                    
                                    out.println("HTTP/1.1 200 OK");
                                    out.println("Content-Type: text/html");
                                    out.println();
                                    httpCats(out, 200);
                                    } else {
                                        out.println("HTTP/1.1 400 Bad Request");
                                        out.println("Content-Type: text/html");
                                        out.println();
                                        httpCats(out, 400);
                                    }
                                }
                            }
                            break;

                        case "PUT":
                            file = new File("src" + File.separator + requestedFile);
                            int contentLength = 0;
                            String line;
                            
                            while ((line = in.readLine()) != null && !line.isEmpty()) {
                                if (line.startsWith("Content-Length:")) {
                                    contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
                                }
                            }
                            
                            StringBuilder requestBody = new StringBuilder();
                            for (int i = 0; i < contentLength; i++) {
                                requestBody.append((char)in.read());
                            }
                            
                            PrintWriter fileWriter = new PrintWriter(file);
                            fileWriter.write(requestBody.toString());
                            fileWriter.close();
                
                            out.println("HTTP/1.1 200 OK");
                            out.println("Content-Type: text/html");
                            out.println();
                            httpCats(out, 200);
                            break;
                            
                        case "DELETE":
                            file = new File("src" + File.separator + requestedFile);
                            if (file.exists()) {
                                if (file.delete()) {
                                    out.println("HTTP/1.1 200 OK");
                                    out.println("Content-Type: text/plain");
                                    out.println();
                                    httpCats(out, 204);
                                } else {
                                    out.println("HTTP/1.1 500 Internal Server Error");
                                    out.println("Content-Type: text/html");
                                    out.println();
                                    httpCats(out, 500);
                                }
                            } else {
                                out.println("HTTP/1.1 404 Not Found");
                                out.println("Content-Type: text/html");
                                out.println();
                                httpCats(out, 404);
                            }
                            break;

                        default:
                            out.println("HTTP/1.1 405 Method Not Allowed");
                            out.println("Content-Type: text/html");
                            out.println();
                            httpCats(out, 405);
                            break;
                    }
                }
            } catch(Exception err) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                err.printStackTrace();
                out.println("HTTP/1.1 500 Internal Server Error");
                out.println("Content-Type: text/html");
                out.println();
                httpCats(out, 500);
            }
        }

        private static String getStatusText(int statusCode) {
            switch (statusCode) {
                case 200:
                    return "OK";
                case 201:
                    return "Created";
                case 400:
                    return "Bad Request";
                case 401:
                    return "Unauthorized";
                case 403:
                    return "Forbidden";
                case 404:
                    return "Not Found";
                case 500:
                    return "Internal Server Error";
                default:
                    return "";
            }
        }

        public static void httpCats(PrintWriter out, int errorCode) throws IOException {
            String ERROR = "https://github.com/andrewjumanca/HTTPServer/blob/main/src/cats/" + errorCode + ".jpg?raw=true";
            String errorHtml = "<html style=\"height: 100%;\">\n" +
                    "    <head>\n" +
                    "        <meta name=\"viewport\" content=\"width=device-width, minimum-scale=0.1\">\n" +
                    "        <style type=\"text/css\"></style>\n" +
                    "        <title>" + errorCode + "</title>\n" +
                    "    </head>\n" +
                    "    <body style=\"margin: 0px; background: #0e0e0e; height: 100%\">\n" +
                    "        <img style=\"display: block; margin: auto;background-color: hsl(0, 0%, 90%);transition: background-color 300ms;\" src=\"" + ERROR + "\">\n" +
                    "    </body>\n" +
                    "</html>\n";

            out.write(errorHtml);
            out.flush();
        }
    }
}

