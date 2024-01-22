<script setup lang="ts">
import {computed} from "vue";
import {DateTime} from "luxon";
import ShowDisciplinaryActionsAndInjuries from "@/components/showReport/ShowDisciplinaryActionsAndInjuries.vue";
import ShowPitchReport from "@/components/showReport/ShowPitchReport.vue";
import type {Report} from "@/types/report_types";
import type {GameReport} from "@/types/game_report_types";
import type {Pitch} from "@/types/pitch_types";
import {compareGameReportByStartTime} from "@/types/game_report_types";

const props = defineProps<{
  currentReport: Report,
  allGameReports: Array<GameReport>,
  allPitchReports: Array<Pitch>,
}>()

const tournamentDate = computed(() => {
  return props.currentReport.tournament.date.toISODate()

})

const gameReportsByTime = computed(() => {
  return props.allGameReports.sort(compareGameReportByStartTime)
})


</script>

<template>
  <div class="flex flex-row justify-center">
    <div class="flex flex-col">
      <div class="bg-white w-full sm:w-[680px]">
        <h1>Tournament Report</h1>
        <h2>Tournament: {{ tournamentDate }} - {{ currentReport.tournament.name }} in
          {{ currentReport.tournament.location }}</h2>
        <div class="italic">Referee: {{ currentReport.referee.firstName }} {{ currentReport.referee.lastName }} -
          {{ currentReport.referee.mail }}
        </div>
        <div v-if="currentReport.additionalInformation && currentReport.additionalInformation !== ''">
          <span class="italic">Additional Information: </span>
          {{ currentReport.additionalInformation}}
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
        <template v-if="allPitchReports.length > 0">
          <h2>Pitch Reports:</h2>
          <div v-for="pitch in allPitchReports">
            <ShowPitchReport :pitch-report="pitch"/>
          </div>
        </template>
        <template v-else>
          <h2>No pitch reports</h2>
        </template>
      </div>

    </div>
  </div>
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
    font-size: 12px;
  }
}
</style>
