//
//  friendListModel.swift
//  SECME
//
//  Created by cmStudent on 2022/12/06.
//

import Foundation
import Combine


class ChatListModel: ObservableObject {

    @Published var uuid:String = ""
    @Published var name:String = ""
    @Published var nickname:String = ""
    @Published var lastMessage:String = ""
    @Published var needReadCount:Int = 0
    
var cancelable: Set<AnyCancellable> = .init()
    
    init(){
        $uuid
            .sink(receiveValue: { value in
                if(value != ""){
                    print("uuid:\(value)")
                }
            })
            .store(in: &cancelable)
    }

}
