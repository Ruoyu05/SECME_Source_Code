//
//  FriendDetailPage.swift
//  SECME
//
//  Created by cmStudent on 2022/11/29.
//

import SwiftUI

struct FriendDetailPage: View {

        @EnvironmentObject var client: WebSocketClient
        private let diviceWidth = UIScreen.main.bounds.width
        
        @State var nickname:String = ""
    @State var applayMessage:String = ""
        @State var friendJson:String = ""
        @State var friendQRInfo:FriendQRInfo

        
        init(friendQRInfo:FriendQRInfo) {
            self.friendQRInfo = friendQRInfo
        }
        
        var body: some View {
            
            VStack{
                HStack{
                    VStack{
                        Text("友達の追加")
                            .font(.body)
                            .fontWeight(.heavy)
                    }
                    .frame(width: diviceWidth, alignment: .center)
                    .padding(.bottom, 10.0)

                }
                .padding(.top, 32.0)
                .background(Color("View_Table_Color"))
                Spacer()
                
                HStack{
                    Text(friendQRInfo.username)
                        .font(.title)
                        .fontWeight(.semibold)
                    Spacer()
                }
                .frame(width: diviceWidth * 0.9, alignment: .center)
                
                HStack{
                    Text("\(friendQRInfo.host):\(friendQRInfo.port)")
                        .font(.headline)
                        .fontWeight(.semibold)
                        .foregroundColor(Color.gray)
                    Spacer()
                }
                .frame(width: diviceWidth * 0.9, alignment: .center)
                .padding(.bottom, 8.0)
                //MD校对框
                VStack{
                    HStack{
                        Text("Md5認証:")
                            .font(.caption)
                        Spacer()
                    }
                    .frame(width: diviceWidth * 0.9, alignment: .center)
                    
                    ZStack{
                        Text(friendQRInfo.md5Str)
                        RoundedRectangle(cornerRadius: 3)
                            .stroke(.gray, lineWidth: 3)
                            .frame(width: diviceWidth * 0.9 ,height: 35, alignment: .center)
                    }
                    .frame(width: diviceWidth * 0.9, alignment: .center)
                }

                
                //昵称设定框
                VStack{
                    HStack{
                        Text("nickname:")
                            .font(.caption)
                        Spacer()
                    }
                    .frame(width: diviceWidth * 0.9, alignment: .center)
                    ZStack{
                        RoundedRectangle(cornerRadius: 3)
                            .stroke(.green, lineWidth: 3)
                            .frame(width: diviceWidth * 0.9,height: 35, alignment: .center)
                        TextField("", text: $nickname)
                            
                            .frame(width: diviceWidth * 0.83,height: 35, alignment: .center)
                    }
                }

                //验证申请框
                VStack{
                    HStack{
                        Text("验证申请:")
                            .font(.caption)
                        Spacer()
                    }
                    .frame(width: diviceWidth * 0.9, alignment: .center)
                    ZStack{
                        RoundedRectangle(cornerRadius: 3)
                            .stroke(.green, lineWidth: 3)
                            .frame(width: diviceWidth * 0.9,height: 35, alignment: .center)
                        TextField("", text: $applayMessage)
                            
                            .frame(width: diviceWidth * 0.83,height: 35, alignment: .center)
                    }
                }

                Spacer()
                Button("友達を追加") {
                    //获取自己的身份私钥
                    let certifyKeyPair =  DBBuilder(db_name: client.userMd5Str).getCertifyKeyPair()
                    let certifyPrivatekeyStr = certifyKeyPair.certifyPrivateKey
                    //let certifyPublickeyStr = certifyKeyPair.certifyPublicKey

                    //创建好友信息
                    var friendData = FriendData(nickname:nickname, friend_certifyPublicKey: friendQRInfo.certifyPublicKey,host: friendQRInfo.host, port: friendQRInfo.port, friendName: friendQRInfo.username, md5Str: friendQRInfo.md5Str)
                    
                    friendData.setNickName(nickname: nickname)
                    
                    //对比数据库并生成最终朋友信息
                    let newfriendData = DBBuilder(db_name: client.userMd5Str).addNewFriend(friendData: friendData)
                    

                    let sendto = SendTo(username: newfriendData.friendName!, host: newfriendData.host!, port: newfriendData.port!)
                    
                    let sendfrom = SendFrom(username: client.username, host: client.host, port: client.portStr)
                    let sendfrom_json_data = try! JSONEncoder().encode(sendfrom)
                    let sendfrom_json = String(bytes: sendfrom_json_data, encoding: .utf8)!
                    
                    //加密
                    let encrypted_sendfrom_json = RSAHelper().encryptByPublicKey(publicKeyStr: newfriendData.friend_certifyPublicKey!, messageStr: sendfrom_json)
        
                    let inside = Inside(friend_chat_publickey: newfriendData.my_chat_publickey, certifyPublicKey: certifyKeyPair.certifyPublicKey, uuid: newfriendData.friend_uuid!, applaymessage: applayMessage)
                    
                    let inside_json_data = try! JSONEncoder().encode(inside)
                    let inside_json = String(bytes: inside_json_data, encoding: .utf8)!
                    let encrypted_inside_json = RSAHelper().encryptByPublicKey(publicKeyStr: newfriendData.friend_certifyPublicKey!, messageStr: inside_json)
                    
                    //计算MD5
                    let signStr = sendfrom_json + inside_json
                    let md5code = getMD5(data: signStr)

                    //签名
                    let signature = RSAHelper().doSign(privateKeyStr: certifyPrivatekeyStr, text: md5code)

                    let contents = Contents(send_from: encrypted_sendfrom_json, signature: signature, send_to: sendto, inside: encrypted_inside_json)
                    let jsonMessage = JsonMessage(type: "PostSendRequest", contents: contents)
                    let jsonMessage_json = JsonHelper().buildJsonMessageStr(input: jsonMessage)
                    let entcrypted_jsonMessage = RSAHelper().encryptByPublicKey(publicKeyStr: client.publickey_from_server, messageStr: jsonMessage_json)
                    client.send(inputData: TypeChangeHepler().strToUTF8(string:entcrypted_jsonMessage))
                    
                    
                    
                }
                    .frame(width: diviceWidth * 0.9 ,height: 40)
                    .background(Color.blue)
                    .foregroundColor(Color.white)
                    .cornerRadius(8)
                
                Spacer()
                
            }
            .ignoresSafeArea()

        }
        
    }

