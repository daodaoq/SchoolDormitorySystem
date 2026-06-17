import * as React from 'react';
import { cn } from '@/lib/utils';
import { cva, type VariantProps } from 'class-variance-authority';
import { X } from 'lucide-react';

const alertVariants = cva('flex items-stretch w-full gap-2', {
  variants: {
    variant: {
      secondary: '',
      primary: '',
      destructive: '',
      success: '',
      info: '',
      warning: '',
    },
    appearance: {
      solid: '',
      outline: '',
      light: '',
    },
    size: {
      lg: 'rounded-lg p-4 gap-3 text-base [&>[data-slot=alert-icon]>svg]:size-6 *:data-slot=alert-icon:mt-0.5',
      md: 'rounded-lg p-3.5 gap-2.5 text-sm [&>[data-slot=alert-icon]>svg]:size-5 *:data-slot=alert-icon:mt-0',
      sm: 'rounded-md px-3 py-2.5 gap-2 text-xs [&>[data-slot=alert-icon]>svg]:size-4',
    },
  },
  compoundVariants: [
    /* Solid */
    { variant: 'secondary', appearance: 'solid', className: 'bg-muted text-foreground' },
    { variant: 'primary', appearance: 'solid', className: 'bg-primary text-primary-foreground' },
    { variant: 'destructive', appearance: 'solid', className: 'bg-destructive text-destructive-foreground' },
    { variant: 'success', appearance: 'solid', className: 'bg-green-500 text-white' },
    { variant: 'info', appearance: 'solid', className: 'bg-blue-600 text-white' },
    { variant: 'warning', appearance: 'solid', className: 'bg-yellow-500 text-white' },

    /* Outline */
    { variant: 'secondary', appearance: 'outline', className: 'border bg-background text-foreground' },
    { variant: 'primary', appearance: 'outline', className: 'border bg-background text-primary' },
    { variant: 'destructive', appearance: 'outline', className: 'border bg-background text-destructive' },
    { variant: 'success', appearance: 'outline', className: 'border bg-background text-green-600' },
    { variant: 'info', appearance: 'outline', className: 'border bg-background text-blue-600' },
    { variant: 'warning', appearance: 'outline', className: 'border bg-background text-yellow-600' },

    /* Light */
    { variant: 'secondary', appearance: 'light', className: 'bg-muted/60 text-foreground' },
    { variant: 'primary', appearance: 'light', className: 'bg-blue-50 text-blue-800 border border-blue-100' },
    { variant: 'destructive', appearance: 'light', className: 'bg-red-50 text-red-800 border border-red-100' },
    { variant: 'success', appearance: 'light', className: 'bg-green-50 text-green-800 border border-green-100' },
    { variant: 'info', appearance: 'light', className: 'bg-blue-50 text-blue-800 border border-blue-100' },
    { variant: 'warning', appearance: 'light', className: 'bg-yellow-50 text-yellow-800 border border-yellow-100' },
  ],
  defaultVariants: {
    variant: 'secondary',
    appearance: 'light',
    size: 'md',
  },
});

interface AlertProps extends React.HTMLAttributes<HTMLDivElement>, VariantProps<typeof alertVariants> {
  close?: boolean;
  onClose?: () => void;
}

function Alert({
  className,
  variant,
  size,
  appearance,
  close = false,
  onClose,
  children,
  ...props
}: AlertProps) {
  return (
    <div
      data-slot="alert"
      role="alert"
      className={cn(alertVariants({ variant, size, appearance }), className)}
      {...props}
    >
      {children}
      {close && (
        <button
          onClick={onClose}
          aria-label="关闭"
          data-slot="alert-close"
          className="ml-auto shrink-0 rounded-sm opacity-60 transition-opacity hover:opacity-100"
        >
          <X className="size-4" />
        </button>
      )}
    </div>
  );
}

function AlertTitle({ className, ...props }: React.HTMLAttributes<HTMLHeadingElement>) {
  return (
    <div
      data-slot="alert-title"
      className={cn('grow font-medium tracking-tight', className)}
      {...props}
    />
  );
}

function AlertIcon({ children, className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div data-slot="alert-icon" className={cn('shrink-0', className)} {...props}>
      {children}
    </div>
  );
}

function AlertDescription({ className, ...props }: React.HTMLAttributes<HTMLParagraphElement>) {
  return (
    <div
      data-slot="alert-description"
      className={cn('text-sm leading-relaxed', className)}
      {...props}
    />
  );
}

export { Alert, AlertDescription, AlertIcon, AlertTitle };
