import { fileURLToPath, URL } from 'url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
const { resolve } = require('path')
// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue(), vueJsx()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    sourcemap: true,
    outDir: '../src/main/resources/static',
    rollupOptions: {
      input: {
        editReport: resolve(__dirname, 'edit_report.html')
      },
    },
    emptyOutDir: true,
    minify: false
  },
})
