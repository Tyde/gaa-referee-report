import {createApp} from "vue";
import App from './ShowReportApp.vue';
import './index.css';
import 'primevue/resources/themes/tailwind-light/theme.css';
import 'primevue/resources/primevue.min.css';
import Button from "primevue/button";


const app = createApp(App);
app.component('Button',Button)
app.mount("#app");