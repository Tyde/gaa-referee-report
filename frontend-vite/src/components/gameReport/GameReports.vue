<script setup lang="ts">

import SingleGameReport from "@/components/gameReport/SingleGameReport.vue";
import type {ExtraTimeOption, GameReport, GameType, Report, Rule, Team} from "@/types";
import {computed, onBeforeMount, onMounted, onUpdated, ref, watch} from "vue";
import { DateTime } from "luxon";
import {checkGameReportNecessary, checkGameReportSuggestion} from "@/utils/gobal_functions";
import {deleteGameReportOnServer} from "@/utils/api/game_report_api";

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


const sortedGameReports = computed(() => {
  return props.gameReports.sort((a, b) => {
    if(a.startTime) {
      if(b.startTime) {
        return a.startTime > b.startTime ? 1 : -1
      } else {
        return -1
      }
    } else {
      return 0
    }
  })
})
const selectedGameReport = ref<GameReport | undefined>(undefined)
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
  emit('update:gameReports',props.gameReports)
}, {deep: true})
const currentReportIndex = ref<number>(0)
watch(selectedGameReport, () => {
  if(selectedGameReport.value) {
    currentReportIndex.value = props.gameReports.indexOf(selectedGameReport.value)
  }
})
onBeforeMount(()=> {
  if(props.gameReports.length==0){
    newGameReport()
  } else {
    selectedGameReport.value = props.gameReports[0]
  }
})

////// DELETE REPORT //////

const deleteReportDialogVisible = ref(false)
const reportToDelete = ref<GameReport | undefined>(undefined)
function startDeleteReport(gReport:GameReport) {
  reportToDelete.value = gReport
  deleteReportDialogVisible.value = true
}

function cancelDeleteReport() {
  deleteReportDialogVisible.value = false
  reportToDelete.value = undefined
}

async function deleteReport() {
  if(reportToDelete.value != undefined) {
    if(reportToDelete.value.id != undefined) {
      await deleteGameReportOnServer(reportToDelete.value)
    }
    let index = props.gameReports.indexOf(reportToDelete.value)
    props.gameReports.splice(index, 1)
    selectedGameReport.value = props.gameReports[Math.min(index, props.gameReports.length - 1)]
    cancelDeleteReport()
  }

}

</script>

<template>
<Toolbar>
  <template #start>
    <SelectButton v-model="selectedGameReport" :options="sortedGameReports" >
      <template #option="slotProps">
        <span v-if="slotProps.option.startTime">{{slotProps.option.startTime.toLocaleString(DateTime.TIME_24_SIMPLE)}} -&nbsp;  </span>
        <span v-if="slotProps.option.teamAReport.team">{{slotProps.option.teamAReport.team.name}}</span>
        <span v-else>...</span>&nbsp;vs.&nbsp;
        <span v-if="slotProps.option.teamBReport.team">{{slotProps.option.teamBReport.team.name}}</span>
        <span v-else>...</span>

        <vue-feather
            type="alert-octagon"
            class="text-red-600 p-1"
            v-if="!checkGameReportNecessary(slotProps.option)"/>
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
      v-if="selectedGameReport"
      :rules="filteredRules"
      :extra-time-options="props.extraTimeOptions"
      :game-types="props.gameTypes"
      :index="currentReportIndex"
      @delete-this-report="(report) => startDeleteReport(report)"
  />

  <Dialog
      v-model:visible="deleteReportDialogVisible"
      :modal="true"
  >
    <template #header>
      <span>Delete Game Report</span>
    </template>

      <p>Are you sure you want to delete this game report?</p>
      <span v-if="reportToDelete?.startTime">{{reportToDelete.startTime.toLocaleString(DateTime.TIME_24_SIMPLE)}} -&nbsp;  </span>
      <span v-if="reportToDelete?.teamAReport.team">{{reportToDelete.teamAReport.team.name}}</span>
      <span v-else>...</span>&nbsp;vs.&nbsp;
      <span v-if="reportToDelete?.teamBReport.team">{{reportToDelete.teamBReport.team.name}}</span>
      <span v-else>...</span>

    <template #footer>
      <Button @click="deleteReport" class="p-button-danger">Delete</Button>
      <Button @click="cancelDeleteReport" class="p-button-secondary">Cancel</Button>
    </template>
  </Dialog>

</template>



<style scoped>

</style>