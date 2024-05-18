<script lang="ts" setup>
import TeamSelector from './components/team/TeamSelector.vue'
import {computed, onMounted, ref, watch} from "vue";
import TournamentSelector from "./components/tournament/TournamentSelector.vue";
import GameReports from "@/components/gameReport/GameReports.vue";
import PitchReports from "@/components/pitch/PitchReports.vue";
import {uploadReport} from "@/utils/api/report_api";
import SubmitReport from "@/components/SubmitReport.vue";
import {ReportEditStage, useReportStore} from "@/utils/edit_report_store";
import type {Team} from "@/types/team_types";
import {useI18n} from "vue-i18n";
import {useConfirm} from "primevue/useconfirm";
import {useLocalStorage} from "@vueuse/core";

const store = useReportStore()
const i18 = useI18n()
const confirmDialog = useConfirm()

const {t} = i18

const reportStarted = ref(false)
const isLoading = ref(false)
const currentStage = ref<ReportEditStage>(ReportEditStage.SelectTournament)
const baseReportJustUploaded = ref(false)
const allStages = ref([
  ReportEditStage.SelectTournament,
  ReportEditStage.SelectTeams,
  ReportEditStage.EditGameReports,
  ReportEditStage.EditPitchReports,
  ReportEditStage.Submit
])
const dontUpdateHref = ref(false)



const translateStageToName = ref(new Map<ReportEditStage, string>());
const storedLocale = useLocalStorage("locale", i18.locale.value)
watch(i18.locale, () => {
  translateStageToName.value.set(ReportEditStage.SelectTournament, t("sections.selectTournament"))
  translateStageToName.value.set(ReportEditStage.SelectTeams, t("sections.selectTeams"))
  translateStageToName.value.set(ReportEditStage.EditGameReports, t("sections.editGameReports"))
  translateStageToName.value.set(ReportEditStage.EditPitchReports, t("sections.editPitchReports"))
  translateStageToName.value.set(ReportEditStage.Submit, t("sections.submitReport"))
}, {immediate: true})

watch(i18.locale, () => {
  storedLocale.value = i18.locale.value
})


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


function submitTeams(teams_added: Array<Team>) {
  store.report.selectedTeams.length = 0;
  store.report.selectedTeams = store.report.selectedTeams.concat(teams_added)
}


watch(() => store.report.tournament, () => {
  if (store.report.tournament && !isLoading.value) {
    currentStage.value = ReportEditStage.SelectTeams
  }
})

async function startReport() {
  isLoading.value = true
  uploadReport(store.report)
      .then((id) => {
        store.report.id = id
        reportStarted.value = true
        baseReportJustUploaded.value = true
        currentStage.value = ReportEditStage.EditGameReports
        location.href = "/report/edit/" + store.report.id + "#edit_game_reports"
      })
      .catch((e) => {
        store.newError(e)
      })
      .finally(() => {
        isLoading.value = false
      })
}

async function updateReport() {
  uploadReport(store.report).catch((e) => {
    store.newError(e)
  })
}


async function loadAndHandleReport(id: number) {
  isLoading.value = true
  await store.loadReport(id)
  if (store.currentErrors.length > 0) {
  } else {
    reportStarted.value = true
  }
  isLoading.value = false
}


watch(currentStage, (newStage, oldStage) => {
  if (oldStage === ReportEditStage.SelectTeams ||
      oldStage === ReportEditStage.SelectTournament) {
    if (baseReportJustUploaded.value) {
      baseReportJustUploaded.value = false
    } else {
      if (store.report.id) {
        updateReport()
      }
    }
  }
  if (!dontUpdateHref.value) {
    switch (newStage) {
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
    dontUpdateHref.value = false
  }

})


function selectStageByURL(specifier: string) {
  //First remove anything trailing the slash
  specifier = specifier.replace(/\/.*/, "")
  dontUpdateHref.value = true
  switch (specifier) {
    case "select_tournament":
      currentStage.value = ReportEditStage.SelectTournament
      break
    case "select_teams":
      currentStage.value = ReportEditStage.SelectTeams
      break
    case "edit_game_reports":
      currentStage.value = ReportEditStage.EditGameReports
      break
    case "edit_pitch_reports":
      currentStage.value = ReportEditStage.EditPitchReports
      break
    case "submit":
      currentStage.value = ReportEditStage.Submit
      break
    default:
      currentStage.value = ReportEditStage.SelectTeams
  }
}


onMounted(() => {
  isLoading.value = true
  i18.locale.value = storedLocale.value
  let loc = new URL(location.href)
  if (loc.pathname.startsWith("/report/new")) {
    store.publicStore.loadAuxiliaryInformationFromSerer()
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
  store.uploadDataToServerBeforeLeaving().then(() => {
    console.log("No issues, leaving")
    location.href = "/"
  }).catch((msg) => {
    //alert("Could not save data: " + msg)
    confirmDialog.require({
      message: t("report.couldNotSaveDataConfirm") + "\n" + msg,
      header: t("report.couldNotSaveDataTitle"),
      icon: 'pi pi-exclamation-triangle',
      rejectClass: 'p-button-secondary p-button-outlined',
      rejectLabel: t("report.couldNotSaveDataLeave"),
      acceptLabel: t("report.couldNotSaveDataContinue"),
      accept: () => {
      },
      reject: () => {
        location.href = "/"
      }
    });
  })
}

function navigate(stage: ReportEditStage) {
  currentStage.value = stage
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
        v-model="currentStage"
        :options="calcStageOptions"
        option-disabled="disabled"
        option-label="name"
        option-value="stage"
    />
  </div>
  <transition-group name="p-message" tag="div">
    <Message v-for="msg in store.currentErrors" severity="error" :key="msg.timestamp">{{ msg.message }}</Message>
  </transition-group>


  <TournamentSelector
      v-model="store.report.tournament"
      v-if="currentStage === ReportEditStage.SelectTournament"
  />
  <TeamSelector
      v-if="currentStage === ReportEditStage.SelectTeams"
      :already-selected-teams="store.report.selectedTeams"
      @submit-teams="submitTeams"
  />

  <GameReports
      v-if="currentStage === ReportEditStage.EditGameReports"/>


  <PitchReports
      v-if="currentStage === ReportEditStage.EditPitchReports && store.publicStore.pitchVariables"
  />

  <SubmitReport
      v-if="currentStage === ReportEditStage.Submit"
      @navigate="navigate"
  />
  <template v-if="readyStartReport">
    <div class="mx-auto w-full xl:w-[47rem]">

      <h4>Code:</h4>
      <div class="flex flex-row justify-center navi">
        <SelectButton
            v-model="store.report.gameCode"
            :options="store.publicStore.codes"
            option-label="name"
            class="m-2 flex flex-row justify-center"
        />
      </div>
      <div class="flex flex-row justify-center">
        <Button
            :disabled="store.report.gameCode === undefined"
            class="p-button-rounded p-button-lg m-2"
            @click="startReport"
        >{{ $t("report.start") }}
        </Button>
      </div>
    </div>
  </template>
  <div class="mx-auto mt-10 flex flex-row justify-center">
    <div v-if="store.report.id && currentStage !== ReportEditStage.Submit">
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
  >
    <div class="shrink">{{ $t("navigation.loading") }}</div>
  </Dialog>
  <ConfirmDialog></ConfirmDialog>
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
