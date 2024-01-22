<script lang="ts" setup>

import {computed, onBeforeUnmount, onMounted, ref, watch} from "vue";
import SingleGameReportSingleTeam from "@/components/gameReport/SingleGameReportSingleTeam.vue";
import {DateTime} from "luxon";
import GameTypeEditor from "@/components/gameReport/GameTypeEditor.vue";
import {useReportStore} from "@/utils/edit_report_store";
import type {GameReport} from "@/types/game_report_types";



const props = defineProps<{
  toBeDeleted: boolean
}>()
const emit = defineEmits<{
  (e: 'deleteThisReport', value: GameReport): void,
}>()
const store = useReportStore()
const isLoading = ref(false)




const gameTypeEditorVisible = ref(false)
function showGameTypeDialog() {
  gameTypeEditorVisible.value = true
}


const sendingCreateRequest = ref(false)
const gameTypesByName = computed(() => {
  return store.gameTypes.sort((a, b) => a.name > b.name ? 1 : -1)
})

function deleteThisReport(report:GameReport | undefined) {
  if(report) {
    emit('deleteThisReport', report)
  }
}


watch(()=>store.selectedGameReport,(value,oldValue)=> {
  if(store.selectedGameReport && oldValue && oldValue.id != -1 && !props.toBeDeleted) {
    store.sendGameReport(oldValue)
        .catch((e) => {
          store.newError(e)
        })
  }
})
onBeforeUnmount(() => {
  if(store.selectedGameReport && !props.toBeDeleted) {
    console.log("On before unmount sending Game Report")
    store.sendGameReport(store.selectedGameReport)
        .then(() => {
          console.log("Game Report sent")
        })
        .catch((e) => {
          store.newError(e)
        })
  }
})




onMounted(() => {

  //updateInternalDateStart()
})
</script>
<template>

  <div class="grid grid-cols-2" v-if="store.selectedGameReport !== undefined">
    <div class="col-span-2 flex flex-wrap flex-col md:flex-row justify-center content-center">
      <div class="field p-2">
        <label for="timeStartGame">Throw in time:</label><br>
        <Calendar
            :model-value="store.selectedGameReport?.startTime?.toJSDate()"
            @update:model-value="(nD:Date) => {
              if(store.selectedGameReport){
                store.selectedGameReport.startTime = DateTime.fromJSDate(nD)
              }
            }"
            id="timeStartGame"
            :showSeconds="false"
            :showTime="true"
            :time-only="true"
            :class="{
                'to-be-filled':store.selectedGameReport.startTime===undefined
            }"
        />

      </div>
      <div class="field p-2 object-center">

        <div class="flex flex-row md:flex-col items-center justify-center">
          <div><label for="umpirePresentCheckBox">Umpires present on time:</label></div>
          <div>
            <input type="checkbox"
                id="umpirePresentCheckBox"
                class="w-6 h-6 rounded m-2"
                v-model="store.selectedGameReport.umpirePresentOnTime"
                :binary="true"/>
          </div>
        </div>
      </div>
      <div v-if="!store.selectedGameReport?.umpirePresentOnTime" class="field p-2">
        <label for="umpireNotes">Comments on Umpires:</label><br>
        <InputText id="umpireNotes" v-model="store.selectedGameReport.umpireNotes" type="text"/>

      </div>
      <div class="field p-2">
        <label for="extraTimeSelect">Extra time:</label><br>
        <Dropdown
            id="extraTimeSelect"
            v-model="store.selectedGameReport.extraTime"
            :options="store.extraTimeOptions"
            option-label="name"
            placeholder="Extra Time"
            :class="{
                'to-be-filled':store.selectedGameReport?.extraTime===undefined
            }"
        >

        </Dropdown>
      </div>
      <div class="p-2 flex flex-row">
        <div class="field pr-2"><label for="gameTypeSelect">Game type:</label><br>
        <Dropdown
            id="gameTypeSelect"
            v-model="store.selectedGameReport.gameType"
            :options="gameTypesByName"
            option-label="name"
            placeholder="Game Type"
            :class="{
                'to-be-filled':store.selectedGameReport?.gameType===undefined
            }"
            class="w-60"
            :filter="true"
            :filter-fields="['name']"

        >


        </Dropdown></div>
        <div class="field pr-2 mt-6">
          <Button @click="showGameTypeDialog" class="p-button-success h-12 p-button-outlined">
            <i class="pi pi-plus"></i> </Button>
        </div>
      </div>

      <div class="p-2 mt-6">
        <Button
            @click="deleteThisReport(store.selectedGameReport)"
            class="p-button-danger h-12 p-button-outlined"
        >Delete this game report</Button>
      </div>
    </div>
    <div  class="single-team-report">
      <div>Team A:</div>
    <SingleGameReportSingleTeam
        :is-team-a = "true" />

    </div>
    <div  class="single-team-report">
      <div>Team B:</div>
    <SingleGameReportSingleTeam
        :is-team-a = "false" />

    </div>

    <div class="col-span-2 flex flex-row justify-center">
      <Textarea
          v-model="store.selectedGameReport.generalNotes"
          placeholder="Notes"
          rows="5"
          cols="30" />
    </div>
    <GameTypeEditor
        v-model:visible = "gameTypeEditorVisible"
    />
  </div>


</template>


<style>
.single-team-report {
  @apply p-4 col-span-2 lg:col-span-1 flex;
  @apply flex-col border-2 border-gray-800 m-2 bg-gray-200;
  @apply rounded-lg md:border-0;

}
.to-be-filled {
  @apply ring-2;
  @apply ring-red-400;
  @apply ring-offset-1;
}
</style>
