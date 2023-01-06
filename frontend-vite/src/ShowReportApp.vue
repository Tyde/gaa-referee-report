<script lang="ts" setup>

import {computed, onMounted, ref} from "vue";
import type {CompleteReportDEO} from "@/utils/api/report_api";
import {completeReportDEOToReport, extractGameReportsFromCompleteReportDEO, loadReportDEO} from "@/utils/api/report_api";
import type {ExtraTimeOption, GameCode, GameReport, GameType, Pitch, Report, Rule} from "@/types";
import type {PitchVariables} from "@/utils/api/pitch_api"
import {getPitchVariables, pitchDEOtoPitch} from "@/utils/api/pitch_api";
import {DateTime} from "luxon";
import ShowDisciplinaryActionsAndInjuries from "@/components/showReport/ShowDisciplinaryActionsAndInjuries.vue";
import ShowPitchReport from "@/components/showReport/ShowPitchReport.vue";

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
  let id = Number(loc.pathname.split("/")[3])
  if (id) {
    downloadReport(id)
  }
})
const tournamentDate = computed(() => {
  return currentReport?.value?.tournament?.date?.toISODate() ?? "Loading"

})

const gameReportsByTime = computed(() => {
  return allGameReports.value.sort((a, b) => {
    if (a.startTime) {
      if (b.startTime) {
        return a.startTime > b.startTime ? 1 : -1
      } else {
        return -1
      }
    } else {
      return 0
    }
  })
})
/*const html2Pdf = ref(null);

function print() {
  html2Pdf.value?.generatePdf();
}*/

</script>

<template>
  <div class="z-[10000] flex relative">
    <span v-if="isLoading">Loading</span>
  </div>
  <template v-if="loadingComplete">
    <div class="flex flex-row justify-center">
      <div class="flex flex-col">
        <div class="bg-white w-[680px]">
          <h1>Tournament Report</h1>
          <h2>Tournament: {{ tournamentDate }} - {{ currentReport.tournament.name }} in
            {{ currentReport.tournament.location }}</h2>
          <div class="italic">Referee: {{ currentReport.referee.firstName }} {{ currentReport.referee.lastName }} -
            {{ currentReport.referee.mail }}
          </div>
          <!--<div v-if="currentReport."-->
          <div>Code: {{ currentReport.gameCode.name }}</div>
          <h2>Game Reports</h2>
          <div v-for="gr in gameReportsByTime" class="game-report-style">
            <h3>{{ gr.startTime?.toLocaleString(DateTime.TIME_24_SIMPLE) }}</h3>
            <h3>
              <template v-if="gr.umpirePresentOnTime">Umpires present on time</template>
              <template v-else>Umpires not present on time. Note: {{ gr.umpireNotes }}</template>
            </h3>
            <h3>
              {{ gr.gameType?.name }}
            </h3>
            <div class="flex flex-row">
              <div class="flex-1 flex">
                <div class="flex flex-col flex-grow">
                  <div class="flex flex-row">
                    <div class="flex-1">{{ gr.teamAReport.team?.name }}</div>
                    <div class="flex-1">{{ gr.teamAReport.goals }} - {{ gr.teamAReport.points }}</div>
                  </div>
                  <ShowDisciplinaryActionsAndInjuries :team-report="gr.teamAReport"/>
                </div>
              </div>
              <div class="flex-1 flex">
                <div class="flex flex-col flex-grow">
                  <div class="flex flex-row">
                    <div class="flex-1 text-right">{{ gr.teamBReport.goals }} - {{ gr.teamBReport.points }}</div>
                    <div class="flex-1 text-right">{{ gr.teamBReport.team?.name }}</div>
                  </div>
                  <ShowDisciplinaryActionsAndInjuries :team-report="gr.teamBReport"/>
                </div>
              </div>
            </div>
          </div>
          <hr class="border-2">
          <h2>Pitch Reports:</h2>
          <div v-for="pitch in allPitchReports">
            <ShowPitchReport :pitch-report="pitch"/>
          </div>
        </div>

      </div>
    </div>
  </template>


</template>


<style scoped>
h1 {
  @apply text-xl font-bold text-center;
}

h2 {
  @apply text-lg font-bold text-center;
}

h3 {
  @apply text-center;
}

.game-report-style {
  @apply pt-6 p-4 border-t-2 border-t-gray-600 odd:bg-white even:bg-gray-200 break-inside-avoid;
}

</style>

<style>
@media print {
  body {
    -webkit-print-color-adjust: exact; /*Chrome, Safari */
    color-adjust: exact; /*Firefox*/
    print-color-adjust: exact; /*Firefox*/
  }
}
</style>