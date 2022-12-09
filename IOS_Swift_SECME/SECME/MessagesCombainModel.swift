//
//  MessagesCombainHelper.swift
//  SECME
//
//  Created by cmStudent on 2022/12/06.
//

import Foundation
import Combine


class MessagesCombainModel: ObservableObject {

    @Published var messages:[MessagesView]
    @Published var doUpdate:Bool
    
var cancelable: Set<AnyCancellable> = .init()
    
    init(messages:[MessagesView],doUpdate:Bool) {
        self.messages = messages
        self.doUpdate = doUpdate
        
        $messages
            .sink{ value in
 
            }
            .store(in: &cancelable)
    }
    
    
    

}
