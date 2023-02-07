//
//  JsonHelper.swift
//  SECME
//
//  Created by cmStudent on 2022/11/24.
//

import Foundation

class JsonHelper:ObservableObject{
    
//    func test() throws {}
    //检测获取到服务器公钥
    func isJson_ServerPublicKey(inputData:Data) -> Bool{
        do{
            let jsonMessage = try JSONDecoder().decode(JsonMessage.self, from: inputData)
            if(jsonMessage.type == "ServerPublicKey"){
                return true
            }
            return false
            
        }catch{
            return false
        }
    }
    
    func isJson_GotClientPublicKey(inputData:Data) -> Bool{
        do{
            let jsonMessage = try JSONDecoder().decode(JsonMessage.self, from: inputData)
            if(jsonMessage.type == "GotClientPublicKey"){
                return true
            }else{
                return false
            }
            
        }catch{
            
        }
        return false
    }
    
    //提取Json中的服务器公钥
    func getServerPublicKey(inputData:Data) -> String{
        do{
            let jsonMessage = try JSONDecoder().decode(JsonMessage.self, from: inputData)
            return RSAHelper().format_key(input: jsonMessage.contents.serverPublickey!)
        }catch{
            return "Get Server PublicKey Fail"
        }
    }
    
    //提取Json中的GotClientPublicKey结果
    func getGotClientPublicKeyResult(inputData:Data) -> String{
        do{
            let jsonMessage = try JSONDecoder().decode(JsonMessage.self, from: inputData)
            return jsonMessage.contents.reasion!
        }catch{
            return ""
        }
    }
    
    //编辑密钥发送报文
    func buildJsonMessageStr(input:JsonMessage) -> String{
        let JsonData = try! JSONEncoder().encode(input)
        return String(bytes: JsonData, encoding: .utf8)!
    }
    
    //编辑密钥发送报文
    func buildJsonMessageData(input:JsonMessage) -> Data{
        return try! JSONEncoder().encode(input)
        
    }
    
    func isQRInfo(inputStr:String) -> Bool{
        let inputData = TypeChangeHepler().strToUTF8(string: inputStr)
        do{
            let _ = try JSONDecoder().decode(QRInfo.self, from: inputData)
        }catch{
            return false
        }
        return true
        
    }
    

    
}
