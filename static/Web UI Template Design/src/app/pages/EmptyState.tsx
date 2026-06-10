import { Link } from 'react-router';
import { TopNavigation } from '../components/TopNavigation';
import { Empty } from '../components/ui/Empty';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';

export function EmptyState() {
  return (
    <div className="min-h-screen bg-[#F5F7FA]">
      <TopNavigation
        isLoggedIn={false}
        onSearch={(query) => console.log('搜索:', query)}
      />

      <main className="max-w-[1280px] mx-auto px-4 pt-20 pb-8">
        {/* 页面标题 */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-[#303133]">空状态页面示例</h1>
          <p className="text-sm text-[#909399] mt-1">展示各种空状态和错误提示</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {/* 无视频状态 */}
          <Card className="p-6">
            <h3 className="text-base font-bold text-[#303133] mb-4">无视频</h3>
            <Empty image="noData" description="暂无视频内容" />
            <div className="mt-4 text-center">
              <Link to="/home">
                <Button variant="primary" size="medium">
                  去首页看看
                </Button>
              </Link>
            </div>
          </Card>

          {/* 搜索无结果 */}
          <Card className="p-6">
            <h3 className="text-base font-bold text-[#303133] mb-4">搜索无结果</h3>
            <Empty image="noSearch" description="没有找到相关内容" />
            <div className="mt-4 text-center">
              <p className="text-sm text-[#909399] mb-3">试试其他关键词</p>
              <Link to="/search">
                <Button variant="secondary" size="medium">
                  重新搜索
                </Button>
              </Link>
            </div>
          </Card>

          {/* 未登录提示 */}
          <Card className="p-6">
            <h3 className="text-base font-bold text-[#303133] mb-4">未登录</h3>
            <Empty image="noAuth" description="请先登录账号" />
            <div className="mt-4 text-center">
              <Link to="/login">
                <Button variant="primary" size="medium">
                  立即登录
                </Button>
              </Link>
            </div>
          </Card>

          {/* 无订阅 */}
          <Card className="p-6">
            <h3 className="text-base font-bold text-[#303133] mb-4">无订阅</h3>
            <Empty image="noData" description="还没有关注任何UP主" />
            <div className="mt-4 text-center">
              <Link to="/home">
                <Button variant="primary" size="medium">
                  发现更多
                </Button>
              </Link>
            </div>
          </Card>

          {/* 无播放历史 */}
          <Card className="p-6">
            <h3 className="text-base font-bold text-[#303133] mb-4">无播放历史</h3>
            <Empty image="noData" description="暂无播放历史记录" />
            <div className="mt-4 text-center">
              <Link to="/home">
                <Button variant="secondary" size="medium">
                  开始观看
                </Button>
              </Link>
            </div>
          </Card>

          {/* 404 页面 */}
          <Card className="p-6">
            <h3 className="text-base font-bold text-[#303133] mb-4">404 页面未找到</h3>
            <Empty image="noSearch" description="页面不存在或已被删除" />
            <div className="mt-4 text-center">
              <Link to="/home">
                <Button variant="primary" size="medium">
                  返回首页
                </Button>
              </Link>
            </div>
          </Card>
        </div>

        {/* 全屏空状态示例 */}
        <div className="mt-8">
          <h2 className="text-xl font-bold text-[#303133] mb-4">全屏空状态示例</h2>
          <Card className="p-12">
            <Empty image="noData" description="这是一个全屏空状态示例" />
            <div className="mt-6 flex items-center justify-center gap-3">
              <Link to="/home">
                <Button variant="primary" size="large">
                  返回首页
                </Button>
              </Link>
              <Link to="/search">
                <Button variant="secondary" size="large">
                  搜索内容
                </Button>
              </Link>
            </div>
          </Card>
        </div>
      </main>
    </div>
  );
}
