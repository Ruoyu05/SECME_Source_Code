//
//  PHPickerView.swift
//  FaceCheck0138
//
//  Created by cmStudent on 2021/07/16.
//


import SwiftUI
import PhotosUI
import AVFoundation


struct PhotolibraryickerView: UIViewControllerRepresentable {
    
    //sheetが表示されているか
    @Binding var isShowPhotolibrary:Bool
    //フォトライブラリーから読み込む写真
    @Binding var qrCodeImage:UIImage?
    @Binding var qrCodeMessage:String
    
    //Coordinator でコントローラのdelegateを管理
    class Coordinator:NSObject,
                      PHPickerViewControllerDelegate{
        //PHPickerView型の変数を用意
        var parent:PhotolibraryickerView
        //イニシャライザ
        init(parent:PhotolibraryickerView){
            self.parent = parent
        }
        //フォトライブラリーで写真を選択・キャンセルした時に実行される
        //delegate メソッド、必ず必要
        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            
            //写真は１つだけ選べる設定なので、最初の１件を指定
            if let result = results.first{
                //UIImage型の写真のみ非同期で取
                result.itemProvider.loadObject(ofClass: UIImage.self){
                    (image,error)in
                    if let unwrapImage = image as? UIImage{
                        DispatchQueue.main.async {
                           
                            self.parent.qrCodeImage = unwrapImage
                        
                            
                        }
                        
                    }else{
                        print("使用できる写真がないです")
                    }
                }
            }else{
                print("選択された写真はないです")
            }//sheetを閉じる
            
            parent.isShowPhotolibrary = false
            
        }//picker*
        
    }//Coordinator*
    
    func readQRMessage(qrCodeImg:UIImage){
        let ciImage:CIImage = CIImage(image:qrCodeImg)!
        let context = CIContext(options: nil)
        let detector = CIDetector(ofType: CIDetectorTypeQRCode, context: context,
                                  options: [CIDetectorAccuracy:CIDetectorAccuracyHigh])
        let features = detector?.features(in: ciImage)
        print("扫描到二维码个数：\(features?.count ?? 0)")
        //遍历所有的二维码，并框出
        for feature in features as! [CIQRCodeFeature] {
            print(feature.messageString ?? "")
        }
        
    }
    //Coordinatorを生成、SwiftUIによって自動的に呼び出し
    func makeCoordinator() -> Coordinator {
        //Coordinatorクラスのインスタンス
        Coordinator(parent: self)
    }
    //Viewを生成する時に実行
    func makeUIViewController(context: UIViewControllerRepresentableContext<PhotolibraryickerView>)
    -> PHPickerViewController {
        //PHPickerViewControllerのカスタマイズ
        var configuration = PHPickerConfiguration()
        //静止面を選択
        configuration.filter = .images
        //フォトライブラリーで選択できる枚数を一枚にする
        configuration.selectionLimit = 1
        //PHPickerViewControllerのインスタンスを生成
        let picker = PHPickerViewController(configuration: configuration)
        //delegate設定
        picker.delegate = context.coordinator
        //PHPickerViewControllerを返す
        return picker
    }
    //Viewが更新された時実行
    func updateUIViewController(_ uiViewController: PHPickerViewController, context: UIViewControllerRepresentableContext<PhotolibraryickerView>) {
        //処理なし
    }
    
    
}


