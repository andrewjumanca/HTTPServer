import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPServer {
	public void Start(int port) {
		try(ServerSocket server = new ServerSocket(port)) {
			System.out.println("Server listening on port: " + port);
            Socket socket = null;
			Handlers.RootHandler rootHandler = new Handlers.RootHandler();
            while ((socket = server.accept()) != null) {
                Socket threadSocket = socket;
                new Thread( () -> {
					try {
						rootHandler.handle(threadSocket);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}).start();
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
	}
}