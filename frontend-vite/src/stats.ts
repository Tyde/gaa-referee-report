import {createApp} from 'vue'
import App from './StatsApp.vue'
import PrimeVue from 'primevue/config';
import Button from "primevue/button";
import Message from "primevue/message";
import Panel from "primevue/panel";
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import './index.css';
import 'primeicons/primeicons.css';
import {createRouter, createWebHashHistory} from "vue-router";
import {createPinia} from "pinia";
import {RefReportColorPreset} from "@/utils/colors";
import VueFeather from 'vue-feather';

const router = createRouter({
    history: createWebHashHistory(),
    routes: [
        {
            path: '/',
            component: App
        }
    ]
})

const pinia = createPinia()
const app = createApp(App);
app.use(PrimeVue, {
    theme: {
        preset: RefReportColorPreset
    }
});
app.use(pinia)
app.use(router)
app.component('Button', Button)
app.component('Message', Message)
app.component('Panel', Panel)
app.component('DataTable', DataTable)
app.component('Column', Column)
app.component(VueFeather.name!, VueFeather)
app.mount('#app')
