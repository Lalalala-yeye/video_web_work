import { useState } from 'react';
import { Link } from 'react-router';
import { Card } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';

export function Register() {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
  });

  const [errors, setErrors] = useState({
    username: false,
    password: false,
    confirmPassword: false,
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // 简单验证
    const newErrors = {
      username: formData.username.length < 3,
      password: formData.password.length < 6,
      confirmPassword: formData.password !== formData.confirmPassword,
    };

    setErrors(newErrors);

    if (!Object.values(newErrors).some((error) => error)) {
      console.log('注册:', formData);
      // 这里添加注册逻辑
    }
  };

  return (
    <div className="min-h-screen bg-[#F5F7FA] flex items-center justify-center p-4">
      <Card className="w-full max-w-md p-8">
        {/* Logo & Title */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-[#409EFF] rounded-lg mb-4">
            <span className="text-white font-bold text-3xl">D</span>
          </div>
          <h1 className="text-2xl font-bold text-[#303133] mb-2">创建账号</h1>
          <p className="text-sm text-[#909399]">加入 doinb 视频平台</p>
        </div>

        {/* Register Form */}
        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm text-[#606266] mb-2">账号</label>
            <Input
              type="text"
              placeholder="请输入账号（至少3个字符）"
              value={formData.username}
              onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              error={errors.username}
              required
            />
            {errors.username && (
              <p className="mt-1 text-xs text-[#F56C6C]">账号至少需要3个字符</p>
            )}
          </div>

          <div>
            <label className="block text-sm text-[#606266] mb-2">密码</label>
            <Input
              type="password"
              placeholder="请输入密码（至少6个字符）"
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              error={errors.password}
              required
            />
            {errors.password && (
              <p className="mt-1 text-xs text-[#F56C6C]">密码至少需要6个字符</p>
            )}
          </div>

          <div>
            <label className="block text-sm text-[#606266] mb-2">确认密码</label>
            <Input
              type="password"
              placeholder="请再次输入密码"
              value={formData.confirmPassword}
              onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
              error={errors.confirmPassword}
              required
            />
            {errors.confirmPassword && (
              <p className="mt-1 text-xs text-[#F56C6C]">两次输入的密码不一致</p>
            )}
          </div>

          <div className="text-xs text-[#909399]">
            注册即表示您同意我们的
            <a href="#" className="text-[#409EFF] hover:text-[#66b1ff]">
              服务条款
            </a>
            和
            <a href="#" className="text-[#409EFF] hover:text-[#66b1ff]">
              隐私政策
            </a>
          </div>

          <Button type="submit" variant="primary" className="w-full">
            注册
          </Button>
        </form>

        {/* Login Link */}
        <div className="mt-6 text-center text-sm text-[#909399]">
          已有账号？
          <Link to="/login" className="text-[#409EFF] hover:text-[#66b1ff] ml-1">
            立即登录
          </Link>
        </div>
      </Card>
    </div>
  );
}
