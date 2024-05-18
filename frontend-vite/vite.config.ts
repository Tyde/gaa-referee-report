import { fileURLToPath, URL } from 'url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
const { resolve } = require('path')
// https://vitejs.dev/config/


import { Plugin } from 'vite';
import {exec} from "child_process";

const gradleAssembler = (): Plugin => {
  let isWatchMode = false; // Variable to hold the watch mode status

  return {
    name: 'gradle-assembler',
    config(config, { command }) {
      if (command === 'build') {
        // Check if watch mode is active in the build command
        isWatchMode = !!config.build?.watch;
        console.log(`Is watch mode: ${isWatchMode}`);
      }
    },
    buildEnd() {
      // You can use isWatchMode here
      if (isWatchMode) {

        console.log('Build ended in watch mode...');
        const { exec } = require('child_process')
        /*exec('./gradlew processResources',{cwd: '../'}, (err, stdout, stderr) => {
          if (err) {
            console.error(err)
            return
          }
          console.log(stdout)
          console.log("Gradle build complete")
        })*/
        // Your logic for handling the end of a build in watch mode
      }
    }
  };
};
export default defineConfig(({ mode }) => {
  return {
    plugins: [
      vue(),
      vueJsx(),
      gradleAssembler()
    ],
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
      minify: mode === "development" ? false : 'esbuild',
    },
  }
})
