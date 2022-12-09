//
//  QRInfo.swift
//  SECME
//
//  Created by cmStudent on 2022/11/29.
//

import Foundation

class QRInfo:Codable{
    var user:String
    var host:String
    var port:String
    var certifyKey:String
    
    init(user:String,host:String,port:String,certifyKey:String) {
        self.user = user
        self.host = host
        self.port = port
        self.certifyKey = certifyKey
    }
    
}
