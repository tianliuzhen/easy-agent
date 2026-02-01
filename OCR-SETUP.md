# OCR 功能配置指南

## 概述

项目已集成 Tesseract OCR 引擎，支持从图片中识别文字并上传到知识库。

支持的图片格式：
- JPG / JPEG
- PNG
- BMP
- TIFF
- GIF

## 安装 Tesseract OCR

### Windows

1. **下载 Tesseract 安装包**
   - 访问：https://github.com/UB-Mannheim/tesseract/wiki
   - 下载最新版本的 Windows 安装程序（推荐 64-bit）

2. **安装 Tesseract**
   - 运行安装程序
   - 默认安装路径：`C:\Program Files\Tesseract-OCR`
   - 安装时注意勾选"Additional language data"中的 **Chinese (Simplified)** 语言包

3. **配置环境变量（可选）**
   ```
   变量名：TESSDATA_PREFIX
   变量值：C:\Program Files\Tesseract-OCR\tessdata
   ```

### Linux (Ubuntu/Debian)

```bash
# 安装 Tesseract
sudo apt-get update
sudo apt-get install tesseract-ocr

# 安装中文语言包
sudo apt-get install tesseract-ocr-chi-sim

# 验证安装
tesseract --version
```

### macOS

```bash
# 使用 Homebrew 安装
brew install tesseract
brew install tesseract-lang  # 安装所有语言包

# 验证安装
tesseract --version
```

## 下载语言数据文件

如果安装时没有包含中文语言包，可以手动下载：

1. **下载地址**：
   - https://github.com/tesseract-ocr/tessdata/blob/main/chi_sim.traineddata
   - https://github.com/tesseract-ocr/tessdata/blob/main/eng.traineddata

2. **放置位置**：
   - Windows: `C:\Program Files\Tesseract-OCR\tessdata\`
   - Linux: `/usr/share/tesseract-ocr/4.00/tessdata/`
   - macOS: `/usr/local/share/tessdata/`

## 项目配置

项目会自动查找 Tesseract 安装路径，支持以下位置：

1. 环境变量 `TESSDATA_PREFIX`
2. Windows 默认路径
3. Linux 默认路径
4. macOS 默认路径

无需额外配置，安装完成后即可使用。

## 使用方式

### 1. 文件上传模式

在知识库管理页面：
1. 点击"上传文档"
2. 选择"文件上传"
3. 选择图片文件（jpg、png 等）
4. 填写知识库名称和描述
5. 点击确定上传

### 2. 识别过程

- 后端会自动使用 OCR 识别图片中的文字
- 识别出的文字会被保存到向量数据库
- 支持中英文混合识别

### 3. 识别效果

为了获得最佳识别效果：
- ✅ 使用清晰的图片（分辨率 ≥ 300 DPI）
- ✅ 确保文字清晰可见
- ✅ 避免模糊、倾斜的图片
- ✅ 推荐使用黑色文字、白色背景

## 故障排查

### 问题 1：找不到 tessdata

**错误信息**：`Error opening data file...`

**解决方案**：
1. 确认 Tesseract 已正确安装
2. 检查语言数据文件是否存在
3. 设置环境变量 `TESSDATA_PREFIX`

### 问题 2：识别结果不准确

**解决方案**：
1. 提高图片清晰度
2. 调整图片对比度
3. 使用扫描版本而非拍照版本

### 问题 3：中文识别失败

**解决方案**：
1. 确认安装了 `chi_sim.traineddata` 语言包
2. 检查语言包文件位置是否正确
3. 重启应用

## 技术细节

- **OCR 引擎**：Tesseract 5.x
- **Java 库**：Tess4J 5.9.0
- **识别语言**：中文简体 + 英文
- **OCR 模式**：LSTM Neural Network
- **页面分割**：自动全页分割

## API 端点

```
POST /knowledge/upload
Content-Type: multipart/form-data

参数：
- kbName: 知识库名称
- kbDesc: 描述
- file: 文件（支持 txt、pdf、图片）
```

## 性能说明

- 识别速度取决于图片大小和质量
- 一般 A4 页面图片识别时间：2-5 秒
- 建议异步处理大批量图片

## 参考资料

- Tesseract 官方文档：https://github.com/tesseract-ocr/tesseract
- Tess4J 文档：http://tess4j.sourceforge.net/
- 语言数据下载：https://github.com/tesseract-ocr/tessdata
