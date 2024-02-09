<script lang="ts" setup>
import TeamSelector from './components/team/TeamSelector.vue'
import {computed, onMounted, ref, watch} from "vue";
import TournamentSelector from "./components/tournament/TournamentSelector.vue";
import GameReports from "@/components/gameReport/GameReports.vue";
import PitchReports from "@/components/pitch/PitchReports.vue";
import {
  uploadReport
} from "@/utils/api/report_api";
import SubmitReport from "@/components/SubmitReport.vue";
import {ReportEditStage, useReportStore} from "@/utils/edit_report_store";
import type {Team} from "@/types/team_types";
import type {DatabaseTournament} from "@/types/tournament_types";
import {useI18n} from "vue-i18n";

const store = useReportStore()





const allStages = ref([
  ReportEditStage.SelectTournament,
  ReportEditStage.SelectTeams,
  ReportEditStage.EditGameReports,
  ReportEditStage.EditPitchReports,
  ReportEditStage.Submit
])

const i18 = useI18n()
const {t} = i18
const translateStageToName = ref(new Map<ReportEditStage, string>());
translateStageToName.value.set(ReportEditStage.SelectTournament, t("sections.selectTournament"))
translateStageToName.value.set(ReportEditStage.SelectTeams, t("sections.selectTeams"))
translateStageToName.value.set(ReportEditStage.EditGameReports, t("sections.editGameReports"))
translateStageToName.value.set(ReportEditStage.EditPitchReports, t("sections.editPitchReports"))
translateStageToName.value.set(ReportEditStage.Submit, t("sections.submitReport"))
watch(i18.locale, () => {
  console.log("New locale",i18.locale)
  translateStageToName.value.set(ReportEditStage.SelectTournament, t("sections.selectTournament"))
  translateStageToName.value.set(ReportEditStage.SelectTeams, t("sections.selectTeams"))
  translateStageToName.value.set(ReportEditStage.EditGameReports, t("sections.editGameReports"))
  translateStageToName.value.set(ReportEditStage.EditPitchReports, t("sections.editPitchReports"))
  translateStageToName.value.set(ReportEditStage.Submit, t("sections.submitReport"))
})
const reportStarted = ref(false)



const calcStageOptions = computed(() => {
  return allStages.value.map(stage => {
    let obj = {
      name: translateStageToName.value.get(stage) || "",
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
  if (store.report.tournament && !isLoading.value) {
    current_stage.value = ReportEditStage.SelectTeams
  }
})

function select_tournament(tournament: DatabaseTournament) {
  store.report.tournament = tournament
  current_stage.value = ReportEditStage.SelectTeams
}


async function start_report() {
  isLoading.value = true
  try {
    store.report.id = await uploadReport(store.report)
    reportStarted.value = true
    baseReportJustUploaded.value = true
    current_stage.value = ReportEditStage.EditGameReports
    location.href = "/report/edit/" + store.report.id + "#edit_game_reports"
  } catch (e) {
    console.error(e)
  } finally {
    isLoading.value = false
  }
}

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
  await store.loadReport(id)
  if (store.currentErrors.length > 0) {
    //TODO: Show error
  } else {
    reportStarted.value = true
  }
  isLoading.value = false
}
const dontUpdateHref = ref(false)

watch(current_stage, (new_stage, old_stage) => {
  console.log("stage change ",old_stage,new_stage)
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
  if (!dontUpdateHref.value) {
    switch (new_stage) {
      case ReportEditStage.SelectTournament:
        location.href = "#select_tournament"
        break
      case ReportEditStage.SelectTeams:
        location.href = "#select_teams"
        break
      case ReportEditStage.EditGameReports:
        location.href = "#edit_game_reports"
        break
      case ReportEditStage.EditPitchReports:
        location.href = "#edit_pitch_reports"
        break
      case ReportEditStage.Submit:
        location.href = "#submit"
        break
    }
  } else {
    console.log("Not updating href as it is loaded on begin")
    dontUpdateHref.value = false
  }

})


function selectStageByURL(specifier:string) {
  //First remove anything trailing the slash
  specifier = specifier.replace(/\/.*/, "")
  console.log("Modified specifier",specifier)
  dontUpdateHref.value = true
  switch (specifier) {
    case "select_tournament":
      current_stage.value = ReportEditStage.SelectTournament
      break
    case "select_teams":
      current_stage.value = ReportEditStage.SelectTeams
      break
    case "edit_game_reports":
      current_stage.value = ReportEditStage.EditGameReports
      break
    case "edit_pitch_reports":
      current_stage.value = ReportEditStage.EditPitchReports
      break
    case "submit":
      current_stage.value = ReportEditStage.Submit
      break
    default:
      current_stage.value = ReportEditStage.SelectTeams
  }
}


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
          .then(() => {
            selectStageByURL(loc.hash.substring(1))
          })
    }


  }

})

function backToDashboard() {
  location.href = "/"
}

function navigate(stage:ReportEditStage) {
  current_stage.value = stage
}

</script>

<template>
  <div class="xl:absolute m-2">
    <select v-model="$i18n.locale" class="p-2">
      <option v-for="locale in $i18n.availableLocales" :key="`locale-${locale}`" :value="locale">{{ locale }}</option>
    </select>
  </div>

  <div class="mx-auto w-full xl:w-[47rem] navigation-bar">
    <SelectButton
        v-model="current_stage"
        :options="calcStageOptions"
        option-disabled="disabled"
        option-label="name"
        option-value="stage"

    />
  </div>
<transition-group name="p-message" tag="div">
  <Message v-for="msg in store.currentErrors" severity="error" :key="msg.timestamp">{{msg.message}}</Message>
</transition-group>


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
      @navigate="navigate"
      />
  <template v-if="readyStartReport">
    <div class="mx-auto w-full xl:w-[47rem]">

      <h4>Code:</h4>
      <div class="flex flex-row justify-center navi">
        <SelectButton
            v-model="store.report.gameCode"
            :options="store.codes"
            option-label="name"
            class="m-2 flex flex-row justify-center"
        />
      </div>
      <div class="flex flex-row justify-center">
        <Button
            :disabled="store.report.gameCode === undefined"
            class="p-button-rounded p-button-lg m-2"
            @click="start_report"
        >{{ $t("report.start") }}
        </Button>
      </div>
    </div>
  </template>
  <div class="mx-auto mt-10 flex flex-row justify-center">
  <div v-if="store.report.id && current_stage !== ReportEditStage.Submit">
    <Button
        class="p-button-success"
        @click="() => navigate(ReportEditStage.Submit)">
      {{ $t("report.submit") }}
    </Button>
  </div>
  </div>

  <div class="mx-auto mt-2 flex flex-row justify-center">
  <div>
    <Button
        class="p-button-outlined p-button-secondary"
        @click="backToDashboard">
      {{ $t("navigation.backToDashboard") }}
    </Button>
  </div>
  </div>


  <Dialog
      v-model:visible="isLoading"
      :closable="false"
      :close-on-escape="false"
      :modal="true"
      content-class="loading-dialog"
  ><div class="shrink">{{ $t("navigation.loading") }} </div></Dialog>

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
h4 {
  @apply text-center;
  @apply text-lg;
  @apply font-bold;
  @apply mt-1;
  @apply mb-2;
}



.navigation-bar:deep(.p-button:first-of-type) {
  @apply rounded-none;
  @apply md:rounded-l-lg;
}
.navigation-bar:deep(.p-button:last-of-type) {
  @apply rounded-none;
  @apply md:rounded-r-lg;
}
.navigation-bar:deep(.p-button) {
  @apply text-sm;
  @apply md:text-base;
}
.navigation-bar:deep(.p-selectbutton) {
  @apply flex-row flex justify-center flex-wrap;
}
</style>
