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
  <router-view></router-view>
</template>
<style>

</style>

<style scoped>

</style>
