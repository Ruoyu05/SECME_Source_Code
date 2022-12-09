//
//  Text.swift
//  SECME
//
//  Created by cmStudent on 2022/12/01.
//

import SwiftUI

struct TextPage: View {
    
    let messages: [MessageView] = [
        .init(contentMessage: "あめんぼあかいなあいうえおうきもに　こえびも　およいでる", isCurrentUser: true),
        .init(contentMessage: "あめんぼあかいなあいうえおうきもにこえびもおよいでる", isCurrentUser: false),
        .init(contentMessage: "あめんぼあかいなあいうえおうきもに　こえびも　およいでる", isCurrentUser: false),
        .init(contentMessage: "あめんぼあかいなあいうえおうきもにこえびもおよいでる", isCurrentUser: false),
        .init(contentMessage: "あめんぼあかいなあいうえおうきもにこえびもおよいでる", isCurrentUser: false),
        .init(contentMessage: "かきのきくりのきかきくけこ　きつつきこつこつかれけやき", isCurrentUser: true),
        .init(contentMessage: "大角豆に 酢をかけ さしすせそその魚 浅瀬で 刺しました", isCurrentUser: true),
        .init(contentMessage: "たちましょらっぱでたちつてと　とてとてたったととびたった", isCurrentUser: true),
        .init(contentMessage: "なめくじのろのろなにぬねのなんどにぬめってなにねばる", isCurrentUser: true),
        .init(contentMessage: "はとぽっぽほろほろはひふへほひなたのおへやにゃふえをふく", isCurrentUser: true)
    ]
    
    @State var scrollProxy: ScrollViewProxy?
    var body: some View {
        
        GeometryReader { geometry in
            let width = geometry.size.width
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading) {
                        
                        ForEach(0..<messages.count, id: \.self) { number in
                            let isCurrentUser = messages[number].isCurrentUser
                            HStack {
                                if isCurrentUser { Spacer(minLength: width - 250) }
                                messages[number]
                                    .padding(20)
                                    .id(number)
                                if !isCurrentUser { Spacer(minLength: width - 250) }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .onAppear{
                        scrollProxy = proxy
                        scrollProxy?.scrollTo(messages.count - 1, anchor: .bottom)
                    }
                }
            }
            
        }
        
    }
}
struct MessageView: View {
    var contentMessage: String
    var isCurrentUser: Bool
    
    var body: some View {
        Text(contentMessage)
            .lineLimit(nil)
            .padding(10)
            .foregroundColor(isCurrentUser ? Color.white : Color.black)
            .background(isCurrentUser ? Color.blue : Color(UIColor(red: 240/255, green: 240/255, blue: 240/255, alpha: 1.0)))
            .cornerRadius(10)
    }
}
