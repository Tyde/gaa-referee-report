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
import ShowPitchReport from "@/components/showReport/ShowPitchReport.vue";
import {pitchDEOtoPitch} from "@/utils/api/pitch_api";

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

const pitchReports = computed(() => {
  return rawTournamentReportDEO.value?.pitches?.map(pitchDEO => {
        const report = gameReports.value
            ?.map(it => it.report)
            ?.find(it => it.id === pitchDEO.report)
        if(report && store.pitchVariables) {
          return pitchDEOtoPitch(
              pitchDEO,
              report,
              store.pitchVariables
          )
        }
        return undefined
      }
  ).filter(it => it !== undefined)
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
      referee: referee,
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

const referees = computed(() => {
  return gameReports
      ?.value
      ?.filter((obj, index, self) => {
        const found = self.findIndex((it) => {
          return it.report.referee.id === obj.report.referee.id
        })
        return index === found
      })
      ?.map(it => it.report.referee)
      ?.map(it => {
        return {
          ...it,
          fullName: it.firstName + " " + it.lastName
        }
      })
})

const gameCodes = computed(() => {
  return store.codes.filter(it => {
        return gameReports?.value?.some(gr => gr.report.gameCode.id === it.id)
      }
  )
})

const gameTypes = computed(() => {
  return store.gameTypes.filter(it => {
    return gameReports?.value?.some(gr => gr.gameType?.id === it.id)
  })
})


const gameReports = computed(() => {
  if (rawTournamentReportDEO.value) {
    return transformDEO(rawTournamentReportDEO.value)
        .toSorted(compareGameReportByStartTime)
  } else {
    return [] as Array<GameReport>
  }
})

const selectedReferees = ref<Referee[]>([])
const onlyShowRedCardReports = ref(false)
const selectedCodes = ref<GameCode[]>([])
const selectedGameTypes = ref<GameCode[]>([])

const filteredGameReports = computed(() => {
  return gameReports
      ?.value
      ?.filter(it => {
        if (selectedReferees.value.length > 0) {
          return selectedReferees.value.some(ref => ref.id === it.report.referee.id)
        } else {
          return true
        }
      })
      ?.filter(it => {
        if (onlyShowRedCardReports.value) {
          return it.teamAReport.disciplinaryActions.some(da => da.redCardIssued || da.rule?.isRed) ||
              it.teamBReport.disciplinaryActions.some(da => da.redCardIssued || da.rule?.isRed)
        } else {
          return true
        }
      })
      ?.filter(it => {
        if (selectedCodes.value.length > 0) {
          return selectedCodes.value.some(code => code.id === it.report.gameCode.id)
        } else {
          return true
        }
      })
      ?.filter(it => {
        if (selectedGameTypes.value.length > 0) {
          return selectedGameTypes.value.some(gt => gt.id === it.gameType?.id)
        } else {
          return true
        }
      })

})

const allAdditionalInfo = computed(() => {
  return gameReports
      ?.value
      ?.filter((obj, index, self) => {
        const found = self.findIndex((it) => {
          return it.report.referee.id === obj.report.referee.id
        })
        return index === found
      })
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

const gameCodeColorMap: { [key: string]: string } = {
  "Hurling": 'hurling-game',
  "Camogie": 'camogie-game',
  "Mens Football": 'mens-football-game',
  "Ladies Football": 'ladies-football-game'
}

function gameCodeColor(gameCode: GameCode) {
  return gameCodeColorMap[gameCode.name]
}

function scrollToPitchReports() {
  const pitchReports = document.getElementById('pitch-reports')
  if (pitchReports) {
    pitchReports.scrollIntoView({behavior: "smooth"})
  }
}

function scrollToGameReports() {
  const gameReports = document.getElementById('game-reports')
  if (gameReports) {
    gameReports.scrollIntoView({behavior: "smooth"})
  }
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
          <span class="italic">Additional Information: </span><br>
          <span v-html="formattedAdditionalInfoString"></span>
        </div>
        <Accordion :active-index="null" >
          <AccordionTab header="Filter">
          <div class="filter-row">
            <label for="filter">Filter by Referee</label>
            <div>
              <Button v-if="selectedReferees.length > 0" link @click="selectedReferees = []">Clear</Button>
              <MultiSelect
                  :options="referees"
                  v-model="selectedReferees"
                  option-label="fullName"
                  placeholder="Select Referee"
              >
              </MultiSelect>
            </div>

          </div>
          <div class="filter-row">
            <label for="filter">Only show Reports with red card:</label>
            <Checkbox
                v-model="onlyShowRedCardReports"
                binary
            />

          </div>
          <div class="filter-row">
            <label for="filter">Filter by code:</label>
            <div>
              <Button v-if="selectedCodes.length > 0" link @click="selectedCodes = []">Clear</Button>
              <MultiSelect
                  :options="gameCodes"
                  v-model="selectedCodes"
                  option-label="name"
                  placeholder="Select Game Code"/>
            </div>

          </div>
          <div class="filter-row">
            <label for="filter">Filter by game type:</label>
            <div>
              <Button v-if="selectedGameTypes.length > 0" link @click="selectedGameTypes = []">Clear</Button>
              <MultiSelect
                  :options="gameTypes"
                  v-model="selectedGameTypes"
                  option-label="name"
                  placeholder="Select Game Type"/>
            </div>

          </div>
          </AccordionTab>
        </Accordion>
        <div class="flex flex-row justify-center">
          <Button  link @click="scrollToPitchReports">Scroll to Pitch Reports</Button>
        </div>
        <h2 id="game-reports">Game Reports</h2>
        <div
            v-for="gr in filteredGameReports" class="game-report-style"
            key="gr.report.id"
            :class="gameCodeColor(gr.report.gameCode)"
        >
          <h3>{{ gr.startTime?.toLocaleString(DateTime.TIME_24_SIMPLE) }} - {{ gr.report.gameCode.name }}</h3>
          <h3>Referee: {{ gr.report.referee.firstName }} {{ gr.report.referee.lastName }}</h3>
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
            <span class="italic">Referee Notes: </span>&nbsp;{{ gr.generalNotes }}

          </div>
        </div>

        <h2 id="pitch-reports">Pitch Reports</h2>
        <div class="flex flex-row justify-center">
          <Button  link @click="scrollToGameReports">Scroll to Game Reports</Button>
        </div>
        <ShowPitchReport v-for="pitchReport in pitchReports" :key="pitchReport?.id" :pitch-report="pitchReport" :show-referee-name="true"/>
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

.filter-row {
  @apply flex flex-row justify-between items-center;
  @apply mt-2;
  @apply min-h-9;
}

</style>
