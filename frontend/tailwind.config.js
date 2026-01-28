/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'western': {
          'sand': '#E8D4A8',
          'brown': '#8B4513',
          'dark': '#2C1810',
          'gold': '#DAA520',
          'leather': '#964B00',
          'red': '#8B0000'
        }
      },
      fontFamily: {
        'western': ['Rye', 'serif'],
        'body': ['Cinzel', 'serif']
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'bounce-slow': 'bounce 2s infinite',
        'shake': 'shake 0.5s ease-in-out',
        'card-hover': 'cardHover 0.3s ease-out forwards'
      },
      keyframes: {
        shake: {
          '0%, 100%': { transform: 'translateX(0)' },
          '25%': { transform: 'translateX(-5px)' },
          '75%': { transform: 'translateX(5px)' }
        },
        cardHover: {
          '0%': { transform: 'translateY(0) scale(1)' },
          '100%': { transform: 'translateY(-20px) scale(1.1)' }
        }
      }
    },
  },
  plugins: [],
}
