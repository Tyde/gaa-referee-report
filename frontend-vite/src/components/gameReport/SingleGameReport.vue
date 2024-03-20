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
  return store.publicStore.gameTypes.toSorted((a, b) => a.name > b.name ? 1 : -1)
})

const teamAEqualTeamB = computed(() => {
  if (store.selectedGameReport && store.selectedGameReport.teamAReport.team) {
    return store.selectedGameReport.teamAReport.team === store.selectedGameReport.teamBReport.team
  }
  return false
})

function deleteThisReport(report: GameReport | undefined) {
  if (report) {
    emit('deleteThisReport', report)
  }
}


watch(() => store.selectedGameReport, (value, oldValue) => {
  if (store.selectedGameReport && oldValue && oldValue.id != -1 && !props.toBeDeleted) {
    store.sendGameReport(oldValue)
        .catch((e) => {
          store.newError(e)
        })
  }
})
onBeforeUnmount(() => {
  if (store.selectedGameReport && !props.toBeDeleted) {
    //console.log("On before unmount sending Game Report")
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
        <label for="timeStartGame">{{ $t('gameReport.throwInTime') }}:</label><br>
        <Calendar
            :model-value="store.selectedGameReport?.startTime?.toJSDate()"
            @update:model-value="(nD:Date) => {
              console.log('Updated Start Time', nD)
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
            :pt="{
                input: { class: 'p-1 md:p-2'}
            }"
        />

      </div>
      <div class="field p-2 object-center">

        <div class="flex flex-row md:flex-col items-center md:justify-center">
          <div><label for="umpirePresentCheckBox">{{ $t('gameReport.umpiresPresentOnTime') }}:</label></div>
          <div>
            <input type="checkbox"
                   id="umpirePresentCheckBox"
                   class="w-4 h-4 md:w-6 md:h-6 rounded m-2"
                   v-model="store.selectedGameReport.umpirePresentOnTime"
                   :binary="true"

            />
          </div>
        </div>
      </div>
      <div v-if="!store.selectedGameReport?.umpirePresentOnTime" class="field p-2">
        <label for="umpireNotes">{{ $t('gameReport.commentOnUmpires') }}:</label><br>
        <InputText
            id="umpireNotes"
            v-model="store.selectedGameReport.umpireNotes"
            type="text"
            class="p-1 md:p-2"
            :pt="{input: { class: 'p-1 md:p-2'}}"
        />

      </div>
      <div class="field p-2">
        <label for="extraTimeSelect">{{ $t('gameReport.extraTime') }}:</label><br>
        <Dropdown
            id="extraTimeSelect"
            v-model="store.selectedGameReport.extraTime"
            :options="store.publicStore.extraTimeOptions"
            option-label="name"
            :placeholder="$t('gameReport.extraTime')"
            :class="{
                'to-be-filled':store.selectedGameReport?.extraTime===undefined
            }"
            :pt="{input: { class: 'p-1 md:p-2'}}"
        >

        </Dropdown>
      </div>
      <div class="p-2 flex flex-col">
        <div><label for="gameTypeSelect">{{ $t('gameReport.gameType') }}:</label></div>
        <div class="flex flex-row items-start">
          <Dropdown
              id="gameTypeSelect"
              v-model="store.selectedGameReport.gameType"
              :options="gameTypesByName"
              option-label="name"
              :placeholder="$t('gameReport.gameType')"
              :class="{
                'to-be-filled':store.selectedGameReport?.gameType===undefined
              }"
              class="w-60 mr-2"
              :filter="true"
              :filter-fields="['name']"
              :pt="{input: { class: 'p-1 md:p-2'}}"

          >


          </Dropdown>

          <Button
              @click="showGameTypeDialog"
              class="p-button-success h-9 md:h-[40px] p-button-outlined min-w-0 ">
            <i class="pi pi-plus"></i></Button>
        </div>
      </div>

      <div class="p-2 md:mt-6">
        <Button
            @click="deleteThisReport(store.selectedGameReport)"
            class="p-button-danger h-[40px] p-button-outlined"
        >{{ $t('gameReport.deleteGameReport') }}</Button>
      </div>
    </div>
    <div v-if="teamAEqualTeamB" class="col-span-2 flex flex-row justify-center bg-amber-400 rounded p-2 text-lg">
      {{ $t('gameReport.alertIfBothTeamsAreTheSame')}}
    </div>
    <div class="single-team-report">
      <div>{{ $t('gameReport.teamA') }}:</div>
      <SingleGameReportSingleTeam
          :is-team-a="true"/>

    </div>
    <div class="single-team-report">
      <div>{{ $t('gameReport.teamB') }}:</div>
      <SingleGameReportSingleTeam
          :is-team-a="false"/>

    </div>

    <div class="col-span-2 flex flex-row justify-center">
      <Textarea
          v-model="store.selectedGameReport.generalNotes"
          :placeholder="$t('gameReport.notes')"
          rows="5"
          cols="30"/>
    </div>
    <GameTypeEditor
        v-model:visible="gameTypeEditorVisible"
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
