import {createApp} from 'vue'
import App from './EditReportApp.vue'
import PrimeVue from 'primevue/config';
import Card from "primevue/card";
import Button from "primevue/button";
import Listbox from "primevue/listbox";
import InputText from "primevue/inputtext";
import 'primevue/resources/themes/saga-blue/theme.css';
import 'primevue/resources/primevue.min.css';
import 'primeicons/primeicons.css';

const app = createApp(App);
app.use(PrimeVue)
app.component('Card',Card)
app.component('Button',Button)
app.component('Listbox',Listbox)
app.component('InputText',InputText)
app.mount('#app')
