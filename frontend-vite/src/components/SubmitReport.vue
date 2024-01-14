<script lang="ts" setup>

import {computed, onMounted, ref, watch} from "vue";
import {DateTime} from "luxon";
import {disciplinaryActionIsBlank} from "@/utils/api/disciplinary_action_api";
import {injuryIsBlank} from "@/utils/api/injuries_api";
import {submitReportToServer, updateReportAdditionalInformation} from "@/utils/api/report_api";
import debounce from "debounce"
import {ReportEditStage, useReportStore} from "@/utils/edit_report_store";
import {delay} from "@/utils/api/api_utils";
import type {DisciplinaryAction, GameReport, Injury} from "@/types/game_report_types";
import type {Pitch} from "@/types/pitch_types";
/*
const props = defineProps<{
  tournament: DatabaseTournament,
  gameReports: Array<GameReport>,
  pitches: Array<Pitch>,
  report: Report
}>()*/
const store = useReportStore()

const emit = defineEmits<{
  (e: 'navigate', stage:ReportEditStage): void
}>()
const tournamentIssues = computed(() => {
  if (store.report.tournament == undefined) {
    return "There is currently no tournament selected for this report. Please fix this before submitting."
  } else {
    return ""
  }
})



enum GameReportIssue {
  NoGameType,
  NoStartingTime,
  NoExtraTimeOption,
  NoTeamA,
  NoTeamB,
  NoScores,
  InjuriesIncomplete,
  DisciplinaryActionsIncomplete,
}

enum DisciplinaryActionIssue {
  NoRule,
  NoName,
  NoNumber
}

class DisciplinaryActionIssues {
  issues: Array<DisciplinaryActionIssue>
  action: DisciplinaryAction

  constructor(issues: Array<DisciplinaryActionIssue>, action: DisciplinaryAction) {
    this.issues = issues
    this.action = action
  }
}

class InjuriesIssues {
  issues: Array<InjuryIssue>
  action: Injury

  constructor(issues: Array<InjuryIssue>, action: Injury) {
    this.issues = issues
    this.action = action
  }
}

class GameReportIssues {
  issues: Array<GameReportIssue> = []
  disciplinaryActionIssues = new Array<DisciplinaryActionIssues>()
  injuriesIssues = new Array<InjuriesIssues>()
  gameReport: GameReport

  constructor(gameReport: GameReport, issues: Array<GameReportIssue>, disciplinaryActionIssues: Array<DisciplinaryActionIssues>, injuriesIssues: Array<InjuriesIssues>) {
    this.gameReport = gameReport
    this.issues = issues
    this.disciplinaryActionIssues = disciplinaryActionIssues
    this.injuriesIssues = injuriesIssues
  }
}

enum InjuryIssue {
  NoName,
  NoDetails
}


function disciplinaryActionIssuesForGameReport(gameReport: GameReport): Array<DisciplinaryActionIssues> {
  return gameReport.teamAReport.disciplinaryActions.concat(
      gameReport.teamBReport.disciplinaryActions
  ).map((disciplinaryAction) => {
    let issues: Array<DisciplinaryActionIssue> = []
    let fullName = disciplinaryAction.firstName + " " + disciplinaryAction.lastName
    if (!disciplinaryActionIsBlank(disciplinaryAction)) {
      if (fullName.trim().length == 0) {
        issues.push(DisciplinaryActionIssue.NoName)
      }
      if (disciplinaryAction.number === undefined) {
        issues.push(DisciplinaryActionIssue.NoNumber)
      }
      if (disciplinaryAction.rule === undefined) {
        issues.push(DisciplinaryActionIssue.NoRule)
      }
      return new DisciplinaryActionIssues(issues, disciplinaryAction)
    } else {
      return undefined
    }
  }).filter((dai) => (dai?.issues.length || 0) > 0)
      .filter((dai): dai is DisciplinaryActionIssues => !!dai)
}

function injuryIssuesForGameReport(gameReport: GameReport): Array<InjuriesIssues> {
  return gameReport.teamAReport.injuries.concat(
      gameReport.teamBReport.injuries
  ).map((injury) => {
    if (!injuryIsBlank(injury)) {
      let fullName = injury.firstName + " " + injury.lastName
      let issues: Array<InjuryIssue> = []
      if (fullName.trim().length == 0) {
        issues.push(InjuryIssue.NoName)
      }
      if (injury.details.trim().length == 0) {
        issues.push(InjuryIssue.NoDetails)
      }
      return new InjuriesIssues(issues, injury)
    } else {
      return undefined
    }
  }).filter((ii) => (ii?.issues.length || 0) > 0)
      .filter((ii): ii is InjuriesIssues => (!!ii))
}

const gameReportIssues = computed(() => {
  return store.gameReports.map((gameReport) => {
    let issues: Array<GameReportIssue> = []
    if (gameReport.gameType == undefined) {
      issues.push(GameReportIssue.NoGameType)
    }
    if (gameReport.startTime == undefined) {
      issues.push(GameReportIssue.NoStartingTime)
    }
    if (gameReport.extraTime == undefined) {
      issues.push(GameReportIssue.NoExtraTimeOption)
    }
    if (gameReport.teamAReport.team == undefined) {
      issues.push(GameReportIssue.NoTeamA)
    }
    if (gameReport.teamBReport.team == undefined) {
      issues.push(GameReportIssue.NoTeamB)
    }
    if ((gameReport.teamAReport?.goals || 0) +
        (gameReport.teamBReport?.goals || 0) +
        (gameReport.teamAReport?.points || 0) +
        (gameReport.teamBReport?.points || 0) == 0) {
      issues.push(GameReportIssue.NoScores)
    }
    let daIssues = disciplinaryActionIssuesForGameReport(gameReport)
    let inIssues = injuryIssuesForGameReport(gameReport)


    return new GameReportIssues(gameReport, issues, daIssues, inIssues)
  }).filter((gris) => (gris.issues.length +
      gris.injuriesIssues.length +
      gris.disciplinaryActionIssues.length) > 0)
})

enum PitchReportIssue {
  NoName,
  NoSurface,
  MissingDimension,
  MarkingsIncomplete,
  GoalInfoIncomplete,
}

const pitchReportIssues = computed(() => {
  return store.pitchReports.map((pitch) => {
    let issues: Array<PitchReportIssue> = []
    if (pitch.name.trim().length == 0) {
      issues.push(PitchReportIssue.NoName)
    }
    if (pitch.surface == undefined) {
      issues.push(PitchReportIssue.NoSurface)
    }
    if (pitch.length == undefined || pitch.width == undefined) {
      issues.push(PitchReportIssue.MissingDimension)
    }
    if (pitch.smallSquareMarkings == undefined ||
        pitch.penaltySquareMarkings == undefined ||
        pitch.thirteenMeterMarkings == undefined ||
        pitch.twentyMeterMarkings == undefined ||
        pitch.longMeterMarkings == undefined
    ) {
      issues.push(PitchReportIssue.MarkingsIncomplete)
    }
    if (pitch.goalPosts == undefined ||
        pitch.goalDimensions == undefined
    ) {
      issues.push(PitchReportIssue.GoalInfoIncomplete)
    }
    return ([pitch, issues] as [Pitch, Array<PitchReportIssue>])
  }).filter(([pitch, issues]) => issues.length > 0)
})

const reportReadyForUpload = computed(() => {
  return gameReportIssues.value.filter((gris) =>
          gameReportIssuesAreSerious(gris)
      ).length == 0 &&
      pitchReportIssues.value.length == 0 &&
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
    submitReportToServer(store.report).then(() => {
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
        &nbsp;Updating Database
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
        &nbsp;has the following issues:
        <ul>
          <li v-for="issue in gris.issues">
            <template v-if="issue===GameReportIssue.NoGameType">
              No game type selected
            </template>
            <template v-else-if="issue===GameReportIssue.NoStartingTime">
              No starting time selected
            </template>
            <template v-else-if="issue===GameReportIssue.NoExtraTimeOption">
              No extra time option selected
            </template>
            <template v-else-if="issue===GameReportIssue.NoTeamA">
              No team A selected
            </template>
            <template v-else-if="issue===GameReportIssue.NoTeamB">
              No team B selected
            </template>
            <template v-else-if="issue===GameReportIssue.NoScores">
              No scores entered - This might be correct if the game was a draw.
            </template>
            <!-- go to report button -->
            <br><Button
              class="p-button-info p-button-raised p-button-text"
              @click="goToGameReport(gris.gameReport.id)"
          >Go to report</Button>
          </li>
          <template v-for="inIssues in gris.injuriesIssues">
            <li v-for="issue in inIssues.issues">
              <template v-if="issue===InjuryIssue.NoDetails">
                No details entered for Injury of {{ inIssues.action.firstName }} {{ inIssues.action.lastName }}
              </template>
              <template v-if="issue===InjuryIssue.NoName">
                Missing name for Injury with details: {{ inIssues.action.details }}
              </template>
              <br><Button
                class="p-button-info p-button-raised p-button-text"
                @click="goToGameReport(gris.gameReport.id)"
            >Go to report</Button>
            </li>
          </template>
          <template v-for="disIssues in gris.disciplinaryActionIssues">
            <li v-for="issue in disIssues.issues">
              <template v-if="issue===DisciplinaryActionIssue.NoName">
                Missing name for disciplinary action with details: {{ disIssues.action.details }}
              </template>
              <template v-if="issue===DisciplinaryActionIssue.NoNumber">
                Missing number for disciplinary action of {{ disIssues.action.firstName }} {{
                  disIssues.action.lastName
                }}
              </template>
              <template v-if="issue===DisciplinaryActionIssue.NoRule">
                Missing rule for disciplinary action of {{ disIssues.action.firstName }} {{ disIssues.action.lastName }}
              </template>
              <br><Button
                class="p-button-info p-button-raised p-button-text"
                @click="goToGameReport(gris.gameReport.id)"
            >Go to report</Button>
            </li>
          </template>
        </ul>
      </div>

      <div
          v-for="prTuple in pitchReportIssues"
          class="bg-red-300 rounded-lg p-4 m-4">
        <span v-if="prTuple[0].name">{{ prTuple[0].name }}</span>
        <span v-else>Unnamed pitch</span>
        &nbsp;has the following issues:
        <ul>
          <li v-for="issue in prTuple[1]">
            <template v-if="issue===PitchReportIssue.NoName">
              No name entered
            </template>
            <template v-else-if="issue===PitchReportIssue.NoSurface">
              No surface selected
            </template>
            <template v-else-if="issue===PitchReportIssue.MissingDimension">
              At least one pitch dimension is missing
            </template>
            <template v-else-if="issue===PitchReportIssue.MarkingsIncomplete">
              Markings incomplete
            </template>
            <template v-else-if="issue===PitchReportIssue.GoalInfoIncomplete">
              Goal information incomplete
            </template>
          </li>
        </ul>
      </div>
      <div class="field p-2 justify-center flex-col text-center">
        <label for="additionalInfo">Additional comments for the report:</label><br>

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
        Submit report
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
