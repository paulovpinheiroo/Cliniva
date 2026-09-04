import type { InputHTMLAttributes } from 'react'

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
}

export function TextField({ label, error, className = '', ...props }: TextFieldProps) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-sm font-medium text-lilac">{label}</span>
      <input
        className={`rounded-lg border border-borderline bg-carbon px-3 py-2 text-white placeholder:text-slate-500 outline-none transition-colors focus:border-sage ${className}`}
        {...props}
      />
      {error && <span className="text-xs text-red-400">{error}</span>}
    </label>
  )
}