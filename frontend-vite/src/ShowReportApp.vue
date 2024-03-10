<script lang="ts" setup>

import {onMounted, ref} from "vue";
import {
  completeReportDEOToReport,
  extractGameReportsFromCompleteReportDEO,
  loadReportDEO,
  loadReportDEOByUUID
} from "@/utils/api/report_api";
import type {ExtraTimeOption, GameCode, GameType} from "@/types";
import {getPitchVariables, pitchDEOtoPitch} from "@/utils/api/pitch_api";
import ShowFullReport from "@/components/showReport/ShowFullReport.vue";
import type {Report} from "@/types/report_types";
import type {GameReport} from "@/types/game_report_types";
import type {Rule} from "@/types/rules_types";
import type {Pitch, PitchVariables} from "@/types/pitch_types";

import {CompleteReportDEO} from "@/types/complete_report_types";
import {usePublicStore} from "@/utils/public_store";

const props = defineProps<{
  id?: number
}>()

const store = usePublicStore()

const isLoading = ref(false)
const loadingComplete = ref(false)


const currentReport = ref<Report>({} as Report)
const allGameReports = ref<Array<GameReport>>([])
const allPitchReports = ref<Array<Pitch>>([])



async function downloadReport(id: number) {
  isLoading.value = true
  try {
    const report: CompleteReportDEO = await loadReportDEO(id)
    await handleDownloadedReport(report)

  } catch (e) {
    console.log(e)

  } finally {
    isLoading.value = false
    loadingComplete.value = true
  }
}

async function downloadReportByUUID(uuid: string) {
  isLoading.value = true
  try {
    const report: CompleteReportDEO = await loadReportDEOByUUID( uuid)
    await handleDownloadedReport(report)

  } catch (e) {
    console.log(e)

  } finally {
    isLoading.value = false
    loadingComplete.value = true
  }
}

async function handleDownloadedReport(report: CompleteReportDEO) {
  await Promise.all(promiseList.value)
  currentReport.value = completeReportDEOToReport(report, store.codes)
  allGameReports.value = extractGameReportsFromCompleteReportDEO(
      report,
      currentReport.value,
      store.gameTypes,
      store.extraTimeOptions,
      store.rules,
      store.teams
  )


  let pitches = report.pitches
  const pV = store.pitchVariables
  if (pitches && pV) {

    allPitchReports.value = pitches.map(pitch => {
      return pitchDEOtoPitch(pitch, currentReport.value, pV)
    }) || []
  }
}
const promiseList = ref<Promise<any>[]>([])
onMounted(() => {
  isLoading.value = true

  promiseList.value.push(store.loadAuxiliaryInformationFromSerer().then(() => {
    console.log("Loaded auxiliary information")
  }))
  promiseList.value.push(store.loadTeams())

  let loc = new URL(location.href)
  // If loc contains "share" in path, then we have to load by uuid given
  if(loc.pathname.includes("share")) {
    let uuid = loc.pathname.split("/")[3]
    if (uuid) {
      downloadReportByUUID(uuid)
    }
  }
  let id = props.id || Number(loc.pathname.split("/")[3])
  if (id) {
    downloadReport(id)
  }

})

/*const html2Pdf = ref(null);

function print() {
  html2Pdf.value?.generatePdf();
}*/

function backToDashboard() {
  location.href = "/"
}
</script>

<template>
  <div class="z-[10000] flex relative">
    <span v-if="isLoading">Loading</span>
  </div>
  <template v-if="loadingComplete">

    <div v-if="!props.id" class="fixed top-4 right-4 no-print">
      <Button @click="backToDashboard">Back to Dashboard</Button>
    </div>
    <ShowFullReport
        :current-report="currentReport"
        :all-game-reports="allGameReports"
        :all-pitch-reports="allPitchReports"
        />
  </template>


</template>

<style>
@media print {
  .no-print, .no-print * {
    display: none !important;
  }
}
</style>


