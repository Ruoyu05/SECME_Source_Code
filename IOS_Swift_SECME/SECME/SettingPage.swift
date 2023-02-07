//
//  SettingPage.swift
//  SECME
//
//  Created by cmStudent on 2022/11/24.
//

import SwiftUI


struct SettingPage: View {
    @EnvironmentObject var client: WebSocketClient
    var body: some View {
        VStack{

            
            Button{
                DBBuilder(db_name: client.userMd5Str).updateCertifyPublicKey(certifyPublicKey: "none")
                DBBuilder(db_name: client.userMd5Str).updateCertifyPrivateKey(certifyPrivateKey: "none")
            }label:{
                Text("(開発者用)全部の鍵を削除")
                    .frame(width: 200,height: 40,  alignment: .center)
                    .background(Color.red)
                    .foregroundColor(Color.white)
                    .cornerRadius(8)
            }
            .padding(.vertical, 20.0)
            
            Button("Logout") {
                client.disconnect()
            }
            .padding(.vertical, 20.0)
            .frame(width: 200,height: 40,  alignment: .center)
            .background(Color.red)
            .foregroundColor(Color.white)
            .cornerRadius(8)
            
        }
        .ignoresSafeArea()
        .navigationTitle("")
        .navigationBarHidden(false)
    }
}

struct SettingPage_Previews: PreviewProvider {
    static var previews: some View {
        SettingPage()
    }
}
