package com.ruoyu.secme.HelperFile;

import android.util.Log;

import com.google.gson.Gson;
import com.ruoyu.secme.JsonType.CertifyKeyCheck;
import com.ruoyu.secme.JsonType.CertifyKeyCheckResult;
import com.ruoyu.secme.JsonType.Contents;
import com.ruoyu.secme.JsonType.GotResult;
import com.ruoyu.secme.JsonType.JsonMessage;
import com.ruoyu.secme.JsonType.RegisterOrLoginResult;
import com.ruoyu.secme.JsonType.ResultForJson;
import com.ruoyu.secme.JsonType.ServerPublicKey;

public class JsonHelper {

    public static boolean isJsonMessage(String str) {
        try {
            Gson gson = new Gson();
            gson.fromJson(str, JsonMessage.class);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static String buildJsonMessage(JsonMessage jsonMessage) {
        Gson gson = new Gson();
        return gson.toJson(jsonMessage);
    }

    public static JsonMessage getJsonMessage(String msg) {
        Gson gson = new Gson();
        return gson.fromJson(msg, JsonMessage.class);
    }

    public static ResultForJson readJsonMessage(String msg) {
        Log.i("testDebug", "开始readJsonMessage");
        Gson gson = new Gson();
        JsonMessage jsonMessage = gson.fromJson(msg, JsonMessage.class);
        String contentsJson = gson.toJson(jsonMessage.contents);
        ResultForJson resultForJson = new ResultForJson();
        switch (jsonMessage.type) {
            case "ServerPublicKey":
                Log.i("testDebug", "识别为ServerPublicKey");
                resultForJson.needReplay = true;
                resultForJson.needDealReturnValue = true;

                ServerPublicKey serverPublicKey = gson.fromJson(contentsJson, ServerPublicKey.class);

                resultForJson.returnCode = "GotServerPublicKey";
                resultForJson.returnValue = serverPublicKey.server_publickey;
                break;
            case "GotClientPublicKey":
                Log.i("testDebug", "识别为GotClientPublicKey");
                resultForJson.needReplay = false;
                resultForJson.needDealReturnValue = true;

                GotResult gotResult = gson.fromJson(contentsJson, GotResult.class);
                resultForJson.returnCode = "ConnectionResult";
                if (gotResult.result) {
                    resultForJson.returnValue = "true";
                } else {
                    resultForJson.returnValue = "false";
                }
                break;
            case "RegisterResult":
                //账户注册的结果
                RegisterOrLoginResult registerResult = gson.fromJson(contentsJson, RegisterOrLoginResult.class);
                resultForJson.needReplay = false;
                resultForJson.needDealReturnValue = true;

                resultForJson.returnCode = "";
                if (registerResult.result) {
                    Log.i("testDebug", "注册成功");
                    resultForJson.returnCode = "RegisterSuccess";
                } else {
                    Log.i("testDebug", "注册失败");
                    resultForJson.returnCode = "RegisterFailure";
                }
                resultForJson.returnValue = registerResult.reasion;
                break;
            case "LoginResult":
                Log.i("testDebug", "收到登陆结果消息");
                resultForJson.needReplay = false;
                resultForJson.needDealReturnValue = true;

                resultForJson.returnCode = "";
                //用户登陆的结果
                RegisterOrLoginResult loginResult = gson.fromJson(contentsJson, RegisterOrLoginResult.class);
                if (loginResult.result) {
                    Log.i("testDebug", "登陆成功");
                    resultForJson.returnCode = "LoginSuccess";
                } else {
                    Log.i("testDebug", "登陆失败");
                    resultForJson.returnCode = "LoginFailure";
                }
                resultForJson.returnValue = loginResult.reasion;
                break;
            case "CertifyKeyCheckResult":
                Log.i("testDebug", "收到密钥验证结果消息");
                resultForJson.needReplay = false;
                resultForJson.needDealReturnValue = true;

                CertifyKeyCheckResult certifyKeyCheckResult = gson.fromJson(contentsJson, CertifyKeyCheckResult.class);
                if (certifyKeyCheckResult.result) {
                    //需要生成或者获取
                    resultForJson.returnCode = certifyKeyCheckResult.reasion;
                } else {
                    //验证成功
                    resultForJson.returnCode = "certifyKeyChecked";
                }
                break;
            case "UpdateCertifyResult":
                Log.i("testDebug", "收到更新密钥结果");
                resultForJson.needReplay = false;
                resultForJson.needDealReturnValue = true;
                GotResult updateCertifyResult = gson.fromJson(contentsJson, GotResult.class);
                resultForJson.returnCode = "updateCertifyResult";
                resultForJson.returnValue = "" + updateCertifyResult.result;
                break;
            case "NewLetter":
                Log.i("testDebug", "收到NewLetter");
                resultForJson.needReplay = true;
                resultForJson.needDealReturnValue = true;
                Contents contents = gson.fromJson(contentsJson, Contents.class);

                //返回服务器接收到消息
                JsonMessage rpmsg = new JsonMessage();
                rpmsg.type = "GotLetter";

                Contents rpContents = new Contents();
                rpContents.message_uuid = contents.message_uuid;
                rpmsg.contents = rpContents;
                String rp = JsonHelper.buildJsonMessage(rpmsg);
                resultForJson.repalyMessage = rp;

                //解码sendfrom
                resultForJson.returnCode = "newLetter";
                resultForJson.returnValue = contentsJson;

                break;
            default:
                break;
        }
        return resultForJson;
    }

}


