//
//  HomePage.swift
//  SECME
//
//  Created by cmStudent on 2022/11/18.
//

import SwiftUI


struct HomePage: View {
    @EnvironmentObject var client: WebSocketClient
    
    var body: some View {
        ZStack{
            TabView {
                ChatListPage()
                    .tabItem {
                        Image(systemName: "gear.circle.fill")
                        Text("チャット")
                    }
                FriendsListPage()
                    .tabItem {
                        Image(systemName: "gear.circle.fill")
                        Text("フレンド")
                    }
                CertifyKeyQRCodePage()
                    .tabItem {
                        Image(systemName: "gear.circle.fill")
                        Text("鍵の管理")
                    }
                SettingPage()
                    .tabItem {
                        Image(systemName: "gear.circle.fill")
                        Text("設定")
                    }
            }
            .onAppear{
                
                if(client.needActive){
                    //检测本地密钥库并通知服务器
                   doCheckKeys()
                }
                
            }
            
            if(client.needActive){
                ActiveDivicePage()
            }
            
        }
        .ignoresSafeArea()
        .navigationBarHidden(true)
        
    }
    private func doCheckKeys(){
        var jsonStr = ""
        let isNoData = DBBuilder(db_name: client.userMd5Str).certifyStorageHaveNoData()
//        print("数据库不存在数据:\(isNoData)")
//        let keyPair = DBBuilder(db_name: client.userMd5Str).getCertifyKeyPair()
        //验证加密
        if(!isNoData && (RSAHelper().decryptByPrivateKey(privateKeyStr: DBBuilder(db_name: client.userMd5Str).getCertifyKeyPair().certifyPrivateKey, encryptedBase64String: RSAHelper().encryptByPublicKey(publicKeyStr: DBBuilder(db_name: client.userMd5Str).getCertifyKeyPair().certifyPublicKey, messageStr: "Text")) == "Text")){
            print("本地数据库查询 存在密钥 且 密钥成对")
            //告诉服务器数据库中的密钥
            let jsonMessage = JsonMessage(type: "CertifyKeyCheck", contents: Contents(username: client.username, certifyPublicKey: DBBuilder(db_name: client.userMd5Str).getCertifyKeyPair().certifyPublicKey))
            jsonStr = JsonHelper().buildJsonMessageStr(input: jsonMessage)
        }else{
            print("本地数据库不存在密钥 或 密钥不成对2")
            //告诉服务器没有密钥
            let jsonMessage = JsonMessage(type: "CertifyKeyCheck", contents: Contents(username: client.username, certifyPublicKey: "none"))
            jsonStr = JsonHelper().buildJsonMessageStr(input: jsonMessage)
            
        }

        client.send(inputData:TypeChangeHepler().strToUTF8(string: RSAHelper().encryptByPublicKey(publicKeyStr: client.publickey_from_server, messageStr: jsonStr)))
        
    }
}

struct HomePage_Previews: PreviewProvider {
    static var previews: some View {
        HomePage()
    }
}
