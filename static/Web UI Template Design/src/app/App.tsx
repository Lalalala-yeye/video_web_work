import { BrowserRouter, Routes, Route, Navigate } from 'react-router';

// Pages
import { Index } from './pages/Index';
import { Home } from './pages/Home';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { VideoDetail } from './pages/VideoDetail';
import { LiveList } from './pages/LiveList';
import { LiveRoom } from './pages/LiveRoom';
import { Subscribe } from './pages/Subscribe';
import { Search } from './pages/Search';
import { Upload } from './pages/Upload';
import { Profile } from './pages/Profile';
import { DesignSystem } from './pages/DesignSystem';
import { EmptyState } from './pages/EmptyState';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 导航索引页 */}
        <Route path="/" element={<Index />} />

        {/* 视频首页 */}
        <Route path="/home" element={<Home />} />

        {/* 认证页面 */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* 视频相关 */}
        <Route path="/video/:id" element={<VideoDetail />} />
        <Route path="/upload" element={<Upload />} />

        {/* 直播相关 */}
        <Route path="/live" element={<LiveList />} />
        <Route path="/live/:id" element={<LiveRoom />} />

        {/* 订阅和搜索 */}
        <Route path="/subscribe" element={<Subscribe />} />
        <Route path="/search" element={<Search />} />

        {/* 用户中心 */}
        <Route path="/profile" element={<Profile />} />

        {/* 设计系统和空状态示例 */}
        <Route path="/design-system" element={<DesignSystem />} />
        <Route path="/empty-state" element={<EmptyState />} />

        {/* 404 重定向 */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}