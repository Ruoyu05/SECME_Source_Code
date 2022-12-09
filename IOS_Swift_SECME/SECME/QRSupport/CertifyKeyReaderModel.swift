//
//  CertifyKeyModel.swift
//  SECME
//
//  Created by cmStudent on 2022/11/28.
//


import SwiftUI
import Combine


class CertifyKeyReaderModel: ObservableObject {
    
    @Published var db_name:String = ""
    @Published var qrCodeImg:UIImage!
    @Published var scannerMessage:String
    @Published var result:String
    
    var cancelable: Set<AnyCancellable> = .init()
    
    init(){
    
        qrCodeImg =  UIImage(named: "QRCodeImg")!
        scannerMessage = ""
        result = ""
        $scannerMessage
            .sink{ [self] value in
                if(value != ""){
                    result = ""
//                    print("scannerMessage -> \(value)")
                    checkKeys(inputKeyStr: value)
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
                        scannerMessage = feature.messageString ?? ""
//                        print(feature.messageString ?? "")
                    }
                }else{
                    result = "图片中存在多个二维码"
                }

        }
            .store(in: &cancelable)
    }
    
    func setDB_Name(db_name:String){
        self.db_name = db_name
    }
    
    func checkKeys(inputKeyStr:String){
       
        let isPublicKey = RSAHelper().testPublicKey(publicKeyStr: inputKeyStr)
//        print("is PublicKey:\(isPublicKey)")
        
        
        let isPrivateKey = RSAHelper().testPrivateKey(privateKeyStr: inputKeyStr)
//        print("is PraviteKey:\(isPrivateKey)")
        
        if(isPublicKey){
            result = "is PublicKey"
            DBBuilder(db_name: db_name).updateCertifyPublicKey(certifyPublicKey: inputKeyStr)
        }else if(isPrivateKey){
            result = "is PrivateKey"
            DBBuilder(db_name: db_name).updateCertifyPrivateKey(certifyPrivateKey: inputKeyStr)
           
        }else {
            result = "not a key"
        }
        
    }
    
}
