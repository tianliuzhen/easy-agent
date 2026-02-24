package com.aaa.easyagent.common.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * @author liuzhen.tian
 * @version 1.0 PinyinUtil.java  2026/2/7 23:08
 */

public class PinyinUtil {

    /**
     * 生成拼音首字母
     *
     * @param agentName 中文字符串
     * @return 拼音首字母字符串，英文字符保持不变（统一大写）
     */
    public static String getPinyin(String agentName) {
        if (agentName == null || agentName.trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.UPPERCASE); // 大写
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE); // 无声调

        char[] chars = agentName.trim().toCharArray();

        for (char c : chars) {
            // 判断是否为中文字符
            if (Character.toString(c).matches("[\\u4E00-\\u9FA5]")) {
                try {
                    // 获取拼音数组（处理多音字）
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        // 多音字默认取第一个读音
                        result.append(pinyinArray[0].toLowerCase()).append("_");
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    // 转换失败，跳过该字符
                }
            } else {
                // 非中文字符（英文、数字等）原样保留，统一转为大写
                result.append(Character.toUpperCase(c));
            }
        }

        return result.toString();
    }
}
