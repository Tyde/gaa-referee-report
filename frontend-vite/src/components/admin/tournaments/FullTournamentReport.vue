<script setup lang="ts">

import {usePublicStore} from "@/utils/public_store";
import {computed, onMounted, ref} from "vue";
import {loadCompleteTournamentReport} from "@/utils/api/tournament_api";
import {Report} from "@/types/report_types";
import type {GameReport} from "@/types/game_report_types";
import {compareGameReportByStartTime} from "@/types/game_report_types";
import {CompleteTournamentReportDEO} from "@/types/complete_tournament_types";
import {Referee} from "@/types/referee_types";
import {gameReportDEOToGameReport} from "@/utils/api/game_report_api";
import {DateTime} from "luxon";
import ShowDisciplinaryActionsAndInjuries from "@/components/showReport/ShowDisciplinaryActionsAndInjuries.vue";
import {GameCode} from "@/types";

const props = defineProps<{
  id: string
}>()

const store = usePublicStore()

const rawTournamentReportDEO = ref<CompleteTournamentReportDEO>()
loadCompleteTournamentReport(parseInt(props.id))
    .then(it => rawTournamentReportDEO.value = it)
    .catch(e => store.newError(e))


const tournament = computed(() => {
  return rawTournamentReportDEO.value?.tournament
})
const tournamentDate = computed(() => {
  return tournament.value?.date.toISODate()
})

function transformDEO(ctr: CompleteTournamentReportDEO): Array<GameReport> {
  const tournament = ctr.tournament
  return ctr.games.map(it => {
    const code = store.findCodeById(it.refereeReport.code)

    const referee = Referee.parse({
      id: it.refereeReport.refereeId,
      firstName: it.refereeReport.refereeName,
      lastName: "",
      mail: ""
    })
    const report = {
      id: it.refereeReport.id,
      tournament: tournament,
      selectedTeams: ctr.teams,
      gameCode: code,
      additionalInformation: it.refereeReport.additionalInformation,
      isSubmitted: it.refereeReport.isSubmitted,
      submitDate: it.refereeReport.submitDate ?? undefined,
      referee: referee
    } as Report
    let gr = it.gameReport
    return gameReportDEOToGameReport(
        gr,
        report,
        store.gameTypes,
        store.extraTimeOptions,
        store.rules
    )

  }).filter(it => it !== undefined) as Array<GameReport>

}


const gameReports = computed(() => {
  if (rawTournamentReportDEO.value) {
    return transformDEO(rawTournamentReportDEO.value)
        .sort(compareGameReportByStartTime)
  } else {
    return [] as Array<GameReport>
  }
})

const allAdditionalInfo = computed(() => {
  return gameReports
      ?.value
      ?.map(it => {
        return {
          info: it.report.additionalInformation ?? "",
          referee: it.report.referee.firstName + " " + it.report.referee.lastName
        }
      })
      ?.filter(it => it.info !== "")
})

const formattedAdditionalInfoString = computed(() => {
  return allAdditionalInfo.value?.map(it => {
    return it.referee + ": " + it.info
  }).join("<br>\n")
})
onMounted(() => {

})

const gameCodeColorMap: {[key: string]:string} = {
  "Hurling": 'hurling-game',
  "Camogie": 'camogie-game',
  "Mens Football": 'mens-football-game',
  "Ladies Football": 'ladies-football-game'
}

function gameCodeColor(gameCode: GameCode) {
  return gameCodeColorMap[gameCode.name]
}
</script>

<template>
  <div class="flex flex-row justify-center">
    <div class="flex flex-col content-center justify-center">
      <!-- Header -->
      <div class="bg-white w-full sm:w-[680px]">
        <h1>Tournament Report</h1>
        <h2>Tournament: {{ tournamentDate }} - {{ tournament?.name ?? "" }} in
          {{ tournament?.location ?? "" }}</h2>
        <div v-if="allAdditionalInfo && allAdditionalInfo.length > 0">
          <span class="italic">Additional Information: </span>
          {{ formattedAdditionalInfoString }}
        </div>

        <h2>Game Reports</h2>
        <div
            v-for="gr in gameReports" class="game-report-style"
            key="gr.report.id"
            :class="gameCodeColor(gr.report.gameCode)"
        >
          <h3>{{ gr.startTime?.toLocaleString(DateTime.TIME_24_SIMPLE) }} - {{gr.report.gameCode.name}}</h3>
          <h3>Referee: {{ gr.report.referee.firstName}} {{gr.report.referee.lastName}}</h3>
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
          <div
              v-if="gr.generalNotes && gr.generalNotes !== ''"
              class="flex flex-row justify-center m-2"
          >
            <span class="italic">Referee Notes: </span>
            {{ gr.generalNotes }}
          </div>
        </div>
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
  @apply pt-6 p-4 border-t-2 border-t-gray-600 break-inside-avoid;
}

</style>
