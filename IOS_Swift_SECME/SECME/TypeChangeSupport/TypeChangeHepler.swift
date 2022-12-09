//
//  TypeChange.swift
//  SECME
//
//  Created by cmStudent on 2022/11/24.
//

import Foundation

class TypeChangeHepler:ObservableObject{
    // MARK: - 转换为Data
    
    //Base64String -> utf8Data
    func base64StrToUTF8Data(base64Str:String) -> Data?{
        return base64Str.data(using: String.Encoding.utf8)
    }
    
    //String -> utf8Data
    func strToUTF8(string:String) -> Data{
        return string.data(using: String.Encoding.utf8)!
    }
    
    
    
}
