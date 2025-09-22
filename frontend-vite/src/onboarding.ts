import {createApp} from 'vue'
import App from './Onboarding.vue'
import PrimeVue from 'primevue/config';
import Card from "primevue/card";
import Button from "primevue/button";
import Listbox from "primevue/listbox";
import InputText from "primevue/inputtext";
import SelectButton from "primevue/selectbutton";
import Dropdown from "primevue/dropdown";
import './index.css';
import 'primeicons/primeicons.css';
import InputNumber from "primevue/inputnumber";
import Checkbox from "primevue/checkbox";
import Dialog from "primevue/dialog";
import Toolbar from "primevue/toolbar";
import VueFeather from 'vue-feather';
import Textarea from "primevue/textarea";
// @ts-ignore
import {Vue3Mq} from "vue3-mq";
import Tooltip from "primevue/tooltip";
import {createPinia} from "pinia";
import Message from "primevue/message";
import Password from "primevue/password";
import {RefReportColorPreset} from "@/utils/colors";
import {DatePicker} from "primevue";
import Select from "primevue/select";

const pinia = createPinia()
const app = createApp(App);
app.use(pinia)
app.use(PrimeVue, {
    theme: {
        preset: RefReportColorPreset
    }
});
app.use(Vue3Mq);
app.component('Card',Card)
app.component('Button',Button)
app.component('Listbox',Listbox)
app.component('InputText',InputText)
app.component('Password', Password)
app.component('DatePicker',DatePicker)

app.component('SelectButton',SelectButton)
app.component('Select',Select)
app.component('InputNumber',InputNumber)
app.component('Checkbox',Checkbox)
app.component('Dialog',Dialog)
app.component('Toolbar',Toolbar)
app.component('Textarea', Textarea)
app.component('Message', Message)
app.directive('tooltip', Tooltip)
app.component(VueFeather.name!!,VueFeather)
app.mount('#app')
