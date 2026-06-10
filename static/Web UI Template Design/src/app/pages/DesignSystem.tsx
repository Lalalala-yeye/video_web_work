import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Card } from '../components/ui/Card';
import { Tag } from '../components/ui/Tag';
import { Avatar } from '../components/ui/Avatar';
import { Empty } from '../components/ui/Empty';

export function DesignSystem() {
  const colors = [
    { name: '主色', value: '#409EFF', var: '--primary' },
    { name: '成功', value: '#67C23A', var: '--success' },
    { name: '警告', value: '#E6A23C', var: '--warning' },
    { name: '错误', value: '#F56C6C', var: '--danger' },
    { name: '信息', value: '#909399', var: '--info' },
  ];

  const textColors = [
    { name: '主要文字', value: '#303133', var: '--text-primary' },
    { name: '常规文字', value: '#606266', var: '--text-regular' },
    { name: '次要文字', value: '#909399', var: '--text-secondary' },
    { name: '占位文字', value: '#C0C4CC', var: '--text-placeholder' },
  ];

  const borderColors = [
    { name: '基础边框', value: '#DCDFE6', var: '--border-base' },
    { name: '浅色边框', value: '#E4E7ED', var: '--border-light' },
    { name: '更浅边框', value: '#EBEEF5', var: '--border-lighter' },
  ];

  const bgColors = [
    { name: '页面背景', value: '#F5F7FA', var: '--background' },
    { name: '卡片背景', value: '#FFFFFF', var: '--card' },
    { name: '填充色', value: '#F0F2F5', var: '--fill-base' },
  ];

  return (
    <div className="min-h-screen bg-[#F5F7FA] py-8">
      <div className="max-w-[1280px] mx-auto px-4">
        {/* 标题 */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-[#303133] mb-2">doinb 设计系统</h1>
          <p className="text-base text-[#909399]">Element Plus 风格 · 视频平台设计规范</p>
        </div>

        {/* 颜色系统 */}
        <section className="mb-8">
          <h2 className="text-2xl font-bold text-[#303133] mb-4">颜色系统</h2>

          {/* 主题色 */}
          <Card className="p-6 mb-4">
            <h3 className="text-lg font-bold text-[#303133] mb-4">主题色</h3>
            <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
              {colors.map((color) => (
                <div key={color.name}>
                  <div
                    className="h-24 rounded-lg mb-2"
                    style={{ backgroundColor: color.value }}
                  />
                  <p className="text-sm font-medium text-[#303133]">{color.name}</p>
                  <p className="text-xs text-[#909399]">{color.value}</p>
                  <p className="text-xs text-[#C0C4CC] font-mono">{color.var}</p>
                </div>
              ))}
            </div>
          </Card>

          {/* 文字颜色 */}
          <Card className="p-6 mb-4">
            <h3 className="text-lg font-bold text-[#303133] mb-4">文字颜色</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {textColors.map((color) => (
                <div key={color.name}>
                  <div
                    className="h-16 rounded-lg mb-2 flex items-center justify-center"
                    style={{ backgroundColor: color.value }}
                  >
                    <span className="text-white font-medium">Aa</span>
                  </div>
                  <p className="text-sm font-medium text-[#303133]">{color.name}</p>
                  <p className="text-xs text-[#909399]">{color.value}</p>
                </div>
              ))}
            </div>
          </Card>

          {/* 边框和背景 */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Card className="p-6">
              <h3 className="text-lg font-bold text-[#303133] mb-4">边框颜色</h3>
              <div className="space-y-3">
                {borderColors.map((color) => (
                  <div key={color.name} className="flex items-center gap-3">
                    <div
                      className="w-24 h-10 rounded border-2"
                      style={{ borderColor: color.value }}
                    />
                    <div>
                      <p className="text-sm font-medium text-[#303133]">{color.name}</p>
                      <p className="text-xs text-[#909399]">{color.value}</p>
                    </div>
                  </div>
                ))}
              </div>
            </Card>

            <Card className="p-6">
              <h3 className="text-lg font-bold text-[#303133] mb-4">背景颜色</h3>
              <div className="space-y-3">
                {bgColors.map((color) => (
                  <div key={color.name} className="flex items-center gap-3">
                    <div
                      className="w-24 h-10 rounded border border-[#DCDFE6]"
                      style={{ backgroundColor: color.value }}
                    />
                    <div>
                      <p className="text-sm font-medium text-[#303133]">{color.name}</p>
                      <p className="text-xs text-[#909399]">{color.value}</p>
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          </div>
        </section>

        {/* 字体系统 */}
        <section className="mb-8">
          <h2 className="text-2xl font-bold text-[#303133] mb-4">字体系统</h2>
          <Card className="p-6">
            <div className="space-y-4">
              <div className="pb-4 border-b border-[#EBEEF5]">
                <h1>标题 H1 - 24px / Medium</h1>
                <p className="text-xs text-[#909399] mt-1">font-size: 1.5rem (24px) · font-weight: 500</p>
              </div>
              <div className="pb-4 border-b border-[#EBEEF5]">
                <h2>标题 H2 - 20px / Medium</h2>
                <p className="text-xs text-[#909399] mt-1">font-size: 1.25rem (20px) · font-weight: 500</p>
              </div>
              <div className="pb-4 border-b border-[#EBEEF5]">
                <h3>标题 H3 - 18px / Medium</h3>
                <p className="text-xs text-[#909399] mt-1">font-size: 1.125rem (18px) · font-weight: 500</p>
              </div>
              <div className="pb-4 border-b border-[#EBEEF5]">
                <p className="text-base">正文 - 16px / Normal</p>
                <p className="text-xs text-[#909399] mt-1">font-size: 1rem (16px) · font-weight: 400</p>
              </div>
              <div>
                <p className="text-sm text-[#909399]">辅助说明 - 14px / Normal</p>
                <p className="text-xs text-[#C0C4CC] mt-1">font-size: 0.875rem (14px) · font-weight: 400</p>
              </div>
            </div>
          </Card>
        </section>

        {/* 圆角和间距 */}
        <section className="mb-8">
          <h2 className="text-2xl font-bold text-[#303133] mb-4">圆角和间距</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Card className="p-6">
              <h3 className="text-lg font-bold text-[#303133] mb-4">圆角 Border Radius</h3>
              <div className="space-y-3">
                <div className="flex items-center gap-3">
                  <div className="w-16 h-16 bg-[#409EFF] rounded" />
                  <div>
                    <p className="text-sm font-medium">小圆角 - 4px</p>
                    <p className="text-xs text-[#909399]">border-radius: 0.25rem</p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <div className="w-16 h-16 bg-[#67C23A] rounded-md" />
                  <div>
                    <p className="text-sm font-medium">中圆角 - 6px</p>
                    <p className="text-xs text-[#909399]">border-radius: 0.375rem</p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <div className="w-16 h-16 bg-[#E6A23C] rounded-lg" />
                  <div>
                    <p className="text-sm font-medium">大圆角 - 8px</p>
                    <p className="text-xs text-[#909399]">border-radius: 0.5rem</p>
                  </div>
                </div>
              </div>
            </Card>

            <Card className="p-6">
              <h3 className="text-lg font-bold text-[#303133] mb-4">间距 Spacing (8px 栅格)</h3>
              <div className="space-y-3">
                {[8, 16, 24, 32, 48].map((size) => (
                  <div key={size} className="flex items-center gap-3">
                    <div
                      className="h-6 bg-[#409EFF]"
                      style={{ width: size }}
                    />
                    <p className="text-sm text-[#606266]">{size}px</p>
                  </div>
                ))}
              </div>
            </Card>
          </div>
        </section>

        {/* 组件展示 */}
        <section className="mb-8">
          <h2 className="text-2xl font-bold text-[#303133] mb-4">基础组件</h2>

          {/* 按钮 */}
          <Card className="p-6 mb-4">
            <h3 className="text-lg font-bold text-[#303133] mb-4">按钮 Button</h3>
            <div className="space-y-4">
              <div>
                <p className="text-sm text-[#909399] mb-3">主要按钮</p>
                <div className="flex flex-wrap items-center gap-3">
                  <Button variant="primary" size="small">小按钮</Button>
                  <Button variant="primary" size="medium">中等按钮</Button>
                  <Button variant="primary" size="large">大按钮</Button>
                  <Button variant="primary" size="medium" disabled>禁用状态</Button>
                </div>
              </div>
              <div>
                <p className="text-sm text-[#909399] mb-3">次要按钮</p>
                <div className="flex flex-wrap items-center gap-3">
                  <Button variant="secondary" size="medium">次要按钮</Button>
                  <Button variant="text" size="medium">文字按钮</Button>
                </div>
              </div>
            </div>
          </Card>

          {/* 输入框 */}
          <Card className="p-6 mb-4">
            <h3 className="text-lg font-bold text-[#303133] mb-4">输入框 Input</h3>
            <div className="max-w-md space-y-3">
              <Input placeholder="普通输入框" />
              <Input placeholder="错误状态输入框" error />
              <Input placeholder="禁用状态" disabled />
            </div>
          </Card>

          {/* 标签 */}
          <Card className="p-6 mb-4">
            <h3 className="text-lg font-bold text-[#303133] mb-4">标签 Tag</h3>
            <div className="flex flex-wrap items-center gap-3">
              <Tag type="success">成功</Tag>
              <Tag type="warning">警告</Tag>
              <Tag type="danger">危险</Tag>
              <Tag type="info">信息</Tag>
              <Tag type="live">直播中</Tag>
              <Tag type="success" size="small">小标签</Tag>
            </div>
          </Card>

          {/* 头像 */}
          <Card className="p-6 mb-4">
            <h3 className="text-lg font-bold text-[#303133] mb-4">头像 Avatar</h3>
            <div className="flex items-center gap-4">
              <Avatar size={32} name="小头像" />
              <Avatar size={48} name="中头像" />
              <Avatar size={64} name="大头像" />
              <Avatar size={80} name="超大头像" />
            </div>
          </Card>

          {/* 空状态 */}
          <Card className="p-6">
            <h3 className="text-lg font-bold text-[#303133] mb-4">空状态 Empty</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <Empty image="noData" description="暂无数据" />
              <Empty image="noSearch" description="搜索无结果" />
              <Empty image="noAuth" description="请先登录" />
            </div>
          </Card>
        </section>
      </div>
    </div>
  );
}
