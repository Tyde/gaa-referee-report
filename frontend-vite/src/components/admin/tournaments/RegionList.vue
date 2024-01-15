<script setup lang="ts">
import {useAdminStore} from "@/utils/admin_store";
import {computed} from "vue";

let store = useAdminStore();

let regions = computed(() => {
  store.regions
})
</script>

<template>
  <div>

  </div>

</template>

<style scoped>

</style>