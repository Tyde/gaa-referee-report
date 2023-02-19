<script lang="ts" setup>

import {onMounted, ref} from "vue";
import type {CompleteReportDEO} from "@/utils/api/report_api";
import {completeReportDEOToReport, extractGameReportsFromCompleteReportDEO, loadReportDEO} from "@/utils/api/report_api";
import type {ExtraTimeOption, GameCode, GameType} from "@/types";
import type {PitchVariables} from "@/utils/api/pitch_api"
import {getPitchVariables, pitchDEOtoPitch} from "@/utils/api/pitch_api";
import ShowFullReport from "@/components/showReport/ShowFullReport.vue";
import type {Report} from "@/types/report_types";
import type {GameReport} from "@/types/game_report_types";
import type {Rule} from "@/types/rules_types";
import type {Pitch} from "@/types/pitch_types";

const props = defineProps<{
  id?: number
}>()

const isLoading = ref(false)
const loadingComplete = ref(false)
const codes = ref<Array<GameCode>>([])
const rules = ref<Array<Rule>>([])
const gameTypes = ref<Array<GameType>>([])
const extraTimeOptions = ref<Array<ExtraTimeOption>>([])
const pitchVariables = ref<PitchVariables | undefined>()

const currentReport = ref<Report>({} as Report)
const allGameReports = ref<Array<GameReport>>([])
const allPitchReports = ref<Array<Pitch>>([])

async function get_codes() {
  let res = await fetch("/api/codes")
  codes.value = (await res.json()) as Array<GameCode>

}

async function get_rules() {
  let res = await fetch("/api/rules")
  rules.value = (await res.json()) as Array<Rule>
}

async function get_game_report_variables() {
  let res = await fetch("/api/game_report_variables")
  let combined = (await res.json())
  gameTypes.value = combined.gameTypes as Array<GameType>
  extraTimeOptions.value = combined.extraTimeOptions as Array<ExtraTimeOption>
}

async function load_pitch_variables() {
  await getPitchVariables().then(res => {
    pitchVariables.value = res
  }, err => {
    console.error(err)
  })

}

async function waitForAllVariablesPresent() {
  const start_time = new Date().getTime()
  while (true) {
    if (
        codes.value.length > 0 &&
        rules.value.length > 0 &&
        gameTypes.value.length > 0 &&
        extraTimeOptions.value.length > 0 &&
        pitchVariables.value
    ) {
      break
    }
    if ((new Date()).getTime() > start_time + 3000) {
      throw new Error("Timeout waiting for variables")
    }
    console.log("Waiting for data")
    await new Promise(resolve => setTimeout(resolve, 500));
  }
}

async function downloadReport(id: number) {
  isLoading.value = true
  try {
    const report: CompleteReportDEO = await loadReportDEO(id)
    await waitForAllVariablesPresent()
    currentReport.value = completeReportDEOToReport(report, codes.value)
    allGameReports.value = extractGameReportsFromCompleteReportDEO(
        report,
        currentReport.value,
        gameTypes.value,
        extraTimeOptions.value,
        rules.value
    )


    let pitches = report.pitches
    const pV = pitchVariables.value
    if (pitches && pV) {

      allPitchReports.value = pitches.map(pitch => {
        return pitchDEOtoPitch(pitch, currentReport.value, pV)
      }) || []
    }

  } catch (e) {
    console.log(e)

  } finally {
    isLoading.value = false
    loadingComplete.value = true
  }
}

onMounted(() => {
  isLoading.value = true
  get_codes()
  get_rules()
  get_game_report_variables()
  load_pitch_variables()
  let loc = new URL(location.href)
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


