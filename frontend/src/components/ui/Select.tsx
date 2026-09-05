import type { ReactNode, SelectHTMLAttributes } from 'react'

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  children: ReactNode
}

export function Select({ label, children, className = '', ...props }: SelectProps) {
  return (
    <label className="flex flex-col">
      <span className="mb-1 text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">
        {label}
      </span>
      <select
        className={`w-full cursor-pointer border-0 border-b border-hairline bg-transparent px-0 py-2 text-sm text-ink outline-none transition-colors duration-150 ease-in-out hover:border-ink/50 focus:border-sage ${className}`}
        {...props}
      >
        {children}
      </select>
    </label>
  )
}