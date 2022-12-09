//
//  DBHelper.swift
//  SECME
//
//  Created by cmStudent on 2022/11/24.
//

import Foundation
import SQLite3


struct DBBuilder{
    //定义数据库
    let db_file_name:String
    let db_Path:String
    var db:OpaquePointer?
    
    init(db_name:String){
        //设置数据库文件名称
        db_file_name = db_name + ".db"
        //        print("启动数据库:\(db_file_name)")
        //查询数据库file文件路径 //如果不存在则新建一个并返回
        let db_Path = try! FileManager.default.url(for: .documentDirectory, in: .userDomainMask,appropriateFor: nil,create: true).appendingPathComponent(db_file_name).path
        self.db_Path = db_Path
        
        guard sqlite3_open(db_Path,&db) == SQLITE_OK else {
            print("ERROR WITH OPEN " + db_file_name)
            return
        }
        //启动数据库
        sqlite3_open(db_Path, &db)
        
        createCertifyStorage()
        creatFriendList()
    }

    
    // MARK: 身份密钥
    //创建身份密钥数据库
    private func createCertifyStorage(){
        //新建身份密钥库
        let sql = "CREATE TABLE IF NOT EXISTS CertifyStorage (id INTEGER PRIMARY KEY AUTOINCREMENT ,certify_publickey TEXT ,certify_privatekey TEXT)"
        guard db_query(sql: sql) else { return }
    }
    
    
    
    // MARK: 身份公钥
    func certifyStorageHaveNoData() -> Bool{
        let sql = "SELECT * FROM `CertifyStorage` WHERE id = 1"
        let result:[[String : AnyObject]]? = queryData(querySQL: sql)
        if(result!.count == 0){
            return true
        }
        return false
    }
    
    
    func updateCertifyPublicKey(certifyPublicKey:String){
        if(certifyStorageHaveNoData()){
            print("数据库无密钥对")
            let sqlInsert = "INSERT INTO `CertifyStorage` (`id`, `certify_publickey`) VALUES (NULL, \'\(certifyPublicKey)\')"
            guard db_query(sql: sqlInsert) else {
                print("插入CertifyKey失败")
                return
            }
            print("插入CertifyPublicKey成功")
        }else{
            print("数据库有密钥对")
            let sqlUpdate = "UPDATE `CertifyStorage` SET `certify_publickey` = \'\(certifyPublicKey)\' WHERE id = 1"
            guard db_query(sql: sqlUpdate) else {
                print("更新CertifyPublicKey失败")
                return
            }
            print("更新CertifyPublicKey成功")
        }
        
    }
    func updateCertifyPrivateKey(certifyPrivateKey:String){
        if(certifyStorageHaveNoData()){
            print("数据库无密钥对")
            let sqlInsert = "INSERT INTO `CertifyStorage` (`id`,  `certify_privatekey`) VALUES (NULL,  \'\(certifyPrivateKey)\')"
            guard db_query(sql: sqlInsert) else {
                print("插入CertifyPrivateKey失败")
                return
            }
            print("插入CertifyKey成功")
        }else{
            print("数据库有密钥对")
            let sqlUpdate = "UPDATE `CertifyStorage` SET `certify_privatekey` = \'\(certifyPrivateKey)\' WHERE id = 1"
            guard db_query(sql: sqlUpdate) else {
                print("更新CertifyPrivateKey失败")
                return
            }
            print("更新CertifyPrivateKey成功")
        }
    }
    
    func updateCertifyKeyPair(certifyPublicKey:String,certifyPrivateKey:String){
        
        if(certifyStorageHaveNoData()){
            print("数据库无密钥对")
            let sqlInsert = "INSERT INTO `CertifyStorage` (`id`, `certify_publickey`, `certify_privatekey`) VALUES (NULL, \'\(certifyPublicKey)\', \'\(certifyPrivateKey)\')"
            guard db_query(sql: sqlInsert) else {
                print("插入CertifyKey失败")
                return
            }
            print("插入CertifyKey成功")
        }else{
            print("数据库有密钥对")
            let sqlUpdate = "UPDATE `CertifyStorage` SET `certify_publickey` = \'\(certifyPublicKey)\', `certify_privatekey` = \'\(certifyPrivateKey)\' WHERE id = 1"
            guard db_query(sql: sqlUpdate) else {
                print("更新CertifyKey失败")
                return
            }
            print("更新CertifyKey成功")
        }
    }
    
    func getCertifyKeyPair() -> CertifyKeyPair{
        var certifyKeyPair = CertifyKeyPair(certifyPublicKey: "", certifyPrivateKey: "")
        let sql = "SELECT * FROM `CertifyStorage` WHERE id = 1"
        let result:[[String : AnyObject]]? = queryData(querySQL: sql)
        for i in 0..<result!.count {
            let keys:[String : AnyObject] = result![i]
            for key in keys {
                switch(key.key){
                case "certify_publickey":
                    certifyKeyPair.certifyPublicKey = key.value as! String
                case "certify_privatekey":
                    certifyKeyPair.certifyPrivateKey = key.value as! String
                default:
                    break
                }
            }
        }
        return certifyKeyPair
    }
    
    // MARK: 好友管理
    
    //创建好友数据库
    private func creatFriendList(){
        //新建好友列表 id friend_certifyPublicKey friend_chat_publickey my_chat_publickey my_chat_privatekey
        //  host port friendName nickName md5Str my_uuid friend_uuid active
        let sql = "CREATE TABLE IF NOT EXISTS FriendList (id INTEGER PRIMARY KEY AUTOINCREMENT,friend_certifyPublicKey TEXT,friend_chat_publickey TEXT,my_chat_publickey TEXT,my_chat_privatekey TEXT,host TEXT,port TEXT,friendName TEXT,nickName TEXT,md5Str TEXT,my_uuid TEXT,friend_uuid TEXT,active TINYINT(1))"
        guard db_query(sql: sql) else { return }
        
    }
    
    func addNewFriend(friendData:FriendData) -> FriendData{
        //查询数据库好友是否存在
        var newFriendData = friendData
        
        let checkNewFriendSql = "SELECT *  FROM `FriendList` WHERE `md5Str` = \'\(newFriendData.md5Str!)\'"
        
        let result:[[String : AnyObject]]? = queryData(querySQL: checkNewFriendSql)
        
        if(result?.count == nil || result?.count == 0){
            print("没有好友记录")
            //生成uuid
            //newFriendData.my_uuid = UUID().uuidString
            newFriendData.friend_uuid = UUID().uuidString
            
            //生成聊天用密钥
            let chatKeyPair = RSAHelper().creatKeyPair(length: 2048)
            newFriendData.my_chat_publickey = chatKeyPair.getPublicKey()
            newFriendData.my_chat_privatekey = chatKeyPair.getPrivateKey()
            
            let addFriendSql = "INSERT INTO `FriendList` ( `nickname`,`friend_uuid`,`my_chat_publickey`,`my_chat_privatekey`,`friend_certifyPublicKey`,`host`, `port`,`friendName`,`md5Str`) VALUES (\'\(newFriendData.nickname!)\', \'\(newFriendData.friend_uuid!)\', \'\(newFriendData.my_chat_publickey!)\', \'\(newFriendData.my_chat_privatekey!)\', \'\(newFriendData.friend_certifyPublicKey!)\', \'\(newFriendData.host!)\', \'\(newFriendData.port!)\', \'\(newFriendData.friendName!)\', \'\(newFriendData.md5Str!)\')"
            //            print(addFriendSql)
            guard db_query(sql: addFriendSql) else {
                print("插入失败")
                return FriendData()
            }
            print("插入成功")
            
        }else{
            print("存在好友记录")
            //取出信息
            for i in 0..<result!.count {
                let friendInfos:[String : AnyObject] = result![i]
                for friendInfo in friendInfos {
                    switch(friendInfo.key){
                    case "id":
                        let idInt:Int = (friendInfo.value as! NSString).integerValue
                        newFriendData.id = idInt
                    case "friend_chat_publickey":
                        newFriendData.friend_chat_publickey = (friendInfo.value as! String)
                    case "nickname":
                        newFriendData.nickname = (friendInfo.value as! String)
                    case "my_uuid":
                        newFriendData.my_uuid = (friendInfo.value as! String)
                    case "friend_uuid":
                        newFriendData.friend_uuid = (friendInfo.value as! String)
                    case "my_chat_publickey":
                        newFriendData.my_chat_publickey = (friendInfo.value as! String)
                    case "my_chat_privatekey":
                        newFriendData.my_chat_privatekey = (friendInfo.value as! String)
                    case "friend_certifyPublicKey":
                        newFriendData.friend_certifyPublicKey = (friendInfo.value as! String)
                    case "host":
                        newFriendData.host = (friendInfo.value as! String)
                    case "port":
                        newFriendData.port = (friendInfo.value as! String)
                    case "friendName":
                        newFriendData.friendName = (friendInfo.value as! String)
                    case "md5Str":
                        newFriendData.md5Str = (friendInfo.value as! String)
                    case "active":
                        print(friendInfo.value)
                        //                        newFriendData.active = (friendInfo.value as! Bool)
                    default:
                        break
                    }
                }
                
            }
            
            
        }
        
        return newFriendData
        
        //如果存在，提取出关键信息
        
        
    }
    
    func checkAgreeFriend(md5Str:String){
        let sql = "SELECT * FROM `FriendList` WHERE `md5Str` = \'\(md5Str)\'"
        let result:[[String : AnyObject]]? = queryData(querySQL: sql)
        if(result?.count == nil || result?.count == 0){
           
        }else{
            for i in 0..<result!.count {
                let friends:[String : AnyObject] = result![i]
                for friend in friends {
                    switch(friend.key){
                    case "friend_uuid":
                        print("friend_uuid:\(friend.value as! String)")
                        if((friend.value as! String == "")){
                            print("friend UUID = NULL")
                            //生成密钥对
                            let keyPair = RSAHelper().creatKeyPair(length: 2048)
                            let my_chat_publickey = keyPair.getPublicKey()
                            let my_chat_privatekey = keyPair.getPrivateKey()
                            //生成uuid
                            let friend_uuid = UUID().uuidString
                            let add_friend_uuid = "UPDATE `FriendList` SET `friend_uuid` = \'\(friend_uuid)\',`my_chat_privatekey` = \'\(my_chat_privatekey)\' ,`my_chat_publickey` = \'\(my_chat_publickey)\',`active` = true WHERE `md5Str` = \'\(md5Str)\'"
                            guard db_query(sql: add_friend_uuid) else {
                                print("checkAgreeFriend失败")
                                return
                            }
                        }
                        
                    default:
                        break
                    }
                }
            }
            
        }
        
        
    }
    
    func getAgreeFromFriend(friend_uuid:String,my_uuid:String,friend_chat_publickey:String){
        let sql = "UPDATE `FriendList` SET `my_uuid` = \'\(my_uuid)\',`friend_chat_publickey` = \'\(friend_chat_publickey)\',`active` = true WHERE `friend_uuid` = \'\(friend_uuid)\'"
        
        guard db_query(sql: sql) else {
            print("getAgreeFromFriend更新数据库失败")
            return
        }
        print("getAgreeFromFriend更新数据库成功")
    }
    
    func getFriendUUID(md5Str:String) -> String{
        let sql = "SELECT * FROM `FriendList` WHERE `md5Str` = \'\(md5Str)\'"
        let result:[[String : AnyObject]]? = queryData(querySQL: sql)
        if(result?.count == nil || result?.count == 0){
        }else{
            for i in 0..<result!.count {
                let friends:[String : AnyObject] = result![i]
                for friend in friends {
                    switch(friend.key){
                    case "friend_uuid":
                        return friend.value as! String
                    default:
                        break
                    }
                }
            }
            
        }
        return "getFriendUUID = none"
    }
    func getFriendChatPrivateKey(md5Str:String) -> String{
        let sql = "SELECT * FROM `FriendList` WHERE `md5Str` = \'\(md5Str)\'"
        let result:[[String : AnyObject]]? = queryData(querySQL: sql)
        if(result?.count == nil || result?.count == 0){
        }else{
            for i in 0..<result!.count {
                let friends:[String : AnyObject] = result![i]
                for friend in friends {
                    switch(friend.key){
                    case "my_chat_privatekey":
                        return friend.value as! String
                    default:
                        break
                    }
                }
            }
            
        }
        return "getFriendChatPublicKey = none"
    }
    func getFriendChatPublicKey(md5Str:String) -> String{
        let sql = "SELECT * FROM `FriendList` WHERE `md5Str` = \'\(md5Str)\'"
        let result:[[String : AnyObject]]? = queryData(querySQL: sql)
        if(result?.count == nil || result?.count == 0){
        }else{
            for i in 0..<result!.count {
                let friends:[String : AnyObject] = result![i]
                for friend in friends {
                    switch(friend.key){
                    case "my_chat_publickey":
                        return friend.value as! String
                    default:
                        break
                    }
                }
            }
            
        }
        return "getFriendChatPublicKey = none"
    }
    
    func getFriendDataBuild(friend_uuid:String) -> String{

        let findFriendSql = "SELECT * FROM `FriendList` WHERE `friend_uuid` = \'\(friend_uuid)\'"
        let result:[[String : AnyObject]]? = queryData(querySQL: findFriendSql)
        if(result?.count == nil || result?.count == 0){
            print("没有好友记录")
            return "none"
        }else{
            print("存在好友记录")
           //FIXME: 处理
           
            for i in 0..<result!.count {
                let friendChatPrivateKeys:[String : AnyObject] = result![i]
                
                for friendChatPrivateKey in friendChatPrivateKeys {
                    switch(friendChatPrivateKey.key){
                    case "my_chat_privatekey":
                         return friendChatPrivateKey.value as! String
                    default:
                        break
                    }
                }
            }

            return "none"
        }
    }
    
    func gotNewFriendRequest(newFriend:FriendData){
        print("gotNewFriendRequest")
        print(newFriend)
        
        let checkNewFriendSql = "SELECT *  FROM `FriendList` WHERE `md5Str` = \'\(newFriend.md5Str!)\'"
        let result:[[String : AnyObject]]? = queryData(querySQL: checkNewFriendSql)
        if(result?.count == nil || result?.count == 0){
            print("没有好友记录")
            
            let addFriendSql = "INSERT INTO `FriendList` (`my_uuid`,`friend_certifyPublicKey`,`friend_chat_publickey`,`host`, `port`,`friendName`,`md5Str`,`active`) VALUES (\'\(newFriend.my_uuid!)\',  \'\(newFriend.friend_certifyPublicKey!)\',\'\(newFriend.friend_chat_publickey!)\', \'\(newFriend.host!)\', \'\(newFriend.port!)\', \'\(newFriend.friendName!)\', \'\(newFriend.md5Str!)\',false)"
            print(addFriendSql)
            
            guard db_query(sql: addFriendSql) else {
                print("插入失败")
                return
            }
            print("插入成功")
            
            
        }else{
            print("已存在好友记录")
        }
        
    }
    
    
    func deleteAllFriend(){
       let sql = "DELETE FROM `FriendList` WHERE 1;";
        guard db_query(sql: sql) else {
            return
        }
    }
   
    func  deleteFriendRecord(md5Str:String){
       let sql = "DELETE FROM `FriendList` WHERE md5Str = `\(md5Str)`"
        guard db_query(sql: sql) else {
            return
        }
    }
    func getChatDataBuild(friend_uuid:String) -> FriendDataBuild{
        let sql = "SELECT * FROM `FriendList` WHERE `friend_uuid` = \'\(friend_uuid)\'"
        let result:[[String : AnyObject]]? = queryData(querySQL: sql)
        var friendDataBuyild:FriendDataBuild = FriendDataBuild(id: 0, friend_chat_publickey: "", nickname: "", my_uuid: "", friend_uuid: "", my_chat_publickey: "", my_chat_privatekey: "", friend_certifyPublicKey: "", host: "", port: "", friendName: "", md5Str: "", active: false)
        if(result?.count != nil && result?.count != 0){
        for i in 0..<result!.count {
            let friendInfos:[String : AnyObject] = result![i]
            for friendInfo in friendInfos {
                switch(friendInfo.key){
                case "id":
                    let idInt:Int = (friendInfo.value as! NSString).integerValue
                    friendDataBuyild.id = idInt
                case "friend_chat_publickey":
                    friendDataBuyild.friend_chat_publickey = (friendInfo.value as! String)
                case "nickname":
                    friendDataBuyild.nickname = (friendInfo.value as! String)
                case "my_uuid":
                    friendDataBuyild.my_uuid = (friendInfo.value as! String)
                case "friend_uuid":
                    friendDataBuyild.friend_uuid = (friendInfo.value as! String)
                case "my_chat_publickey":
                    friendDataBuyild.my_chat_publickey = (friendInfo.value as! String)
                case "my_chat_privatekey":
                    friendDataBuyild.my_chat_privatekey = (friendInfo.value as! String)
                case "friend_certifyPublicKey":
                    friendDataBuyild.friend_certifyPublicKey = (friendInfo.value as! String)
                case "host":
                    friendDataBuyild.host = (friendInfo.value as! String)
                case "port":
                    friendDataBuyild.port = (friendInfo.value as! String)
                case "friendName":
                    friendDataBuyild.friendName = (friendInfo.value as! String)
                case "md5Str":
                    friendDataBuyild.md5Str = (friendInfo.value as! String)
                case "active":
                    print("active:\(friendInfo.value as! String)")
                    if((friendInfo.value as! String) == "1"){
                        friendDataBuyild.active = true
                    }else{
                        friendDataBuyild.active = false
                    }
                default:
                    break
                }
                
            }
        }
        }
        return friendDataBuyild
    }
    
    
    func getAllFriend() -> [FriendDataBuild] {
        let sql = "SELECT * FROM `FriendList`"
        let result:[[String : AnyObject]]? = queryData(querySQL: sql)
       
        var friendList:[FriendDataBuild] = []
        
        for i in 0..<result!.count {
            print("读取数据库次数 + 1")
            let friendInfos:[String : AnyObject] = result![i]
            var newFriendDataBuild = FriendDataBuild(id: 0, friend_chat_publickey: "", nickname: "", my_uuid: "", friend_uuid: "", my_chat_publickey: "", my_chat_privatekey: "", friend_certifyPublicKey: "", host: "", port: "", friendName: "", md5Str: "", active: false)
            for friendInfo in friendInfos {

                switch(friendInfo.key){
                case "id":
                    let idInt:Int = (friendInfo.value as! NSString).integerValue
                    newFriendDataBuild.id = idInt
                case "friend_chat_publickey":
                    newFriendDataBuild.friend_chat_publickey = (friendInfo.value as! String)
                case "nickname":
                    newFriendDataBuild.nickname = (friendInfo.value as! String)
                case "my_uuid":
                    newFriendDataBuild.my_uuid = (friendInfo.value as! String)
                case "friend_uuid":
                    newFriendDataBuild.friend_uuid = (friendInfo.value as! String)
                case "my_chat_publickey":
                    newFriendDataBuild.my_chat_publickey = (friendInfo.value as! String)
                case "my_chat_privatekey":
                    newFriendDataBuild.my_chat_privatekey = (friendInfo.value as! String)
                case "friend_certifyPublicKey":
                    newFriendDataBuild.friend_certifyPublicKey = (friendInfo.value as! String)
                case "host":
                    newFriendDataBuild.host = (friendInfo.value as! String)
                case "port":
                    newFriendDataBuild.port = (friendInfo.value as! String)
                case "friendName":
                    newFriendDataBuild.friendName = (friendInfo.value as! String)
                case "md5Str":
                    newFriendDataBuild.md5Str = (friendInfo.value as! String)
                case "active":
                    print("active:\(friendInfo.value as! String)")
                    if((friendInfo.value as! String) == "1"){
                        newFriendDataBuild.active = true
                    }else{
                        newFriendDataBuild.active = false
                    }
                default:
                    break
                }
                
            }
            friendList.append(newFriendDataBuild)
            
        }
        return friendList
    }
    
    func getAllfriendUUID() -> [ChatListModel]{
        let sql = "SELECT `friend_uuid`,`friendName`,`nickname` FROM `FriendList`"
        let result:[[String : AnyObject]]? = queryData(querySQL: sql)
        var chatListModels:[ChatListModel] = []
        for i in 0..<result!.count {
            let friendInfos:[String : AnyObject] = result![i]
            var chatListModel:ChatListModel = ChatListModel()
            for friendInfo in friendInfos {
                
                switch(friendInfo.key){
                case "friend_uuid":
                    chatListModel.uuid = friendInfo.value as! String
                    break
                case "friendName":
                    chatListModel.name = friendInfo.value as! String
                    break
                case "nickname":
                    chatListModel.nickname = friendInfo.value as! String
                    break
                default:
                    break
                }
            }
            chatListModels.append(chatListModel)
        }
        return chatListModels
    }
    
    private func queryData(querySQL : String) -> [[String : AnyObject]]? {
        // 创建一个语句对象
        var statement : OpaquePointer? = nil
        
        if querySQL.lengthOfBytes(using: String.Encoding.utf8) > 0 {
            let cQuerySQL = (querySQL.cString(using: String.Encoding.utf8))!
            // 进行查询前的准备工作
            //第一个参数：数据库对象，第二个参数：查询语句，第三个参数：查询语句的长度（如果是全部的话就写-1），第四个参数是：句柄（游标对象）
            if sqlite3_prepare_v2(db, cQuerySQL, -1, &statement, nil) == SQLITE_OK {
                var queryDataArr = [[String: AnyObject]]()
                
                while sqlite3_step(statement) == SQLITE_ROW {
                    // 获取解析到的列
                    let columnCount = sqlite3_column_count(statement)
                    // 遍历某行数据
                    var temp = [String : AnyObject]()
                    for i in 0..<columnCount {
                        // 取出i位置列的字段名,作为temp的键key
                        let cKey = sqlite3_column_name(statement, i)
                        let key : String = String(validatingUTF8: cKey!)!
                        //取出i位置存储的值,作为字典的值value
                        let cValue = sqlite3_column_text(statement, i)
                        
                        //出现空值是出错
                        var value:String = ""
                        if(cValue != nil){
                            value = String(cString: cValue!)
                        }
                        
                        temp[key] = value as AnyObject
                    }
                    queryDataArr.append(temp)
                }
                
                
                return queryDataArr
                
            }
        }
        return nil
    }
    //执行Sql语句
    private func db_query(sql:String) -> Bool{
        let result = sqlite3_exec(db, sql,nil,nil,nil)
        if result == SQLITE_OK {
            return true
        }else{
            return false
        }
        
    }
    
}
