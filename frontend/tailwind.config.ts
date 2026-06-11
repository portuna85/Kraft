import type { Config } from 'tailwindcss'

const config: Config = {
  content: ['./src/**/*.{ts,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        navy: {
          DEFAULT: '#1A1A2E',
          light: '#16213E',
          card: '#0F3460',
        },
        gold: {
          DEFAULT: '#FFC107',
          dark: '#E6AC00',
        },
        ball: {
          yellow: '#F9A825',
          blue: '#42A5F5',
          red: '#EF5350',
          gray: '#757575',
          green: '#66BB6A',
        },
      },
    },
  },
  plugins: [],
}

export default config
