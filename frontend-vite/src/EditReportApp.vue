<script setup lang="ts">
import TeamSelector from './components/TeamSelector.vue'
import {computed, onMounted, ref, watch} from "vue";
import type {
  DatabaseTournament,
  ExtraTimeOption,
  GameCode,
  GameReport,
  GameType,
  Report,
  Rule,
  Team,
  Tournament
} from "@/types";
import TournamentSelector from "./components/TournamentSelector.vue";
import {fromDateToDateString} from "@/utils/gobal_functions";
import GameReports from "@/components/GameReports.vue";
import {DateTime} from "luxon";

enum ReportEditStage {
  SelectTournament,
  SelectTeams,
  EditGameReports,
  EditPitchReports,
  Submit
}



const allStages = ref([
    ReportEditStage.SelectTournament,
    ReportEditStage.SelectTeams,
    ReportEditStage.EditGameReports,
    ReportEditStage.EditPitchReports,
    ReportEditStage.Submit
])

const translateStageToName = new Map<ReportEditStage,string>();
translateStageToName.set(ReportEditStage.SelectTournament,"Select Tournament")
translateStageToName.set(ReportEditStage.SelectTeams,"Select Teams")
translateStageToName.set(ReportEditStage.EditGameReports,"Edit Game Reports")
translateStageToName.set(ReportEditStage.EditPitchReports,"Edit Pitch Reports")
translateStageToName.set(ReportEditStage.Submit,"Submit")
const reportStarted = ref(false)

const calcStageOptions = computed(() => {
  return allStages.value.map(stage => {
    let obj = {
      name: translateStageToName.get(stage) || "",
      disabled: false,
      stage: stage
    }
    if(!reportStarted.value) {
      if (stage === ReportEditStage.EditGameReports  ||
          stage === ReportEditStage.EditPitchReports ||
          stage === ReportEditStage.Submit) {
        obj.disabled = true
      }
    }
    return obj
  })
})


const readyStartReport = computed(() => {
  return current_report.value.selectedTeams.length>1 &&
      current_report.value.tournament &&
      !reportStarted.value
})
const isLoading = ref(false)
const current_stage = ref<ReportEditStage>(ReportEditStage.SelectTournament)
const codes = ref<Array<GameCode>>([])
const rules = ref<Array<Rule>>([])
const gameTypes = ref<Array<GameType>>([])
const extraTimeOptions = ref<Array<ExtraTimeOption>>([])
const current_report = ref<Report>({} as Report)
const allGameReports = ref<Array<GameReport>>([])
current_report.value.selectedTeams = []

function submit_teams(teams_added: Array<Team>) {
  current_report.value.selectedTeams.length = 0;
  current_report.value.selectedTeams = current_report.value.selectedTeams.concat(teams_added)
}

function select_tournament(tournament:DatabaseTournament) {
  current_report.value.tournament = tournament
  current_stage.value = ReportEditStage.SelectTeams
}

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


async function start_report() {
  const requestOptions = {
    method: "POST",
    headers: {
      'Content-Type': 'application/json;charset=utf-8'
    },
    body: JSON.stringify({
      tournament:current_report.value.tournament.id,
      selectedTeams:current_report.value.selectedTeams.map(value => value.id),
      gameCode:current_report.value.gameCode.id
    })
  };
  isLoading.value = true
  const response = await fetch("/api/report/new",requestOptions)
  const id = await response.json()
  current_report.value.id = id
  reportStarted.value = true
  current_stage.value = ReportEditStage.EditGameReports
  isLoading.value = false
}




onMounted(()=> {
  get_codes()
  get_rules()
  get_game_report_variables()
})
</script>

<template>


    <SelectButton
        v-model="current_stage"
        :options="calcStageOptions"
        option-disabled="disabled"
        option-label="name"
        option-value="stage"
    />


    <TournamentSelector
        v-if="current_stage === ReportEditStage.SelectTournament"
        :already-selected-tournament="current_report.tournament"
        @tournament_selected="select_tournament"
    />
    <TeamSelector
        v-if="current_stage === ReportEditStage.SelectTeams"
        @submit-teams="submit_teams"
        :already-selected-teams="current_report.selectedTeams"
    />

    <GameReports
      v-if="current_stage === ReportEditStage.EditGameReports"
      :report="current_report"
      :rules="rules"
      :gameTypes="gameTypes"
      :extraTimeOptions="extraTimeOptions"
      :game-reports = "allGameReports"
      @update:gameReports = "newReports => allGameReports.value = newReports"
    />

  <template v-if="readyStartReport">
    Code:
    <SelectButton
      v-model="current_report.gameCode"
      :options="codes"
      option-label="name"
    />
    <Button
        @click="start_report"
        class="p-button-rounded p-button-lg"
        v-if="current_report.gameCode"
    >Start Report</Button>
  </template>

</template>

<style scoped>

</style>
