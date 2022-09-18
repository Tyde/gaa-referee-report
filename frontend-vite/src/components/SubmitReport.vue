<script lang="ts" setup>

import type {DatabaseTournament, DisciplinaryAction, GameReport, Injury, Pitch, Report} from "@/types";
import {computed, onMounted, ref, watch} from "vue";
import {DateTime} from "luxon";
import {uploadPitch} from "@/utils/api/pitch_api";
import {createGameReport, updateGameReport} from "@/utils/api/game_report_api";
import {disciplinaryActionIsBlank} from "@/utils/api/disciplinary_action_api";
import {injuryIsBlank} from "@/utils/api/injuries_api";
import {updateReportAdditionalInformation} from "@/utils/api/report_api";
import debounce from "debounce"

const props = defineProps<{
  tournament: DatabaseTournament,
  gameReports: Array<GameReport>,
  pitches: Array<Pitch>,
  report: Report
}>()


const tournamentIssues = computed(() => {
  if (props.tournament == undefined) {
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
  NoNumber,
  NoDetails
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
      if (disciplinaryAction.details.trim().length == 0) {
        issues.push(DisciplinaryActionIssue.NoDetails)
      }
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
  }).filter((dai)=> (dai?.issues.length || 0) > 0)
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
  }).filter((ii)=> (ii?.issues.length || 0) > 0)
      .filter((ii): ii is InjuriesIssues => (!!ii))
}

const gameReportIssues = computed(() => {
  return props.gameReports.map((gameReport) => {
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
  return props.pitches.map((pitch) => {
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
  let promisesPitch = props.pitches.map((pitch) => {
    return uploadPitch(pitch)
  })
  let promisesReports = props.gameReports.map((gameReport) => {
    if (gameReport.id === undefined) {
      return createGameReport(gameReport)
    } else {
      return updateGameReport(gameReport)
    }
  })
  await Promise.all(promisesPitch)
  await Promise.all(promisesReports)
  isUploadingToServer.value = false
  uploadComplete.value = true
}

const debouncedUpload = debounce(() => {
  updateReportAdditionalInformation(props.report)
}, 2000)
watch(() => props.report.additionalInformation, () => {
  debouncedUpload()

})

onMounted(() => {
  if (reportReadyForUpload.value) {
    uploadAllData()
  }
})

function submitReport() {

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
</script>

<template>
  <div class="flex justify-center content-center">
    <div class="flex flex-col w-280 p-4 m-4">
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
            <template v-if="issue==GameReportIssue.NoGameType">
              No game type selected
            </template>
            <template v-else-if="issue==GameReportIssue.NoStartingTime">
              No starting time selected
            </template>
            <template v-else-if="issue==GameReportIssue.NoExtraTimeOption">
              No extra time option selected
            </template>
            <template v-else-if="issue==GameReportIssue.NoTeamA">
              No home team selected
            </template>
            <template v-else-if="issue==GameReportIssue.NoTeamB">
              No away team selected
            </template>
            <template v-else-if="issue==GameReportIssue.NoScores">
              No scores entered - This might be correct if the game was a draw.
            </template>
          </li>
          <template v-for="inIssues in gris.injuriesIssues">
            <li v-for="issue in inIssues.issues">
              <template v-if="issue==InjuryIssue.NoDetails">
                No details entered for Injury of {{ inIssues.action.firstName}} {{inIssues.action.lastName }}
              </template>
              <template v-if="issue==InjuryIssue.NoName">
                Missing name for Injury with details: {{ inIssues.action.details }}
              </template>
            </li>
          </template>
          <template v-for="disIssues in gris.disciplinaryActionIssues">
            <li v-for="issue in disIssues.issues">
              <template v-if="issue==DisciplinaryActionIssue.NoName">
                Missing name for disciplinary action with details: {{ disIssues.action.details }}
              </template>
              <template v-if="issue==DisciplinaryActionIssue.NoDetails">
                No details entered for disciplinary action of {{ disIssues.action.firstName}} {{disIssues.action.lastName }}
              </template>
              <template v-if="issue==DisciplinaryActionIssue.NoNumber">
                Missing number for disciplinary action of {{ disIssues.action.firstName}} {{disIssues.action.lastName }}
              </template>
              <template v-if="issue==DisciplinaryActionIssue.NoRule">
                Missing rule for disciplinary action of {{ disIssues.action.firstName}} {{disIssues.action.lastName }}
              </template>
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
            <template v-if="issue==PitchReportIssue.NoName">
              No name entered
            </template>
            <template v-else-if="issue==PitchReportIssue.NoSurface">
              No surface selected
            </template>
            <template v-else-if="issue==PitchReportIssue.MissingDimension">
              At least one pitch dimension is missing
            </template>
            <template v-else-if="issue==PitchReportIssue.MarkingsIncomplete">
              Markings incomplete
            </template>
            <template v-else-if="issue==PitchReportIssue.GoalInfoIncomplete">
              Goal information incomplete
            </template>
          </li>
        </ul>
      </div>
      <div class="field p-2 justify-center flex-col text-center">
        <label for="additionalInfo">Additional comments for the report:</label><br>

        <Textarea
            id="additionalInfo"
            v-model="report.additionalInformation"
            class="w-280"
            cols="40"
            placeholder=""
            rows="4"
        />
      </div>
      <Button
          v-if="reportReadyForUpload && uploadComplete"
          @click="submitReport">
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