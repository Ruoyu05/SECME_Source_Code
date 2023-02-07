import java.sql.SQLException;

import org.java_websocket.WebSocket;

public class ClientHolder implements Runnable {

	WebSocket conn;
	String username;
	String online_uuid;
	String chat_publickey;

	public ClientHolder(WebSocket conn, String username,String chat_publickey) {
		this.conn = conn;
		this.username = username;
		this.chat_publickey = chat_publickey;
		//为在线用户创建一条online_uuid记录
		try {
			online_uuid = new MySqlConnect().updateOnlineUUID(username);
			System.out.println("Client Online UUID:" + online_uuid);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		for (;;) {
			try {
				if (conn.isClosed()) {
					// System.out.println("服务已经断开,连接结束");
					break;
				} else {
					try {
						// System.out.println("数据库中UUID:" + new MySqlConnect().getOnlineUUID(username));
						// System.out.println("新建立的UUID:" + online_uuid);
						//检查数据库 如果在线服务器的UUID与自己不同 则退出
						if(new MySqlConnect().getOnlineUUID(username).equals(online_uuid)){
							// System.out.println("UUID相同,保持连接");
						}else{
							// System.out.println("UUID不同,结束连接");
							conn.close();
							break;
						}
						// System.out.println("查询新消息并发送给客户端");
						// 检查数据库 如果数据库中有新的消息 则发给客户端
						new MySqlConnect().sendNewLetter(conn, username,chat_publickey);


					} catch (SQLException e1) {
						e1.printStackTrace();
					}
					// new MysqlConnect().getNewMessage(conn, username);

				}
				// 延迟5秒
				Thread.sleep(5000);
			} catch (Exception e) {
				System.out.println("Exception");
				e.printStackTrace();
				break;
			}

		}

	}

}
