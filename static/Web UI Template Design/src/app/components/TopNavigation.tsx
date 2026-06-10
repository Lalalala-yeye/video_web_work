import { useState } from 'react';
import { Link, useLocation } from 'react-router';
import { Search, ChevronDown, User, LogOut } from 'lucide-react';
import { Avatar } from './ui/Avatar';
import { Button } from './ui/Button';
import { Input } from './ui/Input';

interface TopNavigationProps {
  isLoggedIn?: boolean;
  userInfo?: {
    name: string;
    avatar?: string;
  };
  onSearch?: (query: string) => void;
}

export function TopNavigation({ isLoggedIn = false, userInfo, onSearch }: TopNavigationProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [showUserMenu, setShowUserMenu] = useState(false);
  const location = useLocation();

  const navItems = [
    { path: '/home', label: '首页' },
    { path: '/live', label: '直播' },
    { path: '/subscribe', label: '订阅' },
  ];

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim() && onSearch) {
      onSearch(searchQuery);
    }
  };

  return (
    <header className="fixed top-0 left-0 right-0 h-14 bg-white border-b border-[#EBEEF5] z-50">
      <div className="max-w-[1280px] mx-auto h-full px-4 flex items-center justify-between gap-6">
        {/* Logo */}
        <Link to="/home" className="flex items-center gap-2 flex-shrink-0">
          <div className="w-8 h-8 bg-[#409EFF] rounded flex items-center justify-center">
            <span className="text-white font-bold text-lg">D</span>
          </div>
          <span className="text-xl font-bold text-[#303133]">doinb</span>
        </Link>

        {/* Navigation */}
        <nav className="flex items-center gap-8">
          {navItems.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className={`text-base transition-colors ${
                location.pathname === item.path
                  ? 'text-[#409EFF] font-medium'
                  : 'text-[#606266] hover:text-[#409EFF]'
              }`}
            >
              {item.label}
            </Link>
          ))}
        </nav>

        {/* Search */}
        <form onSubmit={handleSearch} className="flex-1 max-w-md">
          <div className="relative">
            <Input
              type="text"
              placeholder="搜索视频、直播、用户"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pr-10"
            />
            <button
              type="submit"
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[#909399] hover:text-[#409EFF] transition-colors"
            >
              <Search size={18} />
            </button>
          </div>
        </form>

        {/* User Actions */}
        {isLoggedIn && userInfo ? (
          <div className="relative flex-shrink-0">
            <button
              onClick={() => setShowUserMenu(!showUserMenu)}
              className="flex items-center gap-2 hover:bg-[#F5F7FA] px-3 py-2 rounded transition-colors"
            >
              <Avatar size={32} src={userInfo.avatar} name={userInfo.name} />
              <span className="text-sm text-[#606266]">{userInfo.name}</span>
              <ChevronDown size={16} className="text-[#909399]" />
            </button>

            {showUserMenu && (
              <>
                <div
                  className="fixed inset-0 z-40"
                  onClick={() => setShowUserMenu(false)}
                />
                <div className="absolute right-0 top-full mt-2 w-48 bg-white rounded-md border border-[#EBEEF5] shadow-lg z-50">
                  <Link
                    to="/profile"
                    className="flex items-center gap-2 px-4 py-3 hover:bg-[#F5F7FA] transition-colors text-[#606266]"
                    onClick={() => setShowUserMenu(false)}
                  >
                    <User size={16} />
                    <span>个人中心</span>
                  </Link>
                  <button
                    className="w-full flex items-center gap-2 px-4 py-3 hover:bg-[#F5F7FA] transition-colors text-[#606266] border-t border-[#EBEEF5]"
                    onClick={() => setShowUserMenu(false)}
                  >
                    <LogOut size={16} />
                    <span>退出登录</span>
                  </button>
                </div>
              </>
            )}
          </div>
        ) : (
          <div className="flex items-center gap-3 flex-shrink-0">
            <Link to="/login">
              <Button variant="text" size="medium">
                登录
              </Button>
            </Link>
            <Link to="/register">
              <Button variant="primary" size="medium">
                注册
              </Button>
            </Link>
          </div>
        )}
      </div>
    </header>
  );
}
