package com.ruoyu.secme.HelperFile;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.ruoyu.secme.FriendQRResult;
import com.ruoyu.secme.JsonType.ClientPublicKey;
import com.ruoyu.secme.JsonType.Contents;
import com.ruoyu.secme.JsonType.FriendInfo;
import com.ruoyu.secme.JsonType.Inside;
import com.ruoyu.secme.JsonType.JsonMessage;
import com.ruoyu.secme.JsonType.ResultForJson;
import com.ruoyu.secme.JsonType.SendFrom;
import com.ruoyu.secme.JsonType.UserAccount;
import com.ruoyu.secme.MainActivity;


import org.bouncycastle.jcajce.provider.asymmetric.RSA;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;


import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import java.security.NoSuchAlgorithmException;

public class WebSocketHelper extends WebSocketClient {


    public String host;
    public String portStr;

    public String username;
    private String password;
    public String userMd5Str;
    //        userMd5Str = getMD5(data: host + portStr + username)
    public String client_publicKeyStr;
    private String client_privateKeyStr;
    public String server_publicKeyStr = "";

    private String certifyPrivateKey = "";

    public Boolean authed = false;
    public Boolean authedFailure = false;

    public Boolean doLogin = false;
    public Boolean isConnected = false;
    public Boolean haveServerPK = false;

    public Boolean needCreateCertifyKey = false;
    public Boolean needCopyCertifyKey = false;
    public Boolean certifyKeyChecked = false;

    public Boolean certifyAuthed = false;
    public Boolean certifyAuthCancel = false;

    public Boolean gotRegisterSuccess = false;
    public Boolean gotRegisterFailure = false;
    public String resultMessage = "";

    //    public SQLiteDatabase db = null;
    public DBHelper dbHelper = null;

    public Boolean haveNewMessage = false;


    public Boolean isAuthed() {
        return authed;
    }

    public Boolean isConnected() {
        return isConnected;
    }

    public void setCertifyPrivateKey(String certifyPrivateKey) {
        this.certifyPrivateKey = certifyPrivateKey;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setUserMd5Str(String userMd5Str) {
        this.userMd5Str = userMd5Str;
    }

    public void setUserInfo(String username, String password) {
        setUsername(username);
        setPassword(password);
        setUserMd5Str(TypeChangeHelper.getMd5(host + portStr + username));
        DBHelper helper = new DBHelper(null, "SECME_DB");
    }

    @Override
    public void connect() {
        super.connect();
        Log.i("testDebug", "connect()");
    }

    public WebSocketHelper(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {

        authedFailure = false;
        certifyAuthCancel = false;

        Log.i("testDebug", "onOpen()");
        isConnected = true;
    }

    @Override
    public boolean isOpen() {
        return super.isOpen();
    }

    @Override
    public void onMessage(String message) {
        Log.i("testDebug", "onMessage(String)" + message);
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        super.onMessage(bytes);
        Charset charset = StandardCharsets.UTF_8;
        String messageStr = charset.decode(bytes).toString();
        Log.i("testDebug", "onMessage(ByteBuffer)" + messageStr);

        if (JsonHelper.isJsonMessage(messageStr)) {
            ResultForJson resultForJson = JsonHelper.readJsonMessage(messageStr);

            if (resultForJson.needDealReturnValue) {
//                Log.i("testDebug", "needDealReturnValue");
                switch (resultForJson.returnCode) {
                    case "GotServerPublicKey":
                        //获得服务器密钥
                        server_publicKeyStr = resultForJson.returnValue;
                        try {
                            //生成密钥对
                            KeyPair keyPair = RSAHelper.createKeyPair(2048);
                            client_publicKeyStr = RSAHelper.getStringFromKey(keyPair.getPublic());
                            client_privateKeyStr = RSAHelper.getStringFromKey(keyPair.getPrivate());
                        } catch (NoSuchAlgorithmException e) {
                            //关闭链接
                            close();
                            break;
                        }
                        //创建报文返回密钥对
                        JsonMessage jsonMessage = new JsonMessage();
                        jsonMessage.type = "ClientPublicKey";
                        ClientPublicKey clientPublicKey = new ClientPublicKey();
                        clientPublicKey.client_publickey = client_publicKeyStr;
                        jsonMessage.contents = clientPublicKey;
                        String jsonMessageStr = JsonHelper.buildJsonMessage(jsonMessage);
                        try {
                            resultForJson.repalyMessage = RSAHelper.encryptJsonMessage(jsonMessageStr, server_publicKeyStr);
                        } catch (Exception e) {
                            close();
                            break;
                        }
                        break;
                    case "ConnectionResult":
                        close();
                    case "LoginResult":
                        Log.i("testDebug", "接收到LoginResult");
                    default:
                        break;
                }
            }

            if (resultForJson.needReplay) {
                Log.i("testDebug", "needReplay1");

                byte[] repalyMessageBytes = resultForJson.repalyMessage.getBytes();
                send(repalyMessageBytes);
            }

        } else {
            //尝试解密
            try {
                String decryptedMessage = RSAHelper.decryptByPrivateKey(messageStr, client_privateKeyStr);
                Log.i("testDebug", "接收到加密内容:" + decryptedMessage);
                if (JsonHelper.isJsonMessage(decryptedMessage)) {
                    ResultForJson resultForJson = JsonHelper.readJsonMessage(decryptedMessage);
                    if (resultForJson.needDealReturnValue) {
                        Log.i("testDebug", "needDealReturnValue:" + resultForJson.returnCode);
                        switch (resultForJson.returnCode) {
                            case "ConnectionResult":
                                if (resultForJson.returnValue.equals("true")) {
                                    isConnected = true;
                                    Log.i("testDebug", "线路加密成功");
                                    if (doLogin) {
                                        Log.i("testDebug", "执行用户验证");
                                        JsonMessage loginRequest = new JsonMessage();
                                        loginRequest.type = "LoginRequest";
                                        UserAccount userAccount = new UserAccount();
                                        userAccount.username = username;
                                        userAccount.password = password;
                                        loginRequest.contents = userAccount;
                                        String loginRequestStr = JsonHelper.buildJsonMessage(loginRequest);

                                        String encryptedLoginRequestStr = "";
                                        try {
                                            encryptedLoginRequestStr = RSAHelper.encryptJsonMessage(loginRequestStr, server_publicKeyStr);
                                        } catch (Exception e) {
                                            close();
                                            break;
                                        }
                                        //发送登陆报文
                                        Log.i("testDebug", "发送json:" + loginRequestStr);
                                        send(encryptedLoginRequestStr.getBytes());
                                    }
                                } else {
                                    Log.i("testDebug", "线路加密失败");
                                    close();
                                }
                                break;
                            case "RegisterSuccess":
                                gotRegisterSuccess = true;
                                resultMessage = resultForJson.returnValue;
                                break;
                            case "RegisterFailure":
                                gotRegisterFailure = true;
                                resultMessage = resultForJson.returnValue;
                                break;
                            case "LoginSuccess":
                                authed = true;
                                Log.i("testDebug", "authed = true");
                                break;
                            case "LoginFailure":
                                //用户验证失败，退出登陆
                                authedFailure = true;
                                resultMessage = resultForJson.returnValue;
                                close();
                                break;
                            case "need_create":
                                Log.i("testDebug", "执行need_create");
                                needCreateCertifyKey = true;
                                break;
                            case "need_copy":
                                Log.i("testDebug", "执行need_copy");
                                needCopyCertifyKey = true;
                                break;
                            case "certifyKeyChecked":
                                Log.i("testDebug", "执行certifyKeyChecked");
                                certifyKeyChecked = true;
                                break;
                            case "updateCertifyResult":
                                Log.i("testDebug", "密钥更新结果:" + resultForJson.returnValue);
                                if ("true".equals(resultForJson.returnValue)) {
                                    certifyAuthed = true;
                                    Log.i("testDebug", "更新成功！，通过登陆");
                                } else {
                                    Log.i("testDebug", "更新失败！，取消登陆");
                                    certifyAuthCancel = true;
                                }
                                break;
                            case "newLetter":
                                Log.i("testDebug", "in newLetter");
                                Gson gson = new Gson();
                                String letter = resultForJson.returnValue;
                                Contents contents = gson.fromJson(letter, Contents.class);

                                //FIXME 获取私钥----------

                                Log.i("FixDebug", "certifyPrivateKey:"+certifyPrivateKey);
                                //解码sendfrom
                                String send_from_str = RSAHelper.decryptByPrivateKey(contents.send_from, certifyPrivateKey);
                                Log.i("testDebug", "send_from_str:" + send_from_str);
                                SQLiteDatabase db = dbHelper.getReadableDatabase();
                                SendFrom sendFrom = gson.fromJson(send_from_str, SendFrom.class);

                                if (sendFrom.uuid == null) {
                                    Log.i("testDebug", "UUID为空值");
                                    //解码inside
                                    String insideStr = RSAHelper.decryptByPrivateKey(contents.inside, certifyPrivateKey);
                                    Log.i("testDebug", "解码出inside:" + insideStr);
                                    Inside inside = gson.fromJson(insideStr, Inside.class);

                                    //检测好友是否存在 将好友申请插入数据库
                                    FriendInfo friendInfo = new FriendInfo();

                                    friendInfo.friendName = sendFrom.username;
                                    friendInfo.host = sendFrom.host;
                                    friendInfo.port = sendFrom.port;
                                    friendInfo.friend_certifyPublicKey = inside.certify_publickey;
                                    friendInfo.friend_chat_publickey = inside.friend_chat_publickey;
                                    friendInfo.my_uuid = inside.uuid;
                                    friendInfo.active = false;
                                    //计算md5
                                    String verifyMd5Str = sendFrom.host + sendFrom.port + sendFrom.username + inside.certify_publickey;
                                    friendInfo.md5Str = TypeChangeHelper.getMd5(verifyMd5Str);
                                    //判断好友是否存在
                                    if (dbHelper.isFriendExist(db, friendInfo.md5Str)) {
                                        //好友已经存在
                                        Log.i("testDebug", "好友已经存在！不再重复添加好友");
                                    } else {
                                        //插入好友
                                        dbHelper.insertNewFriend(db, friendInfo);
                                    }
                                } else {
                                    Log.i("testDebug", "UUID不为空值");
                                    //通过uuid查找好友
                                    try {
                                        String my_chat_privateKey = dbHelper.getMyChatPrivateKey(db, sendFrom.uuid);
                                        Log.i("testDebug", "查询到通讯私钥:" + my_chat_privateKey);
                                        //使用私钥解码

                                        String insideStr = RSAHelper.decryptByPrivateKey(contents.inside, my_chat_privateKey);
                                        Log.i("testDebug", "解码出inside:" + insideStr);


                                        Inside inside = gson.fromJson(insideStr, Inside.class);
                                        if (inside.message == null) {
                                            Log.i("testDebug", "inside.message 等于 null");
                                            //将钥匙和uuid插入数据库
                                            String friend_chat_publickey = inside.friend_chat_publickey;
                                            String my_uuid = inside.uuid;
                                            Double time = inside.time;
                                            DBHelper.updateActiveFriendInfo(db, sendFrom.uuid, friend_chat_publickey, my_uuid);

                                        } else {
                                            Log.i("testDebug", "inside.message 不等于 null");
                                            //将数据存储到聊天数据库
                                            String message =  inside.message;

                                            Double time =  inside.time;
                                            String friend_uuid = sendFrom.uuid;
                                            //新建一个好友的数据库  名字 friend_uuid
                                            dbHelper.createChatList(db,friend_uuid);
                                            dbHelper.gotNewMessage(db,friend_uuid,message,time);
                                            //Fixme 新消息的处理
                                            haveNewMessage = true;
                                            Log.i("testDebug", "已存储进数据库");

                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    if (resultForJson.needReplay) {
                        Log.i("testDebug", "needReplay2");
                        try {
                            String encryptedmsg = RSAHelper.encryptJsonMessage(resultForJson.repalyMessage, server_publicKeyStr);
                            byte[] repalyMessageBytes = encryptedmsg.getBytes();
                            send(repalyMessageBytes);
                        } catch (Exception e) {
                            Log.i("testDebug", "needReplay加密返回消息失败");
                        }
                    }
                }
            } catch (Exception e) {
                //解密失败
                e.printStackTrace();
                close();
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i("testDebug", "onClose()");
        //恢复至默认状态
        setInDefult();
    }

    private void setInDefult() {
        host = "";

        portStr = "";
        username = "";
        password = "";

        isConnected = false;
        authed = false;
        doLogin = false;
        haveServerPK = false;
        needCreateCertifyKey = false;
        needCopyCertifyKey = false;
        certifyKeyChecked = false;
        certifyAuthed = false;


        gotRegisterSuccess = false;
        gotRegisterFailure = false;
        resultMessage = "";

        client_publicKeyStr = "";
        client_privateKeyStr = "";
        server_publicKeyStr = "";
        certifyPrivateKey = "";
        dbHelper = null;
        haveNewMessage = false;

    }

    @Override
    public void onError(Exception ex) {
        Log.i("testDebug", "onError() " + ex);
    }

}
