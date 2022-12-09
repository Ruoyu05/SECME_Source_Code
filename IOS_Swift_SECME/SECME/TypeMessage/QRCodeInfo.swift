//
//  Info_QRCode.swift
//  SECME
//
//  Created by cmStudent on 2022/11/29.
//

import SwiftUI
import Combine


class QRCodeInfo: ObservableObject {
    @Published var host:String
    @Published var port:String
    @Published var username:String
    @Published var certifyPublicKey:String
    
    var cancelable: Set<AnyCancellable> = .init()
    
    init(){
        username = ""
        host = ""
        port = ""
        certifyPublicKey = ""
    }
    
    
    
}
