package com.aaa.easyagent.common.util;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * OCR 工具类 - 集成 Tess4J 5.13.0
 * 优化说明：采用方法内实例化，支持多线程并发执行
 *
 * @author liuzhen.tian
 * @version 1.1 OcrUtil.java 2026/2/1
 */
@Slf4j
@Component
public class OcrUtil {

    /**
     * 从图片中识别文字 (多线程安全)
     *
     * @param file 图片文件
     * @return 识别出的文字
     */
    public String recognizeText(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            return recognizeText(image);
        } catch (IOException e) {
            log.error("读取图片文件失败", e);
            throw new RuntimeException("读取图片文件失败: " + e.getMessage());
        }
    }

    /**
     * 核心识别方法 (多线程安全)
     */
    public String recognizeText(BufferedImage image) {
        // 1. 每次调用创建一个新实例，确保多线程安全
        Tesseract tesseract = new Tesseract();
        
        try {
            // 2. 确定语言数据路径
            // 方案：寻找包含 chi_sim.traineddata 的 tessdata 目录
            String tessdataPath = findTessdataPath();
            
            // 关键：Tess4J 的 setDatapath 通常需要指向包含 "tessdata" 文件夹的父目录
            // 或者直接指向包含 .traineddata 文件的目录（取决于版本配置）
            // 我们这里统一指向 .traineddata 文件所在的直接目录
            tesseract.setDatapath(tessdataPath); 
            log.info("最终使用的 OCR 语言包目录: {}", tessdataPath);

            // 3. 设置识别参数
            tesseract.setLanguage("chi_sim+eng"); // 简体中文 + 英文
            tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO); // 自动分割页面
            tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY); // 强制使用 LSTM 引擎

            // 4. 执行识别
            String result = tesseract.doOCR(image);
            return result != null ? result.trim() : "";
            
        } catch (TesseractException e) {
            log.error("OCR 识别失败: {}", e.getMessage());
            throw new RuntimeException("图片文字识别失败，请检查语言包配置: " + e.getMessage());
        } catch (Error e) {
            // 捕获 Invalid memory access 等底层错误
            log.error("Tesseract 底层引擎崩溃，通常是由于语言包路径不正确或文件损坏引起", e);
            throw new RuntimeException("OCR 引擎初始化失败，请确认 tessdata 目录下包含 chi_sim.traineddata 文件");
        }
    }

    /**
     * 智能寻找 tessdata 路径
     */
    private String findTessdataPath() {
        // 尝试路径 1: 源码资源目录 (IDE 运行)
        File path1 = new File("src/main/resources/tessdata");
        if (path1.exists() && path1.isDirectory()) {
            return path1.getAbsolutePath();
        }

        // 尝试路径 2: 运行根目录下的 tessdata (部署环境)
        File path2 = new File("tessdata");
        if (path2.exists() && path2.isDirectory()) {
            return path2.getAbsolutePath();
        }

        // 尝试路径 3: 环境变量
        String envPath = System.getenv("TESSDATA_PREFIX");
        if (envPath != null) {
            return envPath;
        }

        // 如果都没找到，返回一个默认值，让 Tesseract 自己报错
        return path1.getAbsolutePath();
    }

    /**
     * 检查是否支持指定的图片格式
     */
    public boolean isSupportedImageFormat(String fileName) {
        if (fileName == null) return false;
        String lowerCase = fileName.toLowerCase();
        return lowerCase.endsWith(".jpg") || 
               lowerCase.endsWith(".jpeg") || 
               lowerCase.endsWith(".png") || 
               lowerCase.endsWith(".bmp") || 
               lowerCase.endsWith(".tiff");
    }
}
