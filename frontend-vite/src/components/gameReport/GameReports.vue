<script setup lang="ts">

import SingleGameReport from "@/components/gameReport/SingleGameReport.vue";
import type {GameReport} from "@/types";
import {computed, onBeforeMount, ref} from "vue";
import { DateTime } from "luxon";
import {checkGameReportNecessary, checkGameReportSuggestion} from "@/utils/gobal_functions";
import {useReportStore} from "@/utils/edit_report_store";
/*
const props = defineProps<{
  report:Report,
  rules:Array<Rule>,
  gameTypes:Array<GameType>,
  extraTimeOptions:Array<ExtraTimeOption>,
  gameReports:Array<GameReport>,
}>()
const emit = defineEmits<{
  (e: 'update:gameReports', value: Array<GameReport>): void
}>()*/
const store = useReportStore()


const sortedGameReports = computed(() => {
  return store.gameReports.sort((a, b) => {
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
//const localGameReportSelection = ref<GameReport | undefined>(undefined)
const filteredRules = computed(() => {
  return store.rules.filter(rule => rule.code == store.report.gameCode.id)
})
function newGameReport() {
  let estimatedStartingTime = store.selectedGameReport?.startTime?.plus({
    minutes: 30
  })
  store.gameReports.push({
    report:store.report,
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
  store.selectedGameReportIndex = store.gameReports.length - 1
  //selectedGameReport.value = store.gameReports[store.gameReports.length-1]
}


//const currentReportIndex = ref<number>(0)
/*watch(() => store.selectedGameReport, () => {
  if (store.selectedGameReport) {
    currentReportIndex.value = store.gameReports.indexOf(selectedGameReport.value)
  }
})*/
onBeforeMount(()=> {
  if(store.gameReports.length==0){
    newGameReport()
  } else {
    store.selectedGameReportIndex = 0
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
    await store.deleteGameReport(reportToDelete.value)
        .catch((error) => {
          store.newError(error)
        })
    cancelDeleteReport()
  }

}

const gameReportsListIndices = computed(() => {
  let indices = []
  console.log("Update indices list " + store.gameReports.length)
  for(let i = 0; i < store.gameReports.length; i++) {
    indices.push(i)
  }
  return indices
})



</script>

<template>
<Toolbar>
  <template #start>
    <SelectButton v-model="store.selectedGameReportIndex" :options="gameReportsListIndices" >
      <template #option="slotProps">
        <span v-if="store.gameReports[slotProps.option].startTime">{{store.gameReports[slotProps.option].startTime.toLocaleString(DateTime.TIME_24_SIMPLE)}} -&nbsp;  </span>
        <span v-if="store.gameReports[slotProps.option].teamAReport.team">{{store.gameReports[slotProps.option].teamAReport.team.name}}</span>
        <span v-else>...</span>&nbsp;vs.&nbsp;
        <span v-if="store.gameReports[slotProps.option].teamBReport.team">{{store.gameReports[slotProps.option].teamBReport.team.name}}</span>
        <span v-else>...</span>

        <vue-feather
            type="alert-octagon"
            class="text-red-600 p-1"
            v-if="!checkGameReportNecessary(store.gameReports[slotProps.option])"/>
        <vue-feather
            type="circle"
            class="text-yellow-200 p-1"
            v-else-if="!checkGameReportSuggestion(store.gameReports[slotProps.option])"/>
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

<!--       v-model="selectedGameReport"
      v-if="selectedGameReport"
      :rules="filteredRules"
      :extra-time-options="store.extraTimeOptions"
      :game-types="store.gameTypes"
      :index="currentReportIndex" -->
  <SingleGameReport
      v-if="store.selectedGameReport"
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