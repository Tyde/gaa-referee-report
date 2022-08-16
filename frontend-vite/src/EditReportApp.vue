<script lang="ts" setup>
import TeamSelector from './components/TeamSelector.vue'
import {computed, onMounted, ref, watch} from "vue";
import type {
  DatabaseTournament,
  ExtraTimeOption,
  GameCode,
  GameReport,
  GameType,
  Pitch,
  Report,
  Rule,
  Team
} from "@/types";
import TournamentSelector from "./components/TournamentSelector.vue";
import GameReports from "@/components/GameReports.vue";
import PitchReports from "@/components/PitchReports.vue";
import {
  completeReportDEOToReport,
  extractGameReportsFromCompleteReportDEO,
  loadReport,
  uploadReport
} from "@/utils/api/report_api";
import type {PitchVariables} from "@/utils/api/pitch_api";
import {getPitchVariables, pitchDEOtoPitch} from "@/utils/api/pitch_api";
import SubmitReport from "@/components/SubmitReport.vue";

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

const translateStageToName = new Map<ReportEditStage, string>();
translateStageToName.set(ReportEditStage.SelectTournament, "Select Tournament")
translateStageToName.set(ReportEditStage.SelectTeams, "Select Teams")
translateStageToName.set(ReportEditStage.EditGameReports, "Edit Game Reports")
translateStageToName.set(ReportEditStage.EditPitchReports, "Edit Pitch Reports")
translateStageToName.set(ReportEditStage.Submit, "Submit")
const reportStarted = ref(false)

const calcStageOptions = computed(() => {
  return allStages.value.map(stage => {
    let obj = {
      name: translateStageToName.get(stage) || "",
      disabled: false,
      stage: stage
    }
    if (!reportStarted.value) {
      if (stage === ReportEditStage.EditGameReports ||
          stage === ReportEditStage.EditPitchReports ||
          stage === ReportEditStage.Submit) {
        obj.disabled = true
      }
    }
    return obj
  })
})


const readyStartReport = computed(() => {
  return currentReport.value.selectedTeams.length > 1 &&
      currentReport.value.tournament &&
      !reportStarted.value
})
const isLoading = ref(false)
const current_stage = ref<ReportEditStage>(ReportEditStage.SelectTournament)
const codes = ref<Array<GameCode>>([])
const rules = ref<Array<Rule>>([])
const gameTypes = ref<Array<GameType>>([])
const extraTimeOptions = ref<Array<ExtraTimeOption>>([])
const currentReport = ref<Report>({} as Report)
const allGameReports = ref<Array<GameReport>>([])
const allPitchReports = ref<Array<Pitch>>([])
const pitchVariables = ref<PitchVariables | undefined>()
const baseReportJustUploaded = ref(false)
currentReport.value.selectedTeams = []

function submit_teams(teams_added: Array<Team>) {
  currentReport.value.selectedTeams.length = 0;
  currentReport.value.selectedTeams = currentReport.value.selectedTeams.concat(teams_added)
}


watch(() => currentReport.value.tournament, () => {
  if (currentReport.value.tournament) {
    current_stage.value = ReportEditStage.SelectTeams
  }
})

function select_tournament(tournament: DatabaseTournament) {
  currentReport.value.tournament = tournament
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

async function load_pitch_variables() {
  await getPitchVariables().then(res => {
    pitchVariables.value = res
  }, err => {
    console.error(err)
  })

}


async function start_report() {
  isLoading.value = true
  try {
    currentReport.value.id = await uploadReport(currentReport.value)
    reportStarted.value = true
    baseReportJustUploaded.value = true
    current_stage.value = ReportEditStage.EditGameReports
  } catch (e) {
    console.error(e)
  } finally {
    isLoading.value = false
  }
}

async function updateReport() {
  isLoading.value = true
  try {
    await uploadReport(currentReport.value)
  } catch (e) {
    console.error(e)
  } finally {
    isLoading.value = false
  }
}


async function loadAndHandleReport(id: number) {
  isLoading.value = true
  try {
    const report = await loadReport(id)
    await waitForAllVariablesPresent()
    console.log(report)
    currentReport.value = completeReportDEOToReport(report, codes.value)
    allGameReports.value = extractGameReportsFromCompleteReportDEO(
        report,
        currentReport.value,
        gameTypes.value,
        extraTimeOptions.value,
        rules.value
    )

    allPitchReports.value = report.pitches?.map(pitch => {
      let pdtp = pitchDEOtoPitch(pitch, currentReport.value, pitchVariables.value)
      console.log(`Pitch now id: ${pitch.id}`)
      console.log(pdtp)
      return pdtp

    }) || []


    reportStarted.value = true
  } catch (e) {
    console.log(e)
  } finally {
    isLoading.value = false

  }

}

watch(current_stage, (new_stage, old_stage) => {
  if (old_stage === ReportEditStage.SelectTeams ||
      old_stage === ReportEditStage.SelectTournament) {
    if (baseReportJustUploaded.value) {
      baseReportJustUploaded.value = false
    } else {
      if (currentReport.value.id) {
        updateReport()
      }
    }
  }
})

async function waitForAllVariablesPresent() {
  var start_time = new Date().getTime()
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
async function waitForLoadingComplete() {
  await waitForAllVariablesPresent()
  isLoading.value = false
}
onMounted(() => {
  isLoading.value = true
  get_codes()
  get_rules()
  get_game_report_variables()
  load_pitch_variables()
  let loc = new URL(location.href)
  if (loc.pathname.startsWith("/report/new")) {
    waitForLoadingComplete()
  } else if (loc.pathname.startsWith("/report/edit/")) {
    let id = Number(loc.pathname.split("/")[3])
    if (id) {
      //We are in edit mode, load all data
      isLoading.value = true
      loadAndHandleReport(id)
    }


  }
  console.log()

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
      v-model="currentReport.tournament"
  />
  <TeamSelector
      v-if="current_stage === ReportEditStage.SelectTeams"
      :already-selected-teams="currentReport.selectedTeams"
      @submit-teams="submit_teams"
  />

  <GameReports
      v-if="current_stage === ReportEditStage.EditGameReports"
      :extraTimeOptions="extraTimeOptions"
      :game-reports="allGameReports"
      :gameTypes="gameTypes"
      :report="currentReport"
      :rules="rules"
      @update:gameReports="newReports => allGameReports.value = newReports"
  />

  <PitchReports
      v-if="current_stage === ReportEditStage.EditPitchReports"
      v-model="allPitchReports"
      :pitch-report-options="pitchVariables"
      :report="currentReport"
  />

  <SubmitReport
      v-if="current_stage === ReportEditStage.Submit"
      :game-reports="allGameReports"
      :tournament="currentReport.tournament"
      />
  <template v-if="readyStartReport">
    Code:
    <SelectButton
        v-model="currentReport.gameCode"
        :options="codes"
        option-label="name"
    />
    <Button
        v-if="currentReport.gameCode"
        class="p-button-rounded p-button-lg"
        @click="start_report"
    >Start Report
    </Button>
  </template>


  <Dialog
      v-model:visible="isLoading"
      :closable="false"
      :close-on-escape="false"
      :modal="true"
      content-class="loading-dialog"
  ><div class="shrink">Loading ... </div></Dialog>

</template>
<style>
.loading-dialog {
  @apply w-80;
  @apply h-64;
  @apply flex;
  @apply justify-center;
  @apply content-center;
}
</style>

<style scoped>

</style>
