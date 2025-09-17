<script lang="ts" setup>

import {computed, onBeforeUnmount, onMounted, ref, watch} from "vue";
import SingleGameReportSingleTeam from "@/components/gameReport/SingleGameReportSingleTeam.vue";
import {DateTime} from "luxon";
import GameTypeEditor from "@/components/gameReport/GameTypeEditor.vue";
import {useReportStore} from "@/utils/edit_report_store";
import type {GameReport} from "@/types/game_report_types";
import MobileDropdown from "@/components/util/MobileDropdown.vue";
import DateTimePicker from "@/components/util/DateTimePicker.vue";


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

const isLeagueGame = computed(
    () => store.report.tournament.isLeague
)

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

function correctStartTime(value:DateTime) {
  if (!store.report.tournament.isLeague) {
    //Calendar has the issue that it may change the date to the actual date if I enter the time by hand
    //so we reset the date to the tournament date, but keep the time
    const tournamentDate = store.report.tournament.date
    if (value) {
      return DateTime.fromObject({
        year: tournamentDate.year,
        month: tournamentDate.month,
        day: tournamentDate.day,
        hour: value.hour,
        minute: value.minute,
        second: value.second
      }, {zone: value.zone})
    }
  }
  return value
}




const nativePickerUsed = ref(false)


function detectTouchscreen() {
  var result = false;
  if (window.PointerEvent && ('maxTouchPoints' in navigator)) {
    console.log("Pointer Events supported")
    // if Pointer Events are supported, just check maxTouchPoints
    if (navigator.maxTouchPoints > 0) {
      console.log("Pointer Events with maxTouchPoints")
      result = true;
    }
  } else {
    // no Pointer Events...
    if (window.matchMedia && window.matchMedia("(any-pointer:coarse)").matches) {
      console.log("Media query for any-pointer:coarse")
      // check for any-pointer:coarse which mostly means touchscreen
      result = true;
    } else if (window.TouchEvent || ('ontouchstart' in window)) {
      console.log("TouchEvent or ontouchstart in window")
      // last resort - check for exposed touch events API / event handler
      result = true;
    }
  }
  console.log("Touchscreen detected: ", result)
  return result;
}

onMounted(() => {
  const nativePickerAvailable = 'showPicker' in HTMLInputElement.prototype
  const isTouch = detectTouchscreen()
  console.log("Native picker available: ", nativePickerAvailable, "Is touch: ", isTouch)
  nativePickerUsed.value = nativePickerAvailable && isTouch
  //updateInternalDateStart()
})
</script>
<template>

  <div class="grid grid-cols-2" v-if="store.selectedGameReport !== undefined">
    <div class="col-span-2 flex flex-wrap flex-col md:flex-row justify-center content-center">
      <div class="field p-2">
        <label for="timeStartGame">{{ $t('gameReport.throwInTime') }}:</label><br>
        <DateTimePicker
            v-model="store.selectedGameReport.startTime"
            :correct-start-date="store.report.tournament.isLeague ? undefined : store.report.tournament.date"
            :time-only="!isLeagueGame"
            :class="{
                'to-be-filled':store.selectedGameReport.startTime===undefined
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
        <Select
            id="extraTimeSelect"
            v-model="store.selectedGameReport.extraTime"
            :options="store.publicStore.extraTimeOptions"
            option-label="name"
            :placeholder="$t('gameReport.extraTime')"
            :class="{
                'to-be-filled':store.selectedGameReport?.extraTime===undefined
            }"
            :pt="{input: { class: 'p-1 md:p-2'}}"
            :reset-filter-on-hide="true"
        >

        </Select>
      </div>
      <div class="field p-2">
        <label for="gameLengthSelect">{{ $t('gameReport.gameLength') }}:</label><br>
        <Select
            id="gameLengthSelect"
            v-model="store.selectedGameReport.gameLength"
            :options="store.publicStore.gameLengthOptions"
            option-label="name"
            :placeholder="$t('gameReport.gameLength')"
            :class="{
                'to-be-filled':store.selectedGameReport?.gameLength===undefined
            }"
            :pt="{input: { class: 'p-1 md:p-2'}}"
            :reset-filter-on-hide="true"
        />
      </div>
      <div class="p-2 flex flex-col">
        <div><label for="gameTypeSelect">{{ $t('gameReport.gameType') }}:</label></div>
        <div class="flex flex-row items-start">
          <Select
              id="gameTypeSelect"
              v-model="store.selectedGameReport.gameType"
              :options="gameTypesByName"
              option-label="name"
              :placeholder="$t('gameReport.gameType')"
              :class="{
                'to-be-filled':store.selectedGameReport?.gameType===undefined
              }"
              class="w-60 mr-2 hidden md:flex"
              :filter="true"
              :filter-fields="['name']"
              :pt="{input: { class: 'p-2'}}"
              :reset-filter-on-hide="true"

          >


          </Select>
          <MobileDropdown
              id="gameTypeSelectMobile"
              v-model="store.selectedGameReport.gameType"
              :options="gameTypesByName"
              option-label="name"
              :placeholder="$t('gameReport.gameType')"
              :class="{
                'to-be-filled':store.selectedGameReport?.gameType===undefined
              }"
              class="flex grow md:hidden"
              :filter-fields="['name']"/>

          <Button
              @click="showGameTypeDialog"
              class="p-button-success mt-2 md:mt-0 p-2 h-9 md:h-[40px] p-button-outlined min-w-0 ">
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
  @apply flex-col border border-surface-500 m-2 bg-surface-600;
  @apply rounded-lg md:border-0;

}

.to-be-filled {
  @apply !ring-2;
  @apply !ring-red-400;
  @apply !ring-offset-1;
  @apply border-red-600;
}
</style>
