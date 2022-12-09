//
//  ChatDBBuilder.swift
//  SECME
//
//  Created by cmStudent on 2022/12/06.
//

import Foundation
import SQLite3


struct ChatDBBuilder{
    //定义数据库
    let db_file_name:String
    let db_Path:String
    var db:OpaquePointer?
    var withFriendUUID:String
    
    init(db_name:String,withFriendUUID:String){
        self.withFriendUUID = withFriendUUID
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
        createChatHistoryStorage(withFriendUUID: withFriendUUID)
    }
    
    //创建聊天数据库
    private func createChatHistoryStorage(withFriendUUID:String){
        //新建聊天数据库
        let sql = "CREATE TABLE IF NOT EXISTS `\(withFriendUUID)` (id INTEGER PRIMARY KEY AUTOINCREMENT, message TEXT, is_in TINYINT(1),in_time DATETIME,alreadyRead TINYINT(1))"
        guard db_query(sql: sql) else { return }
    }
    
    func sendMessage(message:String,in_time:Date){
        let sql = "INSERT INTO `\(withFriendUUID)` (`message`,`in_time`,`is_in`,`alreadyRead`) VALUES (\'\(message)\',\'\(in_time)\', \'false\',\'true\')"
        guard db_query(sql: sql) else { return }
    }
    
    func gotMessage(message:String,in_time:Date){
        let sql = "INSERT INTO `\(withFriendUUID)` (`message`,`in_time`,`is_in`,`alreadyRead`) VALUES (\'\(message)\',\'\(in_time)\', \'true\',\'false\')"
        guard db_query(sql: sql) else { return }
    }
    
    func getMessages() -> [MessagesView]{
        var messagesResult:[MessagesView] = []
        let sql = "SELECT * FROM `\(withFriendUUID)`"
        let result:[[String : AnyObject]]? = queryData(querySQL: sql)
        for i in 0..<result!.count {
            let messages:[String : AnyObject] = result![i]
            var messageMod:MessagesView = MessagesView(contentMessage: "", isCurrentUser: false)
            for message in messages {
                switch(message.key){
                case "message":
                    messageMod.contentMessage = message.value as! String
                    break
                case "is_in":
                    if((message.value as! String) == "true"){
                        messageMod.isCurrentUser = false
                    }else if((message.value as! String) == "false"){
                        messageMod.isCurrentUser = true
                    }
                    break
                case "in_time":
                    break
                case "alreadyRead":
                    break
                default:
                    break
                }
                
            }
            messagesResult.append(messageMod)
        }
        //设置所有消息已读
        setAllMessageBeRead()
        return messagesResult
    }
    
    func getLastMessage() -> String{
        let sql = "SELECT message FROM `\(withFriendUUID)` ORDER BY in_time DESC LIMIT 1";
        let result:[[String : AnyObject]]? = queryData(querySQL: sql)
        for i in 0..<result!.count {
            let lastMessages:[String : AnyObject] = result![i]
            for lastMessage in lastMessages {
                switch(lastMessage.key){
                case "message":
                    return lastMessage.value as! String
                default:
                    break
                }
            }
        }
        return ""
    }
    func getCount_NeedRead() -> Int{
        let sql = "SELECT * FROM `\(withFriendUUID)` WHERE `alreadyRead` = \'false\'"
        let result:[[String : AnyObject]]? = queryData(querySQL: sql)
        return result?.count == nil ? 0 : result!.count
    }
    
    func setAllMessageBeRead(){
        let sql = "UPDATE `\(withFriendUUID)` SET `alreadyRead`= true WHERE 1"
        guard db_query(sql: sql) else { return }
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
