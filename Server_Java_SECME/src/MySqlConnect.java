import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.java_websocket.WebSocket;

import ClassType.JsonMessage;
import ClassType.PostMessage;
import ClassType.SendTo;

public class MySqlConnect {

	private Connection connection = null;
	private Statement stmt = null;
	private ResultSet rs = null;

	private String host = "localhost";
	private int port = 3306;
	private String db_name = "SECME_APP";
	private String db_account = "root";
	private String db_password = "12345678";

	public MySqlConnect() throws SQLException {
		creatDatabase();
	}

	// 新建数据库结构
	private void creatDatabase() throws SQLException {

		String urlStr = "jdbc:mysql://" + host + ":" + port;
		Connection connect = DriverManager.getConnection(urlStr, db_account, db_password);
		Statement statement = connect.createStatement();
		statement.executeUpdate(
				"CREATE DATABASE IF NOT EXISTS " + db_name + " default character set utf8mb4 COLLATE utf8mb4_general_ci");
		statement.close();
		connect.close();

		urlStr = "jdbc:mysql://" + host + ":" + port + "/" + db_name;
		connection = DriverManager.getConnection(urlStr, db_account, db_password);
		stmt = connection.createStatement();

		// 新建基本数据表
		// 存储表 server_info 服务器公钥私钥 id privatekey publickey
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS server_info (id INTEGER PRIMARY KEY AUTO_INCREMENT,privatekey TEXT,publickey TEXT)");

		// 存储表 user_account 用户账户 id username password certify_publickey
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS user_account (id INTEGER PRIMARY KEY AUTO_INCREMENT,username TEXT,password TEXT,certify_publickey TEXT,online_uuid TEXT)");

		// 聊天 chat_message 数据缓存 signature send_from username host port inside got_time
		// uuid
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS chat_message (id INTEGER PRIMARY KEY AUTO_INCREMENT,username TEXT,uuid TEXT,signature TEXT,send_from TEXT,host TEXT,port TEXT,inside TEXT,got_time DATE)");
	}

	public String updateOnlineUUID(String username) {
		// 为在线设备注册一个online_uuid
		String online_uuid = UUID.randomUUID().toString();
		String sql = "UPDATE `user_account` SET `online_uuid` = \'" + online_uuid + "\' WHERE `username` = \'" + username
				+ "\'";
		try {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return online_uuid;
	}

	public String getOnlineUUID(String username) {
		String online_uuid = "";

		try {
			String sql = "SELECT `online_uuid` FROM `user_account` WHERE `username` = \'" + username + "\'";
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				online_uuid = rs.getString("online_uuid");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return online_uuid;
	}

	public void sendNewLetter(WebSocket conn, String username, String chat_publickey) {
		String sql = "SELECT * FROM `chat_message` WHERE `username` = \'" + username + "\'";
		try {
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				SendTo send_to = new SendTo();
				send_to.host = rs.getString("host");
				send_to.port = rs.getString("port");
				send_to.username = rs.getString("username");

				PostMessage postMessage = new PostMessage();
				postMessage.inside = rs.getString("inside");
				postMessage.send_from = rs.getString("send_from");
				postMessage.signature = rs.getString("signature");
				postMessage.send_to = send_to;
				postMessage.message_uuid = rs.getString("uuid");

				JsonMessage jsonMessage = new JsonMessage();
				jsonMessage.type = "NewLetter";
				jsonMessage.contents = postMessage;

				String jsonMessageStr = JsonHelper.buildJsonMessage(jsonMessage);
				String encryptedMessage = RSAHelper.encryptJsonMessage(jsonMessageStr, chat_publickey);

				conn.send(encryptedMessage.getBytes());

			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteLetter(String message_uuid) {
		try {
			String sql = "DELETE FROM `chat_message` WHERE uuid = \'" + message_uuid + "\'";
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	public Boolean savePostMessage(PostMessage postLetter) {

		String uuid = UUID.randomUUID().toString();
		// System.out.println("uuid:" + uuid);

		String signature = postLetter.signature;
		String inside = postLetter.inside;
		String send_from = postLetter.send_from;
		String username = postLetter.send_to.username;
		String host = postLetter.send_to.host;
		String port = postLetter.send_to.port;

		// 如果用户存在
		if (isUserExist(username)) {
			try {
				String sql = "INSERT INTO `chat_message` (`uuid`, `signature`, `send_from`, `username`, `host`, `port`, `inside`, `got_time`) VALUES (\'"
						+ uuid + "\', \'" + signature + "\', \'" + send_from + "\', \'" + username + "\', \'" + host
						+ "\', \'"
						+ port + "\', \'" + inside + "\', NOW())";
				// System.out.println("插入数据库的SQL:" + sql);
				stmt.executeUpdate(sql);
				// System.out.println("插入通信数据成功");
				return true;
			} catch (SQLException e) {
				// System.out.println("插入通信数据失败");
				e.printStackTrace();
				return false;
			}
		}
		return false;

	}

	private Boolean isUserExist(String username) {
		String sql = "SELECT *  FROM `user_account` WHERE `username` = \'" + username + "\'";
		try {
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				// System.out.println("用户" + username + "存在");
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// System.out.println("用户" + username + "不存在");
		return false;
	}

	public void updateServerInfo(String privateKeyStr, String publicKeyStr) throws SQLException {
		String sql = "SELECT *  FROM `server_info` WHERE `id` = 1";
		rs = stmt.executeQuery(sql);
		if (rs.next()) {
			sql = "UPDATE `server_info` SET `privatekey` = \'" + privateKeyStr + "\',`publickey` = \'" + publicKeyStr
					+ "\' WHERE `id` = 1";
			// System.out.println("SQL = " + sql);
		} else {
			sql = "INSERT INTO `server_info` (`privatekey`,`publickey`) VALUES (\'" + privateKeyStr + "\',\'"
					+ publicKeyStr + "\')";
			// System.out.println("SQL = " + sql);
		}
		stmt.executeUpdate(sql);

	}

	public String getServerPublicKeyStr() throws SQLException {
		String sql = "SELECT `publickey` FROM `server_info` WHERE `id` = 1";
		String publicKeyStr = "none";
		rs = stmt.executeQuery(sql);
		while (rs.next()) {
			publicKeyStr = rs.getString("publickey");
		}
		return publicKeyStr;
	}

	public String getServerPrivateKeyStr() throws SQLException {
		String sql = "SELECT `privatekey` FROM `server_info` WHERE `id` = 1";
		String privateKeyStr = "none";
		rs = stmt.executeQuery(sql);
		while (rs.next()) {
			privateKeyStr = rs.getString("privatekey");
		}
		return privateKeyStr;
	}

	public boolean checkinUser(String username, String password) {
		String sql = "SELECT *  FROM `user_account` WHERE `username` = \'" + username + "'";
		String passwordRecord = "";
		try {
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				passwordRecord = rs.getString("password");
			}
			if (password.equals(passwordRecord)) {
				return true;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	public String getUserCertifyPublicKey(String username) {
		// 存储表 user_account 用户账户 id username password certify_publickey
		String sql = "SELECT `certify_publickey`  FROM `user_account` WHERE `username` = \'" + username + "'";
		String certifyPublicKey = "";

		try {
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				certifyPublicKey = rs.getString("certify_publickey");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return certifyPublicKey;
	}

	public Boolean updateCertifyPublicKey(String username, String password, String certify_publickey) {
		String sql = "SELECT * FROM `user_account` WHERE `username` = \'" + username + "'";
		String certifyPublicKey = "";
		String passwordRecord = "";
		// System.out.println("核对密码及检查公钥");
		try {
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				certifyPublicKey = rs.getString("certify_publickey");
				// System.out.println("certifyPublicKey = " + certifyPublicKey);
				passwordRecord = rs.getString("password");
				// System.out.println("passwordRecord = " + passwordRecord);
			}

			if (passwordRecord.equals(password) && null == certifyPublicKey) {
				// System.out.println("执行写入公钥");

				sql = "UPDATE `user_account` SET `certify_publickey` = \'" + certify_publickey + "\' WHERE `username` = \'"
						+ username + "'";
				stmt.executeUpdate(sql);
				// System.out.println("更新PublicKey");
				return true;
			} else {
				// System.out.println("核对未通过");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;

	}

	public boolean registerAccount(String username, String password) {
		String sql = "SELECT *  FROM `user_account` WHERE `username` = \'" + username + "'";
		String user = "";
		try {
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				user = rs.getString("username");
			}
			if (!user.equals("")) {
				// 用户存在
				// System.out.println("用户" + username + "存在");
				return false;
			} else {
				// 用户不存在
				// System.out.println("用户" + username + "不存在");
				sql = "INSERT INTO `user_account` (`username`, `password`) VALUES (\'" + username
						+ "\', \'"
						+ password + "\')";
				stmt.executeUpdate(sql);
				return true;
			}

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

	}

	public void close() {
		try {
			if (connection != null) {
				stmt.close();
				connection.close();
				// System.out.println("Close Mysql Database Successful!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
