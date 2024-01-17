<script setup lang="ts">
import {usePublicStore} from "@/utils/public_store";
import {computed, ref} from "vue";
import {loadPublicTournamentReport} from "@/utils/api/tournament_api";
import {PublicTournamentReportDEO} from "@/types/tournament_types";
import {DateTime} from "luxon";
import ShowDisciplinaryActionsAndInjuries from "@/components/showReport/ShowDisciplinaryActionsAndInjuries.vue";
import type {GameReportDEO} from "@/types/game_report_types";
import type {GameCode} from "@/types";
import type {Team} from "@/types/team_types";

let {id} = defineProps<{
  id: string
}>()

let store = usePublicStore()
let selectedCode = ref<GameCode | undefined>()
let tournament = computed(() => {
  return store.tournaments.filter(it => it.id == Number(id))[0]
})

let report = ref<PublicTournamentReportDEO | undefined>(undefined)
loadPublicTournamentReport(Number(id))
    .then(it => report.value = it)
    .catch(err => store.newError(err))

let gameReportsByTime = computed(() => {

    return report.value?.games.sort((first,second) => {
      return first.gameReport.startTime.diff(second.gameReport.startTime).milliseconds < 0 ? -1 : 1
    })
        .filter(it => {
          if (selectedCode.value) {
            return it.code == selectedCode.value?.id
          } else {
            return true
          }
        })
        .filter(it=>{
          if (selectedTeam.value) {
            return it.gameReport.teamA == selectedTeam.value?.id || it.gameReport.teamB == selectedTeam.value?.id
          } else {
            return true
          }
        })

})

function translateTeamIdToTeam(teamID: number) {
  return report.value?.teams.filter(it => it.id == teamID)[0]
}

function translateGameTypeIdToGameType(gameTypeID: number) {
  return store.gameTypes.filter(it => it.id == gameTypeID)[0]
}

function translateCodeIdToGameCode(codeID: number) {
  return store.codes.filter(it => it.id == codeID)[0]
}


function isDraw(gameReport:GameReportDEO) {
  return (gameReport.teamAGoals*3 + gameReport.teamAPoints) == (gameReport.teamBGoals*3 + gameReport.teamBPoints)
}
function isTeamAWinner(gameReport:GameReportDEO) {
  return (gameReport.teamAGoals*3 + gameReport.teamAPoints) > (gameReport.teamBGoals*3 + gameReport.teamBPoints)
}

let codesAtTournament = computed(() => {
  return [...new Set(report.value?.games.map(it => it.code))].map(it => translateCodeIdToGameCode(it))
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

const allTeams = computed(() => {
  let teams = report.value?.games
      .map(it => it.gameReport.teamA)
      .concat(report.value?.games
          .map(it => it.gameReport.teamB))

  let uniqueSet = new Set(teams)
  return [...uniqueSet].map(it => translateTeamIdToTeam(it))
})
const selectedTeam = ref<Team>()

</script>

<template>

  <div class="flex flex-row justify-center">
  <div class="flex flex-col lg:w-7/12 w-full">
    <div class="m-2">
      <h1>Tournament: {{ tournament.date.toISODate() }} -  {{tournament.name}} in {{tournament.location}}</h1>
    </div>
    <div class="flex flex-row justify-center content-centerm-2">
      <SelectButton
          v-model="selectedCode"
          :options="codesAtTournament"
          optionLabel="name"
          />
      <div v-if="selectedCode" class="ml-2">
        <Button @click="selectedCode = undefined">X</Button>
      </div>
    </div>
    <div class="flex flex-row justify-center content-center m-2">
      <Dropdown
          v-model="selectedTeam"
          :options="allTeams"
          optionLabel="name"
          placeholder="Filter by team"
          filter
          />
      <div v-if="selectedTeam" class="ml-2">
        <Button @click="selectedTeam = undefined">X</Button>
      </div>
    </div>
    <div
        v-for="game in gameReportsByTime"
        key="game.gameReport.id"
        class="rounded-lg bg-gray-200 p-2 m-2 shadow-lg"
        :class="gameCodeColor(translateCodeIdToGameCode(game.code))"
    >
      <h3>{{ game.gameReport.startTime.toLocaleString(DateTime.TIME_24_SIMPLE) }} - {{ translateCodeIdToGameCode(game.code)?.name}}</h3>
      <span v-if="game.gameReport.gameType" class="italic">{{ translateGameTypeIdToGameType(game.gameReport.gameType)?.name }}</span>

      <div class="flex flex-row">
        <div class="flex-1 flex">
          <div class="flex flex-col flex-grow">
            <div class="flex flex-row">
              <div
                  class="flex-1"
                  :class="{ 'font-bold': isTeamAWinner(game.gameReport) && !isDraw(game.gameReport) }"
              >
                {{ translateTeamIdToTeam(game.gameReport.teamA)?.name }}</div>
              <div class="flex-1 text-right">{{ game.gameReport.teamAGoals }} - {{ game.gameReport.teamAPoints }} (
                {{game.gameReport.teamAGoals*3 + game.gameReport.teamAPoints}}
                )</div>
            </div>
          </div>
        </div>
        <div class="flex-1 flex ml-4">
          <div class="flex flex-col flex-grow">
            <div class="flex flex-row">
              <div class="flex-1">{{ game.gameReport.teamBGoals }} - {{ game.gameReport.teamBPoints }} (
                {{game.gameReport.teamBGoals*3 + game.gameReport.teamBPoints}}
                )</div>
              <div
                  class="flex-1 text-right"
                  :class="{ 'font-bold': !isTeamAWinner(game.gameReport) && !isDraw(game.gameReport) }"
              >{{ translateTeamIdToTeam(game.gameReport.teamB)?.name }}</div>

            </div>
          </div>
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


.hurling-game {
  background-color: #c7522a;
  @apply text-white;
}
.camogie-game {
  background-color: #e5c185;
}
.mens-football-game {
  background-color: #74a892;

}
.ladies-football-game {
  background-color: #008585;
  @apply text-white;

}
</style>
