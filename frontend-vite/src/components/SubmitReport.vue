<script lang="ts" setup>

import type {DatabaseTournament, GameReport, Pitch} from "@/types";
import {computed, ref} from "vue";
import {DateTime} from "luxon";

const props = defineProps<{
  tournament: DatabaseTournament,
  gameReports: Array<GameReport>,
  pitches: Array<Pitch>,
}>()
const isUploadingToServer = ref(false)


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
  NoScores
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
    console.log(issues)
    return ([gameReport, issues] as [GameReport, Array<GameReportIssue>])
  }).filter(([gameReport, issues]) => issues.length > 0)
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

</script>

<template>
  <div class="flex justify-center content-center">
    <div class="flex flex-col w-280 p-4 m-4">

      <div
          v-if="tournamentIssues.trim().length > 0"
          class="bg-red-300 rounded-lg p-4">
        {{ tournamentIssues }}
      </div>

      <div
          v-for="grTuple in gameReportIssues"
          class="bg-red-300 rounded-lg p-4 m-4">
      <span
          v-if="grTuple[0].startTime">{{ grTuple[0].startTime.toLocaleString(DateTime.TIME_24_SIMPLE) }} -&nbsp;</span>
        <span v-if="grTuple[0].teamAReport.team">{{ grTuple[0].teamAReport.team.name }}</span>
        <span v-else>...</span>&nbsp;vs.&nbsp;
        <span v-if="grTuple[0].teamBReport.team">{{ grTuple[0].teamBReport.team.name }}</span>
        <span v-else>...</span>
        &nbsp;has the following issues:
        <ul>
          <li v-for="issue in grTuple[1]">
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
      <!--
          <div
              class="bg-blue-200 rounded p-4"
              v-if="isUploadingToServer"
          >
            <span>Updating Database</span>
          </div>
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