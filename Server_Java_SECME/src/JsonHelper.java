import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.Base64;

import org.java_websocket.WebSocket;

import com.google.gson.Gson;

import ClassType.*;

public class JsonHelper {
	public static String buildJsonMessage(JsonMessage jsonMessage) {
		Gson gson = new Gson();
		return gson.toJson(jsonMessage);
	}

	public static ResultForJson readJsonMessage(String jsonStr, WebSocket conn, String chat_publickey) {
		Gson gson = new Gson();
		JsonMessage jsonMessage = gson.fromJson(jsonStr, JsonMessage.class);
		System.out.println("收到的消息格式为:" + jsonMessage.type);
		ResultForJson result = new ResultForJson();
		String contentsJson = gson.toJson(jsonMessage.contents);
		switch (jsonMessage.type) {

			case "ClientPublicKey":

				ClientPublicKey clientPublicKey = gson.fromJson(contentsJson, ClientPublicKey.class);
				String client_publickey = clientPublicKey.client_publickey;

				PublicKey publicKey = null;
				// 获得公钥成功 报文
				JsonMessage repalyMessage = new JsonMessage();
				ReturnResult returnResult = new ReturnResult();
				repalyMessage.type = "GotClientPublicKey";

				result = new ResultForJson();
				result.needReplay = true;
				result.needDealReturnValue = true;
				String repaly_Json = "";

				if (RSAHelper.is_IOS_PublicKey(client_publickey) || RSAHelper.is_Java_PublicKey(client_publickey)) {

					try {
						publicKey = RSAHelper.getPublicKey_from_ios(client_publickey);
						System.out.println("从JavaKey获得公钥成功");
					} catch (Exception e) {
						System.out.println("从JavaKey获得公钥失败");
						try {
							publicKey = RSAHelper.getPublicKey_from_java(client_publickey);
							System.out.println("从IOSKey获得公钥成功");
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}

					returnResult.result = true;
					result.returnCode = "client_publickey";
					result.returnValue = Base64.getEncoder().encodeToString(publicKey.getEncoded());

				} else {
					returnResult.result = false;
					returnResult.reasion = "发送至服务器的公钥不正确";
					result.returnCode = "client_publickey_error";
				}
				repalyMessage.contents = returnResult;
				repaly_Json = JsonHelper.buildJsonMessage(repalyMessage);
				result.repalyMessage = repaly_Json;

				break;
			case "LoginRequest":
				System.out.println("收到LoginRequest");
				LoginAccount loginAccount = gson.fromJson(contentsJson, LoginAccount.class);

				// 获得公钥成功 报文
				JsonMessage repalyLoginResult = new JsonMessage();
				ReturnResult returnLoginResult = new ReturnResult();

				repalyLoginResult.type = "LoginResult";

				result = new ResultForJson();
				result.needReplay = true;
				result.needDealReturnValue = false;

				try {
					Boolean loginResult = new MySqlConnect().checkinUser(loginAccount.username, loginAccount.password);
					System.out.println("loginResult:"+loginResult);
					returnLoginResult.result = loginResult;
					if (!loginResult) {
						System.out.println("用户验证失败");
						returnLoginResult.reasion = "ユーザー名またはパースワードが間違います!";
					}
					if (loginResult) {
						// 返回用户名
						System.out.println("返回用户名处理");
						result.needDealReturnValue = true;
						result.returnCode = "SetUserName";
						result.returnValue = loginAccount.username;
					}

				} catch (SQLException error) {
					returnLoginResult.reasion = "Database Error";
					returnLoginResult.result = false;
					error.printStackTrace();
				}
				repalyLoginResult.contents = returnLoginResult;
				repaly_Json = JsonHelper.buildJsonMessage(repalyLoginResult);
				result.repalyMessage = repaly_Json;

				break;
			case "RegisterAccount":
				System.out.println("收到RegisterAccount");
				RegisterAccount userAccount = gson.fromJson(contentsJson, RegisterAccount.class);
				result = new ResultForJson();
				result.needReplay = true;
				result.needDealReturnValue = false;

				JsonMessage repalyRegisterResult = new JsonMessage();
				repalyRegisterResult.type = "RegisterResult";
				ReturnResult returnRegisterResult = new ReturnResult();
				try {
					Boolean registerResult = new MySqlConnect().registerAccount(userAccount.username, userAccount.password);
					if (registerResult) {
						// 新建成功
						returnRegisterResult.result = true;
						returnRegisterResult.reasion = "アカンウトを作りました!";
					} else {
						returnRegisterResult.result = false;
						returnRegisterResult.reasion = "ユーザーがもう存在しています!";
					}
				} catch (SQLException e) {
					// 新建用户失败
					returnRegisterResult.result = false;
					returnRegisterResult.reasion = "Database Error!";
					e.printStackTrace();
				}
				repalyRegisterResult.contents = returnRegisterResult;
				repaly_Json = JsonHelper.buildJsonMessage(repalyRegisterResult);
				result.repalyMessage = repaly_Json;
				break;
			case "CertifyKeyCheck":
				result.needReplay = true;
				result.needDealReturnValue = false;

				// 编辑返回的报文
				JsonMessage repalyCertifyKeyCheckResult = new JsonMessage();
				repalyCertifyKeyCheckResult.type = "CertifyKeyCheckResult";
				ReturnResult returnCertifyKeyCheckResult = new ReturnResult();

				System.out.println("收到CertifyKeyCheck");
				CertifyKeyCheck certifyKeyCheck = gson.fromJson(contentsJson, CertifyKeyCheck.class);
				String certifyPublicKey = null;
				if (certifyKeyCheck.certify_publickey.equals("none")) {
					// 客户端没有密钥
					// System.out.println("客户端没有密钥");
					try {
						certifyPublicKey = new MySqlConnect().getUserCertifyPublicKey(certifyKeyCheck.username);
						// System.out.println("查询username:"+ certifyKeyCheck.username);
					} catch (SQLException e) {
						// System.out.println("客户端没有密钥 catch1");
						e.printStackTrace();
					}

					if (null == certifyPublicKey) {
						// System.out.println("服务器内的密钥为null");
						returnCertifyKeyCheckResult.reasion = "need_create";

					} else {
						// System.out.println("服务器内有密钥");
						returnCertifyKeyCheckResult.reasion = "need_copy";
					}
					returnCertifyKeyCheckResult.result = true;

				} else {
					String publicKeyStr = "";
					if (RSAHelper.is_IOS_PublicKey(certifyKeyCheck.certify_publickey)) {

						// 为IOS公钥
						// System.out.println("certifyKey为IOS公钥");
						try {
							publicKeyStr = RSAHelper
									.getStringFromKey(RSAHelper.getPublicKey_from_ios(certifyKeyCheck.certify_publickey));
							// System.out.println("换为Java公钥:" + publicKeyStr);
						} catch (Exception e) {
							e.printStackTrace();
						}

					} else if (RSAHelper.is_Java_PublicKey(certifyKeyCheck.certify_publickey)) {
						// 为Java公钥
						// System.out.println("certifyKey为Java公钥");
						publicKeyStr = certifyKeyCheck.certify_publickey;
					} else {
						// 都不是
						// System.out.println("certifyKey不是公钥");
						publicKeyStr = "none";
					}
					try {
						certifyPublicKey = new MySqlConnect().getUserCertifyPublicKey(certifyKeyCheck.username);
					} catch (SQLException e) {
						e.printStackTrace();
					}
					if (null == certifyPublicKey) {
						// System.out.println("服务器内certifyKey为null");
						returnCertifyKeyCheckResult.result = true;
						returnCertifyKeyCheckResult.reasion = "need_create";

					} else {
						if (publicKeyStr.equals(certifyPublicKey)) {

							// System.out.println("密钥相符");
							// 返回到上级进行处理
							result.needDealReturnValue = true;
							result.returnCode = "AuthKeysSuccessful";
							result.repalyMessage = "Auth Keys Successful";
							returnCertifyKeyCheckResult.result = false;
						} else {
							// System.out.println("密钥不相符");
							// System.out.println("publicKeyStr:"+publicKeyStr);
							// System.out.println("certifyPublicKey:"+certifyPublicKey);
							returnCertifyKeyCheckResult.result = true;
							returnCertifyKeyCheckResult.reasion = "need_copy";
						}
					}

				}
				repalyCertifyKeyCheckResult.contents = returnCertifyKeyCheckResult;
				repaly_Json = JsonHelper.buildJsonMessage(repalyCertifyKeyCheckResult);
				result.repalyMessage = repaly_Json;

				break;
			case "UpdateCertifyPublicKey":
				// System.out.println("收到UpdateCertifyPublicKey");
				UpdateCertifyPublicKey updateCertifyPublicKey = gson.fromJson(contentsJson, UpdateCertifyPublicKey.class);
				// 编辑返回的报文
				JsonMessage repalyUpdateCertifyResult = new JsonMessage();
				repalyUpdateCertifyResult.type = "UpdateCertifyResult";
				ReturnResult returnUpdateCertifyResult = new ReturnResult();
				result.needReplay = true;

				try {
					String key_for_store = updateCertifyPublicKey.certify_publickey;

					if (RSAHelper.is_IOS_PublicKey(updateCertifyPublicKey.certify_publickey)) {
						// System.out.println("收到更新的公钥为IOS公钥,执行转换.");
						key_for_store = RSAHelper
								.getStringFromKey(RSAHelper.getPublicKey_from_ios(updateCertifyPublicKey.certify_publickey));
					}
					// System.out.println("转换后的公钥:" + key_for_store);
					returnUpdateCertifyResult.result = new MySqlConnect().updateCertifyPublicKey(
							updateCertifyPublicKey.username,
							updateCertifyPublicKey.password, key_for_store);
				} catch (Exception e) {
					returnUpdateCertifyResult.result = false;
					returnUpdateCertifyResult.reasion = "更新失败!";
					e.printStackTrace();
				}

				repalyUpdateCertifyResult.contents = returnUpdateCertifyResult;
				repaly_Json = JsonHelper.buildJsonMessage(repalyUpdateCertifyResult);
				result.repalyMessage = repaly_Json;

				// 返回到上级进行处理
				result.needDealReturnValue = true;
				result.returnCode = "AuthKeysSuccessful";
				// result.repalyMessage = "Auth Keys Successful";

				break;

			case "PostMessage":
				// System.out.println("收到PostMessage");
				PostMessage postLetter = gson.fromJson(contentsJson, PostMessage.class);
				// 存储数据
				result.needReplay = true;
				result.needDealReturnValue = false;
				try {
					Boolean savePostMessage = new MySqlConnect().savePostMessage(postLetter);
					result.repalyMessage = "{\"type\":\"SavePostLetterResult\",\"Contents\":" + savePostMessage + "}";
				} catch (SQLException e) {
					// System.out.println("数据存储失败");
					e.printStackTrace();
				}
				break;
			case "GotLetter":
				// System.out.println("收到GotLetter");
				GotLetter gotLetter = gson.fromJson(contentsJson, GotLetter.class);
				String message_uuid = gotLetter.message_uuid;
				try {
					new MySqlConnect().deleteLetter(message_uuid);
					// System.out.println("删除已发送的消息成功");
				} catch (SQLException e) {
					e.printStackTrace();
				}
				break;

			case "PostSendRequest":
				// System.out.println("收到PostSendRequest");
				result.needReplay = false;
				result.needDealReturnValue = false;
				// 重建报文
				PostMessage postSendRequest = gson.fromJson(contentsJson, PostMessage.class);
				String tohost = postSendRequest.send_to.host;
				String toport = postSendRequest.send_to.port;

				JsonMessage rebuildRostMessage = new JsonMessage();
				rebuildRostMessage.type = "PostMessage";
				rebuildRostMessage.contents = postSendRequest;
				String rebuildRostMessageStr = gson.toJson(rebuildRostMessage);

				// 启动机器人
				try {
					URI uri = new URI("ws://" + tohost + ":" + toport);
					new Thread(new RobotClient(uri, rebuildRostMessageStr)).run();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				// System.out.println("Robot启动成功");

				break;
			default:
				break;

		}
		return result;

	}

	public static boolean isJson(String str) {
		try {
			Gson gson = new Gson();
			gson.fromJson(str, JsonMessage.class);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
