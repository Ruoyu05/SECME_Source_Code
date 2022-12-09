//
//  FriendData.swift
//  SECME
//
//  Created by cmStudent on 2022/11/29.
//

import Foundation

struct FriendData: Codable ,Identifiable{
    
    var id:Int? = nil
    var friend_chat_publickey:String? = nil
    var nickname:String? = nil
    var my_uuid:String? = nil
    var friend_uuid:String? = nil
    var my_chat_publickey:String? = nil
    var my_chat_privatekey:String? = nil
    var friend_certifyPublicKey:String? = nil
    var host:String? = nil
    var port:String? = nil
    var friendName:String? = nil
    var md5Str:String? = nil
    var active:Bool? = false
    
    mutating func setNickName(nickname:String){
        if(nickname != ""){
            self.nickname = nickname
        }
    }
    mutating func setFriendActive(){
        active = true
    }
    mutating func set_my_uuid(){
        my_uuid = UUID().uuidString
    }
    mutating func set_friend_uuid(){
        friend_uuid = UUID().uuidString
    }
    
}
