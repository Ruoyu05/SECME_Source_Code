//
//  ChatRoom.swift
//  SECME
//
//  Created by cmStudent on 2022/11/21.
//

import SwiftUI

struct ChatRoom: View {
    @EnvironmentObject var client: WebSocketClient
    private let diviceWidth = UIScreen.main.bounds.width
    private let diviceHeight = UIScreen.main.bounds.height
    
    @State private var message = ""
    
    @State var selectedFriend:FriendDataBuild
    
    @StateObject var messagesModel = MessagesCombainModel(messages: [], doUpdate: false)
    
    
    var body: some View {
        VStack{
            HStack{
            }
            .padding(.top, 40.0)
            .background(Color("Chat_Table_Color"))
            .ignoresSafeArea()
            .onChange(of: client.haveNewMessage, perform: { value in
                print("检查value")
                if(value){
                    print("有新消息")
                    //从数据库查询一遍消息
                    messagesModel.messages = ChatDBBuilder(db_name: client.userMd5Str, withFriendUUID: selectedFriend.friend_uuid).getMessages()
                    messagesModel.doUpdate.toggle()
                    client.haveNewMessage = false
                    
                }
            })
                MessageTableView(messagesModel: messagesModel)
                    .padding(.top, -45.0)
                    .onAppear{
                        print("生成MessageTable")
                        messagesModel.messages = ChatDBBuilder(db_name: client.userMd5Str, withFriendUUID: selectedFriend.friend_uuid).getMessages()
                    }

            HStack{
                VStack{
                    VStack{
                        TextField("message", text: $message)
                            .font(.body)
                            .padding(.leading, 4.0)
                            .frame(height: 35, alignment: .center)
                            .background(Color("Chat_Input_Color"))
                            
                    }
                    .padding(.horizontal, 5.0)
                    .background(Color("Chat_Input_Color"))
                    .cornerRadius(5)
                }.padding([.top, .leading, .bottom], 8.0)
                
                
                Button{
                    if(message != ""){
                        print("Send Message:\(message)")
                        //存储消息
                        let in_time = Date()
                        ChatDBBuilder(db_name: client.userMd5Str, withFriendUUID: selectedFriend.friend_uuid).sendMessage(message: message, in_time: in_time)
                        messagesModel.messages.append(MessagesView(contentMessage: message, isCurrentUser: true))
                        messagesModel.doUpdate.toggle()
                        
                        //获取自己的身份私钥
                        let certifyKeyPair =  DBBuilder(db_name: client.userMd5Str).getCertifyKeyPair()
                        let certifyPrivatekeyStr = certifyKeyPair.certifyPrivateKey
                        
                        let sendto = SendTo(username: selectedFriend.friendName, host: selectedFriend.host, port: selectedFriend.port)
                        
                        let sendfrom = SendFrom(username: client.username, host: client.host, port: client.portStr,uuid: selectedFriend.my_uuid)
                        let sendfrom_json_data = try! JSONEncoder().encode(sendfrom)
                        let sendfrom_json = String(bytes: sendfrom_json_data, encoding: .utf8)!
                        let encrypted_sendfrom_json = RSAHelper().encryptByPublicKey(publicKeyStr: selectedFriend.friend_certifyPublicKey, messageStr: sendfrom_json)
                        
                        let inside = Inside(message:message)
                        
                        let inside_json_data = try! JSONEncoder().encode(inside)
                        let inside_json = String(bytes: inside_json_data, encoding: .utf8)!
                        let encrypted_inside_json = RSAHelper().encryptByPublicKey(publicKeyStr: selectedFriend.friend_chat_publickey, messageStr: inside_json)
                        
                        //计算MD5
                        let signStr = sendfrom_json + inside_json
                        let md5code = getMD5(data: signStr)
                        
                        //签名
                        let signature = RSAHelper().doSign(privateKeyStr: certifyPrivatekeyStr, text: md5code)
                        
                        let contents = Contents(send_from: encrypted_sendfrom_json, signature: signature, send_to: sendto, inside: encrypted_inside_json)
                        let jsonMessage = JsonMessage(type: "PostSendRequest", contents: contents)
                        let jsonMessage_json = JsonHelper().buildJsonMessageStr(input: jsonMessage)
                        let entcrypted_jsonMessage = RSAHelper().encryptByPublicKey(publicKeyStr: client.publickey_from_server, messageStr: jsonMessage_json)
                        
                        //发送
                        client.send(inputData: TypeChangeHepler().strToUTF8(string:entcrypted_jsonMessage))
                        message = ""
                    }
                    
                    
                }label: {
                    Image(systemName: "paperplane.fill")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .rotationEffect(Angle(degrees: 45))
                        .frame(width: 30,height: 30,alignment: .center)
                }
                .disabled(message == "")
                .padding(.leading, -20.0)
                .frame(width: 45,height: 35,alignment: .center)
            }
            .background(Color("Chat_Table_Color"))
            .onTapGesture(perform: {
                print("onTapGesture")
                messagesModel.doUpdate.toggle()
            })
        }
        
        .background(Color("Chat_Background_Color"))
        .navigationBarTitleDisplayMode(NavigationBarItem.TitleDisplayMode.inline)
        .navigationBarTitle(selectedFriend.friendName)
        .navigationBarHidden(false)
    }
        
}

