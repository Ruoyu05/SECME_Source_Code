import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.Gson;

import ClassType.JsonMessage;
import ClassType.ServerPublicKey;



public class Postman extends WebSocketClient {
	String username;
	String ip;
	String port;
	String postMessage;

	public Postman(URI serverUri,String postMessage) {
		super(serverUri);
		this.postMessage = postMessage;
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		// System.out.println("Postman: On Open");
	}
	@Override
	public void onMessage(ByteBuffer bytes) {
		super.onMessage(bytes);
		Charset charset = StandardCharsets.UTF_8;
		String message = charset.decode(bytes).toString();
		dealWithMessage(message);
	}

	@Override
	public void onMessage(String message) {
		System.out.println("Postman(Received):" + message);
		dealWithMessage(message);
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		// System.out.println("Postman: On Close");
	}

	@Override
	public void onError(Exception ex) {
		// System.out.println("Postman: On Error");

	}

	public void dealWithMessage(String message){
		if (JsonHelper.isJson(message)) {
			Gson gson = new Gson();
			var json = gson.fromJson(message, JsonMessage.class);
			// System.out.println("Postman:Get Type:" + json.type);
			// System.out.println("Postman:Get JsonMessage:" + json.contents.toString());
			String contentsJson = gson.toJson(json.contents);
			ServerPublicKey serverPublicKey_Json = gson.fromJson(contentsJson, ServerPublicKey.class);
			String server_publickey = serverPublicKey_Json.server_publickey;
			try {
				PublicKey pk = RSAHelper.getPublicKey_from_java(server_publickey);
				String temp_postMessage = RSAHelper.encryptJsonMessage(postMessage, pk);
				send(temp_postMessage.getBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if(message.equals("false")){
			// System.out.println("Postman:Get False");
			//关闭连接
			close();
		}else if(message.equals("true")){
			// System.out.println("Postman:Get True");
			//关闭连接
			close();
		}
	}



}
