//
//  FriendsListPage.swift
//  SECME
//
//  Created by cmStudent on 2022/11/18.
//

import SwiftUI


struct FriendsListPage: View {
    
    @EnvironmentObject var client: WebSocketClient
    @State var isAddFriend:Bool = false
    @State var isFriendDetailPage:Bool = false
    
    @State var friendList:[FriendDataBuild] = []
    @State var selectedFriend:FriendDataBuild = FriendDataBuild(id: 0, friend_chat_publickey: "", nickname: "", my_uuid: "", friend_uuid: "", my_chat_publickey: "", my_chat_privatekey: "", friend_certifyPublicKey: "", host: "", port: "", friendName: "", md5Str: "", active: false)
    
    var body: some View {
        VStack{
            HStack{
                ZStack{
                    Text("フレンド")
                        .font(.body)
                        .fontWeight(.heavy)
                    HStack{
                        Spacer()
                        Button{
                            isAddFriend = true
                        }label: {
                            Image(systemName:"person.crop.circle.badge.plus")
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 24, height: 24, alignment: .center)
                                .foregroundColor(Color.blue)
                        }
                        .padding(.trailing, 15.0)
                    }
                }.padding(.bottom, 10.0)
            }
            .padding(.top, 32.0)
            .background(Color("View_Table_Color"))
            
            
            GeometryReader { geometry in
                let width = geometry.size.width
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(alignment: .leading) {
                            ForEach(0..<friendList.count, id: \.self) { number in
                                FriendsTable(friend: friendList[number], isFriendDetailPage: $isFriendDetailPage, selectedFriend: $selectedFriend)
                            }
                        }
                    }
                    .onAppear{
                        print("List onAppear")
                        DispatchQueue.main.async {
                        friendList = DBBuilder(db_name: client.userMd5Str).getAllFriend()
                        }
                    }
                }
            }
            
 

            NavigationLink(destination: AddFriendPage(),isActive: $isAddFriend) {
                //前往添加好友
            }
            NavigationLink(destination: FriendProfilePage(selectedFriend: selectedFriend),isActive: $isFriendDetailPage) {
                //前往朋友详细页面
            }
            
        }
        .ignoresSafeArea()
        .navigationTitle("")
        .navigationBarHidden(false)
    }
}

struct FriendsTable: View {
    @EnvironmentObject var client: WebSocketClient
    
    @State var friend:FriendDataBuild
    
    @Binding var isFriendDetailPage:Bool
    @Binding var selectedFriend:FriendDataBuild
    


    var onClick:some Gesture{
        TapGesture(count: 1)
            .onEnded{ _ in
                print("TapGesture isClicked")
                print("shelected:\(friend.friendName)")
                selectedFriend = friend
                isFriendDetailPage = true
            }
        
    }
    private let diviceWidth = UIScreen.main.bounds.width
    var body: some View {
        
        VStack{
            HStack{
                Image(systemName: "person.crop.square")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width:36,height: 36,alignment: .center)
                    .foregroundColor(Color.gray)
                
                if(friend.nickname == ""){
                    Text(friend.friendName)
                }else{
                    Text("\(friend.nickname)(\(friend.friendName))")
                }
                Spacer()
                if(!friend.active){
                    if((friend.friend_uuid == "")){
                        Button{
                            print("Click Agree")
                            //同意添加好友
                            sendAgreeMessage()
                        }label: {
                            Image(systemName: "plus")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width:18,height: 18,alignment: .center)
                                .foregroundColor(Color.white)
                        }
                        .frame(width:55,height: 30,alignment: .center)
                        .background(Color.green)
                        .cornerRadius(5)
                        //防止表单中多按钮重复响应
                        .buttonStyle(BorderlessButtonStyle())
                        
                        Button{
                            
                            DBBuilder(db_name: client.userMd5Str).deleteFriendRecord(md5Str:friend.md5Str)
                            
                            print("Click Delate")
                        }label: {
                            Image(systemName: "trash")
                                .frame(width: 55, height: 30, alignment: .center)
                                .background(Color.red)
                                .foregroundColor(Color.white)
                                .cornerRadius(5)
                        }
                        
                        
                        
                        //防止表单中多按钮重复响应
                        .buttonStyle(BorderlessButtonStyle())
                        
                    }else{
                        
                        Image(systemName: "hourglass.circle")
                            .frame(width: 35, height: 35, alignment: .center)
                            .background(Color.yellow)
                            .foregroundColor(Color.black)
                            .cornerRadius(5)
                        
                    }
                }
                
                
                
            }
            .padding(.horizontal, 15.0)
            VStack{
               
            }
            .frame(width: diviceWidth, height: 0.5, alignment: .center)
            .background(Color.gray)
        }
        .gesture(onClick)
        
    }
    
    func sendAgreeMessage(){
        //获取自己的身份私钥
        let certifyKeyPair =  DBBuilder(db_name: client.userMd5Str).getCertifyKeyPair()
        let certifyPrivatekeyStr = certifyKeyPair.certifyPrivateKey
        
        //获取好友公钥
        let friend_certifyPublicKey = friend.friend_certifyPublicKey
        let friend_chat_publickey = friend.friend_chat_publickey
        
        //检查数据库中已经存在数据
        DBBuilder(db_name: client.userMd5Str).checkAgreeFriend(md5Str: friend.md5Str)
        
        //读取密钥对
        let my_chat_publickey = DBBuilder(db_name: client.userMd5Str).getFriendChatPublicKey(md5Str: friend.md5Str)
        //        let my_chat_privatekey = DBBuilder(db_name: client.userMd5Str).getFriendChatPrivateKey(md5Str: friend.md5Str)
        
        //读取uuid
        let friend_uuid = DBBuilder(db_name: client.userMd5Str).getFriendUUID(md5Str: friend.md5Str)
        
        
        let send_from = SendFrom(username: client.username, host: client.host, port: client.portStr, uuid: friend.my_uuid)
        let send_from_json_data = try! JSONEncoder().encode(send_from)
        let send_from_json = String(bytes: send_from_json_data, encoding: .utf8)!
        let encrypted_send_from_json = RSAHelper().encryptByPublicKey(publicKeyStr: friend_certifyPublicKey, messageStr: send_from_json)
        
        
        let inside = Inside(friend_chat_publickey: my_chat_publickey,uuid: friend_uuid)
        let inside_json_data = try! JSONEncoder().encode(inside)
        let inside_json = String(bytes: inside_json_data, encoding: .utf8)!
        let encrypted_inside_json = RSAHelper().encryptByPublicKey(publicKeyStr: friend_chat_publickey, messageStr: inside_json)
        
        let send_to = SendTo(username: friend.friendName, host: friend.host, port: friend.port, uuid: friend_uuid)
        
        //计算MD5
        let signStr = send_from_json + inside_json
        let md5code = getMD5(data: signStr)
        
        //签名
        let signature = RSAHelper().doSign(privateKeyStr: certifyPrivatekeyStr, text: md5code)
        
        let contents = Contents(send_from: encrypted_send_from_json, signature: signature, send_to: send_to, inside: encrypted_inside_json)
        let jsonMessage = JsonMessage(type: "PostSendRequest", contents: contents)
        
        //加密
        let jsonMessage_json = JsonHelper().buildJsonMessageStr(input: jsonMessage)
        let entcrypted_jsonMessage = RSAHelper().encryptByPublicKey(publicKeyStr: client.publickey_from_server, messageStr: jsonMessage_json)
        client.send(inputData: TypeChangeHepler().strToUTF8(string:entcrypted_jsonMessage))
        
    }
}


struct FriendsListPage_Previews: PreviewProvider {
    static var previews: some View {
        FriendsListPage()
    }
}
