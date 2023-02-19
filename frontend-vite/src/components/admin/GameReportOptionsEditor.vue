<script setup lang="ts">

import {useAdminStore} from "@/utils/admin_store";
import GameTypeEditor from "@/components/admin/gameReport/GameTypeEditor.vue";
import RulesEditor from "@/components/admin/gameReport/RulesEditor.vue";
import ExtraTimeOptionEditor from "@/components/admin/gameReport/ExtraTimeOptionEditor.vue";

const store = useAdminStore()


</script>

<template>
  <div class="flex flex-row flex-wrap justify-center mt-2">
    <div class="sbs-container">
      <h2>Game Types</h2>
      <GameTypeEditor />
    </div>
    <div class="sbs-container">
      <h2>Extra Time options</h2>
      <ExtraTimeOptionEditor />
    </div>
  </div>


</template>

<style scoped>
.sbs-container {
  @apply md:w-5/12 w-full sm:w-10/12;
  @apply m-4;
  @apply sm:rounded-2xl sm:shadow-lg;
}
h2 {
  @apply text-center text-2xl;
  @apply font-bold;
  @apply mb-2;
}
</style>