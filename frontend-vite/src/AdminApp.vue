<script lang="ts" setup>
import {ref} from "vue";
import {useAdminStore} from "@/utils/admin_store";

const store = useAdminStore();
store.fetchPitchVariables()
store.fetchGameReportVariables()

const items = ref([
  {
    label: "Report options",
    items: [
      {
        label: "Pitch options",
        to: "/pitch-options"
      },
      {
        label: "Game report options",
        icon: 'pi pi-fw pi-cog',
        to: "/game-report-options"
      }
    ]
  }
])
</script>

<template>
  <div>
    <Menubar :model="items" />

  </div>
  <transition-group name="p-message" tag="div">
    <Message v-for="msg in store.currentErrors" severity="error" :key="msg.timestamp">{{msg.message}}</Message>
  </transition-group>
  <router-view></router-view>
</template>
<style>

</style>

<style scoped>

</style>
