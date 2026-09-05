import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost'
type Size = 'sm' | 'md'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
  children: ReactNode
}

const base =
  'inline-flex cursor-pointer items-center justify-center gap-2 uppercase transition-colors duration-150 ease-in-out disabled:cursor-not-allowed disabled:opacity-40'

const variants: Record<Variant, string> = {
  primary: 'bg-sage text-carbon hover:bg-sage-dark',
  secondary: 'border border-hairline text-ink hover:border-ink hover:bg-ink hover:text-ivory',
  danger: 'border border-red-700/40 text-red-700 hover:bg-red-700 hover:text-ivory',
  dangerText: 'text-red-700 underline-offset-4 hover:text-red-900 hover:underline',
  ghost: 'text-ink-soft underline-offset-4 hover:text-ink hover:underline',
}

const sizes: Record<Size, string> = {
  sm: 'px-3 py-1.5 text-[11px] font-medium tracking-[0.18em]',
  md: 'px-5 py-2.5 text-[11px] font-medium tracking-[0.18em]',
}

export function Button({ variant = 'primary', size = 'md', className = '', ...props }: ButtonProps) {
  return <button className={`${base} ${variants[variant]} ${sizes[size]} ${className}`} {...props} />
}