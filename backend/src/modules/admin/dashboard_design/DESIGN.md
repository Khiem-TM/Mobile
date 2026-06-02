---
name: VitalAI Glacier Admin
colors:
  surface: '#031427'
  surface-dim: '#031427'
  surface-bright: '#2a3a4f'
  surface-container-lowest: '#000f21'
  surface-container-low: '#0b1c30'
  surface-container: '#102034'
  surface-container-high: '#1b2b3f'
  surface-container-highest: '#26364a'
  on-surface: '#d3e4fe'
  on-surface-variant: '#bec8ce'
  inverse-surface: '#d3e4fe'
  inverse-on-surface: '#213145'
  outline: '#899298'
  outline-variant: '#3f484e'
  surface-tint: '#7bd1fa'
  primary: '#c5eaff'
  on-primary: '#003547'
  primary-container: '#7dd3fc'
  on-primary-container: '#005b78'
  inverse-primary: '#006686'
  secondary: '#b9c7e0'
  on-secondary: '#233144'
  secondary-container: '#3c4a5e'
  on-secondary-container: '#abb9d2'
  tertiary: '#dde4ff'
  on-tertiary: '#283044'
  tertiary-container: '#c0c8e2'
  on-tertiary-container: '#4c5369'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#c0e8ff'
  primary-fixed-dim: '#7bd1fa'
  on-primary-fixed: '#001e2b'
  on-primary-fixed-variant: '#004d66'
  secondary-fixed: '#d5e3fd'
  secondary-fixed-dim: '#b9c7e0'
  on-secondary-fixed: '#0d1c2f'
  on-secondary-fixed-variant: '#3a485c'
  tertiary-fixed: '#dae2fd'
  tertiary-fixed-dim: '#bec6e0'
  on-tertiary-fixed: '#131b2e'
  on-tertiary-fixed-variant: '#3f465c'
  background: '#031427'
  on-background: '#d3e4fe'
  surface-variant: '#26364a'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-sm:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-caps:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.05em
  data-mono:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  sidebar_width: 260px
  topbar_height: 64px
  gutter: 1rem
  margin_page: 1.5rem
  density_compact: 0.5rem
  density_comfortable: 1rem
---

## Brand & Style
The design system for VitalAI is built on a **Corporate / Modern** aesthetic with a **Glassmorphic** twist, specifically tailored for a high-density SaaS admin environment. The personality is precise, cold, and professional—evoking the feeling of a high-tech command center in a frozen landscape. 

The primary goal is clarity and data-efficiency. By utilizing translucent layers and subtle blurs, the interface maintains a sense of depth without sacrificing the speed or density required for monitoring AI operations. The user should feel in total control of complex systems through a UI that feels responsive and structurally sound.

## Colors
The color palette is rooted in the "Frozen Light" theme. 
- **Primary Ice-blue (#7dd3fc):** Used for primary actions, active navigation states, and highlighting key data points.
- **Backgrounds:** The base layer is a deep midnight navy (#020617). Surface containers use a slightly lighter slate to provide separation.
- **Status Colors:** Semantic colors are strictly defined for immediate recognition in data tables. Green (Thành công), Red (Lỗi), Orange (Cảnh báo), and Blue (Thông tin) follow standard accessibility contrast ratios against the dark background.
- **Glass Effects:** Translucency is applied to sidebars and topbars to create a "Glacier" layering effect using `backdrop-filter: blur(12px)`.

## Typography
This design system utilizes **Inter** as its primary typeface to maximize legibility at small sizes within dense data tables. For technical data strings and logs, **JetBrains Mono** is used to provide a clear distinction between narrative UI text and system-generated values.

Vietnamese character support is prioritized, ensuring diacritics do not interfere with line heights in dense layouts. Headings are kept compact, and `body-sm` is the default for table content to ensure high information density.

## Layout & Spacing
The layout follows a **Fixed Sidebar / Fluid Content** model. 
- **Sidebar:** Fixed at 260px, containing the primary navigation (Menu chính). 
- **Topbar:** Fixed at 64px, housing the search bar (Tìm kiếm), notifications (Thông báo), and user profile.
- **Grid:** A 12-column system is used within the fluid content area for KPI cards and charts. 
- **Data Density:** Tables use a `density_compact` (8px) padding for rows to allow for more vertical data visibility. Margins between major sections are set to 24px (`margin_page`) to prevent visual clutter.

## Elevation & Depth
Depth is created through **Tonal Layers** and **Backdrop Blurs** rather than traditional heavy shadows.
- **Level 0 (Base):** Background (#020617).
- **Level 1 (Cards/Tables):** Surface color (#0f172a) with a subtle 1px border (#1e293b).
- **Level 2 (Modals/Drawers):** Floating elements use the same surface color but include a 15% opacity white border and a diffused `0 20px 25px -5px rgba(0, 0, 0, 0.5)` shadow.
- **Interaction:** Hovering over interactive rows or cards increases the border brightness and primary color tint.

## Shapes
The shape language is controlled and geometric. A `roundedness` of **Soft** (6px - 8px) is applied consistently across cards, buttons, and input fields. 
- **Buttons & Inputs:** 6px radius.
- **Cards & Modals:** 8px radius.
- **Status Badges (Chips):** 4px radius for a sharper, more technical look.
- **Search Bar:** 8px radius to match primary containers.

## Components
- **Bảng Dữ Liệu (Data Tables):** Rows must have a hover state using a subtle blue tint. Filters are located in a toolbar immediately above the header. Headers are sticky and use `label-caps` typography.
- **Thanh Điều Hướng (Sidebar):** Icons are outlined (20px). Active items use a ghost-pill background with a left-accent border in Ice-blue.
- **Thẻ KPI (KPI Cards):** Display a large value, a trend sparkline (SVG), and a percentage change label (Tăng/Giảm).
- **Hộp Thoại (Modals & Dialogs):** Confirmation dialogs (Xác nhận) must have a high-contrast backdrop overlay (80% opacity black). Action buttons are aligned to the right.
- **Trạng Thái (Status Badges):** Use a low-opacity background of the status color with high-opacity text (e.g., "Đang chạy" in green).
- **Trường Nhập Liệu (Input Fields):** Focused states use a 1px primary ice-blue glow. Labels are placed above the field in `body-sm`.