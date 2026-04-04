import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import {installApiInterceptor} from './utils/apiInterceptor'

// 安装 API 拦截器，统一处理 401 错误
installApiInterceptor()

createRoot(document.getElementById('root')!).render(
    // 注意：StrictMode 会导致组件渲染两次，用于检测副作用
    // 如果不需要可以暂时注释掉
    // <StrictMode>
    <App/>
    // </StrictMode>
)
