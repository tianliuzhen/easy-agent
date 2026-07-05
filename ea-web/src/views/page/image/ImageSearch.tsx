import React, {useEffect, useRef, useState} from 'react';
import ReactCrop, {type Crop, type PixelCrop} from 'react-image-crop';
import 'react-image-crop/dist/ReactCrop.css';
import {
    Button,
    Card,
    Empty,
    Form,
    Image,
    Input,
    InputNumber,
    message,
    Modal,
    Popconfirm,
    Space,
    Spin,
    Tag,
    Typography,
    Upload,
} from 'antd';
import type {UploadFile} from 'antd';
import {
    DeleteOutlined,
    InboxOutlined,
    PictureOutlined,
    ReloadOutlined,
    SearchOutlined,
    UploadOutlined,
} from '@ant-design/icons';
import {imageSearchApi, resolveImageUrl} from '../../api/ImageSearchApi';
import type {ImageItem} from '../../api/ImageSearchApi';

const {Title, Text} = Typography;

const ImageSearch: React.FC = () => {
    const [dataSource, setDataSource] = useState<ImageItem[]>([]);
    const [loading, setLoading] = useState(false);

    const [uploadOpen, setUploadOpen] = useState(false);
    const [uploadFileList, setUploadFileList] = useState<UploadFile[]>([]);
    const [uploadName, setUploadName] = useState('');
    const [uploading, setUploading] = useState(false);

    const [searchOpen, setSearchOpen] = useState(false);
    const [topK, setTopK] = useState(10);
    const [searching, setSearching] = useState(false);
    const [searchResults, setSearchResults] = useState<ImageItem[]>([]);
    // 查询图 + 局部框选
    const [queryFile, setQueryFile] = useState<File | null>(null);
    const [queryPreviewUrl, setQueryPreviewUrl] = useState('');
    const [crop, setCrop] = useState<Crop>();
    const [completedCrop, setCompletedCrop] = useState<PixelCrop>();
    const imgRef = useRef<HTMLImageElement>(null);

    const loadData = async () => {
        setLoading(true);
        try {
            const res = await imageSearchApi.list();
            if (res.success) {
                setDataSource(res.data || []);
            } else {
                message.error(res.message || '加载失败');
            }
        } catch (e) {
            message.error('加载图片列表失败');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, []);

    const setItemStatus = (uid: string, status: UploadFile['status'], percent?: number) => {
        setUploadFileList((prev) =>
            prev.map((f) => (f.uid === uid ? {...f, status, percent: percent ?? f.percent} : f)));
    };

    const handleUpload = async () => {
        const items = uploadFileList.filter((f) => f.originFileObj);
        if (items.length === 0) {
            message.warning('请选择图片');
            return;
        }
        setUploading(true);
        let ok = 0;
        let fail = 0;
        try {
            for (const item of items) {
                const file = item.originFileObj as File;
                // 仅单张时使用自定义名称，批量时用各自文件名
                const name = items.length === 1 && uploadName ? uploadName : file.name;
                setItemStatus(item.uid, 'uploading', 30);
                try {
                    const res = await imageSearchApi.upload(file, name);
                    if (res.success) {
                        ok++;
                        setItemStatus(item.uid, 'done', 100);
                    } else {
                        fail++;
                        setItemStatus(item.uid, 'error', 100);
                    }
                } catch (e) {
                    fail++;
                    setItemStatus(item.uid, 'error', 100);
                }
            }
            if (ok > 0) {
                message.success(`上传成功 ${ok} 张${fail ? `，失败 ${fail} 张` : ''}`);
                loadData();
            } else {
                message.error('上传失败');
            }
            // 全部成功才关闭并清空；有失败则保留弹窗展示失败项
            if (fail === 0) {
                setUploadOpen(false);
                setUploadFileList([]);
                setUploadName('');
            }
        } finally {
            setUploading(false);
        }
    };

    const doSearch = async (file: File) => {
        setSearching(true);
        try {
            const res = await imageSearchApi.search(file, topK);
            if (res.success) {
                setSearchResults(res.data || []);
            } else {
                message.error(res.message || '搜索失败');
            }
        } catch (e) {
            message.error('搜索失败');
        } finally {
            setSearching(false);
        }
    };

    // 设置查询图（重置框选），并生成用于展示/裁剪的 data URL
    const setQueryImage = (file: File) => {
        setQueryFile(file);
        setCrop(undefined);
        setCompletedCrop(undefined);
        const reader = new FileReader();
        reader.onload = () => setQueryPreviewUrl(reader.result as string);
        reader.readAsDataURL(file);
    };

    // 搜索整图
    const handleSearchWhole = () => {
        if (!queryFile) {
            message.warning('请先上传或粘贴查询图片');
            return;
        }
        doSearch(queryFile);
    };

    // 搜索框选的局部区域：用 canvas 按原图分辨率裁剪出选区
    const handleSearchCrop = () => {
        const img = imgRef.current;
        if (!img || !completedCrop || !completedCrop.width || !completedCrop.height) {
            message.warning('请先在图片上拖动框选一个区域');
            return;
        }
        const scaleX = img.naturalWidth / img.width;
        const scaleY = img.naturalHeight / img.height;
        const canvas = document.createElement('canvas');
        canvas.width = Math.round(completedCrop.width * scaleX);
        canvas.height = Math.round(completedCrop.height * scaleY);
        const ctx = canvas.getContext('2d');
        if (!ctx) {
            message.error('裁剪失败');
            return;
        }
        ctx.drawImage(
            img,
            completedCrop.x * scaleX,
            completedCrop.y * scaleY,
            completedCrop.width * scaleX,
            completedCrop.height * scaleY,
            0,
            0,
            canvas.width,
            canvas.height,
        );
        canvas.toBlob((blob) => {
            if (!blob) {
                message.error('裁剪失败');
                return;
            }
            doSearch(new File([blob], 'crop.png', {type: 'image/png'}));
        }, 'image/png');
    };

    // 搜索弹窗打开时，支持直接粘贴截图（Ctrl/Cmd+V），粘贴即自动搜整图
    useEffect(() => {
        if (!searchOpen) {
            return;
        }
        const handlePaste = (e: ClipboardEvent) => {
            const items = e.clipboardData?.items;
            if (!items) {
                return;
            }
            for (let i = 0; i < items.length; i++) {
                const item = items[i];
                if (item.type.indexOf('image') !== -1) {
                    const file = item.getAsFile();
                    if (file) {
                        setQueryImage(file);
                        message.success('截图已粘贴，正在搜索…（可再框选局部）');
                        doSearch(file);
                        e.preventDefault();
                    }
                    break;
                }
            }
        };
        document.addEventListener('paste', handlePaste);
        return () => document.removeEventListener('paste', handlePaste);
    }, [searchOpen, topK]);

    const handleDelete = async (id: string) => {
        try {
            const res = await imageSearchApi.delete(id);
            if (res.success) {
                message.success('已删除');
                loadData();
            } else {
                message.error(res.message || '删除失败');
            }
        } catch (e) {
            message.error('删除失败');
        }
    };

    const uploadProps = (fileList: UploadFile[], setFileList: (f: UploadFile[]) => void, multiple = false) => ({
        fileList,
        multiple,
        maxCount: multiple ? undefined : 1,
        accept: 'image/*',
        listType: 'picture' as const,
        beforeUpload: () => false,
        onChange: (info: {fileList: UploadFile[]}) =>
            setFileList(multiple ? info.fileList : info.fileList.slice(-1)),
        onRemove: (file: UploadFile) => setFileList(fileList.filter((f) => f.uid !== file.uid)),
    });

    return (
        <div style={{padding: 16}}>
            <Space style={{width: '100%', justifyContent: 'space-between', marginBottom: 16}}>
                <div>
                    <Title level={4} style={{margin: 0}}>
                        <PictureOutlined/> 图片搜索
                    </Title>
                    <Text type="secondary">上传商品图片入库，支持以图搜图（向量检索）</Text>
                </div>
                <Space>
                    <Button type="primary" icon={<UploadOutlined/>} onClick={() => setUploadOpen(true)}>上传图片</Button>
                    <Button icon={<SearchOutlined/>} onClick={() => {
                        setSearchResults([]);
                        setQueryFile(null);
                        setQueryPreviewUrl('');
                        setCrop(undefined);
                        setCompletedCrop(undefined);
                        setSearchOpen(true);
                    }}>图片搜索</Button>
                    <Button icon={<ReloadOutlined/>} onClick={loadData}>刷新</Button>
                </Space>
            </Space>

            <Spin spinning={loading}>
                {dataSource.length === 0 ? (
                    <Empty description="还没有图片，点击右上角上传"/>
                ) : (
                    <Image.PreviewGroup>
                        <div style={{display: 'flex', flexWrap: 'wrap', gap: 12}}>
                            {dataSource.map((item) => (
                                <Card
                                    key={item.id}
                                    size="small"
                                    style={{width: 160}}
                                    cover={
                                        <Image
                                            src={resolveImageUrl(item.imageUrl)}
                                            alt={item.name}
                                            height={140}
                                            width="100%"
                                            style={{objectFit: 'cover'}}
                                        />
                                    }
                                    actions={[
                                        <Popconfirm
                                            key="del"
                                            title="确认删除这张图片？"
                                            onConfirm={() => handleDelete(item.id)}
                                        >
                                            <DeleteOutlined/>
                                        </Popconfirm>,
                                    ]}
                                >
                                    <Card.Meta
                                        title={<span style={{fontSize: 13}}>{item.name}</span>}
                                        description={<Tag>{item.ext}</Tag>}
                                    />
                                </Card>
                            ))}
                        </div>
                    </Image.PreviewGroup>
                )}
            </Spin>

            {/* 上传弹窗 */}
            <Modal
                title="上传图片"
                open={uploadOpen}
                onOk={handleUpload}
                confirmLoading={uploading}
                onCancel={() => setUploadOpen(false)}
                okText="上传"
                cancelText="取消"
            >
                <Form layout="vertical">
                    <Form.Item label="名称（可选，仅单张上传时生效）">
                        <Input
                            value={uploadName}
                            onChange={(e) => setUploadName(e.target.value)}
                            placeholder="不填或批量上传时用各自文件名"
                        />
                    </Form.Item>
                    <Form.Item label="图片（支持多选批量上传）">
                        <Upload.Dragger {...uploadProps(uploadFileList, setUploadFileList, true)} disabled={uploading}>
                            <p className="ant-upload-drag-icon"><InboxOutlined/></p>
                            <p className="ant-upload-text">点击或拖拽图片到此处，可多选</p>
                        </Upload.Dragger>
                    </Form.Item>
                </Form>
            </Modal>

            {/* 搜索弹窗 */}
            <Modal
                title="以图搜图"
                open={searchOpen}
                footer={null}
                width={720}
                onCancel={() => setSearchOpen(false)}
            >
                <div style={{display: 'flex', gap: 16, marginBottom: 16}}>
                    <div style={{width: 360}}>
                        {queryPreviewUrl ? (
                            <ReactCrop
                                crop={crop}
                                onChange={(c) => setCrop(c)}
                                onComplete={(c) => setCompletedCrop(c)}
                            >
                                <img
                                    ref={imgRef}
                                    src={queryPreviewUrl}
                                    alt="query"
                                    style={{maxWidth: '100%', maxHeight: 360, display: 'block'}}
                                />
                            </ReactCrop>
                        ) : (
                            <Upload.Dragger
                                accept="image/*"
                                showUploadList={false}
                                beforeUpload={(file) => {
                                    setQueryImage(file as unknown as File);
                                    return false;
                                }}
                            >
                                <p className="ant-upload-drag-icon"><InboxOutlined/></p>
                                <p className="ant-upload-text">上传查询图片</p>
                                <p className="ant-upload-hint">支持 Ctrl/Cmd+V 粘贴截图</p>
                            </Upload.Dragger>
                        )}
                    </div>
                    <Space direction="vertical">
                        <Text>返回数量 topK</Text>
                        <InputNumber min={1} max={50} value={topK} onChange={(v) => setTopK(v || 10)}/>
                        <Button type="primary" icon={<SearchOutlined/>} loading={searching}
                                disabled={!queryFile} onClick={handleSearchWhole}>
                            搜索整图
                        </Button>
                        <Button icon={<SearchOutlined/>} loading={searching}
                                disabled={!completedCrop?.width} onClick={handleSearchCrop}>
                            搜索选区
                        </Button>
                        {queryFile && (
                            <Button type="link" onClick={() => {
                                setQueryFile(null);
                                setQueryPreviewUrl('');
                                setCrop(undefined);
                                setCompletedCrop(undefined);
                            }}>换一张</Button>
                        )}
                        <Text type="secondary" style={{fontSize: 12, maxWidth: 180}}>
                            在图上拖动框选局部，点"搜索选区"只搜该区域
                        </Text>
                    </Space>
                </div>

                {searchResults.length > 0 && (
                    <Image.PreviewGroup>
                        <div style={{display: 'flex', flexWrap: 'wrap', gap: 12}}>
                            {searchResults.map((r) => (
                                <Card key={r.id} size="small" style={{width: 160}}
                                      cover={<Image src={resolveImageUrl(r.imageUrl)} alt={r.name}
                                                    height={140} width="100%" style={{objectFit: 'cover'}}/>}>
                                    <Card.Meta
                                        title={<span style={{fontSize: 13}}>{r.name}</span>}
                                        description={<Tag color="blue">相似度 {r.score?.toFixed(4)}</Tag>}
                                    />
                                </Card>
                            ))}
                        </div>
                    </Image.PreviewGroup>
                )}
            </Modal>
        </div>
    );
};

export default ImageSearch;
