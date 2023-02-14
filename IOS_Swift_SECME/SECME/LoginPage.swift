//
//  LoginPage.swift
//  SECME
//
//  Created by cmStudent on 2022/11/18.
//

import SwiftUI


struct LoginPage: View {
    @EnvironmentObject var client: WebSocketClient
    
    //    DispatchQueue.main.
    private let diviceWidth = UIScreen.main.bounds.width
    private  let diviceHeight = UIScreen.main.bounds.height
    
    @State var username:String = "Asuka"
    @State var password:String = "asuka"
    
    //    @AppStorage("server_host") var host:String = "localhost"//数据初期化
    //    @AppStorage("server_host") var host:String = "172.20.10.13"//数据初期化
    
    //        @AppStorage("server_host") var host:String = "192.168.100.31"//数据初期化
            @AppStorage("server_host") var host:String = "47.74.1.184"//数据初期化
        @AppStorage("server_port") var portStr:String = "10086"//数据初期化
//    @AppStorage("server_host") var host:String = ""//数据初期化
//    @AppStorage("server_port") var portStr:String = ""//数据初期化
    
    
    var body: some View {
        ZStack{
            VStack{
                
                Spacer()
                HStack{
                    Text("コネクション:")
                        .padding(.bottom, 4.0)
                        .frame(height: 22, alignment: .leading )
                    Spacer()
                }
                .frame(width: diviceWidth * 0.7, alignment: .center)
                .onAppear{
                    print("onAppear")
                    
                    if(client.isAuthed){
                        print("client.isAuthed")
                    }else{
                        client.disconnect()
                    }
                    
                }
                .onChange(of: client.isConnected, perform: { value in
                    if(value){
                        if(client.username != ""){
                            let contents = Contents(username: username, password: password)
                            let jsonMessage = JsonMessage(type: "LoginRequest", contents: contents)
                            let jsonMessageStr =  JsonHelper().buildJsonMessageStr(input: jsonMessage)
                            let encryptedMessageStr = RSAHelper().encryptByPublicKey(publicKeyStr: client.publickey_from_server, messageStr: jsonMessageStr)
                            if(username.count != 0){
                                client.send(inputData: TypeChangeHepler().strToUTF8(string: encryptedMessageStr))
                            }
                        }
                    }
                })
                
                VStack{
                    VStack{
                        HStack{
                            Image(systemName: "server.rack")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 22,height: 22,alignment: .center)
                            HStack{
                                TextField("Host", text: $host)
                                    .frame(width: diviceWidth * 0.35,height: 0.5,  alignment: .center)
                                    .foregroundColor(Color("Font_Color_Black"))
                                
                                VStack{}
                                    .frame(width: 1,height: 20,  alignment: .center)
                                    .background(Color.gray)
                                    .cornerRadius(1)
                                TextField("Port", text: $portStr)
                                    .foregroundColor(Color("Font_Color_Black"))
                            }
                            
                        }
                        .frame(height: 30,  alignment: .center)
                        
                        VStack{}
                            .frame(width: diviceWidth * 0.7,height: 0.5,  alignment: .center)
                            .background(Color.gray)
                            .cornerRadius(1)
                        
                        
                        HStack{
                            Image(systemName: "person.crop.circle")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 22,height: 22,alignment: .center)
                            TextField("Username", text: $username)
                                .foregroundColor(Color("Font_Color_Black"))
                        }.frame(height: 30,  alignment: .center)
                        
                        VStack{}
                            .frame(width: diviceWidth * 0.7,height: 0.5,  alignment: .center)
                            .background(Color.gray)
                            .cornerRadius(1)
                        HStack{
                            Image(systemName: "key.fill")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 22,height: 22,alignment: .center)
                            SecureField(/*@START_MENU_TOKEN@*//*@PLACEHOLDER=Label@*/"Password"/*@END_MENU_TOKEN@*/, text: $password)
                                .foregroundColor(Color("Font_Color_Black"))
                        }.frame(height: 30,  alignment: .center)
                    }
                    .frame(width: diviceWidth * 0.7,height: 135,  alignment: .center)
                    
                }
                .padding(.vertical, 8.0)
                .frame(width: diviceWidth * 0.8,height: 135,  alignment: .center)
                .background(Color("Form_Background_Color"))
                .cornerRadius(8)
                
                Text(client.errorMessage)
                    .frame(height: 20,alignment: .center)
                    .foregroundColor(Color.red)
                
                Button{
                    if(username == ""){
                        client.errorMessage = "ユーザー名を入力してください!"
                    }else if(password == ""){
                        client.errorMessage = "パスワードを入力してください!"
                    }else{
                        client.isOnConnecting = true
                        client.setup(host: host, portStr: portStr, username: username, password: password)
                        client.connect()
                    }
                }label: {
                    Text("ログイン")
                        .frame(width: diviceWidth * 0.8,height: 40,  alignment: .center)
                        .background(Color.blue)
                        .foregroundColor(Color.white)
                        .cornerRadius(8)
                }
                .disabled(client.isOnConnecting)
                
                NavigationLink(destination: RegisterPage()) {
                    Text("新規登録")
                        .frame(width: diviceWidth * 0.8,height: 40,  alignment: .center)
                        .background(Color.blue)
                        .foregroundColor(Color.white)
                        .cornerRadius(8)
                }
                .disabled(client.isOnConnecting)
                
                Text("パスワードを忘れた")
                
                Spacer()
                
                Button{
                    
                }label: {
                    Text("(仮)オフラインで使用")
                        .frame(width: diviceWidth * 0.8,height: 40,  alignment: .center)
                        .background(Color.green)
                        .foregroundColor(Color.white)
                        .cornerRadius(8)
                }
                .disabled(client.isOnConnecting)
                
                Spacer()
                
                
            }
            .frame(width: diviceWidth * 0.9,height:450)
            .background(Color("Form_Table_Color"))
            .cornerRadius(8)
            
            if(client.isOnConnecting || client.isConnected){
                VStack{
                    ZStack{
                        VStack{
                            ProgressView()
                                .padding(.vertical, 20.0)
                            Text("ログイン中...")
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
        .frame(width: diviceWidth, height: diviceHeight, alignment: .center)
        .ignoresSafeArea()
        .navigationBarHidden(true)
        
    }
    
    
    
}

struct LoginPage_Previews: PreviewProvider {
    static var previews: some View {
        LoginPage()
    }
}
