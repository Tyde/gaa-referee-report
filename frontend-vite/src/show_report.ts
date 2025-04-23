import {createApp} from "vue";
import App from './ShowReportApp.vue';
import PrimeVue from 'primevue/config';
import './index.css';
import Button from "primevue/button";
import {createPinia} from "pinia";
import Lara from "@primeuix/themes/lara";

const pinia = createPinia()
const app = createApp(App);
app.use(PrimeVue, {
    theme: {
        preset: Lara
    }
});
app.use(pinia)
app.component('Button',Button)
app.mount("#app");
