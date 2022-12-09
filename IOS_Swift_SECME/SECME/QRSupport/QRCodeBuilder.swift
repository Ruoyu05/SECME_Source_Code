//
//  QRCodeBuilder.swift
//  SECME
//
//  Created by cmStudent on 2022/11/26.
//

import Foundation
import SwiftUI

class QRCodeBuilder:ObservableObject{
    
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
