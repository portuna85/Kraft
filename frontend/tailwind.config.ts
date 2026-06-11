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
          blue: '#1565C0',
          red: '#B71C1C',
          gray: '#424242',
          green: '#1B5E20',
        },
      },
    },
  },
  plugins: [],
}

export default config
