//
//  WebSocketClient.swift
//  SECME
//
//  Created by cmStudent on 2022/11/22.
//

import Foundation
import Combine

class WebSocketClient: NSObject, ObservableObject {
    
    private var webSocketTask: URLSessionWebSocketTask?

    //服务器设置
    @Published var host:String = "127.0.0.1"
    @Published var port:Int = 10086
    @Published var portStr:String = "10086"
    
    //用户 是否传入password
    @Published var username:String = ""
    @Published var password:String = ""
    @Published var userMd5Str:String = ""
    
    
    //用于服务器通讯的密钥对
    @Published var publickey_from_server:String = ""
    private var privatekey_for_server:String = ""
    
    //状态
    @Published var isOnConnecting: Bool = false
    @Published var isConnected: Bool = false
    @Published var isAuthed: Bool = false
    @Published var isRegisterSuccessful: Bool = false
    @Published var needActive: Bool = true
    @Published var needCreateCertifyKey: Bool = false
    @Published var needCopyCertifyKey: Bool = false
    @Published var errorMessage:String = ""
    
    @Published var haveNewMessage:Bool = false
    
    var cancelable: Set<AnyCancellable> = .init()

    
    
    func recoverState(){
        
        username = ""
        password = ""
        userMd5Str = ""
        publickey_from_server = ""
        privatekey_for_server = ""
        isOnConnecting = false
        isConnected = false
        isAuthed = false
        isRegisterSuccessful = false
        needActive = true
        needCreateCertifyKey = false
        needCopyCertifyKey = false
        haveNewMessage = false

    }
    
    //服务器连接设置
    func setup(host: String,portStr:String,username:String,password:String) {
        setup(host: host, portStr: portStr)
        setup(username: username, password: password)
    }
    
    //服务器连接设置
    func setup(host: String,portStr:String){
        //检查输入的服务器配置
        if(checkHost(input:host) && checkPort(input:portStr)){
            
            self.host = host
            self.portStr = portStr
            let port:Int = (portStr as NSString).integerValue
            self.port = port
            let urlSession = URLSession(configuration: .default, delegate: self, delegateQueue: OperationQueue())
            let url = "ws://\(host):\(portStr)"
            webSocketTask = urlSession.webSocketTask(with: URL(string: url)!)
        }else{
            DispatchQueue.main.async { [self] in
                errorMessage = "服务器配置不正确. IPv6填写例->[IPv6]"
                recoverState()
            }
        }
    }
    
    //服务器连接设置
    func setup(username:String,password:String){
        self.password = password
        self.username = username

    }
    
    func connect() {
        errorMessage = ""
        webSocketTask?.resume()
        receive()
    }
    
    func disconnect() {
        errorMessage = ""
        webSocketTask?.cancel(with: .goingAway, reason: TypeChangeHepler().strToUTF8(string: "关闭连接"))
    }
    
    //发送文本消息
    func send(inputStr: String){
        let msg = URLSessionWebSocketTask.Message.string(inputStr)
        webSocketTask?.send(msg) { error in
            if let error = error {
                print(error)
            }
        }
    }
    
    //发送二进制消息
    func send(inputData: Data){
        let msg = URLSessionWebSocketTask.Message.data(inputData)
        webSocketTask?.send(msg) { error in
            if let error = error {
                print("send发生了错误")
                print(error)
            }
        }
    }
    
    
    //接收消息
    private func receive() {
        webSocketTask?.receive { [weak self] result in
            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    DispatchQueue.main.async {
                        
                        print("Received(String):\(text)")
                        
                    }
                case .data(let data):
                    DispatchQueue.main.async {
                        let text = String(data: data, encoding: .utf8)!
                        print("Received(Binary):\(text)")
                        //处理收到的Binary消息
                        self?.dealWithBinaryMessage(inputData: data)
                        }

                    
                @unknown default:
                    print("Received(UnKnown)")
                }
                self?.receive()
            case .failure(let error):
                print("Failed to Receive Message: \(error)")
            }
        }
        
    }
    
    private func dealWithBinaryMessage(inputData:Data){
        if(JsonHelper().isJson_ServerPublicKey(inputData: inputData)){
            publickey_from_server = JsonHelper().getServerPublicKey(inputData: inputData)
            if(RSAHelper().testPublicKey(publicKeyStr: publickey_from_server)){
                //正确的服务器公钥
                replayClientPublicKey()
            }else{
                //错误的服务器公钥
                errorMessage = "错误的服务器公钥"
                webSocketTask?.cancel(with: .goingAway, reason: TypeChangeHepler().strToUTF8(string: "服务器公钥不正确"))
            }
            
        }else if(JsonHelper().isJson_GotClientPublicKey(inputData: inputData)){
            //获取错误消息
            print("isJson_GotClientPublicKey")
            let reasion = JsonHelper().getGotClientPublicKeyResult(inputData: inputData)
            print("reasion:\(reasion)")
            
        }else{
            //使用通讯私钥进行解密
            let encryptedMessage = String(data: inputData, encoding: .utf8)!
            let jsonMessage = RSAHelper().decryptByPrivateKey(privateKeyStr: privatekey_for_server, encryptedBase64String: encryptedMessage)
            print("解码出的Json:\(jsonMessage)")
            if(jsonMessage == "decryptByPrivateKey in error"){
                print("解码Json失败")
            }else{
                readJsonMessage(jsonStr: jsonMessage)
            }
            
        }
        
    }
    
    private func dealWithTextMessage(){}
   
    private func replayClientPublicKey(){
        //为通讯生成对密钥
        let keyPair_for_server = RSAHelper().creatKeyPair(length: 2048)
        privatekey_for_server = keyPair_for_server.getPrivateKey()
        //创建通讯报文_返回客户端公钥
        let contents = Contents(clientPublickey: keyPair_for_server.getPublicKey())
        let jsonMessage = JsonMessage(type: "ClientPublicKey", contents: contents)
        let jsonStr = JsonHelper().buildJsonMessageStr(input: jsonMessage)
        
        //对消息进行加密
        let encryptedMessage = RSAHelper().encryptByPublicKey(publicKeyStr: publickey_from_server, messageStr: jsonStr)
        send(inputData:TypeChangeHepler().base64StrToUTF8Data(base64Str: encryptedMessage)!)
        
    }
    private func readJsonMessage(jsonStr:String){
        do{
            let jsonMessage = try JSONDecoder().decode(JsonMessage.self, from: TypeChangeHepler().strToUTF8(string: jsonStr))
            switch jsonMessage.type {
            case "GotClientPublicKey":
                print("识别为GotClientPublicKey")
                isOnConnecting = false
                isConnected = jsonMessage.contents.result!

            case "RegisterResult":
                print("识别为RegisterResult")
                isRegisterSuccessful = jsonMessage.contents.result!
                errorMessage = jsonMessage.contents.reasion!

            case "LoginResult":
                print("识别为LoginResult")
                
                isAuthed = jsonMessage.contents.result!
                if(isAuthed){
                    isOnConnecting = false
                    userMd5Str = getMD5(data: host + portStr + username)
                    print("登陆成功")
                }else{
                    isOnConnecting = false
                    disconnect()
                    errorMessage = jsonMessage.contents.reasion!
                    print("登陆失败")
                }
                
            case "CertifyKeyCheckResult":
                print("识别为CertifyKeyCheckResult")
                needActive = jsonMessage.contents.result!
                if(jsonMessage.contents.reasion == "need_create"){
                    //需要新建密钥
                    print("显示选项 创建密钥对")
                    needCreateCertifyKey = true
                    
                }else if(jsonMessage.contents.reasion == "need_copy"){
                    //需要获取密钥
                    print("显示选项 获取密钥对")
                    needCopyCertifyKey = true
                }
               
                break
            case "UpdateCertifyResult":
                print("识别为UpdateCertifyResult")
                if(jsonMessage.contents.result!){
                    needActive = false
                }else{
                    needCreateCertifyKey = true
                }
                break
                
            case "NewLetter":
                print("收到NewLetter")
                let send_from = jsonMessage.contents.send_from!
                let inside = jsonMessage.contents.inside!
                
                let message_uuid = jsonMessage.contents.message_uuid!
                //返回服务器接收到消息
                
                let contents = Contents(message_uuid: message_uuid)
                let reJsonMessage = JsonMessage.init(type:"GotLetter", contents: contents)
                let re_json_Str = JsonHelper().buildJsonMessageStr(input: reJsonMessage)
                
                let encryptedMessage = RSAHelper().encryptByPublicKey(publicKeyStr: publickey_from_server, messageStr: re_json_Str)
                
                // MARK: 发送删除报文
                send(inputData: TypeChangeHepler().strToUTF8(string: encryptedMessage))
                
                let myCertifyKeyPair = DBBuilder(db_name: userMd5Str).getCertifyKeyPair()
                print("解码SendFrom")
                //解码SendFrom
                //print("send_from = \(send_from)")
                let send_from_str = RSAHelper().decryptByPrivateKey(privateKeyStr: myCertifyKeyPair.certifyPrivateKey, encryptedBase64String: send_from)
                print("Send_from_str = \(send_from_str)")
                let send_from_modle = try JSONDecoder().decode(SendFrom.self, from: TypeChangeHepler().strToUTF8(string: send_from_str))
                
                var inside_json_str = ""
                var friend_certifyPublicKey = ""
                
                if(send_from_modle.uuid == nil){
                    print("UUID为空值")
                    //解码inside
                    inside_json_str = RSAHelper().decryptByPrivateKey(privateKeyStr: myCertifyKeyPair.certifyPrivateKey, encryptedBase64String: inside)
                    let inside_json_modle = try JSONDecoder().decode(Inside.self, from: TypeChangeHepler().strToUTF8(string: inside_json_str))
                    print("\(inside_json_str)")
                    
                    friend_certifyPublicKey = inside_json_modle.certifyPublicKey!
                    //验签
                    let signature = jsonMessage.contents.signature!
                    let message_md5 = getMD5(data: send_from_str + inside_json_str)
                    
                    print("MD5:\(message_md5)")
                    print("signature:\(signature)")
                   
                    let chekSignResult = RSAHelper().checkSign(publicKeyStr: friend_certifyPublicKey, signatureStr: signature, message: message_md5)
                    print("验签结果:\(chekSignResult)")
                    
                    if(chekSignResult){
                        
                        //新建好友数据
                        let friendStr = send_from_modle.host + send_from_modle.port + send_from_modle.username + inside_json_modle.certifyPublicKey!
                        let friendMd5Str = getMD5(data: friendStr)
                        
                        let newFriend = FriendData(friend_chat_publickey: inside_json_modle.friend_chat_publickey, nickname: "", my_uuid: inside_json_modle.uuid!,friend_certifyPublicKey: inside_json_modle.certifyPublicKey, host: send_from_modle.host, port: send_from_modle.port, friendName: send_from_modle.username, md5Str: friendMd5Str, active: false)
                        //数据库处理新好友申请
                        DBBuilder(db_name: userMd5Str).gotNewFriendRequest(newFriend: newFriend)
                        
                    }else{
                        //验签失败
                    }
                    
                    
                }else{
                    print("UUID不为空值")
                    //通过uuid查找好友
                    let friend_uuid = send_from_modle.uuid!
                    print("解码出发信人的uuid:\(friend_uuid)")
                    let chat_privatekey = DBBuilder(db_name: userMd5Str).getFriendDataBuild(friend_uuid: friend_uuid)
                    print("查询到的聊天用privatekey:\(chat_privatekey)")
                    inside_json_str = RSAHelper().decryptByPrivateKey(privateKeyStr: chat_privatekey, encryptedBase64String: inside)
                    print("inside:\(inside_json_str)")
                    let inside_json_modle = try JSONDecoder().decode(Inside.self, from: TypeChangeHepler().strToUTF8(string: inside_json_str))
                    //如果不活动
                    
                    if((inside_json_modle.message) != nil){
                        print("检测到message")
                        let newMessage = inside_json_modle.message!
                        print("newMessage:\(newMessage)")
                        ChatDBBuilder(db_name: userMd5Str, withFriendUUID: friend_uuid).gotMessage(message: newMessage, in_time: inside_json_modle.time)
                        
                        //接到了一条新的聊天消息
                        
                        haveNewMessage = true
                        print("检测haveNewMessage->true")
                        
                        
                        
                    }else{
                        print("未检测到message")
                        //将钥匙和uuid插入数据库
                        let my_uuid = inside_json_modle.uuid!
                        let friend_chat_publickey = inside_json_modle.friend_chat_publickey!
                        DBBuilder(db_name: userMd5Str).getAgreeFromFriend(friend_uuid: friend_uuid, my_uuid: my_uuid, friend_chat_publickey: friend_chat_publickey)
                    }
                }

                break
            default:
                break

            }
            
        }catch{
            
        }
        
    }
    
}

extension WebSocketClient: URLSessionWebSocketDelegate {
    
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didOpenWithProtocol protocol: String?) {
        print("Start Client Connect")
        DispatchQueue.main.async {
            
        }
    }
    
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didCloseWith closeCode: URLSessionWebSocketTask.CloseCode, reason: Data?) {
        print("关闭Socket连接:\(closeCode) 原因: \(String(describing: reason))")
        DispatchQueue.main.async { [self] in
            //状态重置
//            print(String(describing: reason))
            print("Disconnect1")
            print("状态重置1")
            self.recoverState()
            
        }
    }
    
    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        print("didCompleteWithError error: \(String(describing: error))")
        print("Socket错误代码:\(error?._code)")
        DispatchQueue.main.async { [self] in
            print("Disconnect2")
            let errorCode = error?._code
            
            //错误表示
            if(errorCode == -1004){
                errorMessage = "无法连接到服务器"
                print("Could not connect to the server")
            }
            
           print("状态重置2")
            //状态重置
            self.recoverState()
            
        }
    }
    
}

enum SomeError: Error {
    case illegalArg(String)
    case outOfBounds(Int, Int)
    case outOfMemory
}


