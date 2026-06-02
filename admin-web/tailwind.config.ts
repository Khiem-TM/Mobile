import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        background: '#f8faf0',
        surface: '#ffffff',
        'surface-low': '#f3f5e9',
        'surface-high': '#e7eadf',
        border: '#d3dac7',
        primary: '#4a7c59',
        'primary-dark': '#376545',
        'primary-soft': '#dbeebe',
        'primary-faint': '#eef7e4',
        text: '#191d17',
        muted: '#57614e',
        danger: '#ba1a1a',
        'danger-soft': '#ffdad6',
        warning: '#a25d00',
        'warning-soft': '#ffe2b7',
        info: '#39656d',
        'info-soft': '#d6eef3',
      },
      fontFamily: {
        sans: ['Nunito Sans', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
      spacing: {
        sidebar: '260px',
        topbar: '64px',
      },
      boxShadow: {
        soft: '0 1px 2px rgba(0, 0, 0, 0.05)',
        float: '0 20px 40px rgba(42, 55, 33, 0.18)',
      },
    },
  },
  plugins: [],
} satisfies Config;
