//
//  ChatListPage.swift
//  SECME
//
//  Created by cmStudent on 2022/11/21.
//

import SwiftUI


struct ChatListPage: View {
    @EnvironmentObject var client: WebSocketClient
    @State var isSelectedChat:Bool = false
    
    @State var lastMessageList:[ChatListModel] = []
    
//    var onClick:some Gesture{
//        TapGesture(count: 1)
//            .onEnded{ _ in
//                print("Chat isClicked")
//
//            }
//
//    }
    
    
    var body: some View {
        VStack{
            HStack{
                HStack{
                    Spacer()
                    Text("Chat")
                        .font(.body)
                        .fontWeight(.heavy)
                    Spacer()
                }.padding(.bottom, 10.0)
            }
            .padding(.top, 32.0)
            .background(Color("View_Table_Color"))
            
            
            GeometryReader { geometry in
//                let width = geometry.size.width
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(alignment: .leading) {
                            ForEach(0..<lastMessageList.count, id: \.self) { number in
                        
                                
                                let friendDatabuild = DBBuilder(db_name: client.userMd5Str).getChatDataBuild(friend_uuid: lastMessageList[number].uuid)
                                
                                NavigationLink(destination: ChatRoom(selectedFriend: friendDatabuild)) {
                                    HStack{
                                        Text("")
                                            .font(.largeTitle)
                                            .frame(width: 40, height: 40, alignment: .center)
                                            .background(Color.green)
                                            .cornerRadius(5)
                                        
                                        VStack{
                                            
                                            HStack{
                                                Text(lastMessageList[number].name)
                                                    .foregroundColor(Color("Font_Color_Black"))
                                                Spacer()
                                            }
                                            HStack{
                                                Text(lastMessageList[number].lastMessage)
                                                    .foregroundColor(Color.gray)
                                                    .lineLimit(1)
                                                Spacer()
                                            }
                                        }
                                        ZStack{
                                            if(lastMessageList[number].needReadCount > 0){
                                                Image(systemName: "circle.fill")
                                                    .resizable()
                                                    .aspectRatio(contentMode: .fit)
                                                    .frame(width: 20, height: 20, alignment: .center)
                                                    .foregroundColor(Color.red)
                                                
                                                if(lastMessageList[number].needReadCount > 9){
                                                    Text("n.")
                                                        .font(.headline)
                                                        .foregroundColor(Color.white)
                                                }else{
                                                    Text("\(lastMessageList[number].needReadCount)")
                                                        .font(.headline)
                                                        .foregroundColor(Color.white)
                                                }
                                            }
                                        }.frame(width: 40, height: 40, alignment: .center)
                                        
                                    }.padding(.leading, 15.0)
                                }
                                

                            
                            }

                        }
                    }
                }
                .onAppear{
                    DispatchQueue.main.async {
                        let chatList = DBBuilder(db_name: client.userMd5Str).getAllfriendUUID()
                        print("读取到好友数量:\(chatList.count)")
                        var i = 0
                        while(i < chatList.count){
                            print("=============================================")
                            print("count:\(i)")
                            print("friend:\(chatList[i].name)")
                            print("uuid:\(chatList[i].uuid)")
                            chatList[i].lastMessage =  ChatDBBuilder(db_name: client.userMd5Str, withFriendUUID: chatList[i].uuid).getLastMessage()
                            print("lastMessage:\(chatList[i].lastMessage)")
                            
                            chatList[i].needReadCount = ChatDBBuilder(db_name: client.userMd5Str, withFriendUUID: chatList[i].uuid).getCount_NeedRead()
                            
                            print("Message:\(chatList[i].needReadCount)")
                            i += 1
                            print("=============================================")
                        }
                        lastMessageList = chatList
                    }
                    
                }

            }

            
            
        }
        .ignoresSafeArea()
        .navigationTitle("")
        .navigationBarHidden(false)
        
    }
}

struct ChatListPage_Previews: PreviewProvider {
    static var previews: some View {
        ChatListPage()
    }
}
