//
//  QRMessageModel.swift
//  SecureMessage
//
//  Created by cmStudent on 2022/11/08.
//

import SwiftUI
import Combine


class FriendQRInfo: ObservableObject {
    @Published var qrCodeImg:UIImage!
    @Published var message:String
    @Published var username:String
    @Published var host:String
    @Published var port:String
    @Published var certifyPublicKey:String
    @Published var result:String
    @Published var isRSAPublicKey:Bool
    
    @Published var md5Str:String
    
    var cancelable: Set<AnyCancellable> = .init()
    init(){
        qrCodeImg =  UIImage(named: "QRCodeImg")!
        message = ""
        username = ""
        host = ""
        port = ""
        certifyPublicKey = ""
        result = ""
        md5Str = ""
        isRSAPublicKey = false
        
        $message
            .sink{ [self] value in
                print("QRInfo->\(value)")
                result = ""
                if(JsonHelper().isQRInfo(inputStr: value)){
                    print("读取到了添加好友用QR")
                    readNewFriend(str: value)
                   
                }else{
                    print("未识别添加好友用QR")
                }
            }
            .store(in: &cancelable)
        
        $qrCodeImg
            .sink{ [self] value in
                result = ""

                let ciImage:CIImage = CIImage(image:value!)!
                let context = CIContext(options: nil)
                let detector = CIDetector(ofType: CIDetectorTypeQRCode, context: context,
                                          options: [CIDetectorAccuracy:CIDetectorAccuracyHigh])
                let features = detector?.features(in: ciImage)
                print("扫描到二维码个数：\(features?.count ?? 0)")
                let countQR = features?.count ?? 0
                if(countQR == 0){
                    result = "未识别到二维码"
                }else if(countQR == 1){
                    //遍历所有的二维码，并框出
                    for feature in features as! [CIQRCodeFeature] {
                        message = feature.messageString ?? ""
                        print(feature.messageString ?? "")
                    }
                }else{
                    result = "图片中存在多个二维码"
                }
        }
            .store(in: &cancelable)
    }

    func readNewFriend(str:String){
        result = ""
        let inputData = TypeChangeHepler().strToUTF8(string: str)
        do{
            let json = try JSONDecoder().decode(QRInfo.self, from: inputData)
            username = json.user
            host = json.host
            port = json.port
            certifyPublicKey = json.certifyKey
            isRSAPublicKey = RSAHelper().testPublicKey(publicKeyStr: certifyPublicKey)
            print("isRSAPublicKey = \(isRSAPublicKey)")
            md5Str = getMD5(data: host + port + username + certifyPublicKey)
            
        }catch{
            result = "读取失败"
        }
        
    }
}
