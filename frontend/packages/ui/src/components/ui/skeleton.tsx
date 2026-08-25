import * as React from "react"

import { cn } from "../../lib/utils"

function Skeleton({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("animate-pulse rounded-card bg-border", className)}
      {...props}
    />
  )
}

export { Skeleton }
