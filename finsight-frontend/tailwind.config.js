/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: '#0b0e14',
        card: '#161b28',
        'card-hover': '#1c2233',
        border: '#232a3d',
        lime: '#c8f064',
        teal: '#4af4c2',
        orange: '#f4a24a',
        red: '#f46060',
        muted: '#8a92a6',
      },
      fontFamily: {
        display: ['"DM Serif Display"', 'serif'],
        body: ['Outfit', 'sans-serif'],
        mono: ['"DM Mono"', 'monospace'],
      },
      borderRadius: {
        xl: '1rem',
        '2xl': '1.25rem',
      },
    },
  },
  plugins: [],
};
