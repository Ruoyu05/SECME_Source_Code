import java.net.URI;


public class RobotClient implements Runnable {

	URI serverUri;
	String username;
	String ip;
	String port;
	String postMessage;

	public RobotClient(URI serverUri,String postMessage) {
		this.serverUri = serverUri;
		this.postMessage = postMessage;
	}

	@Override
	public void run() {
		System.out.println("Server: Run a Client Robot");
		System.out.println("Client Robot:"+serverUri);
		Postman postman = new Postman(serverUri,postMessage);
		postman.connect();

	}
}
