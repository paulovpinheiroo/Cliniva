import type { ReactNode, SelectHTMLAttributes } from 'react'

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  children: ReactNode
}

export function Select({ label, children, className = '', ...props }: SelectProps) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-sm font-medium text-lilac">{label}</span>
      <select
        className={`rounded-lg border border-borderline bg-carbon px-3 py-2 text-white outline-none transition-colors focus:border-sage ${className}`}
        {...props}
      >
        {children}
      </select>
    </label>
  )
}