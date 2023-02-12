
<script lang="ts" setup>

import {useDashboardStore} from "@/utils/dashboard_store";
import {onMounted} from "vue";

const store = useDashboardStore()
onMounted(() => {
  store.fetchMyReports()
})

function newReport() {
  location.href = "/report/new"
}

</script>

<template>
  <transition-group name="p-message" tag="div">
    <Message v-for="msg in store.currentErrors" severity="error" :key="msg.timestamp">{{msg.message}}</Message>
  </transition-group>
  <div class="flex flex-row justify-center">
    <div class="w-10/12">
      <div class="flex flex-row justify-center">
        <Button label="Create new report" icon="pi pi-plus" class="mb-2" @click="newReport"></Button>
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