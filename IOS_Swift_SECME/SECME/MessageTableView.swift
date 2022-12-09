//
//  ContentView.swift
//  ChatViewTest
//
//  Created by cmStudent on 2022/12/06.
//


import SwiftUI

struct MessageTableView: View {
    
    @StateObject var messagesModel:MessagesCombainModel
    @State var scrollProxy: ScrollViewProxy?
    
    var body: some View {
        ZStack{
            GeometryReader { geometry in
                let width = geometry.size.width
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(alignment: .leading) {
                            
                            ForEach(0..<messagesModel.messages.count, id: \.self) { number in
                                let isCurrentUser = messagesModel.messages[number].isCurrentUser
                                HStack {
                                    if isCurrentUser { Spacer(minLength: width - 250) }
                                    messagesModel.messages[number]
                                        .padding(10)
                                        .id(number)
                                    if !isCurrentUser { Spacer(minLength: width - 250) }
                                }
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .onAppear{
                            print("初始化scroll")
                            scrollProxy = proxy
                            messagesModel.doUpdate.toggle()
                        }
                    }
                }
                
            }
            .onChange(of: messagesModel.doUpdate, perform: { _ in
                print("scrollProxy定位")
                scrollProxy?.scrollTo(messagesModel.messages.count - 1, anchor: .bottom)
            })
        }
        .navigationBarHidden(true)
    }
}


struct MessagesView: View {
    var contentMessage: String
    var isCurrentUser: Bool
    
    var body: some View {
        Text(contentMessage)
            .lineLimit(nil)
            .padding(10)
            .foregroundColor(isCurrentUser ? Color.white : Color("Font_Color_Black"))
            .background(isCurrentUser ? Color.blue : Color("Chat_Message_Table_Color_get"))
            .cornerRadius(10)
    }
}
