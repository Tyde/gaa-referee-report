<script lang="ts" setup>

import {onMounted, ref, watch, watchEffect} from "vue";
import type {ExtraTimeOption, GameReport, GameType, Rule} from "@/types";
import SingleGameReportSingleTeam from "@/components/SingleGameReportSingleTeam.vue";
import {DateTime} from "luxon";
import {checkGameReportMinimal} from "@/utils/gobal_functions";


const props = defineProps<{
  modelValue: GameReport,
  rules: Array<Rule>,
  gameTypes: Array<GameType>,
  extraTimeOptions: Array<ExtraTimeOption>,
  index: number,
}>()


const emit = defineEmits<{
  (e: 'update:modelValue', value: GameReport): void
}>()

const isLoading = ref(false)




async function sendGameReport(gameReport: GameReport) {
  console.log("Sending game report?")
  if (checkGameReportMinimal(gameReport)) {
    console.log("Sending game report!")
    if (gameReport.id) {
      await updateGameReport(gameReport)
    } else {
      await createGameReport(gameReport)
    }
  }
}

function gameReportToGameReportDAO(gameReport: GameReport) {
  return {
    "id": gameReport.id,
    "report": gameReport.report.id,
    "teamA": gameReport.teamAReport.team?.id,
    "teamB": gameReport.teamBReport.team?.id,
    "teamAGoals": gameReport.teamAReport.goals,
    "teamAPoints": gameReport.teamAReport.points,
    "teamBGoals": gameReport.teamBReport.goals,
    "teamBPoints": gameReport.teamBReport.points,
    "startTime": gameReport.startTime?.toISO(),
    "extraTime": gameReport.extraTime?.id,
    "gameType": gameReport.gameType?.id,
    "umpirePresentOnTime": gameReport.umpirePresentOnTime,
    "umpireNotes": gameReport.umpireNotes,
  }
}

async function updateGameReport(gameReport:GameReport) {
  isLoading.value = true
  const requestOptions = {
    method: "POST",
    headers: {
      'Content-Type': 'application/json;charset=utf-8'
    },
    body: JSON.stringify(
        gameReportToGameReportDAO(gameReport)
    )
  }
  const response = await fetch("/api/gamereport/update",requestOptions)
  const data = await response.json()
  if (data.id) {
    console.log("Update complete")
  } else {
    console.log(data)
  }
  isLoading.value = false

}

const sendingCreateRequest = ref(false)

async function createGameReport(gameReport:GameReport) {
  if (!sendingCreateRequest.value) {
    sendingCreateRequest.value = true
    const requestOptions = {
      method: "POST",
      headers: {
        'Content-Type': 'application/json;charset=utf-8'
      },
      body: JSON.stringify(gameReportToGameReportDAO(gameReport))
    };
    isLoading.value = true
    const response = await fetch("/api/gamereport/new", requestOptions)
    const data = await response.json()

    console.log(data)
    if (data.id) {
      gameReport.id = data.id
      console.log("Game report created")
      sendingCreateRequest.value = false
    }
    isLoading.value = false
  }
}

watch(props.modelValue, (newVal, odlVal) => {

  emit('update:modelValue', props.modelValue)
})

watch(()=>props.modelValue,(value,oldValue)=> {
  console.log("Switched report, oldReport is ")
  sendGameReport(oldValue)
})
const internalDateStart = ref<Date | undefined>()
var dateUpdateLock = false
watch(internalDateStart, () => {
  if (internalDateStart.value && !dateUpdateLock) {
    props.modelValue.startTime = DateTime.fromJSDate(internalDateStart.value)
    dateUpdateLock = true
  } else {
    dateUpdateLock = false
  }

})
watch(() => props.modelValue.startTime, () => {
  if (props.modelValue.startTime != undefined) {
    if (!dateUpdateLock) {
      console.log("Setting internal date")
      internalDateStart.value = props.modelValue.startTime.toJSDate()
      dateUpdateLock = true
    } else {

      dateUpdateLock = false
    }
  } else {
    internalDateStart.value = undefined
  }
})

onMounted(() => {
  console.log(props.modelValue)
})
</script>
<template>

  <div class="grid grid-cols-2">
    <div class="col-span-2 flex flex-wrap">
      <div class="field p-2">
        <label for="timeStartGame">Throw in time:</label><br>
        <Calendar
            id="timeStartGame"
            v-model="internalDateStart"
            :showSeconds="false"
            :showTime="true"
            :time-only="true"
            :class="{
                'to-be-filled':internalDateStart==undefined
            }"
        />

      </div>
      <div class="field p-2 object-center">
        <label for="umpirePresentCheckBox"> Umpires present on time:</label><br>
        <Checkbox
            id="umpirePresentCheckBox"
            v-model="props.modelValue.umpirePresentOnTime"
            :binary="true"/>
      </div>
      <div v-if="!modelValue.umpirePresentOnTime" class="field p-2">
        <label for="umpireNotes">Comment on Umpires:</label><br>
        <InputText id="umpireNotes" v-model="modelValue.umpireNotes" type="text"/>

      </div>
      <div class="field p-2">
        <label for="extraTimeSelect">Extra time:</label><br>
        <Dropdown
            id="extraTimeSelect"
            v-model="props.modelValue.extraTime"
            :options="props.extraTimeOptions"
            option-label="name"
            placeholder="Extra Time"
            :class="{
                'to-be-filled':props.modelValue.extraTime==undefined
            }"
        >

        </Dropdown>
      </div>
      <div class="field p-2">
        <label for="gameTypeSelect">Extra time:</label><br>
        <Dropdown
            id="gameTypeSelect"
            v-model="props.modelValue.gameType"
            :options="props.gameTypes"
            option-label="name"
            placeholder="Game Type"
            :class="{
                'to-be-filled':props.modelValue.gameType==undefined
            }"
        >

        </Dropdown>
      </div>
    </div>
    <SingleGameReportSingleTeam
        v-model="modelValue.teamAReport"
        :rules="rules"
        :selected-teams="modelValue.report.selectedTeams"
    />

    <SingleGameReportSingleTeam
        v-model="modelValue.teamBReport"
        :rules="rules"
        :selected-teams="modelValue.report.selectedTeams"
        class="p-4"
    />
  </div>
</template>


<style>
.to-be-filled {
  @apply ring-2;
  @apply ring-red-400;
  @apply ring-offset-1;
}
</style>