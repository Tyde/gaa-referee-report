import {createApp} from 'vue'
import App from './AdminApp.vue'
import PrimeVue from 'primevue/config';
import Card from "primevue/card";
import Button from "primevue/button";
import Listbox from "primevue/listbox";
import InputText from "primevue/inputtext";
import SelectButton from "primevue/selectbutton";
import Select from "primevue/select";
import './index.css';
import 'primeicons/primeicons.css';
import InputNumber from "primevue/inputnumber";
import Checkbox from "primevue/checkbox";
import Dialog from "primevue/dialog";
import ConfirmDialog from "primevue/confirmdialog";
import ConfirmationService from 'primevue/confirmationservice';
import Toolbar from "primevue/toolbar";
import VueFeather from 'vue-feather';
import Textarea from "primevue/textarea";
import {createRouter, createWebHashHistory} from "vue-router";

import {createPinia} from "pinia";
import {routes} from "@/router/admin_router";
import MegaMenu from "primevue/megamenu";
import Menubar from "primevue/menubar";
import Accordion from 'primevue/accordion';
import AccordionPanel from 'primevue/accordionpanel';
import AccordionHeader from 'primevue/accordionheader';
import AccordionContent from 'primevue/accordioncontent';
import FloatLabel from 'primevue/floatlabel';
import Tabs from 'primevue/tabs';
import TabList from 'primevue/tablist';
import Tab from 'primevue/tab';
import TabPanels from 'primevue/tabpanels';
import TabPanel from 'primevue/tabpanel';
import Message from "primevue/message";
import Panel from "primevue/panel";
import Toast from "primevue/toast";
import ToastService from 'primevue/toastservice';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Row from 'primevue/row';
import BlockUI from "primevue/blockui";
import Paginator from "primevue/paginator";
import SplitButton from "primevue/splitbutton";
import MultiSelect from "primevue/multiselect";
import {createI18n} from "vue-i18n";
import {messages} from "@/i18n/edit_report/edit_report_i18n";
import IconField from "primevue/iconfield";
import InputIcon from "primevue/inputicon";
import {RefReportColorPreset} from "@/utils/colors";
import {DatePicker} from "primevue"; //optional for row


const router = createRouter({
    history: createWebHashHistory(),
    routes: routes
})

const i18n = createI18n({
    legacy: false,
    locale: 'en',
    messages
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
app.use(i18n)
app.use(ConfirmationService)
app.component('Card',Card)
app.component('Button',Button)
app.component('Listbox',Listbox)
app.component('InputText',InputText)
app.component('DatePicker',DatePicker)
app.component('SelectButton',SelectButton)
app.component('Select',Select)
app.component('InputNumber',InputNumber)
app.component('IconField',IconField)
app.component('InputIcon',InputIcon)
app.component('Tabs', Tabs)
app.component('TabList', TabList)
app.component('Tab', Tab)
app.component('TabPanels', TabPanels)
app.component('TabPanel', TabPanel)
app.component('Checkbox',Checkbox)
app.component('Dialog',Dialog)
app.component('Toolbar',Toolbar)
app.component('Textarea', Textarea)
app.component('MegaMenu', MegaMenu)
app.component('Menubar', Menubar)
app.component('ConfirmDialog', ConfirmDialog)
app.component('Accordion', Accordion)
app.component('AccordionPanel', AccordionPanel)
app.component('AccordionHeader', AccordionHeader)
app.component('AccordionContent', AccordionContent)
app.component('Message',Message)
app.component('Panel',Panel)
app.component('DataTable',DataTable)
app.component('Column',Column)
app.component('Row',Row)
app.component('BlockUI',BlockUI)
app.component('Paginator', Paginator)
app.component('Toast', Toast)
app.component('SplitButton',SplitButton)
app.component('MultiSelect', MultiSelect)
app.component('FloatLabel', FloatLabel)
app.use(ToastService);

app.component(VueFeather.name!!,VueFeather)
app.mount('#app')
