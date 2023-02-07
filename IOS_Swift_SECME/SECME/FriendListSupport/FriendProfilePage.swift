//
//  FriendProfilePage.swift
//  SECME
//
//  Created by cmStudent on 2022/11/18.
//

import SwiftUI


struct FriendProfilePage: View {
    private let diviceWidth = UIScreen.main.bounds.width
    @EnvironmentObject var client: WebSocketClient
    @State var isSelectedChat:Bool = false
    
    @State var selectedFriend:FriendDataBuild
    
    init(selectedFriend:FriendDataBuild){
        self.selectedFriend = selectedFriend
    }
    
    
    var body: some View {
        VStack{
            
            Text(selectedFriend.friendName)
                .font(.title)
                .fontWeight(.semibold)
            
            HStack{
                Spacer()
                Text(selectedFriend.host)
                VStack{}
                    .frame(width: 1,height: 20,  alignment: .center)
                    .background(Color.gray)
                    .cornerRadius(1)
                Text(selectedFriend.port)
                Spacer()
            }
            
            VStack{}
                .frame(height: 80)
            
            
            Button{
                isSelectedChat = true
            }label: {
                Text("チャット")
                    .frame(width: diviceWidth * 0.8,height: 40,  alignment: .center)
                    .background(selectedFriend.active ? Color.blue : Color.gray)
                    .foregroundColor(Color.white)
                    .cornerRadius(8)
            }
            .disabled(!selectedFriend.active)

            
            Button {
                /*@START_MENU_TOKEN@*//*@PLACEHOLDER=Action@*/ /*@END_MENU_TOKEN@*/
            }label: {
                Text("フレンドを解除")
                    .frame(width: diviceWidth * 0.8,height: 40,  alignment: .center)
                    .background(Color.red)
                    .foregroundColor(Color.white)
                    .cornerRadius(8)
            }
            
            
            NavigationLink(destination: ChatRoom(selectedFriend: selectedFriend),isActive: $isSelectedChat) {
                //前往添加好友
            }
            
        }
        .ignoresSafeArea()
        
    }
}


