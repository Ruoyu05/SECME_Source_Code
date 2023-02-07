package com.ruoyu.secme.JsonType;

public class MyQRCodeInfo {
    public String user;
    public String certifyKey;
    public String host;
    public String port;
    public MyQRCodeInfo(String host, String port, String user, String certifyKey){
        this.host = host;
        this.port = port;
        this.user = user;
        this.certifyKey = certifyKey;
    }


}
