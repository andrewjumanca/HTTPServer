
public class Main {
	public static int port = 8000;
	public static void main(String[] args) {
		HTTPServer httpsServer = new HTTPServer();
		httpsServer.Start(port);
		System.out.println(System.getProperty("user.dir"));
		System.out.println(Main.class.getClassLoader().getResource("").getPath());
		
	}
}