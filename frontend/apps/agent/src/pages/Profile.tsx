import { User, Phone, Mail, LogOut } from 'lucide-react'
import { Card, CardContent, Button, Avatar, AvatarFallback } from '@loqal/ui'

export function Profile() {
  return (
    <div className="space-y-6 pt-2">
      <header>
        <h1 className="font-display text-3xl font-semibold text-foreground">
          Profile
        </h1>
      </header>

      <div className="flex items-center gap-4 rounded-card border border-border bg-surface p-5 shadow-sm">
        <Avatar className="h-16 w-16">
          <AvatarFallback className="bg-accent-light text-accent font-display text-2xl">
            LA
          </AvatarFallback>
        </Avatar>
        <div>
          <p className="font-display text-xl font-semibold text-foreground">
            Delivery Agent
          </p>
          <p className="font-body text-sm text-foreground-secondary">
            Active courier · Loqal
          </p>
        </div>
      </div>

      <Card className="shadow-sm">
        <CardContent className="divide-y divide-border py-2">
          <div className="flex items-center gap-3 py-3">
            <User size={18} className="text-accent" />
            <span className="font-body text-sm text-foreground">Agent ID</span>
            <span className="ml-auto font-body text-sm text-foreground-secondary">
              00000000-0000-0000-0000-000000000002
            </span>
          </div>
          <div className="flex items-center gap-3 py-3">
            <Phone size={18} className="text-accent" />
            <span className="font-body text-sm text-foreground">+91 90000 00000</span>
          </div>
          <div className="flex items-center gap-3 py-3">
            <Mail size={18} className="text-accent" />
            <span className="font-body text-sm text-foreground">
              agent@loqal.shop
            </span>
          </div>
        </CardContent>
      </Card>

      <Button variant="secondary" className="w-full py-4">
        <LogOut size={18} className="mr-2" />
        Sign Out
      </Button>
    </div>
  )
}
