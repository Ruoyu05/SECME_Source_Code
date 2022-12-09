//
//  JsonMessage.swift
//  SECME
//
//  Created by cmStudent on 2022/11/24.
//

import Foundation
import UIKit

struct JsonMessage: Codable {
    let type: String
    let contents: Contents
}

struct Contents: Codable {
    var clientPublickey:String? = nil
    var serverPublickey:String? = nil
    var result:Bool? = nil
    var reasion:String? = nil
    var username:String? = nil
    var password:String? = nil
    var certifyPublicKey:String? = nil
    
    //好友相关
    var send_from:String? = nil
    var signature:String? = nil
    var send_to:SendTo? = nil
    var inside:String? = nil
    var message_uuid:String? = nil
    
    
    
    enum CodingKeys: String, CodingKey {
        case serverPublickey = "server_publickey"
        case clientPublickey = "client_publickey"
        case result = "result"
        case reasion = "reasion"
        case username = "username"
        case password = "password"
        case certifyPublicKey = "certify_publickey"
        case send_from = "send_from"
        case signature = "signature"
        case send_to = "send_to"
        case inside = "inside"
        case message_uuid = "message_uuid"
        
    }
}
struct SendTo: Codable {
    var username:String
    var host:String
    var port:String
    var uuid:String? = nil
}

struct SendFrom: Codable {
    var username:String
    var host:String
    var port:String
    var uuid:String? = nil
}

struct Inside: Codable {
    var friend_chat_publickey:String? = nil
    var certifyPublicKey:String? = nil
    var uuid:String? = nil
    var message:String? = nil
    var applaymessage:String? = nil
    let time:Date = Date()
    
    enum CodingKeys: String, CodingKey {
        case friend_chat_publickey = "friend_chat_publickey"
        case certifyPublicKey = "certify_publickey"
        case uuid = "uuid"
        case applaymessage = "applaymessage"
        case message = "message"
        case time = "time"
    }
}
