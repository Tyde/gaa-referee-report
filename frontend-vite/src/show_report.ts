import {createApp} from "vue";
import App from './ShowReportApp.vue';
import PrimeVue from 'primevue/config';
import './index.css';
import 'primevue/resources/themes/mdc-light-indigo/theme.css';
import 'primevue/resources/primevue.min.css';
import Button from "primevue/button";
import {createPinia} from "pinia";

const pinia = createPinia()
const app = createApp(App);
app.use(PrimeVue)
app.use(pinia)
app.component('Button',Button)
app.mount("#app");
