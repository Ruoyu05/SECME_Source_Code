import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import ClassType.*;

public class WebScoketServerBuilder extends WebSocketServer {

	private String server_publicKeyStr = "";
	private String server_privateKeyStr = "";
	private String client_publicKeyStr = "";
	private String username = "none";

	private MySqlConnect mysqlConnection = null;

	public WebScoketServerBuilder(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
	}

	public WebScoketServerBuilder(InetSocketAddress address) {
		super(address);
	}

	public WebScoketServerBuilder(int port, Draft_6455 draft) {
		super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
	}


	@Override
	public void onStart() {
		try {

			mysqlConnection = new MySqlConnect();

			// 获取服务器公钥
			server_publicKeyStr = mysqlConnection.getServerPublicKeyStr();
			server_privateKeyStr = mysqlConnection.getServerPrivateKeyStr();

			System.out.println("Server: Set Server Public Key -> " + server_publicKeyStr);
			System.out.println("Server: Waitting for Client.");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake arg1) {
		// 客户端连接时

		// 制作报文
		ServerPublicKey serverPublicKey = new ServerPublicKey();
		serverPublicKey.server_publickey = server_publicKeyStr;
		JsonMessage jsonMessage = new JsonMessage();
		jsonMessage.type = "ServerPublicKey";
		jsonMessage.contents = serverPublicKey;

		// 发送服务器公钥给客户端 byte
		conn.send(JsonHelper.buildJsonMessage(jsonMessage).getBytes());

	}

	@Override
	public void onClose(WebSocket conn, int arg1, String arg2, boolean arg3) {
		mysqlConnection.close();
		System.out.println("A client is disconnected.");
	}

	@Override
	public void onError(WebSocket conn, Exception arg1) {
		mysqlConnection.close();
	}

	@Override
	public void onMessage(WebSocket conn, String textStr) {
		// System.out.println("Server Received:(String) -> " + textStr);
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer messageByte) {
		super.onMessage(conn, messageByte);

		Charset charset = StandardCharsets.UTF_8;
		String messageStr = charset.decode(messageByte).toString();
		System.out.println("Received:" + messageStr);

		// 解密
		try {
			String decryptMessage = RSAHelper.decryptByPrivateKey(messageStr, server_privateKeyStr);
			System.out.println("Decrypt:" + decryptMessage);

			ResultForJson resultForJson = JsonHelper.readJsonMessage(decryptMessage, conn, client_publicKeyStr);

			if (resultForJson.needDealReturnValue) {
				switch (resultForJson.returnCode) {
					case "client_publickey":
						client_publicKeyStr = resultForJson.returnValue;
						// System.out.println("保存客户端公钥:" + client_publicKeyStr);
						break;
					case "client_publickey_error":
						conn.send(resultForJson.repalyMessage.getBytes());
						conn.close();
						break;
					case "AuthKeysSuccessful":
						// System.out.println("认证密钥成功，启动轮询消息!");

						// 为已登陆的用户建立一个进程
						// System.out.println("为已登陆的用户建立一个查询进程");
						Thread clientThread = new Thread(new ClientHolder(conn, username, client_publicKeyStr));
						clientThread.setName("Client_" + username);
						clientThread.start();
						break;
						case "SetUserName":
						//设定用户名
						username = resultForJson.returnValue;
						// System.out.println("设定用户名:"+username);
						break;
					default:
						break;
				}
			}

			if (resultForJson.needReplay) {
				if (client_publicKeyStr.equals("")) {
					conn.send(resultForJson.repalyMessage.getBytes());
				} else {
					String encryptedMessage = RSAHelper.encryptJsonMessage(resultForJson.repalyMessage, client_publicKeyStr);
					conn.send(encryptedMessage.getBytes());
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println("Server Received:(ByteBuffer) -> 解密失败,终止链接");
			conn.close();
		}

	}

}
