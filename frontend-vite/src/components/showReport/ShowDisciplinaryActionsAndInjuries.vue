<script setup lang="ts">

import type {SingleTeamGameReport} from "@/types/game_report_types";

const props = defineProps<{
  teamReport:SingleTeamGameReport
}>()
</script>

<template>
  <div class="flex flex-col">
    <div class="p-2" v-if="props.teamReport.disciplinaryActions.length > 0">
      <span class="bg-red-200 print:bg-red-200 text-black">Disciplinary Actions:</span><br>
      <ul>
        <li
            v-for="action in props.teamReport.disciplinaryActions"
            class="border-t-2 border-gray-500"
        >
          <div v-if="action.rule?.isCaution" class="rule-card card-yellow"></div>
          <div v-if="action.rule?.isRed" class="rule-card card-red"></div>
          <div v-if="action.rule?.isBlack" class="rule-card card-black"></div>
          {{action.number}} - {{action.firstName}} {{action.lastName}}: {{action.rule?.description || ''}}<br>
          {{action.details}}
        </li>
      </ul>
    </div>
    <div class="p-2" v-if="props.teamReport.injuries.length > 0">
      <span class="bg-amber-200 text-black">Injuries:</span><br>
      <ul>
        <li
            v-for="injury in props.teamReport.injuries"
            class="border-t-2 border-gray-500"
        >
         {{injury.firstName}} {{injury.lastName}}: {{injury.details}}
        </li>
      </ul>
    </div>
  </div>
</template>



<style scoped>

</style>
