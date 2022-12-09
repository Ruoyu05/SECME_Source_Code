//
//  AddFriendView.swift
//  SecureMessage
//
//  Created by cmStudent on 2022/11/03.
//

import SwiftUI

struct ScanFriendView: View {
    @EnvironmentObject var client: WebSocketClient
    @State var qrCodeImg =  UIImage(named: "QRcodeImg")
    @State var message:String = ""
    @State var myQRMessage:String = ""
    
    @State var isShowPhotolibrary = false
    @State var isShowCamera = false
    @State var isShowAction = false
    
    @State var friendName = ""
    @State var friendNickName = ""
    
    @State var isShowMyQRCode:Bool = false
    
    @StateObject var qrMessageModel = QRMessageModel()
    
    var body: some View {
        VStack{
            
            if(isShowMyQRCode){
                
                HStack{
                    Text(client.username)
                        .font(.title)
                        .fontWeight(.semibold)
                    Spacer()
                }.frame(width: 345)
                
                HStack{
                    Text("\(client.host):" + String(client.port))
                        .font(.headline)
                        .fontWeight(.semibold)
                        .foregroundColor(Color.gray)
                    Spacer()
                }.padding(.bottom, 8.0).frame(width: 345)
                
                //显示我的二维码
                Image(uiImage: qrCodeImg!)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 200, height: 200)
                    .foregroundColor(Color.black)
                
                HStack{
                    Text("Md5校验:")
                        .font(.caption)
                    Spacer()
                }.frame(width: 345)
                
                ZStack{
                    Text(md5(data: myQRMessage))
                    RoundedRectangle(cornerRadius: 3)
                        .stroke(.gray, lineWidth: 3)
                        .frame(width: 345 ,height: 35)
                }
                
                Button("复制到剪贴板") {
                    UIPasteboard.general.string = myQRMessage
                }.frame(width: 150, height: 45, alignment: .center)
                    .background(Color.blue)
                    .foregroundColor(Color.white)
                    .cornerRadius(8)
                Button("扫描二维码") {
                    isShowMyQRCode = false
                }.frame(width: 150, height: 45, alignment: .center)
                    .background(Color.blue)
                    .foregroundColor(Color.white)
                    .cornerRadius(8)
                
            }else{
                //                Text("QR:\(message)")
                Text("QR:\(qrMessageModel.result)")
                Button{
                    isShowAction = true
                }label: {
                    
                    Image(systemName: "qrcode.viewfinder")
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: 200, height: 200)
                        .foregroundColor(Color.gray)
                }
                .sheet(isPresented: $isShowPhotolibrary) {
                    PhotolibraryickerView(
                        isShowPhotolibrary: $isShowPhotolibrary,
                        qrCodeImage: $qrMessageModel.qrCodeImg, qrCodeMessage: $qrMessageModel.message)
                }
                .sheet(isPresented: $isShowCamera) {
                    
                    ScannerView(isReadingQR: $isShowCamera, pasteboard: $qrMessageModel.message)
                    
                }
                .actionSheet(isPresented:$isShowAction){
                    ActionSheet(title: Text("从二维码添加联络人"),
                                message: Text("请选择获取二维码的方式:"),
                                buttons: [
                                    .default(Text("从剪贴板"),action: {
                                        //从剪贴板粘贴
                                        if UIPasteboard.general.hasStrings {
                                            qrMessageModel.message = UIPasteboard.general.string ?? "none"
                                            
                                        }else{
                                            print("未在剪贴板中检测到文版")
                                        }
                                    }),
                                    .default(Text("相机"),action: {
                                        
                                        if UIImagePickerController.isSourceTypeAvailable(.camera){
                                            print("カメラは利用できます。")
                                            isShowCamera = true
                                            
                                        }else{
                                            print("カメラを利用できません")
                                        }
                                    }),
                                    .default(Text("本地相册"),action:{
                                        isShowPhotolibrary = true
                                    }),
                                    .cancel(Text("取消")){
                                        if(qrCodeImg == nil){
                                            //
                                        }else{
                                            //
                                        }
                                        print("点击了取消")
                                    },
                                    
                                ])
                }
                NavigationLink(destination: AddFriendView(name: qrMessageModel.username, host: qrMessageModel.host, port: qrMessageModel.port, md5Str: qrMessageModel.md5Str,certifyPublicKey: qrMessageModel.certifyPublicKey), isActive: $qrMessageModel.isRSAPublicKey, label: {})
                Image(systemName: "photo.on.rectangle")
                Button("出示我的二维码"){
                    //查找身份密钥数据库 如果没有则新建 并生成一对身份密钥
                    let certifyPublicKey = DBBuilder(db_name: "user_certify").creatUserCertify()
                    
                    myQRMessage =  "{\"u\":\"\(client.username)\",\"h\":\"\(client.host)\",\"p\":\"\(client.port)\",\"key\":\"\(certifyPublicKey)\"}"
//                    print("\(myQRMessage)")
                    
                    //生成二维码
                    qrCodeImg = creatQRCodeImage(text: myQRMessage, WH: 250)
                    isShowMyQRCode = true
                    
                    
                    
                    
                }.frame(width: 150, height: 45, alignment: .center)
                    .background(Color.blue)
                    .foregroundColor(Color.white)
                    .cornerRadius(8)
            }
            
        }
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



struct ScanFriendView_Previews: PreviewProvider {
    static var previews: some View {
        ScanFriendView()
    }
}
