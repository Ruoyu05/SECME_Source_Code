//
//  Md5Helper.swift
//  SECME
//
//  Created by cmStudent on 2022/11/25.
//

//MD5支持

import Foundation
import CommonCrypto


extension String {
    var md5:String {
        let utf8 = cString(using: .utf8)
        var digest = [UInt8](repeating: 0, count: Int(CC_MD5_DIGEST_LENGTH))
        CC_MD5(utf8,CC_LONG(utf8!.count - 1),&digest)
        return digest.reduce(""){ $0 + String(format: "%02X", $1)}
    }
}

//返回String
func getMD5(data:String) -> String{
    //MD5加密
    return data.md5
}

