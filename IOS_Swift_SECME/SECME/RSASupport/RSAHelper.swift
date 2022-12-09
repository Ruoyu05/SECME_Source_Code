//
//  RSAHelper.swift
//  SECME
//
//  Created by cmStudent on 2022/11/24.
//


import SwiftyRSA

class RSAHelper:ObservableObject{
    
    // MARK: - 检测
    //检测公钥
    func testPublicKey(publicKeyStr:String) -> Bool{
        do{
            _ = try PublicKey(pemEncoded:format_key(input: publicKeyStr))
        }catch{
            return false
        }
        return true
        
    }
    
    //检测私钥
    func testPrivateKey(privateKeyStr:String) -> Bool{
        do{
            _ = try PrivateKey(pemEncoded: privateKeyStr)
        }catch{
            return false
        }
        return true
    }
    
    
    // MARK: - 密钥处理
    //创建密钥对
    func creatKeyPair(length:Int) -> RSAKeyPair{
        var rsaKeyPair = RSAKeyPair(public_key: "none", private_key: "none")
        do{
            let keyPair =  try SwiftyRSA.generateRSAKeyPair(sizeInBits: length)
            let privateKey = keyPair.privateKey
            let publicKey = keyPair.publicKey
            let pemPrivateKey = try privateKey.pemString()
            let pemPublicKey = try publicKey.pemString()
            rsaKeyPair.setPublicKey(public_key: format_key(input: pemPublicKey))
            rsaKeyPair.setPrivateKey(private_key: format_key(input: pemPrivateKey))
        }catch{
            print("密钥对创建失败")
        }
        return rsaKeyPair
    }
    
    //密钥整形
    func format_key(input:String) -> String{
        let input1 = input.replacingOccurrences(of: "-----BEGIN RSA PRIVATE KEY-----", with: "")
        let input2 = input1.replacingOccurrences(of: "-----END RSA PRIVATE KEY-----", with: "")
        let input3 = input2.replacingOccurrences(of: "-----BEGIN RSA PUBLIC KEY-----", with: "")
        let input4 = input3.replacingOccurrences(of: "-----END RSA PUBLIC KEY-----", with: "")
        let input5 = input4.replacingOccurrences(of: " ", with: "")
        let input6 = input5.replacingOccurrences(of: "\\", with: "")
        let input7 = input6.replacingOccurrences(of: "\n", with: "")
        let output:String = input7
        return output
    }
    
    
    // MARK: - 加解密
    //用公钥(String)加密
    func encryptByPublicKey(publicKeyStr:String,messageStr:String) -> String {
        do{
            let publicKey = try PublicKey(pemEncoded: format_key(input: publicKeyStr))
            let clear = try ClearMessage(string: messageStr, using: .utf8)
            let encrypted = try clear.encrypted(with: publicKey, padding: .PKCS1)
            let base64String = encrypted.base64String
            return base64String
        }catch{
            return "使用公钥加密失败"
        }
    }
 
    //用私钥(String)解密
    func decryptByPrivateKey(privateKeyStr:String,encryptedBase64String:String) -> String{
        do{
            let privateKey = try PrivateKey(pemEncoded: privateKeyStr)
            let encrypted = try EncryptedMessage(base64Encoded: encryptedBase64String)
            let clear = try encrypted.decrypted(with: privateKey, padding: .PKCS1)
            let string = try clear.string(encoding: .utf8)
            return string
        }catch{
            return "decryptByPrivateKey in error"
        }
    }
    
    //使用私钥签名
    func doSign(privateKeyStr:String,text:String) -> String{
        var base64String = ""
        do{
            let privateKey = try PrivateKey(pemEncoded: privateKeyStr)
            let clear = try ClearMessage(string: text, using: .utf8)
            let signature = try clear.signed(with: privateKey, digestType: .sha1)
            base64String = signature.base64String
            print("签名后内容:\(base64String)")
        }catch{
            print("签名失败")
        }
        return base64String
    }
    func checkSign(publicKeyStr:String,signatureStr:String,message:String)->Bool{
        var result = false
        do{
            let publicKey = try PublicKey(pemEncoded: publicKeyStr)
            let signature = try Signature(base64Encoded: signatureStr)
            let clear = try ClearMessage(string: message, using: .utf8)
            result =  try clear.verify(with: publicKey, signature: signature, digestType: .sha1)
        }catch{
            
        }
       return result
    }
    
}



// MARK: - 密钥结构支持

struct RSAKeyPair:Codable {
    var public_key:String
    var private_key:String
    
    mutating func setPublicKey(public_key:String){
        self.public_key = public_key
    }
    mutating func setPrivateKey(private_key:String){
        self.private_key = private_key
    }
    func getPublicKey()->String{
        return public_key
    }
    func getPrivateKey()->String{
        return private_key
    }
    
}
