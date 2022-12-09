//
//  AddFriendPage2.swift
//  SECME
//
//  Created by cmStudent on 2022/11/29.
//

import SwiftUI

struct AddFriendPage2: View {
    @State var isShowMyQRCode:Bool = false
    
    
    @State var isShowAction = false
    @State var isShowPhotolibrary = false
    @State var isShowCamera = false
    
    var body: some View {
        VStack{
            if(isShowMyQRCode){
                Text("Show My QR")
                
                
            }else{
                
                Text("add Friend My QR")
                
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
                    
                }
                .sheet(isPresented: $isShowCamera) {
                    
        
                    
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
            }
        }
    }
}

struct AddFriendPage2_Previews: PreviewProvider {
    static var previews: some View {
        AddFriendPage2()
    }
}
