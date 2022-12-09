//
//  FriendDataBuild.swift
//  SECME
//
//  Created by cmStudent on 2022/11/30.
//


import Foundation

struct FriendDataBuild: Codable ,Identifiable{
    
    var id:Int
    var friend_chat_publickey:String
    var nickname:String
    var my_uuid:String
    var friend_uuid:String
    var my_chat_publickey:String
    var my_chat_privatekey:String
    var friend_certifyPublicKey:String
    var host:String
    var port:String
    var friendName:String
    var md5Str:String
    var active:Bool

}

