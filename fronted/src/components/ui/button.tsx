import * as React from "react"
import { Slot } from "@radix-ui/react-slot"
import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const buttonVariants = cva(
  "inline-flex items-center justify-center whitespace-nowrap font-medium transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#E85D4E]/30 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-40 active:translate-x-[2px] active:translate-y-[2px] active:shadow-none",
  {
    variants: {
      variant: {
        default:
          "bg-[#E85D4E] text-white border-2 border-[#E85D4E] shadow-[2px_2px_0_rgba(26,26,26,0.08)] hover:bg-[#d44d3e] hover:border-[#d44d3e] hover:shadow-[4px_4px_0_rgba(26,26,26,0.10)]",
        destructive:
          "bg-[#E85D4E] text-white border-2 border-[#E85D4E] shadow-[2px_2px_0_rgba(26,26,26,0.08)] hover:bg-[#c0392b] hover:border-[#c0392b]",
        outline:
          "bg-white text-[#1A1A1A] border-2 border-[rgba(26,26,26,0.15)] shadow-[2px_2px_0_rgba(26,26,26,0.04)] hover:bg-[#F5F5F0] hover:border-[rgba(26,26,26,0.22)] hover:shadow-[4px_4px_0_rgba(26,26,26,0.06)]",
        secondary:
          "bg-[#C5B5E0] text-[#1A1A1A] border-2 border-[#C5B5E0] shadow-[2px_2px_0_rgba(26,26,26,0.04)] hover:bg-[#b5a5d0] hover:shadow-[4px_4px_0_rgba(26,26,26,0.06)]",
        ghost:
          "text-[#1A1A1A] hover:bg-[#F5F5F0] border-2 border-transparent",
        link: "text-[#E85D4E] underline-offset-4 hover:underline border-0 shadow-none",
        /* Capsule 彩色变体 */
        lime:
          "bg-[#C4D94E] text-[#1A1A1A] border-2 border-[#C4D94E] shadow-[2px_2px_0_rgba(26,26,26,0.04)] hover:shadow-[4px_4px_0_rgba(26,26,26,0.06)]",
        sky:
          "bg-[#8BB4F7] text-white border-2 border-[#8BB4F7] shadow-[2px_2px_0_rgba(26,26,26,0.04)] hover:shadow-[4px_4px_0_rgba(26,26,26,0.06)]",
        yellow:
          "bg-[#F2D160] text-[#1A1A1A] border-2 border-[#F2D160] shadow-[2px_2px_0_rgba(26,26,26,0.04)] hover:shadow-[4px_4px_0_rgba(26,26,26,0.06)]",
        mint:
          "bg-[#A8E6CF] text-[#1A1A1A] border-2 border-[#A8E6CF] shadow-[2px_2px_0_rgba(26,26,26,0.04)] hover:shadow-[4px_4px_0_rgba(26,26,26,0.06)]",
      },
      size: {
        default: "h-10 px-6 py-2 rounded-[9999px] text-sm",
        sm: "h-9 px-4 py-1.5 rounded-[9999px] text-xs",
        lg: "h-12 px-8 py-3 rounded-[9999px] text-base",
        icon: "h-10 w-10 rounded-full",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
)

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button"
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    )
  },
)
Button.displayName = "Button"

export { Button, buttonVariants }
