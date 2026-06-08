import { HTMLAttributes } from 'react';

interface EmptyProps extends HTMLAttributes<HTMLDivElement> {
  description?: string;
  image?: 'noData' | 'noSearch' | 'noAuth';
}

export function Empty({ description = '暂无数据', image = 'noData', className = '', ...props }: EmptyProps) {
  const images = {
    noData: (
      <svg className="w-32 h-32 text-[#C0C4CC]" fill="none" viewBox="0 0 128 128">
        <circle cx="64" cy="64" r="60" stroke="currentColor" strokeWidth="2" fill="none" opacity="0.3" />
        <path d="M64 32v64M32 64h64" stroke="currentColor" strokeWidth="2" strokeLinecap="round" opacity="0.5" />
        <circle cx="64" cy="64" r="8" fill="currentColor" opacity="0.3" />
      </svg>
    ),
    noSearch: (
      <svg className="w-32 h-32 text-[#C0C4CC]" fill="none" viewBox="0 0 128 128">
        <circle cx="52" cy="52" r="28" stroke="currentColor" strokeWidth="3" />
        <path d="M72 72l24 24" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
        <path d="M40 52h24M52 40v24" stroke="currentColor" strokeWidth="2" strokeLinecap="round" opacity="0.5" />
      </svg>
    ),
    noAuth: (
      <svg className="w-32 h-32 text-[#C0C4CC]" fill="none" viewBox="0 0 128 128">
        <rect x="32" y="52" width="64" height="44" rx="4" stroke="currentColor" strokeWidth="3" />
        <path d="M48 52V40c0-8.837 7.163-16 16-16s16 7.163 16 16v12" stroke="currentColor" strokeWidth="3" />
        <circle cx="64" cy="74" r="6" fill="currentColor" />
        <path d="M64 80v8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      </svg>
    )
  };

  return (
    <div className={`flex flex-col items-center justify-center py-16 ${className}`} {...props}>
      {images[image]}
      <p className="mt-4 text-[#909399] text-sm">{description}</p>
    </div>
  );
}
