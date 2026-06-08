import { useState } from 'react';
import { Link } from 'react-router';
import { Card } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';

export function Login() {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    console.log('登录:', formData);
    // 这里添加登录逻辑
  };

  return (
    <div className="min-h-screen bg-[#F5F7FA] flex items-center justify-center p-4">
      <Card className="w-full max-w-md p-8">
        {/* Logo & Title */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-[#409EFF] rounded-lg mb-4">
            <span className="text-white font-bold text-3xl">D</span>
          </div>
          <h1 className="text-2xl font-bold text-[#303133] mb-2">欢迎回来</h1>
          <p className="text-sm text-[#909399]">登录到 doinb 视频平台</p>
        </div>

        {/* Login Form */}
        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm text-[#606266] mb-2">账号</label>
            <Input
              type="text"
              placeholder="请输入账号"
              value={formData.username}
              onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              required
            />
          </div>

          <div>
            <label className="block text-sm text-[#606266] mb-2">密码</label>
            <Input
              type="password"
              placeholder="请输入密码"
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              required
            />
          </div>

          <div className="flex items-center justify-between text-sm">
            <label className="flex items-center gap-2 text-[#606266] cursor-pointer">
              <input type="checkbox" className="w-4 h-4 rounded border-[#DCDFE6]" />
              <span>记住我</span>
            </label>
            <a href="#" className="text-[#409EFF] hover:text-[#66b1ff]">
              忘记密码？
            </a>
          </div>

          <Button type="submit" variant="primary" className="w-full">
            登录
          </Button>
        </form>

        {/* Register Link */}
        <div className="mt-6 text-center text-sm text-[#909399]">
          还没有账号？
          <Link to="/register" className="text-[#409EFF] hover:text-[#66b1ff] ml-1">
            立即注册
          </Link>
        </div>
      </Card>
    </div>
  );
}
