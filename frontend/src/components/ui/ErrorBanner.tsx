interface ErrorBannerProps {
  message: string
}

export function ErrorBanner({ message }: ErrorBannerProps) {
  return (
    <div className="mb-6 border border-red-200 bg-red-50/60 px-4 py-3 text-sm text-red-700">
      {message}
    </div>
  )
}