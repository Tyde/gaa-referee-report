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
        editReport: resolve(__dirname, 'edit_report.html'),
        admin: resolve(__dirname, 'admin.html'),
        showReport: resolve(__dirname, 'show_report.html'),
        onboarding: resolve(__dirname, 'onboarding.html'),
        userDashboard: resolve(__dirname, 'user_dashboard.html'),
        publicDashboard: resolve(__dirname, 'public_dashboard.html'),
      },
    },
    emptyOutDir: true,
  },
})
