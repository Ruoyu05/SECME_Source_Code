//
//  ContentView.swift
//  SECME
//
//  Created by cmStudent on 2022/11/18.
//

//Security      安全的
//Engeering     エンジニアリング
//Comminication 交流
//Math          数学
//Encryption    暗号化

//Secure Message

import SwiftUI


struct ContentView: View {
    @EnvironmentObject var client: WebSocketClient
    
    var body: some View {
        
        NavigationView{

            VStack{
                
                if(client.isAuthed){
                    HomePage()
                }else{
                    LoginPage()
                }

            }
            .ignoresSafeArea()
            .navigationBarHidden(true)

            
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
