package com.configtool.utils;

/**
 * DFM加密工具类
 * 对应C++的DFMRetBlackKey函数
 */
public class DFMEncryption {
    
    // 特别说明: strKey 的值可以自己修改，但要与解密函数中的一致
    private static final String KEY = "~!@#$%^&*()_-|g5leSzw$1A&n.o";
    
    // 特别说明: strBlackKey 的值可以自己修改(长度不要变)，但要与解密函数中的一致
    private static final String BLACK_KEY_PREFIX = "16324";
    
    /**
     * DFM密码加密函数 (对应C++的DFMRetBlackKey)
     * @param whiteText 明文密码
     * @return 加密后的密码
     */
    public static String encrypt(String whiteText) {
        if (whiteText == null || whiteText.isEmpty()) {
            return "";
        }
        
        String temp;
        String leavings = "";
        
        // 倒置处理
        if (whiteText.length() > KEY.length()) {
            int pos = KEY.length();
            temp = whiteText.substring(0, pos);
            leavings = whiteText.substring(pos);
        } else {
            temp = whiteText;
        }
        
        // 字符串倒置 (对应C++的MakeReverse)
        temp = reverse(temp);
        
        // 加密
        StringBuilder blackKey = new StringBuilder(BLACK_KEY_PREFIX);
        for (int i = 0; i < temp.length(); i++) {
            long code = 255 - (temp.charAt(i) ^ KEY.charAt(i));
            blackKey.append(String.format("%03d", code));
        }
        
        // 最终结果倒置
        String result = reverse(blackKey.toString());
        
        // 如果有剩余部分，加到最前面
        result = leavings + result;
        
        return result;
    }
    
    /**
     * 字符串倒置 (对应C++的MakeReverse)
     */
    private static String reverse(String str) {
        return new StringBuilder(str).reverse().toString();
    }
} 