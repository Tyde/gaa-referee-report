
<script lang="ts" setup>

import {useDashboardStore} from "@/utils/dashboard_store";
import {onMounted} from "vue";
import {useRoute, useRouter} from "vue-router";
import {usePublicStore} from "@/utils/public_store";


const router = useRouter()
const route = useRoute()

const store = useDashboardStore()
const publicStore = usePublicStore()
publicStore.loadAuxiliaryInformationFromSerer()
publicStore.loadTournaments()



onMounted(() => {
  store.checkAdmin()
      .then(() => {
        console.log("isCCC", store.isCCC)
        if (store.isCCC && route.path == '/') {
          publicStore.loadAuxiliaryInformationFromSerer()
          publicStore.loadTournaments()
          console.log("push")
          router.push({path:'/tournament-reports'})
        } else {
          store.fetchMyReports()
        }
      })
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

function navigateToHome() {
  if(store.isCCC) {
    router.push({path:'/tournament-reports'})
  } else {
    router.push({path:'/'})
  }
}

</script>

<template>
  <transition-group name="p-message" tag="div">
    <Message v-for="msg in publicStore.currentErrors" severity="error" :key="msg.timestamp">{{msg.message}}</Message>
  </transition-group>
  <div class="flex flex-row justify-center">
    <div class="referee-container">
      <div class="header no-print">

        <img src="./assets/logo.png" alt="Logo" class="w-12 h-12 m-2" @click="navigateToHome">
        <div class="grow"></div>
        <div class="flex flex-row flex-wrap w-10/12 justify-end">
        <div v-if="route.path != '/'">
          <Button label="Home" icon="pi pi-home" class="m-2" @click="navigateToHome"></Button>
        </div>
        <div v-if="!store.isCCC">
          <Button label="New report" icon="pi pi-plus" class="m-2" @click="newReport"></Button>
        </div>
        <div v-if="route.path != '/profile'">
          <Button label="Profile" icon="pi pi-file" class="m-2" @click="router.push('/profile')"></Button>
        </div>


        <div v-if="store.isAdmin || store.isRefereeAndCCC">
          <Button label="CCC Overview" icon="pi pi-th-large" class="p-button-help m-2 ml-2" @click="router.push('/tournament-reports')"></Button>
        </div>
        <div v-if="store.isAdmin">
          <Button label="Admin" icon="pi pi-cog" class="p-button-info m-2 ml-2" @click="navigateToAdmin"></Button>
        </div>
        <div v-if="store.isRefereeAndCCC || store.isAdmin || store.isCCC">
          <Button label="Setup Tournament"
                  icon="pi pi-cog"
                  class="p-button-success m-2 ml-2" @click="router.push('/setup-tournament')"></Button>
        </div>
        <div >
          <Button label="Logout" icon="pi pi-sign-out" class="p-button-danger m-2 ml-2" @click="logout"></Button>
        </div>

        </div>
      </div>
        <router-view></router-view>
    </div>
  </div>
  <ConfirmDialog></ConfirmDialog>
</template>


<style scoped>

</style>

<style>

.p-button-label {
  @apply text-sm;
}
@media print {
  .no-print, .no-print * {
    display: none !important;
  }
}
</style>
