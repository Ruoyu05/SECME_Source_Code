package com.ruoyu.secme.HelperFile;


import android.util.Log;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

public class RSAHelper {
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    // 状态检测 成功
    public static boolean is_Java_PublicKey(String publicKey) {
        try {
            getPublicKey_from_java(publicKey);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    // 状态检测 成功
    public static boolean is_IOS_PublicKey(String publicKey) {
        try {
            getPublicKey_from_ios(publicKey);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    //检测是私钥
    public static Boolean isPrivateKey(String privateKeyStr){
        try {
            getPrivateKey(privateKeyStr);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // 从PKCS8_String得到PKCS1公钥
    public static PublicKey getPublicKey_from_java(String publicKeyStr) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(bytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
        return publicKey;
    }

    // 从PKCS1_String得到PKCS8公钥 String
    public static PublicKey getPublicKey_from_ios(String publicKeyStr) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(publicKeyStr);
        ASN1InputStream in = new ASN1InputStream(bytes);
        ASN1Primitive asn1Prime = in.readObject();
        in.close();
        org.bouncycastle.asn1.pkcs.RSAPublicKey rsaPub = org.bouncycastle.asn1.pkcs.RSAPublicKey.getInstance(asn1Prime);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey publicKey = kf.generatePublic(new RSAPublicKeySpec(rsaPub.getModulus(), rsaPub.getPublicExponent()));
        return publicKey;
    }

    // 生成钥匙对 执行成功
    public static KeyPair createKeyPair(int length) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(length);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        return keyPair;
    }

    public static String getPublicKeyFromKeyPair(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public static String getPrivateKeyFromKeyPair(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    public static String getStringFromKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    public static String getStringFromKey(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    public static Boolean isKeyPair(String publicKeyStr ,String privateKeyStr){
        try {
            String mark = "asdfasdfasdfafs";
            PrivateKey privateKey = getPrivateKey(privateKeyStr);
            PublicKey publicKey = getPublicKey_from_java(publicKeyStr);
            //用公钥加密
            String encryptStr = encryptJsonMessage(mark,publicKey);
            String decryptedStr = decryptByPrivateKey(encryptStr,privateKeyStr);
            if(mark.equals(decryptedStr)){
                Log.i("testDebug", "isKeyPair.true");
               return true;
            }
        } catch (Exception e) {
            Log.i("testDebug", "isKeyPair.Exception");
            return false;
        }
        Log.i("testDebug", "isKeyPair.false");
        return false;
    }

    // 从String获取私钥
    public static PrivateKey getPrivateKey(String privateKeyStr) throws Exception {
        privateKeyStr = privateKeyStr.replaceAll("\\n", "");
        privateKeyStr = privateKeyStr.replace("-----BEGIN RSA PRIVATE KEY-----", "");
        privateKeyStr = privateKeyStr.replace("-----END RSA PRIVATE KEY-----", "");
        privateKeyStr = privateKeyStr.trim();// 移除字符串两侧的空白字符或其他预定义字符
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyStr);
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        return privateKey;
    }

    public static String getFormMateString(String input){
        return input.trim();
    }

    // 使用私钥解密 base64编码String
    public static String decryptByPrivateKey(String message, String privateKeyStr) throws Exception {
        // 获取公钥的长度
//        Cipher.getInstance("RSA/None/PKCS1Padding")
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey pk = getPrivateKey(privateKeyStr);
        RSAPrivateKeySpec privateKeySpec = keyFactory.getKeySpec(pk, RSAPrivateKeySpec.class);
        BigInteger modulus = privateKeySpec.getModulus();
        int privateKeyLength = modulus.toString(2).length();// 转换为二进制
        // 最大加密长度
        int max_length = privateKeyLength / 8;
        // System.out.print("keyLength"+getPrivateKey(privateKeyStr).toString());
        // 开始解码
        byte[] messageBytes = Base64.getDecoder().decode(message);
        int messageLength = messageBytes.length;
//        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        cipher.init(Cipher.DECRYPT_MODE, getPrivateKey(privateKeyStr));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offset = 0;
        byte[] cache;
        int i = 0;
        // 超长文本的分段解密
        while (messageLength - offset > 0) {
            if (messageLength - offset > max_length) {
                cache = cipher.doFinal(messageBytes, offset, max_length);
            } else {
                cache = cipher.doFinal(messageBytes, offset, messageLength - offset);
            }
            out.write(cache, 0, cache.length);
            i++;
            offset = i * max_length;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return new String(decryptedData);
    }

    // 使用公钥加密 message明文
    public static String encryptJsonMessage(String message, PublicKey publicKey) throws Exception {
        String result = "公钥解密失败!";
        byte[] messageBytes = message.getBytes();// utf-8型message
        int messageLength = messageBytes.length;
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        // cipher.getBlockSize();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offset = 0;
        byte[] cache;
        int i = 0;

        // 计算公钥最大可加密的长度
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = (RSAPublicKeySpec) kf.getKeySpec(publicKey, RSAPublicKeySpec.class);
        BigInteger prime = keySpec.getModulus();
        int keyLength = prime.toString(2).length();
        int max_length = keyLength / 8 - 11;

        // 超长文本的分段解密
        while (messageLength - offset > 0) {
            if (messageLength - offset > max_length) {
                cache = cipher.doFinal(messageBytes, offset, max_length);
            } else {
                cache = cipher.doFinal(messageBytes, offset, messageLength - offset);
            }
            out.write(cache, 0, cache.length);
            i++;
            offset = i * max_length;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();

        result = Base64.getEncoder().encodeToString(encryptedData);
        return result;

    }
    // 使用公钥加密
    public static String encryptJsonMessage(String message, String publicKeyStr) throws Exception{
        PublicKey publicKey = getPublicKey_from_java(publicKeyStr);
        return encryptJsonMessage(message, publicKey);
    }
    //使用私钥签名
    public static String sign(String sign_msg,String privateKeyStr){
        String signature = "error";
        byte[] signed = null;
        try {
            PrivateKey privateKey = getPrivateKey(privateKeyStr);

            Signature Sign = Signature.getInstance(SIGNATURE_ALGORITHM);
            Sign.initSign(privateKey);
            Sign.update(sign_msg.getBytes());
            signed = Sign.sign();
            signature = Base64.getEncoder().encodeToString(signed);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return signature;
    }
}
