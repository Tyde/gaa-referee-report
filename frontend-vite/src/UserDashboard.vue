
<script lang="ts" setup>

import {useDashboardStore} from "@/utils/dashboard_store";
import {onMounted} from "vue";
import {useRoute, useRouter} from "vue-router";

const store = useDashboardStore()
store.fetchMyReports()
store.checkAdmin()
onMounted(() => {

})

function newReport() {
  location.href = "/report/new"
}

function logout() {
  location.href = "/logout"
}

function navigateToAdmin() {
  location.href = "/admin"
}
const router = useRouter()
const route = useRoute()
</script>

<template>
  <transition-group name="p-message" tag="div">
    <Message v-for="msg in store.currentErrors" severity="error" :key="msg.timestamp">{{msg.message}}</Message>
  </transition-group>
  <div class="flex flex-row justify-center">
    <div class="w-full xl:w-10/12">
      <div class="flex flex-row justify-center flex-wrap md:flex-nowrap no-print">
        <div class="m-2">
          <Button label="Create new report" icon="pi pi-plus" class="m-2" @click="newReport"></Button>
        </div>
        <div class="m-2" v-if="route.path != '/profile'">
          <Button label="Profile" icon="pi pi-file" class="m-2" @click="router.push('/profile')"></Button>
        </div>
        <div class="m-2" v-if="route.path != '/'">
          <Button label="Home" icon="pi pi-home" class="m-2" @click="router.push('/')"></Button>
        </div>
        <div class="m-2">
          <Button label="Logout" icon="pi pi-sign-out" class="p-button-danger m-2 ml-2" @click="logout"></Button>
        </div>
        <div class="m-2" v-if="store.isAdmin">
          <Button label="Admin" icon="pi pi-cog" class="p-button-secondary m-2 ml-2" @click="navigateToAdmin"></Button>
        </div>

      </div>
        <router-view></router-view>
    </div>
  </div>
  <ConfirmDialog></ConfirmDialog>
</template>


<style scoped>
h1 {
  @apply text-2xl;
  @apply font-bold;
  @apply text-gray-900;
  @apply text-center;
}
</style>

<style>
@media print {
  .no-print, .no-print * {
    display: none !important;
  }
}
</style>