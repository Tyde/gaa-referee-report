
<script lang="ts" setup>

import {useDashboardStore} from "@/utils/dashboard_store";
import {onMounted} from "vue";

const store = useDashboardStore()
onMounted(() => {
  store.fetchMyReports()
  store.checkAdmin()
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

</script>

<template>
  <transition-group name="p-message" tag="div">
    <Message v-for="msg in store.currentErrors" severity="error" :key="msg.timestamp">{{msg.message}}</Message>
  </transition-group>

  <div class="flex flex-row justify-center">
    <div class="w-10/12">
      <div class="flex flex-row justify-center">
        <div class="m-2">
          <Button label="Create new report" icon="pi pi-plus" class="m-2" @click="newReport"></Button>
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