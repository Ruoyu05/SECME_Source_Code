package com.ruoyu.secme.HelperFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TypeChangeHelper {
    public static String getMd5(String str) {
        byte[] strBytes = str.getBytes();
        try {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            md5Digest.update(strBytes);
            byte messageDigest[] = md5Digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString().toUpperCase();

        } catch (NoSuchAlgorithmException e) {
            return "md5 NG";
        }
    }
}

