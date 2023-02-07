//
//  AddFriendPage.swift
//  SECME
//
//  Created by cmStudent on 2022/11/18.
//

import SwiftUI


struct AddFriendPage: View {
    @EnvironmentObject var client: WebSocketClient
    private let diviceWidth = UIScreen.main.bounds.width
    @State var imageQR:UIImage = UIImage(systemName: "qrcode")!
    
    @State var isShowMyQRCode = false
    @State var isShowAction = false
    
    @State var isShowPhotolibrary = false
    @State var isShowCamera = false
    
    
    @StateObject var friendQRInfo:FriendQRInfo = FriendQRInfo()
    @State var md5Result = ""
    
    
    var body: some View {
        VStack{
            
            HStack{
                VStack{
                    Text("フレンドの追加")
                        .font(.body)
                        .fontWeight(.heavy)
                }
                .frame(width: diviceWidth, alignment: .center)
                .padding(.bottom, 10.0)
                
            }
            .padding(.top, 32.0)
            .background(Color("View_Table_Color"))
            
            VStack{
                
                if(isShowMyQRCode){
                    //显示我的二维码
                    VStack{
                        HStack{
                            Text(client.username)
                                .font(.title)
                                .fontWeight(.semibold)
                            Spacer()
                        }.frame(width: 345)
                        HStack{
                            Text(client.host + String(":\(client.portStr)"))
                                .font(.headline)
                                .fontWeight(.semibold)
                                .foregroundColor(Color.gray)
                            Spacer()
                        }.padding(.bottom, 8.0).frame(width: 345)
                        Image(uiImage: imageQR)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: 250, height: 250)
                            .foregroundColor(Color.black)
                            .onAppear{
                                //生成QR Code
                                let publicKeyStr = DBBuilder(db_name: client.userMd5Str).getCertifyKeyPair().certifyPublicKey
                                let qrInfo = QRInfo(user: client.username, host: client.host, port: client.portStr, certifyKey: publicKeyStr)
                                let JsonData = try! JSONEncoder().encode(qrInfo)
                                let jsonStr = String(bytes: JsonData, encoding: .utf8)!
                                imageQR = creatQRCodeImage(text: jsonStr, WH: 250)
                                
                                let str_for_md5 = client.host + client.portStr + client.username + publicKeyStr
                                md5Result = getMD5(data: str_for_md5)
                            }
                        HStack{
                            Text("Md5認証:")
                                .font(.caption)
                            Spacer()
                        }.frame(width: 345)
                        
                        ZStack{
                            Text(md5Result)
                            RoundedRectangle(cornerRadius: 3)
                                .stroke(.gray, lineWidth: 3)
                                .frame(width: 345 ,height: 35)
                        }
                        
                        Button {
                            isShowMyQRCode = false
                        }label: {
                            ZStack{
                                
                                Circle()
                                    .frame(width: 55, height: 55, alignment: .center)
                                    .foregroundColor(Color("Color_scanner"))
                                Image("scanqrcode")
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .foregroundColor(Color.black)
                                    .frame(width: 55,height: 55,alignment: .center)
                            }

                        }
                        .padding(.top, 50.0)
                        
                        
                        
                        
                    }
                }else{
                    //扫描好友
                    
                   
                    
                    Button {
                        isShowAction = true
                    }label: {
                    
                        HStack{
                            //相机
                            ZStack{
                                Image(systemName: "camera.viewfinder")
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(width: 100, height: 100)
                                    .foregroundColor(Color.gray)
                                
                            }.frame(width: 100, height: 100)
                            
                            ZStack{
                                Image(systemName: "viewfinder")
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(width: 100, height: 100)
                                    .foregroundColor(Color.gray)
                                Image(systemName: "photo")
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(width: 58, height: 48)
                                    .foregroundColor(Color.gray)
                                
                            }.frame(width: 100, height: 100)
                            
                            //剪切板
                            ZStack{
                                Image(systemName: "scissors")
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(width: 28, height: 28)
                                    .foregroundColor(Color.gray)
                                    .position(x: 40, y: 75)
                                Image(systemName: "doc.on.clipboard")
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(width: 85, height: 85)
                                    .foregroundColor(Color.gray)
                            } .frame(width: 100, height: 100)
                            
                        }
                    }
                    .padding(.top, 150.0)
                    .sheet(isPresented: $isShowPhotolibrary) {
                        PhotolibraryickerView(isShowPhotolibrary: $isShowPhotolibrary, qrCodeImage: $friendQRInfo.qrCodeImg, qrCodeMessage: $friendQRInfo.message)
                    }
                    .sheet(isPresented: $isShowCamera) {
                        
                        ScannerView(isReadingQR: $isShowCamera, pasteboard:$friendQRInfo.message)
                    }
                    .actionSheet(isPresented:$isShowAction){
                        ActionSheet(title: Text("フレンドの追加方法"),
                                    message: Text("選んでください:"),
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
                                        .default(Text("クリックボード"),action: {
                                            print("选择了剪贴板")
                                        }),
                                        .cancel(Text("キャンセル")){
                                            
                                            print("点击了取消")
                                        },
                                        
                                    ])//ActinSheetここまで
                        
                    }
                    
                    
                    Button {
                        isShowMyQRCode = true
                    }label: {
                        ZStack{
                            
                            Circle()
                                .frame(width: 55, height: 55, alignment: .center)
                                .foregroundColor(Color("Color_scanner"))
                            Image("QRCodeImg")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .foregroundColor(Color.black)
                                .frame(width: 55,height: 55,alignment: .center)
                        }
                    }
                    .padding(.top, 150.0)
                    
                    
                    NavigationLink(destination:  FriendDetailPage(friendQRInfo: friendQRInfo),isActive: $friendQRInfo.isRSAPublicKey) {
                        //前往添加好友
                    }
                }
                Spacer()
            }
            
            
        }
        .ignoresSafeArea()
        .navigationBarHidden(false)
    }
    func creatQRCodeImage(text: String,WH:CGFloat) -> UIImage{
        //建立滤镜
        let filter = CIFilter(name: "CIQRCodeGenerator")
        //还原滤镜的默认属性
        filter?.setDefaults()
        //设置须要生成二维码的数据
        filter?.setValue(text.data(using: String.Encoding.utf8), forKey: "inputMessage")
        //从滤镜中取出生成的图片
        let ciImage = filter?.outputImage
        //这个清晰度好
        let bgImage = createQRImage(image: ciImage!, size: WH)
        
        return bgImage
    }
    func createQRImage(image: CIImage, size: CGFloat) -> UIImage {
        
        let extent: CGRect = image.extent.integral
        let scale: CGFloat = min(size/extent.width, size/extent.height)
        
        let width = extent.width * scale
        let height = extent.height * scale
        let cs: CGColorSpace = CGColorSpaceCreateDeviceGray()
        let bitmapRef = CGContext(data: nil, width: Int(width), height: Int(height), bitsPerComponent: 8, bytesPerRow: 0, space: cs, bitmapInfo: 0)!
        
        let context = CIContext(options: nil)
        let bitmapImage: CGImage = context.createCGImage(image, from: extent)!
        
        bitmapRef.interpolationQuality = CGInterpolationQuality.none
        bitmapRef.scaleBy(x: scale, y: scale)
        bitmapRef.draw(bitmapImage, in: extent)
        let scaledImage: CGImage = bitmapRef.makeImage()!
        return UIImage(cgImage: scaledImage)
    }
}






struct AddFriendPage_Previews: PreviewProvider {
    static var previews: some View {
        AddFriendPage()
    }
}
