import { FeaturesSection } from '@/components/landing/FeaturesSection'
import { FinalCtaSection } from '@/components/landing/FinalCtaSection'
import { LandingFooter } from '@/components/landing/LandingFooter'
import { LandingHeader } from '@/components/landing/LandingHeader'
import { LandingHero } from '@/components/landing/LandingHero'
import { ProblemsSection } from '@/components/landing/ProblemsSection'
import { TrustSection } from '@/components/landing/TrustSection'

export function LandingPage() {
  return (
    <div className="min-h-screen bg-ivory text-ink">
      <LandingHeader />
      <main>
        <LandingHero />
        <ProblemsSection />
        <FeaturesSection />
        <TrustSection />
        <FinalCtaSection />
      </main>
      <LandingFooter />
    </div>
  )
}