<script lang="ts" setup>

import {computed, onMounted, ref, watch} from "vue";
import {DateTime} from "luxon";
import {submitReportToServer, updateReportAdditionalInformation} from "@/utils/api/report_api";
import debounce from "debounce"
import {ReportEditStage, useReportStore} from "@/utils/edit_report_store";
import {delay} from "@/utils/api/api_utils";
import {useI18n} from "vue-i18n";
import {
  DisciplinaryActionIssue,
  GameReportIssue,
  GameReportIssues,
  InjuryIssue,
  PitchReportIssue
} from "@/types/issue_types";
/*
const props = defineProps<{
  tournament: DatabaseTournament,
  gameReports: Array<GameReport>,
  pitches: Array<Pitch>,
  report: Report
}>()*/
const store = useReportStore()

const {t} = useI18n()
const emit = defineEmits<{
  (e: 'navigate', stage:ReportEditStage): void
}>()
const tournamentIssues = computed(() => {
  if (store.report.tournament == undefined) {
    return t("report.issues.noTournamentSelected")
  } else {
    return ""
  }
})




const gameReportIssues = store.gameReportIssues

const pitchReportIssues = store.pitchReportIssues

const reportReadyForUpload = computed(() => {
  return gameReportIssues.filter((gris) =>
          gameReportIssuesAreSerious(gris)
      ).length == 0 &&
      pitchReportIssues.length == 0 &&
      tournamentIssues.value.trim().length == 0
})
const isUploadingToServer = ref(false);
const uploadComplete = ref(false);

async function uploadAllData() {
  isUploadingToServer.value = true
  await delay(500) // Lets wait for the unwinding of unmounted components to finish

  await store.waitForAllTransfersDone()
  let promisesPitch = store.pitchReports.map((pitch) => {
    return store.sendPitchReport(pitch, true)
  })
  let promisesReports = store.gameReports.map((gameReport) => {
    store.sendGameReport(gameReport,true)
  })
  await Promise.all(promisesPitch).catch((e) => {
    store.newError(e)
  })
  await Promise.all(promisesPitch).catch((e) => {
    store.newError(e)
  })
  await Promise.all(promisesReports).catch((e) => {
    store.newError(e)
  })
  let updateDiAndInjuries = store.gameReports.map((gameReport) => {
    if (gameReport.id) {
      let tAdiP = gameReport.teamAReport.disciplinaryActions.map((da) => {
        store.sendDisciplinaryAction(da, gameReport,true)
      })
      let tBdiP = gameReport.teamBReport.disciplinaryActions.map((da) => {
        store.sendDisciplinaryAction(da, gameReport,true)
      })
      let tAinP = gameReport.teamAReport.injuries.map((injury) => {
        store.sendInjury(injury, gameReport, true)
      })
      let tBinP = gameReport.teamBReport.injuries.map((injury) => {
        store.sendInjury(injury, gameReport, true)
      })
      //concat all four arrays
      let allPromises = tAdiP.concat(tBdiP).concat(tAinP).concat(tBinP)
      return Promise.all(allPromises)
    } else {
      return Promise.reject("Game report not uploaded even though it should have been uploaded before")
    }
  })
  await Promise.all(updateDiAndInjuries).catch((e) => {
    store.newError(e)
  })


  isUploadingToServer.value = false
  uploadComplete.value = true
}

const debouncedUpload = debounce(() => {
  if(store.report.additionalInformation && store.report.additionalInformation.trim().length > 0) {
    updateReportAdditionalInformation(store.report).catch((e) => {
      store.newError(e)
    })
  }
}, 2000)
watch(() => store.report.additionalInformation, () => {
  debouncedUpload()

})

onMounted(() => {
  if (reportReadyForUpload.value) {
    console.log("onMounted called")
    uploadAllData()
  }
})

async function submitReport() {
  if (uploadComplete.value) {
    if(store.report.additionalInformation && store.report.additionalInformation.trim().length > 0) {
      await updateReportAdditionalInformation(store.report).catch((e) => {
        store.newError(e)
      })
    }
    submitReportToServer(store.report).then(async () => {
      location.href = "/#/report/" + store.report.id
    }).catch((e) => {
      store.newError(e)
    })
  }
  console.log("submit report")
}

function gameReportIssuesAreSerious(gris: GameReportIssues) {
  return !(
      gris.issues.includes(GameReportIssue.NoScores) &&
      gris.issues.length == 1 &&
      gris.injuriesIssues.length == 0 &&
      gris.disciplinaryActionIssues.length == 0
  )
}

function goToGameReport(id:number | undefined) {
  store.gameReports.forEach((report, index) => {
    if(report.id == id) {
      store.selectedGameReportIndex = index
    }
  })
  emit("navigate",ReportEditStage.EditGameReports)
}
</script>

<template>
  <div class="flex justify-center content-center">
    <div class="flex flex-col w-screen md:w-[32rem] p-4 m-4">
      <div
          v-if="isUploadingToServer"
          class="bg-blue-200 rounded p-4 text-center">
        <i class="pi pi-spin pi-spinner"></i>
        {{ $t('report.updatingDatabase') }}
      </div>
      <div
          v-if="tournamentIssues.trim().length > 0"
          class="bg-red-300 rounded-lg p-4">
        {{ tournamentIssues }}
      </div>

      <div
          v-for="gris in gameReportIssues"
          :class="{
            'bg-red-300': gameReportIssuesAreSerious(gris),
            'bg-amber-200': !gameReportIssuesAreSerious(gris),
          }"

          class="rounded-lg p-4 m-4">
      <span
          v-if="gris.gameReport.startTime">{{ gris.gameReport.startTime.toLocaleString(DateTime.TIME_24_SIMPLE) }} -&nbsp;</span>
        <span v-if="gris.gameReport.teamAReport.team">{{ gris.gameReport.teamAReport.team.name }}</span>
        <span v-else>...</span>&nbsp;vs.&nbsp;
        <span v-if="gris.gameReport.teamBReport.team">{{ gris.gameReport.teamBReport.team.name }}</span>
        <span v-else>...</span>
        {{ $t('report.issues.hasFollowingIssues') }}:
        <ul>
          <li v-for="issue in gris.issues">
            <template v-if="issue===GameReportIssue.NoGameType">
              {{ $t('report.issues.noGameTypeSelected') }}:
            </template>
            <template v-else-if="issue===GameReportIssue.NoStartingTime">
              {{ $t('report.issues.noStartingTimeSelected') }}:
            </template>
            <template v-else-if="issue===GameReportIssue.NoExtraTimeOption">
              {{ $t('report.issues.noExtraTimeOptionSelected') }}:
            </template>
            <template v-else-if="issue===GameReportIssue.NoTeamA">
              {{ $t('report.issues.noTeamASelected') }}:
            </template>
            <template v-else-if="issue===GameReportIssue.NoTeamB">
              {{ $t('report.issues.noTeamBSelected') }}:
            </template>
            <template v-else-if="issue===GameReportIssue.NoScores">
              {{ $t('report.issues.noScoresEntered') }}:
            </template>
            <template v-else-if="issue===GameReportIssue.TeamAEqualTeamB">
              {{ $t('report.issues.teamAEqualTeamB') }}:
            </template>
            <!-- go to report button -->
            <br><Button
              class="p-button-info p-button-raised p-button-text"
              @click="goToGameReport(gris.gameReport.id)"
          > {{ $t('report.issues.goToGameReport') }}:</Button>
          </li>
          <template v-for="inIssues in gris.injuriesIssues">
            <li v-for="issue in inIssues.issues">
              <template v-if="issue===InjuryIssue.NoDetails">
                {{ $t('report.issues.injuryNoDetails') }}: {{ inIssues.action.firstName }} {{ inIssues.action.lastName }}
              </template>
              <template v-if="issue===InjuryIssue.NoName">
                {{ $t('report.issues.injuryNoName') }}:: {{ inIssues.action.details }}
              </template>
              <br><Button
                class="p-button-info p-button-raised p-button-text"
                @click="goToGameReport(gris.gameReport.id)"
            >{{ $t('report.issues.goToGameReport') }}:</Button>
            </li>
          </template>
          <template v-for="disIssues in gris.disciplinaryActionIssues">
            <li v-for="issue in disIssues.issues">
              <template v-if="issue===DisciplinaryActionIssue.NoName">
                {{ $t('report.issues.disciplinaryActionNoName') }}:: {{ disIssues.action.details }}
              </template>
              <template v-if="issue===DisciplinaryActionIssue.NoNumber">
                {{ $t('report.issues.disciplinaryActionNoNumber') }}{{ disIssues.action.firstName }} {{
                  disIssues.action.lastName
                }}
              </template>
              <template v-if="issue===DisciplinaryActionIssue.NoRule">
                {{ $t('report.issues.disciplinaryActionNoRule') }} {{ disIssues.action.firstName }} {{ disIssues.action.lastName }}
              </template>
              <br><Button
                class="p-button-info p-button-raised p-button-text"
                @click="goToGameReport(gris.gameReport.id)"
            >{{ $t('report.issues.goToGameReport') }}</Button>
            </li>
          </template>
        </ul>
      </div>

      <div
          v-for="prTuple in pitchReportIssues"
          class="bg-red-300 rounded-lg p-4 m-4">
        <span v-if="prTuple[0].name">{{ prTuple[0].name }}</span>
        <span v-else>{{ $t('pitchReport.unnamedPitch') }}</span>
        &nbsp;{{ $t('report.issues.hasFollowingIssues') }}:
        <ul>
          <li v-for="issue in prTuple[1]">
            <template v-if="issue===PitchReportIssue.NoName">
              {{ $t('report.issues.pitchNoName') }}
            </template>
            <template v-else-if="issue===PitchReportIssue.NoSurface">
              {{ $t('report.issues.pitchNoSurface') }}
            </template>
            <template v-else-if="issue===PitchReportIssue.MissingDimension">
              {{ $t('report.issues.pitchMissingDimension') }}
            </template>
            <template v-else-if="issue===PitchReportIssue.MarkingsIncomplete">
              {{ $t('report.issues.pitchMarkingsIncomplete') }}
            </template>
            <template v-else-if="issue===PitchReportIssue.GoalInfoIncomplete">
              {{ $t('report.issues.pitchGoalInfoIncomplete') }}
            </template>
          </li>
        </ul>
      </div>
      <div class="field p-2 justify-center flex-col text-center">
        <label for="additionalInfo">{{ $t('report.additionalInformation') }}:</label><br>

        <Textarea
            id="additionalInfo"
            v-model="store.report.additionalInformation"
            class="w-full m-4"
            cols="40"
            placeholder=""
            rows="4"
        />
      </div>
      <Button
          v-if="reportReadyForUpload && uploadComplete"
          @click="submitReport"
          class="w-full m-4"
      >
        {{ $t('report.submit') }}
      </Button>

      <!--

          <div
              class="bg-green-200 rounded p-4"
              v-else
          >
            <span>Database updated</span>
          </div>
          -->
    </div>
  </div>
</template>


<style scoped>

</style>
