<script lang="ts" setup>

import {computed, onBeforeUnmount, onMounted, ref, watch} from "vue";
import type {GameReport} from "@/types";
import SingleGameReportSingleTeam from "@/components/gameReport/SingleGameReportSingleTeam.vue";
import {DateTime} from "luxon";
import GameTypeEditor from "@/components/gameReport/GameTypeEditor.vue";
import {useReportStore} from "@/utils/edit_report_store";




const emit = defineEmits<{
  (e: 'deleteThisReport', value: GameReport): void,
}>()
const store = useReportStore()
const isLoading = ref(false)



/*
//TODO include this into report store as action
async function sendGameReport(gameReport: GameReport) {
  if (checkGameReportMinimal(gameReport)) {
    if (gameReport.id) {
      await updateGameReport(gameReport)
    } else {
      if (!sendingCreateRequest.value) {
        sendingCreateRequest.value = true
        gameReport.id = await createGameReport(gameReport)
        sendingCreateRequest.value = false
      }
    }
  }
}

 */
const gameTypeEditorVisible = ref(false)
function showGameTypeDialog() {
  gameTypeEditorVisible.value = true
}


const sendingCreateRequest = ref(false)
const gameTypesByName = computed(() => {
  return store.gameTypes.sort((a, b) => a.name > b.name ? 1 : -1)
})


watch(()=>store.selectedGameReport,(value,oldValue)=> {
  if(store.selectedGameReport) {
    store.sendGameReport(store.selectedGameReport)
  }
})
onBeforeUnmount(() => {
  if(store.selectedGameReport) {
    store.sendGameReport(store.selectedGameReport)
  }
})



onMounted(() => {

  //updateInternalDateStart()
})
</script>
<template>

  <div class="grid grid-cols-2">
    <div class="col-span-2 flex flex-wrap">
      <div class="field p-2">
        <label for="timeStartGame">Throw in time:</label><br>
        <Calendar
            :model-value="store.selectedGameReport.startTime?.toJSDate()"
            @update:model-value="(nD:Date) => {store.selectedGameReport.startTime = DateTime.fromJSDate(nD)}"
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
        <label for="umpirePresentCheckBox"> Umpires present on time:</label><br>
        <Checkbox
            id="umpirePresentCheckBox"
            v-model="store.selectedGameReport.umpirePresentOnTime"
            :binary="true"/>
      </div>
      <div v-if="!store.selectedGameReport.umpirePresentOnTime" class="field p-2">
        <label for="umpireNotes">Comment on Umpires:</label><br>
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
                'to-be-filled':store.selectedGameReport.extraTime===undefined
            }"
        >

        </Dropdown>
      </div>
      <div class="field p-2">
        <label for="gameTypeSelect">Game type:</label><br>
        <Dropdown
            id="gameTypeSelect"
            v-model="store.selectedGameReport.gameType"
            :options="gameTypesByName"
            option-label="name"
            placeholder="Game Type"
            :class="{
                'to-be-filled':store.selectedGameReport.gameType===undefined
            }"
            class="w-60"
            :filter="true"
            :filter-fields="['name']"
        >


        </Dropdown>

      </div>
      <div class="field p-2 mt-6">
        <Button @click="showGameTypeDialog" class="p-button-success">
          <i class="pi pi-plus"></i> </Button>
      </div>
    </div>
    <div  class="p-4 col-span-2 lg:col-span-1 flex flex-col">
      <div>Home team:</div>
    <SingleGameReportSingleTeam
        :is-team-a = "true" />

    </div>
    <div  class="p-4 col-span-2 lg:col-span-1 flex flex-col">
      <div>Away team:</div>
    <SingleGameReportSingleTeam
        :is-team-a = "false" />

    </div>

    <div class="col-span-2 p-4 flex justify-center">
      <Button
          @click="emit('deleteThisReport', store.selectedGameReport)"
          class="p-button-danger"
        >Delete this game report</Button>
    </div>
    <GameTypeEditor
        v-model:visible = "gameTypeEditorVisible"
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