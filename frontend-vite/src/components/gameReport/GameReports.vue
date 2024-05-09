<script setup lang="ts">

import SingleGameReport from "@/components/gameReport/SingleGameReport.vue";
import {computed, onBeforeMount, ref, watch} from "vue";
import { DateTime } from "luxon";
import {checkGameReportNecessary, checkGameReportSuggestion} from "@/utils/gobal_functions";
import {useReportStore} from "@/utils/edit_report_store";
import type {GameReport} from "@/types/game_report_types";
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
  return store.gameReports.toSorted((a, b) => {
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
  return store.publicStore.rules.filter(rule => rule.code == store.report.gameCode.id)
})
function newGameReport() {

  let estimatedStartingTime = store.selectedGameReport?.startTime?.plus({
    minutes: 30
  })
  if(!estimatedStartingTime) {
    estimatedStartingTime = store.report.tournament.date?.set({
      hour: 9,
      minute: 0
    })
  }
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
    extraTime: store.publicStore.extraTimeOptions[0],
    generalNotes: "",
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
    console.log("locaiotn",location.href)
    let queryIdSplit = location.href.split("#")[1]?.split("/")[1]
    console.log("quid split",queryIdSplit)
    if(queryIdSplit) {
      let queryId = parseInt(queryIdSplit)
      console.log("QueryID:",queryId)
      store.gameReports.forEach((report, index) => {
        if(report.id == queryId) {
          store.selectedGameReportIndex = index
        }
      })
    } else {
      if(!store.selectedGameReport) {
        store.selectedGameReportIndex = 0
      }
    }
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

const currentReportToBeDeleted = ref(false)
async function deleteReport() {
  if(reportToDelete.value != undefined) {
    currentReportToBeDeleted.value = true
    await store.deleteGameReport(reportToDelete.value)
        .catch((error) => {
          store.newError(error)
        })
    currentReportToBeDeleted.value = false
    cancelDeleteReport()
  }

}

const gameReportsListIndices = computed(() => {
  let indices = []
  for(let i = 0; i < sortedGameReports.value.length; i++) {
    indices.push(store.gameReports.indexOf(sortedGameReports.value[i]))
  }
  return indices
})

watch(() => store.selectedGameReport, () => {
  if(store.selectedGameReport?.id) {
    location.href="#edit_game_reports/"+store.selectedGameReport?.id
  } else {
    location.href="#edit_game_reports/new"
  }
} )




</script>

<template>
<Toolbar>
  <template #start>
    <SelectButton
        v-model="store.selectedGameReportIndex"
        :options="gameReportsListIndices"
        class="gamelist"
    >
      <template #option="slotProps">
        <span v-if="store.gameReports[slotProps.option].startTime">{{store.gameReports[slotProps.option].startTime?.toLocaleString(DateTime.TIME_24_SIMPLE)}} -&nbsp;  </span>
        <span v-if="store.gameReports[slotProps.option].teamAReport.team">{{store.gameReports[slotProps.option].teamAReport.team?.name}}</span>
        <span v-else>...</span>&nbsp;vs.&nbsp;
        <span v-if="store.gameReports[slotProps.option].teamBReport.team">{{store.gameReports[slotProps.option].teamBReport.team?.name}}</span>
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
    <Button @click="newGameReport" class="p-button-success min-w-0"><i class="pi pi-plus"></i> <template v-if="store.gameReports.length < 2">&nbsp;Add another game report</template></Button>
  </template>
</Toolbar>


  <SingleGameReport
      v-if="store.selectedGameReport"
      @delete-this-report="(report) => startDeleteReport(report)"
      :to-be-deleted="currentReportToBeDeleted"
  />

  <Dialog
      v-model:visible="deleteReportDialogVisible"
      :modal="true"
  >
    <template #header>
      <span>{{ $t('gameReport.deleteConfirmTitle') }}</span>
    </template>

      <p>{{ $t('gameReport.deleteConfirmText') }}</p>
      <span v-if="reportToDelete?.startTime">{{reportToDelete.startTime.toLocaleString(DateTime.TIME_24_SIMPLE)}} -&nbsp;  </span>
      <span v-if="reportToDelete?.teamAReport.team">{{reportToDelete.teamAReport.team.name}}</span>
      <span v-else>...</span>&nbsp;vs.&nbsp;
      <span v-if="reportToDelete?.teamBReport.team">{{reportToDelete.teamBReport.team.name}}</span>
      <span v-else>...</span>

    <template #footer>
      <Button @click="deleteReport" class="p-button-danger">{{ $t('general.delete') }}</Button>
      <Button @click="cancelDeleteReport" class="p-button-secondary">{{ $t('general.cancel') }}</Button>
    </template>
  </Dialog>

</template>



<style scoped>
.gamelist:deep(.p-button) {
  @apply border-gray-200 border-[1px];
  @apply md:border-[1px];
}
.gamelist:deep(.p-button:not(:last-child)) {
  border-right: 1px solid;
  @apply border-gray-200;

  @apply md:w-fit w-full;

}

.gamelist:deep(.p-button:first-of-type) {
  @apply rounded-none;
}
.gamelist:deep(.p-button:last-of-type) {
  @apply rounded-none;
}

.p-selectbutton {
  @apply flex-row flex justify-center flex-wrap;
}

</style>
