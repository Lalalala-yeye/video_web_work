import { useState } from 'react';
import { Upload as UploadIcon, Image as ImageIcon, X } from 'lucide-react';
import { TopNavigation } from '../components/TopNavigation';
import { Card } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';

export function Upload() {
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    category: '',
  });

  const [uploadProgress, setUploadProgress] = useState(0);
  const [isUploading, setIsUploading] = useState(false);
  const [videoFile, setVideoFile] = useState<string | null>(null);
  const [coverFile, setCoverFile] = useState<string | null>(null);

  const handleVideoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setVideoFile(file.name);
      // 模拟上传进度
      setIsUploading(true);
      let progress = 0;
      const interval = setInterval(() => {
        progress += 10;
        setUploadProgress(progress);
        if (progress >= 100) {
          clearInterval(interval);
          setIsUploading(false);
        }
      }, 300);
    }
  };

  const handleCoverUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setCoverFile(URL.createObjectURL(file));
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    console.log('提交视频:', { ...formData, videoFile, coverFile });
  };

  return (
    <div className="min-h-screen bg-[#F5F7FA]">
      <TopNavigation
        isLoggedIn={true}
        userInfo={{ name: '测试用户', avatar: undefined }}
        onSearch={(query) => console.log('搜索:', query)}
      />

      <main className="max-w-[900px] mx-auto px-4 pt-20 pb-8">
        {/* 页面标题 */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-[#303133]">上传视频</h1>
          <p className="text-sm text-[#909399] mt-1">分享你的精彩内容</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* 视频上传区 */}
          <Card className="p-6">
            <h2 className="text-lg font-bold text-[#303133] mb-4">视频文件</h2>

            {!videoFile ? (
              <label className="block cursor-pointer">
                <div className="border-2 border-dashed border-[#DCDFE6] rounded-lg p-12 hover:border-[#409EFF] hover:bg-[#ecf5ff] transition-colors">
                  <div className="flex flex-col items-center gap-3">
                    <div className="w-16 h-16 rounded-full bg-[#ecf5ff] flex items-center justify-center">
                      <UploadIcon size={32} className="text-[#409EFF]" />
                    </div>
                    <div className="text-center">
                      <p className="text-base text-[#303133] font-medium mb-1">
                        点击或拖拽文件到此区域上传
                      </p>
                      <p className="text-sm text-[#909399]">
                        支持 MP4, MOV, AVI 格式，文件大小不超过 2GB
                      </p>
                    </div>
                  </div>
                </div>
                <input
                  type="file"
                  accept="video/*"
                  className="hidden"
                  onChange={handleVideoUpload}
                />
              </label>
            ) : (
              <div className="space-y-3">
                <div className="flex items-center justify-between p-4 bg-[#F5F7FA] rounded">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-[#409EFF] rounded flex items-center justify-center">
                      <UploadIcon size={20} className="text-white" />
                    </div>
                    <div>
                      <p className="text-sm font-medium text-[#303133]">{videoFile}</p>
                      <p className="text-xs text-[#909399]">
                        {isUploading ? `上传中 ${uploadProgress}%` : '上传完成'}
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setVideoFile(null);
                      setUploadProgress(0);
                    }}
                    className="text-[#909399] hover:text-[#F56C6C]"
                  >
                    <X size={20} />
                  </button>
                </div>

                {/* 上传进度条 */}
                {isUploading && (
                  <div className="w-full h-2 bg-[#EBEEF5] rounded-full overflow-hidden">
                    <div
                      className="h-full bg-[#409EFF] transition-all duration-300"
                      style={{ width: `${uploadProgress}%` }}
                    />
                  </div>
                )}
              </div>
            )}
          </Card>

          {/* 封面上传 */}
          <Card className="p-6">
            <h2 className="text-lg font-bold text-[#303133] mb-4">封面图</h2>

            <div className="flex gap-4">
              {coverFile ? (
                <div className="relative w-64 aspect-video">
                  <img
                    src={coverFile}
                    alt="封面预览"
                    className="w-full h-full object-cover rounded"
                  />
                  <button
                    type="button"
                    onClick={() => setCoverFile(null)}
                    className="absolute top-2 right-2 w-6 h-6 bg-black/60 rounded-full flex items-center justify-center text-white hover:bg-black/80"
                  >
                    <X size={14} />
                  </button>
                </div>
              ) : (
                <label className="w-64 aspect-video border-2 border-dashed border-[#DCDFE6] rounded-lg flex flex-col items-center justify-center cursor-pointer hover:border-[#409EFF] hover:bg-[#ecf5ff] transition-colors">
                  <ImageIcon size={32} className="text-[#C0C4CC] mb-2" />
                  <p className="text-sm text-[#909399]">上传封面</p>
                  <p className="text-xs text-[#C0C4CC] mt-1">建议 16:9</p>
                  <input
                    type="file"
                    accept="image/*"
                    className="hidden"
                    onChange={handleCoverUpload}
                  />
                </label>
              )}
            </div>
          </Card>

          {/* 视频信息 */}
          <Card className="p-6">
            <h2 className="text-lg font-bold text-[#303133] mb-4">视频信息</h2>

            <div className="space-y-4">
              <div>
                <label className="block text-sm text-[#606266] mb-2">标题 *</label>
                <Input
                  type="text"
                  placeholder="请输入视频标题"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  required
                />
              </div>

              <div>
                <label className="block text-sm text-[#606266] mb-2">简介</label>
                <textarea
                  placeholder="介绍一下你的视频内容"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full h-32 px-3 py-2 text-base rounded border border-[#DCDFE6] hover:border-[#C0C4CC] focus:border-[#409EFF] outline-none transition-colors resize-none"
                />
              </div>

              <div>
                <label className="block text-sm text-[#606266] mb-2">分类 *</label>
                <select
                  value={formData.category}
                  onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                  className="w-full h-10 px-3 text-base rounded border border-[#DCDFE6] hover:border-[#C0C4CC] focus:border-[#409EFF] outline-none transition-colors"
                  required
                >
                  <option value="">请选择分类</option>
                  <option value="tech">科技</option>
                  <option value="game">游戏</option>
                  <option value="study">学习</option>
                  <option value="music">音乐</option>
                  <option value="life">生活</option>
                  <option value="other">其他</option>
                </select>
              </div>
            </div>
          </Card>

          {/* 提交按钮 */}
          <div className="flex items-center justify-end gap-3">
            <Button type="button" variant="secondary" size="large">
              保存草稿
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="large"
              disabled={!videoFile || !formData.title || !formData.category || isUploading}
            >
              发布视频
            </Button>
          </div>
        </form>
      </main>
    </div>
  );
}
