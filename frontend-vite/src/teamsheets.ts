import {createApp} from 'vue'
import App from './TeamsheetDashboard.vue'
import PrimeVue from 'primevue/config';
import Button from "primevue/button";
import InputText from "primevue/inputtext";
import './index.css';
import 'primevue/resources/themes/mdc-light-indigo/theme.css';
import 'primevue/resources/primevue.min.css';
import 'primeicons/primeicons.css';
import InputNumber from "primevue/inputnumber";
import ConfirmationService from 'primevue/confirmationservice';
import VueFeather from 'vue-feather';
import {createRouter, createWebHashHistory} from "vue-router";

import {createPinia} from "pinia";
import Message from "primevue/message";
import Panel from "primevue/panel";
import BlockUI from "primevue/blockui";
import IconField from "primevue/iconfield";
import InputIcon from "primevue/inputicon";
import FileUpload from 'primevue/fileupload';
import ProgressBar from 'primevue/progressbar';

import {routes} from "@/router/teamsheet_router";
import Dropdown from "primevue/dropdown";
import Accordion from "primevue/accordion"; //optional for row
import AccordionTab from 'primevue/accordiontab';


const router = createRouter({
    history: createWebHashHistory(),
    routes: routes
})


const pinia = createPinia()
const app = createApp(App);
app.use(PrimeVue)
app.use(pinia)
app.use(router)
app.use(ConfirmationService)
app.component('Button',Button)
app.component('InputText',InputText)
app.component('InputNumber',InputNumber)
app.component('IconField',IconField)
app.component('InputIcon',InputIcon)
app.component('ProgressBar',ProgressBar)
app.component('Dropdown', Dropdown)

app.component('Message',Message)
app.component('Panel',Panel)
app.component('BlockUI',BlockUI)
app.component('FileUpload',FileUpload)
app.component('Accordion', Accordion)
app.component('AccordionTab',AccordionTab)

app.component(VueFeather.name!!,VueFeather)
app.mount('#app')
