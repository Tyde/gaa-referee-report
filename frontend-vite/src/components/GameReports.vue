<script setup lang="ts">

import SingleGameReport from "@/components/SingleGameReport.vue";
import type {ExtraTimeOption, GameReport, GameType, Report, Rule, Team} from "@/types";
import {computed, onBeforeMount, onMounted, onUpdated, ref, watch} from "vue";
import { DateTime } from "luxon";
import {checkGameReportMinimal, checkGameReportSuggestion} from "@/utils/gobal_functions";

const props = defineProps<{
  report:Report,
  rules:Array<Rule>,
  gameTypes:Array<GameType>,
  extraTimeOptions:Array<ExtraTimeOption>,
  gameReports:Array<GameReport>,
}>()
const emit = defineEmits<{
  (e: 'update:gameReports', value: Array<GameReport>): void
}>()



const selectedGameReport = ref<SingleGameReport | undefined>(undefined)
const filteredRules = computed(() => {
  return props.rules.filter(rule => rule.code == props.report.gameCode.id)
})
function newGameReport() {
  let estimatedStartingTime = selectedGameReport?.value?.startTime?.plus({
    minutes: 30
  })
  props.gameReports.push({
    report:props.report,
    teamAReport : {
      team: undefined,
      goals: 0,
      points: 0,
      injuries : [],
      disciplinaryActions : [],
    },
    teamBReport : {
      team: undefined,
      goals: 0,
      points: 0,
      injuries : [],
      disciplinaryActions : [],
    },
    umpirePresentOnTime: true,
    umpireNotes: "",
    startTime: estimatedStartingTime,
  } as GameReport)
  selectedGameReport.value = props.gameReports[props.gameReports.length-1]
}

watch(props.gameReports, () => {
  console.log("Updating all GameReports")
  emit('update:gameReports',props.gameReports)
}, {deep: true})
const currentReportIndex = ref<number>(0)
watch(selectedGameReport, () => {
  currentReportIndex.value = props.gameReports.indexOf(selectedGameReport.value)
})
onBeforeMount(()=> {
  console.log("Just before mount GameReports is "+props.gameReports)
  if(props.gameReports.length==0){
    newGameReport()
  } else {
    selectedGameReport.value = props.gameReports[0]
  }
})

</script>

<template>
<Toolbar>
  <template #start>
    <SelectButton v-model="selectedGameReport" :options="gameReports" >
      <template #option="slotProps">
        <span v-if="slotProps.option.startTime">{{slotProps.option.startTime.toLocaleString(DateTime.TIME_24_SIMPLE)}} -&nbsp;  </span>
        <span v-if="slotProps.option.teamAReport.team">{{slotProps.option.teamAReport.team.name}}</span>
        <span v-else>...</span>&nbsp;vs.&nbsp;
        <span v-if="slotProps.option.teamBReport.team">{{slotProps.option.teamBReport.team.name}}</span>
        <span v-else>...</span>

        <vue-feather
            type="alert-octagon"
            class="text-red-600 p-1"
            v-if="!checkGameReportMinimal(slotProps.option)"/>
        <vue-feather
            type="circle"
            class="text-yellow-200 p-1"
            v-else-if="!checkGameReportSuggestion(slotProps.option)"/>
        <vue-feather
            class="text-green-600 p-1"
            type="check-circle"
            v-else />
        <!--
        <font-awesome-icon
            icon="fa-regular fa-circle-dashed"
            v-if="!checkGameReportSuggestion(slotProps.option)"/>
        <font-awesome-icon icon="fa-regular fa-circle-check"
            v-else />-->
      </template>
    </SelectButton>
  </template>
  <template #end>
    <Button @click="newGameReport" class="p-button-success"><i class="pi pi-plus"></i> </Button>
  </template>
</Toolbar>


  <SingleGameReport
      v-model="selectedGameReport"
      :rules="filteredRules"
      :extra-time-options="props.extraTimeOptions"
      :game-types="props.gameTypes"
      :index="currentReportIndex"
  />

</template>



<style scoped>

</style>