<script lang="ts" setup>
import TeamSelector from './components/team/TeamSelector.vue'
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
import TournamentSelector from "./components/tournament/TournamentSelector.vue";
import GameReports from "@/components/gameReport/GameReports.vue";
import PitchReports from "@/components/pitch/PitchReports.vue";
import {
  CompleteReportDEO,
  completeReportDEOToReport,
  extractGameReportsFromCompleteReportDEO,
  loadReportDEO,
  uploadReport
} from "@/utils/api/report_api";
import {getPitchVariables, pitchDEOtoPitch, type PitchVariables} from "@/utils/api/pitch_api";
import SubmitReport from "@/components/SubmitReport.vue";
import {useReportStore} from "@/utils/edit_report_store";

const store = useReportStore()

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
  return store.report.selectedTeams.length > 1 &&
      store.report.tournament &&
      !reportStarted.value
})
const isLoading = ref(false)
const current_stage = ref<ReportEditStage>(ReportEditStage.SelectTournament)
/*
const codes = ref<Array<GameCode>>([])
const rules = ref<Array<Rule>>([])
const gameTypes = ref<Array<GameType>>([])
const extraTimeOptions = ref<Array<ExtraTimeOption>>([])
const currentReport = ref<Report>({} as Report)
const allGameReports = ref<Array<GameReport>>([])
const allPitchReports = ref<Array<Pitch>>([])
const pitchVariables = ref<PitchVariables | undefined>()
*/
const baseReportJustUploaded = ref(false)

function submit_teams(teams_added: Array<Team>) {
  store.report.selectedTeams.length = 0;
  store.report.selectedTeams = store.report.selectedTeams.concat(teams_added)
}

watch(() => store.report.tournament, () => {
  if (store.report.tournament) {
    current_stage.value = ReportEditStage.SelectTeams
  }
})

function select_tournament(tournament: DatabaseTournament) {
  store.report.tournament = tournament
  current_stage.value = ReportEditStage.SelectTeams
}


//TODO: integrate this into the store as action
async function start_report() {
  isLoading.value = true
  try {
    store.report.id = await uploadReport(store.report)
    reportStarted.value = true
    baseReportJustUploaded.value = true
    current_stage.value = ReportEditStage.EditGameReports
  } catch (e) {
    console.error(e)
  } finally {
    isLoading.value = false
  }
}

//TODO: integrate this into the store as action
async function updateReport() {
  //isLoading.value = true
  try {
    await uploadReport(store.report)
  } catch (e) {
    console.error(e)
  } finally {
    //isLoading.value = false
  }
}


async function loadAndHandleReport(id: number) {
  isLoading.value = true
  store.loadReport(id)
  if (store.currentError) {
    //TODO: Show error
  } else {
    reportStarted.value = true
  }
  isLoading.value = false
}

watch(current_stage, (new_stage, old_stage) => {
  if (old_stage === ReportEditStage.SelectTeams ||
      old_stage === ReportEditStage.SelectTournament) {
    if (baseReportJustUploaded.value) {
      baseReportJustUploaded.value = false
    } else {
      if (store.report.id) {
        updateReport()
      }
    }
  }
})



onMounted(() => {
  isLoading.value = true
  let loc = new URL(location.href)
  if (loc.pathname.startsWith("/report/new")) {
    store.loadAuxiliaryInformationFromSerer()
        .then(() => isLoading.value = false)
  } else if (loc.pathname.startsWith("/report/edit/")) {
    let id = Number(loc.pathname.split("/")[3])
    if (id) {
      //We are in edit mode, load all data
      isLoading.value = true
      loadAndHandleReport(id)
    }


  }

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
  />
  <TeamSelector
      v-if="current_stage === ReportEditStage.SelectTeams"
      :already-selected-teams="store.report.selectedTeams"
      @submit-teams="submit_teams"
  />

  <GameReports
      v-if="current_stage === ReportEditStage.EditGameReports" />


  <PitchReports
      v-if="current_stage === ReportEditStage.EditPitchReports && store.pitchVariables"
  />

  <SubmitReport
      v-if="current_stage === ReportEditStage.Submit"
      />
  <template v-if="readyStartReport">
    Code:
    <SelectButton
        v-model="store.report.gameCode"
        :options="store.codes"
        option-label="name"
    />
    <Button
        v-if="store.report.gameCode"
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
