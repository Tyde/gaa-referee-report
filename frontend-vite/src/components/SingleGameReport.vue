<script lang="ts" setup>

import {onBeforeUnmount, onMounted, ref, watch, watchEffect} from "vue";
import type {ExtraTimeOption, GameReport, GameType, Rule} from "@/types";
import SingleGameReportSingleTeam from "@/components/SingleGameReportSingleTeam.vue";
import {DateTime} from "luxon";
import {checkGameReportMinimal} from "@/utils/gobal_functions";
import {createGameReport, updateGameReport} from "@/utils/api/game_report_api";
import GameTypeEditor from "@/components/GameTypeEditor.vue";


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
      if (!sendingCreateRequest.value) {
        sendingCreateRequest.value = true
        let id = await createGameReport(gameReport)
        gameReport.id = id
        sendingCreateRequest.value = false
      }
    }
  }
}
const gameTypeEditorVisible = ref(false)
function showGameTypeDialog() {
  gameTypeEditorVisible.value = true
}


const sendingCreateRequest = ref(false)
watch(props.modelValue, (newVal, odlVal) => {
  emit('update:modelValue', props.modelValue)
})


watch(()=>props.modelValue,(value,oldValue)=> {
  console.log("Switched report, oldReport is ")
  sendGameReport(oldValue)
})
onBeforeUnmount(() => {
  sendGameReport(props.modelValue)
})


onMounted(() => {

  console.log(props.modelValue)
  //updateInternalDateStart()
})
</script>
<template>

  <div class="grid grid-cols-2">
    <div class="col-span-2 flex flex-wrap">
      <div class="field p-2">
        <label for="timeStartGame">Throw in time:</label><br>
        <Calendar
            :model-value="modelValue.startTime?.toJSDate()"
            @update:model-value="(nD) => {modelValue.startTime = DateTime.fromJSDate(nD)}"
            id="timeStartGame"
            :showSeconds="false"
            :showTime="true"
            :time-only="true"
            :class="{
                'to-be-filled':modelValue.startTime==undefined
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
        <label for="gameTypeSelect">Game type:</label><br>
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
      <div class="field p-2 mt-6">
        <Button @click="showGameTypeDialog" class="p-button-success">
          <i class="pi pi-plus"></i> </Button>
      </div>
    </div>
    <SingleGameReportSingleTeam
        v-model="modelValue.teamAReport"
        :rules="rules"
        :selected-teams="modelValue.report.selectedTeams"
        @trigger-update="()=> sendGameReport(modelValue)"
        class="p-4"
        :report-id="modelValue.id"
        :report-passes-minimal-requirements="checkGameReportMinimal(modelValue)"
    />

    <SingleGameReportSingleTeam
        v-model="modelValue.teamBReport"
        :rules="rules"
        :selected-teams="modelValue.report.selectedTeams"
        class="p-4"
        @trigger-update="()=> sendGameReport(modelValue)"
        :report-id="modelValue.id"
        :report-passes-minimal-requirements="checkGameReportMinimal(modelValue)"
    />
    <GameTypeEditor
        v-model:visible = "gameTypeEditorVisible"
        :game-types = "gameTypes"
        @new-game-type="(gameType)=> {
          gameTypes.push(gameType)
          props.modelValue.gameType = gameType
        }"

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