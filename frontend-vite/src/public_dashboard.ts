import {createApp} from 'vue'
import App from './PublicResultDashboard.vue'
import PrimeVue from 'primevue/config';
import Card from "primevue/card";
import Button from "primevue/button";
import Listbox from "primevue/listbox";
import InputText from "primevue/inputtext";
import Calendar from "primevue/calendar";
import SelectButton from "primevue/selectbutton";
import Dropdown from "primevue/dropdown";
import './index.css';
import 'primevue/resources/themes/mdc-light-indigo/theme.css';
import 'primevue/resources/primevue.min.css';
import 'primeicons/primeicons.css';
import InputNumber from "primevue/inputnumber";
import Checkbox from "primevue/checkbox";
import Dialog from "primevue/dialog";
import ConfirmDialog from "primevue/confirmdialog";
import ConfirmationService from 'primevue/confirmationservice';
import Toolbar from "primevue/toolbar";
import VueFeather from 'vue-feather';
import Textarea from "primevue/textarea";
import {createRouter,createWebHashHistory} from "vue-router";

import {createPinia} from "pinia";
import {routes} from "@/router/public_router";
import MegaMenu from "primevue/megamenu";
import Menubar from "primevue/menubar";
import Accordion from "primevue/accordion";
import AccordionTab from "primevue/accordiontab";
import TabView from "primevue/tabview";
import TabPanel from "primevue/tabpanel";
import Message from "primevue/message";
import Panel from "primevue/panel";

import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Row from 'primevue/row';
import BlockUI from "primevue/blockui";
import IconField from "primevue/iconfield";
import InputIcon from "primevue/inputicon";                     //optional for row


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
app.component('Card',Card)
app.component('Button',Button)
app.component('Listbox',Listbox)
app.component('InputText',InputText)
app.component('Calendar',Calendar)
app.component('SelectButton',SelectButton)
app.component('Dropdown',Dropdown)
app.component('InputNumber',InputNumber)
app.component('IconField',IconField)
app.component('InputIcon',InputIcon)

app.component('Checkbox',Checkbox)
app.component('Dialog',Dialog)
app.component('Toolbar',Toolbar)
app.component('Textarea', Textarea)
app.component('MegaMenu', MegaMenu)
app.component('Menubar', Menubar)
app.component('ConfirmDialog', ConfirmDialog)
app.component('Accordion', Accordion)
app.component('AccordionTab', AccordionTab)
app.component('TabView',TabView)
app.component('TabPanel',TabPanel)
app.component('Message',Message)
app.component('Panel',Panel)
app.component('DataTable',DataTable)
app.component('Column',Column)
app.component('Row',Row)
app.component('BlockUI',BlockUI)

app.component(VueFeather.name!!,VueFeather)
app.mount('#app')
