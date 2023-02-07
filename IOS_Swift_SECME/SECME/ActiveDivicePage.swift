//
//  ActiveDivicePage.swift
//  SECME
//
//  Created by cmStudent on 2022/11/25.
//

import Foundation
import SwiftUI
import UIKit


struct ActiveDivicePage: View {
    
    @EnvironmentObject var client: WebSocketClient
    
    private let diviceWidth = UIScreen.main.bounds.width
    private  let diviceHeight = UIScreen.main.bounds.height
    
    @State var isReadingQR:Bool = false
    
    @State var isShowPhotolibrary = false
    @State var isShowCamera = false
    @State var isShowActionSheet = false
    
    @State var imageQR:UIImage = UIImage(named: "QRCodeImg")!
    @State var scannerMessage:String = "none"
    
    @StateObject var certifyKeyReaderModel = CertifyKeyReaderModel()
    
    
    var body: some View {
        
        ZStack{
            VStack{}
                .frame(width: diviceWidth, height: diviceHeight, alignment: .center)
                .background(Color.black)
                .opacity(0.4)
                .onAppear{
                    certifyKeyReaderModel.setDB_Name(db_name: (client.userMd5Str))
                    print(certifyKeyReaderModel.db_name)
                }
            VStack{
                if(!client.needCopyCertifyKey && !client.needCreateCertifyKey){
                    VStack{
                        VStack{
                            ZStack{
                                VStack{
                                    ProgressView()
                                        .padding(.vertical, 20.0)
                                    Text("認証中...")
                                }
                                VStack{
                                    Spacer()
                                    Button("キャンセル") {
                                        client.isOnConnecting = false
                                        client.disconnect()
                                    }
                                    .padding(.bottom, 20.0)
                                }
                            }
                            .frame(width: 200, height: 200, alignment: .center)
                            .background(Color("Waitting_Color"))
                            .cornerRadius(16)
                        }
                    }
                }
                
                if(client.needCreateCertifyKey){
                    Button{
                        client.needCreateCertifyKey = false
                        client.needCopyCertifyKey = false
                        
                        let keyPair = RSAHelper().creatKeyPair(length: 1024)
                        
                        DBBuilder(db_name: client.userMd5Str).updateCertifyKeyPair(certifyPublicKey: keyPair.getPublicKey(), certifyPrivateKey: keyPair.getPrivateKey())
                        
                        let jsonMessage = JsonMessage(type: "UpdateCertifyPublicKey", contents: Contents(username: client.username, password: client.password, certifyPublicKey: keyPair.getPublicKey()))
                        let jsonMessageStr =  JsonHelper().buildJsonMessageStr(input: jsonMessage)
                        let encryptedMessageStr = RSAHelper().encryptByPublicKey(publicKeyStr: client.publickey_from_server, messageStr: jsonMessageStr)
                        
                        client.send(inputData: TypeChangeHepler().strToUTF8(string: encryptedMessageStr))
                        
                    }label: {
                        VStack{
                            ZStack{
                                Image(systemName: "plus.circle.fill")
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .frame(width: 40,height: 40,alignment: .center)
                                    .padding([.top, .leading], 85.0)
                                
                                
                                Image(systemName: "key")
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .padding(.trailing, 20.0)
                                    .frame(width: 200,height: 150,alignment: .center)
                                
                            }
                            .padding([.top, .leading, .trailing] , 20)
                            .foregroundColor(Color.black)
                            Text("鍵を作成")
                                .foregroundColor(Color.blue)
                                .padding(.all, 20.0)
                            
                        }
                        .background(Color("Waitting_Color"))
                        
                    }
                    .disabled(!client.needCreateCertifyKey)
                    .cornerRadius(8)
                    
                    
                }
                
                if(client.needCopyCertifyKey){
                    VStack{
                        

                        Button{
                            isShowActionSheet = true
                        }label: {
                            
                            VStack{
                                ZStack{
                                    
                                    Image(systemName: "key.viewfinder")
                                        .resizable()
                                        .aspectRatio(contentMode: .fit)
                                        .padding(.top, 20.0)
                                        .frame(width: 200,height: 150,alignment: .center)
                                    
                                }
                                .padding([.top, .leading, .trailing] , 20)
                                .foregroundColor(Color.black)
                                Text("認証した端末から鍵を読み込む")
                                    .foregroundColor(Color.blue)
                                    .padding(.all, 20.0)
                                
                            }
                            .background(Color("Waitting_Color"))
                            
                        }
                        .disabled(!client.needCopyCertifyKey)
                        .sheet(isPresented: $isShowCamera) {
                            
                            ScannerView(isReadingQR: $isShowCamera, pasteboard: $certifyKeyReaderModel.scannerMessage)
                            
                        }
                        .sheet(isPresented: $isShowPhotolibrary) {
                            PhotolibraryickerView(isShowPhotolibrary: $isShowPhotolibrary, qrCodeImage: $certifyKeyReaderModel.qrCodeImg, qrCodeMessage: $certifyKeyReaderModel.scannerMessage)
                        }
                        .actionSheet(isPresented:$isShowActionSheet){
                            ActionSheet(title: Text("QRコードから読み込む"),
                                        message: Text("読み込む方法を選んでください:"),
                                        buttons: [
                                            
                                            .default(Text("カメラ"),action: {
                                                
                                                if UIImagePickerController.isSourceTypeAvailable(.camera){
                                                    print("カメラは利用できます。")
                                                    isShowCamera = true
                                                    
                                                }else{
                                                    print("カメラを利用できません")
                                                }
                                            }),
                                            .default(Text("フォトラブトリ"),action:{
                                                isShowPhotolibrary = true
                                            }),
                                            .cancel(Text("キャンセル")){
                                                print("点击了取消")
                                            },
                                            
                                        ])
                        }
                        .onChange(of: certifyKeyReaderModel.scannerMessage, perform: { _ in
                            //检测本地密钥库并通知服务器
                            print("重新检测本地密钥")
                            client.needCopyCertifyKey = false
                            doCheckKeys()
                        })
                        .cornerRadius(8)
                    }
                }
                
                if(client.needCopyCertifyKey || client.needCreateCertifyKey){
                    Button{
                        client.disconnect()
                    }label: {
                        Text("キャンセル")
                            .frame(width: 240, height: 40, alignment: .center)
                            .foregroundColor(Color.white)
                            .background(Color.red)
                            .cornerRadius(8)
                    }
                    .padding(.top, 10.0)
                }
                
                
            }
            
        }
        
        
    }
    
    private func doCheckKeys(){
        var jsonStr = ""
        let isNoData = DBBuilder(db_name: client.userMd5Str).certifyStorageHaveNoData()
        //        print("数据库不存在数据:\(isNoData)")
        if(!isNoData && (RSAHelper().decryptByPrivateKey(privateKeyStr: DBBuilder(db_name: client.userMd5Str).getCertifyKeyPair().certifyPrivateKey, encryptedBase64String: RSAHelper().encryptByPublicKey(publicKeyStr: DBBuilder(db_name: client.userMd5Str).getCertifyKeyPair().certifyPublicKey, messageStr: "Text")) == "Text")){
            print("本地数据库查询 存在密钥 且 密钥成对")
            //告诉服务器数据库中的密钥
            let jsonMessage = JsonMessage(type: "CertifyKeyCheck", contents: Contents(username: client.username, certifyPublicKey: DBBuilder(db_name: client.userMd5Str).getCertifyKeyPair().certifyPublicKey))
            jsonStr = JsonHelper().buildJsonMessageStr(input: jsonMessage)
        }else{
            print("本地数据库不存在密钥 或 密钥不成对")
            //告诉服务器没有密钥
            let jsonMessage = JsonMessage(type: "CertifyKeyCheck", contents: Contents(username: client.username, certifyPublicKey: "none"))
            jsonStr = JsonHelper().buildJsonMessageStr(input: jsonMessage)
        }
        client.send(inputData:TypeChangeHepler().strToUTF8(string: RSAHelper().encryptByPublicKey(publicKeyStr: client.publickey_from_server, messageStr: jsonStr)))
    }
    
}


struct ActiveDivicePage_Previews: PreviewProvider {
    static var previews: some View {
        ActiveDivicePage()
    }
}
