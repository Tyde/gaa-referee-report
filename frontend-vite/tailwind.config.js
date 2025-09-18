/** @type {import('tailwindcss').Config} */
import PrimeUI from 'tailwindcss-primeui';
module.exports = {
  content: [
    './src/**/*.{vue,ts,js,jsx,tsx}',
    'edit_report.html',
      'admin.html',
      'show_report.html'
  ],
  theme: {
    extend: {},
  },
  variants: {
    extend: {
      display: ['touch'],  // we want to control `display` on touch devices
      opacity: ['touch'],  // or any other utility you like
    }
  },
  plugins: [
      PrimeUI,
      function({ addVariant }) {
        // media query for "coarse" pointers (i.e. touchscreens)
        addVariant('touch', '@media (pointer: coarse)');
      }
  ],
}
