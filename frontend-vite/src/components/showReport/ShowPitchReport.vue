<script setup lang="ts">

import type {Pitch} from "@/types";

let props = defineProps<{
  pitchReport:Pitch
}>()

</script>

<template>
  <div>
    <h3>{{pitchReport.name}}</h3>
    <table>

    </table>
  </div>

</template>



<style scoped>

</style>