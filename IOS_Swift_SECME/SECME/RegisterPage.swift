//
//  RegisterPage.swift
//  SECME
//
//  Created by cmStudent on 2022/11/18.
//

import SwiftUI


struct RegisterPage: View {
    @EnvironmentObject var client: WebSocketClient
    
    private let diviceWidth = UIScreen.main.bounds.width
    private  let diviceHeight = UIScreen.main.bounds.height
    
    @State var handler : Timer?
    
    @State var host:String = "47.74.1.184"
//    @State var host:String = "172.20.10.13"
//    @State var host:String = "192.168.100.24"
    @State var portStr:String = "10086"
    
    @State var notice:String = ""
    
    @State var username:String = ""
    @State var password:String = ""
    @State var repeatPassword:String = ""
    
    //编辑框高度
    @State var tableHeight:CGFloat = 0.0
    @State var isDabledButton:Bool = false
    
    
    var body: some View {
        VStack{
            if(client.isRegisterSuccessful){
                Spacer()
                RegisterSuccessView()
                Spacer()
            }else{
                VStack{
                    Spacer()
                    VStack{
                        Text(client.isConnected ? "新規" : "サーバー設定")
                            .font(.body)
                            .fontWeight(.heavy)
                            .padding(.top, 20.0)
                            .onChange(of: client.isConnected, perform: { newState in
                                start(newState: newState)
                            })
                            .onAppear{
                                //状态重置
                                print("状态重置")
                                client.recoverState()
                                
                            }
                        
                        
                        ZStack{
                            InputTable()
                            HStack{
                                Spacer()
                                Image(systemName: "server.rack")
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .frame(width: 22,height: 22,alignment: .center)
                                
                                TextField("Host", text: $host)
                                    .frame(width: diviceWidth * 0.35,height: 0.5,  alignment: .center)
                                    .foregroundColor(Color("Font_Color_Black"))
                                    .disabled(client.isConnected)
                                
                                VStack{}
                                    .frame(width: 1,height: 20,  alignment: .center)
                                    .background(Color.gray)
                                    .cornerRadius(1)
                                TextField("Port", text: $portStr)
                                    .foregroundColor(Color("Font_Color_Black"))
                                    .disabled(client.isConnected)
                                
                                Spacer()
                            }
                            .frame(width:diviceWidth * 0.7,height: 30,  alignment: .center)
                            
                        }
                        //服务器状态检测
                        VStack{
                            HStack{
                                Text("サーバーの状態:")
                                
                                if(client.isOnConnecting){
                                    ProgressView()
                                        .frame(width: 13,height: 13,alignment: .center)
                                    Text("接続中......")
                                    
                                }else{
                                    
                                    Image(systemName: "circle.fill")
                                        .resizable()
                                        .aspectRatio(contentMode: .fit)
                                        .foregroundColor(client.isConnected ? Color.green : Color.red)
                                        .frame(width: 15,height: 15,alignment: .center)
                                    Text(client.isConnected ? "オンライン" : "オフライン")
                                        .padding(.leading, -5.0)
                                }
                            }
                            
                            if(client.isOnConnecting){
                                Button("キャンセル") {
                                    client.isOnConnecting = false
                                    client.disconnect()
                                }
                                .frame(width: diviceWidth * 0.75,height: 40,  alignment: .center)
                                .background(isDabledButton ? Color.gray : Color.blue)
                                .foregroundColor(Color.white)
                                .cornerRadius(8)
                            }else{
                                
                                Button{
                                    notice = ""
                                    client.isOnConnecting = true
                                    if(client.isConnected){
                                        client.disconnect()
                                    }else{
                                        client.setup(host: host, portStr: portStr)
                                        client.connect()
                                    }
                                    
                                }label: {
                                    Text(client.isConnected ? "サーバーを切り替える" : "サーバーに接続する")
                                        .frame(width: diviceWidth * 0.75,height: 40,  alignment: .center)
                                        .background(isDabledButton ? Color.gray : Color.blue)
                                        .foregroundColor(Color.white)
                                        .cornerRadius(8)
                                }
                                .disabled(isDabledButton)
                               
                            }
                            
                        }
                        //账号输入
                        VStack{
                            VStack{
                                Text("ユーザー名:")
                                    .padding(.top, 8.0)
                                    .frame(width: diviceWidth * 0.7,height: 30,  alignment: .leading)
                                ZStack{
                                    InputTable()
                                    HStack{
                                        Image(systemName: "person.crop.circle")
                                            .resizable()
                                            .aspectRatio(contentMode: .fit)
                                            .frame(width: 22,height: 22,alignment: .center)
                                        TextField("Username", text: $username)
                                    }
                                    .frame(width: diviceWidth * 0.7, height: 40, alignment: .center)
                                    
                                }
                                .frame(width: diviceWidth * 0.7,height: 30,  alignment: .center)
                                Text("パスワード:")
                                    .padding(.top, 8.0)
                                    .frame(width: diviceWidth * 0.7,height: 30,  alignment: .leading)
                                
                                ZStack{
                                    InputTable()
                                    HStack{
                                        Image(systemName: "key.fill")
                                            .resizable()
                                            .aspectRatio(contentMode: .fit)
                                            .frame(width: 22,height: 22,alignment: .center)
                                        SecureField(/*@START_MENU_TOKEN@*//*@PLACEHOLDER=Label@*/"Password"/*@END_MENU_TOKEN@*/, text: $password)
                                    }
                                    .frame(width: diviceWidth * 0.7, height: 40, alignment: .center)
                                    
                                }
                                
                                Text("パスワードの確認:")
                                    .padding(.top, 8.0)
                                    .frame(width: diviceWidth * 0.7,height: 30,  alignment: .leading)
                                
                                ZStack{
                                    InputTable()
                                    HStack{
                                        Image(systemName: "key.fill")
                                            .resizable()
                                            .aspectRatio(contentMode: .fit)
                                            .frame(width: 22,height: 22,alignment: .center)
                                        SecureField(/*@START_MENU_TOKEN@*//*@PLACEHOLDER=Label@*/"Password"/*@END_MENU_TOKEN@*/, text: $repeatPassword)
                                    }
                                    .frame(width: diviceWidth * 0.7, height: 40, alignment: .center)
                                    
                                }
                                
                                Spacer()
                                    .frame(width: diviceWidth * 0.7,height: 30,  alignment: .leading)
                                
                            }
                            .frame(width: diviceWidth * 0.7, alignment: .center)
                            
                    
                            Text(notice)
                                .frame(height: 20)
                                .foregroundColor(Color.red)
                                .onChange(of: client.errorMessage, perform: { getError in
                                    print("getError收到:\(getError)")
                                    if(getError != ""){
                                        notice = getError
                                    }
                                })
                            
                            Button {
                               
                                if(username == ""){
                                    notice = "ユーザー名を入力してください!"
                                }else if(password == ""){
                                    notice = "パスワードを入力してください!"
                                }else if(password != repeatPassword){
                                    notice = "繰り返しパスワードは一致でわない??"
                                }else{
                                    let contents = Contents(username: username, password: password)
                                    let jsonMessage = JsonMessage(type: "RegisterAccount", contents: contents)
                                    let jsonMessageStr =  JsonHelper().buildJsonMessageStr(input: jsonMessage)
                                    let encryptedMessageStr = RSAHelper().encryptByPublicKey(publicKeyStr: client.publickey_from_server, messageStr: jsonMessageStr)
                                    client.send(inputData: TypeChangeHepler().strToUTF8(string: encryptedMessageStr))
                                }
                            }label: {
                                Text("確認")
                                    .frame(width: diviceWidth * 0.75,height: 40,  alignment: .center)
                                    .background(Color.blue)
                                    .foregroundColor(Color.white)
                                    .cornerRadius(8)
                            }
                        }
                        .padding(.top, 10.0)
                        .frame(height: tableHeight,alignment: .top)
                    }
                    .padding(.bottom, 16.0)
                    .frame(width: diviceWidth * 0.9)
                    .background(Color("Form_Table_Color"))
                    .cornerRadius(8)
                    Spacer()
                }
            }
            
        }
        .frame(width: diviceWidth, height: diviceHeight, alignment: .center)
        .ignoresSafeArea()
        .navigationBarHidden(false)

    }
    func start(newState:Bool) {
        isDabledButton = true
        var isClosed = true
        if(tableHeight == 0.0){
            isClosed = true
        }else{
            isClosed = false
        }
        
        handler = Timer.scheduledTimer(withTimeInterval: 0.001, repeats: true) { _ in
            if(isClosed != !newState){
                if(isClosed){
                    tableHeight += 1.0
                    if(tableHeight >= 370.0){
                        tableHeight = 370.0
                        isDabledButton = false
                        handler?.invalidate()
                    }
                }else{
                    tableHeight -= 1.0
                    if(tableHeight <= 0.0){
                        tableHeight = 0.0
                        isDabledButton = false
                        handler?.invalidate()
                    }
                }
            }else{
                isDabledButton = false
                handler?.invalidate()
            }
            
        }
    }
    
}

struct InputTable: View {
    private let diviceWidth = UIScreen.main.bounds.width
    var body: some View {
        VStack{
            ZStack{
                Rectangle()
                    .trim(from: (diviceWidth * 0.65 + 40) / ( diviceWidth * 1.3 + 80), to: (diviceWidth * 1.3 + 40) / ( diviceWidth * 1.3 + 80))
                    .stroke(.gray, style: StrokeStyle(lineWidth: 1, lineCap: .round, lineJoin: .round))
                    .frame(width:diviceWidth * 0.65,height: 40,alignment: .center)
                Rectangle()
                    .trim(from: 0.0, to: (diviceWidth * 0.65) / ( diviceWidth * 1.3 + 80))
                    .stroke(.gray, style: StrokeStyle(lineWidth: 1, lineCap: .round, lineJoin: .round))
                    .frame(width:diviceWidth * 0.65,height: 40,alignment: .center)
                HStack{
                    Circle()
                        .trim(from: 0.25, to: 0.75)
                        .stroke(.gray, style: StrokeStyle(lineWidth: 1, lineCap: .round, lineJoin: .round))
                        .frame(width:40,height: 40,alignment: .center)
                    Spacer()
                        .frame(width: diviceWidth * 0.65 - 40, height: 40,alignment: .center)
                    
                    Circle()
                        .trim(from: 0.25, to: 0.75)
                        .stroke(.gray, style: StrokeStyle(lineWidth: 1, lineCap: .round, lineJoin: .round))
                        .frame(width:40,height: 40,alignment: .center)
                        .rotationEffect(Angle(degrees: 180))
                    
                }
            }
            .background(Color("Form_Background_Color"))
            .cornerRadius(20)
        }
        
    }
    
}

struct RegisterSuccessView: View {
    @EnvironmentObject var client: WebSocketClient
    private let diviceWidth = UIScreen.main.bounds.width
    var body: some View {
        
        VStack{
            Spacer()
            ZStack{
                
                Image(systemName: "person.crop.circle.badge.checkmark")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 100,height: 100,alignment: .center)
                    .foregroundColor(Color.green)
                Text("アカウント作れました!")
                    .font(.body)
                    .fontWeight(.heavy)
                    .padding(.top, 130.0)
            }
            
            Spacer()
            
            NavigationLink(destination: LoginPage()) {
                Text("ログインへ")
                    .frame(width: diviceWidth * 0.55,height: 40,  alignment: .center)
                    .background(Color.blue)
                    .foregroundColor(Color.white)
                    .cornerRadius(8)
                    .onDisappear{
                        client.disconnect()
                        client.isOnConnecting = false
                    }
            }
            
            .frame(width: 150,height: 40,  alignment: .center)
            .background(Color.blue)
            .foregroundColor(Color.white)
            .cornerRadius(8)
            Spacer()
            
            
        }
        
        .frame(width: 300, height: 300, alignment: .center)
        .background(Color("View_Table_Color"))
        .cornerRadius(8)
        
    }
    
}

struct RegisterPage_Previews: PreviewProvider {
    static var previews: some View {
        RegisterPage()
    }
}
