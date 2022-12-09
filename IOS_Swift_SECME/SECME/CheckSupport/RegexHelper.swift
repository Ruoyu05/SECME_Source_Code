//
//  Check.swift
//  
//
//  Created by cmStudent on 2022/12/07.
//

import Foundation

struct RegexHelper{
    //Swift并没有提供处理正则表达式的类，
    //本文将使用OC正则表达式类，进行正则表达式的检测
    let regex: NSRegularExpression?
    
    //对结构体进行初始化
    //并传入一个字符串参数，作为正则表达式
    init(_ pattern: String)
    {
        //添加一个异常捕捉语句,用来执行正则表达式的匹配工作
        do
        {
            //创建一个正则表达式，并不对大小写进行区分
            regex = try NSRegularExpression(pattern: pattern,
                                            options: NSRegularExpression.Options.caseInsensitive)
        }
        catch
        {
            //如果正则表达式创建失败，则将正则表达式对象置空
            regex = nil
        }
    }
    //创建一个方法，用来执行正则表达式的检测工作，并返回一个布尔结果
    func match(_ input: String) -> Bool
    {
        //开始对字符串进行正则表达式的检测
        if let matches = regex?.matches(in: input,
                                        options: .reportProgress,
                                        range: NSMakeRange(0, input.lengthOfBytes(using: String.Encoding.utf8)))
        {
            //比较在字符串中，匹配正则表达式的位置是否大于0。
            //以判断字符串与正则表达式是否匹配。
            return matches.count > 0
        }
        else
        {
            //如果字符串不匹配正则表达式，则返回否的布尔结果。
            return false
        }
    }
}
func checkIpv6(input:String) -> Bool{
    let pattern =  "^\\[([0-9a-fA-F]{0,4}:){7}([0-9a-fA-F]{0,4})\\]$"
    let matcher = RegexHelper(pattern)
    return matcher.match(input)
}

func checkIpv4(input:String) -> Bool{
    let pattern = "^(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)$"
    let matcher = RegexHelper(pattern)
    return matcher.match(input)
}

func checkDomain(input:String) -> Bool{
    let pattern =  "^(([0-9a-zA-z])+\\.)+[a-zA-z]+$"
    let pattern_localhost = "^localhost$"
    let matcher = RegexHelper(pattern)
    let matcher_localhost = RegexHelper(pattern_localhost)
    if matcher.match(input){
        return true
    }
    return matcher_localhost.match(input)
    
}

func checkPort(input:String) -> Bool{
    let pattern = "^[0-9]+$"
    let matcher = RegexHelper(pattern)
    return matcher.match(input)
}

func checkHost(input:String) -> Bool{
    return checkIpv4(input:input) || checkDomain(input:input) || checkIpv6(input:input)
}



