package com.herwin.netez.ui.theme

import androidx.compose.ui.graphics.Color

// NeTez Brand Identity Tokens (Precision Network & Security)
val BrandNeTezPrimary = Color(0xFF6C8CFF)    // Signature Precision Blue (#6C8CFF)

// NOC / Dark Theme Tokens (Deep Precision Dark Navy)
val DeepNavyBackground = Color(0xFF0B1220)   // Primary Dark Background (#0B1220)
val PrecisionDarkSurface = Color(0xFF121D2F) // Card Surface (#121D2F)
val PrecisionDarkElevated = Color(0xFF1A2940)// Elevated Card Surface (#1A2940)
val PrecisionDarkBorder = Color(0xFF243754)  // Subdued Outline/Divider (#243754)
val PrecisionDarkTextMuted = Color(0xFF94A3B8)// Muted Text (#94A3B8)

// Legacy Backwards Compatibility Tokens
val PremiumNavy = DeepNavyBackground
val SoftDarkBg = PrecisionDarkSurface
val TextGray = PrecisionDarkTextMuted
val CardGradientStart = Color(0xFF4F46E5)
val CardGradientEnd = Color(0xFF7C3AED)

// Soft Precision Palette (Light Theme Tokens)
val SoftTechBackground = Color(0xFFF5F7FB)   // Clean Light Background (#F5F7FB)
val SoftTechSurface = Color(0xFFFFFFFF)      // Pure White Surface
val SoftTechSurfaceVariant = Color(0xFFEEF3F9)// Elevated Light Surface
val SoftTechPrimary = Color(0xFF3B62F6)      // WCAG AA Accessible Blue
val SoftTechSecondary = Color(0xFF10B981)    // Security Verified Green
val SoftTechTertiary = Color(0xFF6366F1)     // Indigo Info
val SoftTechSuccess = Color(0xFF10B981)      // Online / Valid
val SoftTechWarning = Color(0xFFF59E0B)      // Attention / Expiring
val SoftTechDanger = Color(0xFFEF4444)       // Critical / Vulnerable / Expired
val SoftTechTextPrimary = Color(0xFF0F172A)  // Slate-900 Primary Text
val SoftTechTextSecondary = Color(0xFF64748B)// Slate-500 Muted Text
val SoftTechBorder = Color(0xFFE2E8F0)       // Subtle Light Divider
val SoftTechShadowTint = Color(0x1A3B62F6)   // Subtle Blue Tinted Shadow

// Signal Colors (Backwards Compatible)
val SignalExcellent = SoftTechSuccess
val SignalGood = SoftTechWarning
val SignalPoor = SoftTechDanger

// Material 3 Defaults for Dark Mode
val SoftBluePrimary = BrandNeTezPrimary
val SoftBlueSecondary = Color(0xFF10B981)
val SoftBlueTertiary = Color(0xFF6366F1)
