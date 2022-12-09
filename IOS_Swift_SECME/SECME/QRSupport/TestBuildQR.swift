//
//  TestBuildQR.swift
//  SECME
//
//  Created by cmStudent on 2022/11/26.
//

import SwiftUI

struct TestBuildQR: View {
    @EnvironmentObject var client: WebSocketClient
    @State var imageQR:UIImage = UIImage(systemName: "qrcode")!
    @State var isReadingQR:Bool = false
    @State var pasteboard:String = ""
    var body: some View {
        VStack{
            Image(uiImage: imageQR)
                .resizable()
                .aspectRatio(contentMode: .fill)
                .foregroundColor(Color(".foregroundColor(Color.green)"))
                .frame(width: 300, height: 300)
                
            
            NavigationLink(destination: ScannerView(isReadingQR: $isReadingQR,pasteboard: $pasteboard),isActive: $isReadingQR) {
                Text("QR Code Reader")
                    .frame(width: 150, height: 40, alignment: .center)
                    .background(Color.blue)
                    .foregroundColor(Color.white)
                    .cornerRadius(8)
            }
            Button("Create QR Code") {
               pasteboard = ""
                let privateKey = DBBuilder(db_name: client.userMd5Str).getCertifyKeyPair().certifyPrivateKey
                
                
                imageQR = QRCodeBuilder().creatQRCodeImage(text: privateKey, WH: 1024)
                
            }
            .frame(width: 150, height: 40, alignment: .center)
            .background(Color.blue)
            .foregroundColor(Color.white)
            .cornerRadius(8)
        }
       
    }
}

struct TestBuildQR_Previews: PreviewProvider {
    static var previews: some View {
        TestBuildQR()
    }
}
