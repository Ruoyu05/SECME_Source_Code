//
//  ScannerView.swift
//  QRReaderCamera
//
//  Created by cmStudent on 2022/10/13.
//


import SwiftUI

struct ScannerView: View {
    
    @ObservedObject var viewModel = ScannerViewModel()
    
    @Binding var isReadingQR:Bool
    @Binding var pasteboard:String
    
    var body: some View {
        ZStack {
//            Text("Scanner goes here...")
            
            QrCodeScannerView()
                .found(r: self.viewModel.onFoundQrCode)
                .torchLight(isOn: self.viewModel.torchIsOn)
                .interval(delay: self.viewModel.scanInterval)
                
            VStack {
                VStack {
                    Text("请对准需要扫描的二维码")
                        .font(.subheadline)
                        .foregroundColor(Color.white)
                    Text(self.viewModel.lastQrCode)
                        .bold()
                        .foregroundColor(Color.white)
                        .lineLimit(5)
                        .padding()
                        .onReceive(viewModel.$lastQrCode, perform: { receive in
                            print(receive)
                            if("Qr-code goes here".elementsEqual(receive)){
                                print("未检测到QR")
                                print("----------------")
                            }else{
                                
                                print("检测到QR")
                                pasteboard = receive
                                isReadingQR = false
                                print("----------------")
                            }
                           
                        })
                }
                .padding(.vertical, 20)
                
                Spacer()
                HStack {
                    Button(action: {
                        self.viewModel.torchIsOn.toggle()
                    }, label: {
                        ZStack{
                            if(self.viewModel.torchIsOn){
                                Image(systemName: "light.max")
                                    .imageScale(.large)
                                    .foregroundColor(Color.yellow)
                                    .padding(.bottom, 33.0)
                            }
                            Image(systemName: self.viewModel.torchIsOn ? "flashlight.on.fill" : "flashlight.off.fill")
                                .imageScale(.large)
                                .foregroundColor(self.viewModel.torchIsOn ? Color.yellow : Color.blue)
                                .padding()
                        }
                        
                    })
                }
                .frame(width: 60, height: 60, alignment: .center)
                .background(Color.white)
                .cornerRadius(10)
                Button("取消") {
                    isReadingQR = false
                }.frame(width: 160, height: 40, alignment: .center)
                    .background(Color.blue)
                    .foregroundColor(Color.white)
                    .cornerRadius(8)
                
            }.padding()
        }
    }
}
