import type { InputHTMLAttributes } from 'react'

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
}

export function TextField({ label, error, className = '', ...props }: TextFieldProps) {
  return (
    <label className="flex flex-col">
      <span className="mb-1 text-[11px] font-medium uppercase tracking-[0.18em] text-ink-soft">
        {label}
      </span>
      <input
        className={`w-full border-0 border-b border-hairline bg-transparent px-0 py-2.5 text-sm text-ink outline-none transition-colors duration-150 ease-in-out placeholder:text-ink-soft/50 hover:border-ink/50 focus:border-accent ${
          error ? 'border-b-red-600' : ''
        } ${className}`}
        {...props}
      />
      {error && <span className="mt-1 text-xs text-red-600">{error}</span>}
    </label>
  )
}