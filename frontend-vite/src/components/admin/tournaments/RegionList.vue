<script setup lang="ts">
import {useAdminStore} from "@/utils/admin_store";
import {computed} from "vue";

const store = useAdminStore();

const regions = computed(() => {
  store.publicStore.regions
})
</script>

<template>
  <div>

  </div>

</template>

<style scoped>

</style>
