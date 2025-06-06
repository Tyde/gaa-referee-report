<script lang="ts" setup>

import {computed, onBeforeUnmount, watch} from "vue";
import {useReportStore} from "@/utils/edit_report_store";
import type {Pitch} from "@/types/pitch_types";
const props = defineProps<{
  toBeDeleted: boolean
}>()

const store = useReportStore()
const pitch = computed(() => {
  return store.selectedPitchReport!!
})
const emit = defineEmits<{
  (e: 'deleteThisReport', value: Pitch): void
}>()



async function asyncUpload(pitch:Pitch) {
  await store.sendPitchReport(pitch)
      .catch((error) => {
        store.newError(error)
      })
}

function deleteThisReport(pitch: Pitch | undefined) {
  if(pitch) {
    emit('deleteThisReport', pitch)
  }
}

watch(() => store.selectedPitchReport, (value, oldValue) => {
  console.log("Switched report, oldReport is ",oldValue, " newReport is ", value)
  if(!props.toBeDeleted) {
    asyncUpload(oldValue!!)
  }
})
onBeforeUnmount(() => {
  console.log("unmounting SinglePitchReport")
  if(!props.toBeDeleted) {
    asyncUpload(store.selectedPitchReport!!)
  }
})

</script>

<template>
  <div class="flex flex-col">
    <div v-if="pitch.name?.trim().length===0" class="rounded-xl border border-amber-400
            bg-amber-200 text-center text-lg font-sans text-gray-700
            p-4 m-4">
      {{ $t('pitchReport.enterNameReminder') }}
    </div>
    <div class="flex-col flex grow">
      <div class="flex-row flex-wrap flex justify-center">
        <div class="field p-2">
          <label for="pitchName">{{ $t('pitchReport.name') }}:</label><br>
          <div
              :class="{
                'to-be-filled':pitch.name.trim().length===0
            }"
          >
            <InputText
                id="pitchName"
                v-model="pitch.name"
                :placeholder="$t('pitchReport.pitchNamePlaceholder')"
            />
          </div>
        </div>
        <div class="field p-2">

          <label for="surfaceSelect">{{ $t('pitchReport.surface') }}:</label><br>

          <Select
              id="surfaceSelect"
              v-model="pitch.surface"
              :class="{
                'to-be-filled':pitch.surface===undefined
            }"
              :options="store.enabledPitchVariables?.surfaces"
              option-label="name"
              :placeholder="$t('pitchReport.surface')"
          >
          </Select>
        </div>
      </div>
      <div class="flex-row flex-wrap flex justify-center">
        <div class="field p-2">

          <label for="lengthSelect">{{ $t('pitchReport.length') }}:</label><br>
          <Select
              id="lengthSelect"
              v-model="pitch.length"
              :class="{
                'to-be-filled':pitch.length===undefined
            }"
              :options="store.enabledPitchVariables?.lengths"
              option-label="name"
              :placeholder="$t('pitchReport.length')"
          >
          </Select>
        </div>
        <div class="field p-2">

          <label for="widthSelect">{{ $t('pitchReport.width') }}:</label><br>
          <Select
              id="widthSelect"
              v-model="pitch.width"
              :class="{
                'to-be-filled':pitch.width===undefined
            }"
              :options="store.enabledPitchVariables?.widths"
              option-label="name"
              :placeholder="$t('pitchReport.width')"
          >
          </Select>
        </div>
      </div>
      <div class="flex-row flex-wrap flex justify-center">
        <div class="field p-2">

          <label for="smallSquareSelect">{{ $t('pitchReport.smallSquareMarkings') }}:</label><br>
          <Select
              id="smallSquareSelect"
              v-model="pitch.smallSquareMarkings"
              :class="{
                'to-be-filled':pitch.smallSquareMarkings===undefined
            }"
              :options="store.enabledPitchVariables?.markingsOptions"
              class="markings-options"
              option-label="name"
              :placeholder="$t('pitchReport.smallSquareMarkings')"
          >
          </Select>
        </div>
        <div class="field p-2">

          <label for="penaltySquareSelect">{{ $t('pitchReport.penaltySquareMarkings') }}:</label><br>
          <Select
              id="penaltySquareSelect"
              v-model="pitch.penaltySquareMarkings"
              :class="{
                'to-be-filled':pitch.penaltySquareMarkings===undefined
            }"
              :options="store.enabledPitchVariables?.markingsOptions"
              class="markings-options"
              option-label="name"
              :placeholder="$t('pitchReport.penaltySquareMarkings')"
          >
          </Select>
        </div>
        <div class="field p-2">

          <label for="thirteenMarkignsSelect">{{ $t('pitchReport.thirteenMeterMarkings') }}:</label><br>
          <Select
              id="thirteenMarkignsSelect"
              v-model="pitch.thirteenMeterMarkings"
              :class="{
                'to-be-filled':pitch.thirteenMeterMarkings===undefined
            }"
              :options="store.enabledPitchVariables?.markingsOptions"
              class="markings-options"
              option-label="name"
              :placeholder="$t('pitchReport.thirteenMeterMarkings')"
          >
          </Select>
        </div>
        <div class="field p-2">

          <label for="twentyMarkignsSelect">{{ $t('pitchReport.twentyMeterMarkings') }}:</label><br>
          <Select
              id="twentyMarkignsSelect"
              v-model="pitch.twentyMeterMarkings"
              :class="{
                'to-be-filled':pitch.twentyMeterMarkings===undefined
            }"
              :options="store.enabledPitchVariables?.markingsOptions"
              class="markings-options"
              option-label="name"
              :placeholder="$t('pitchReport.twentyMeterMarkings')"
          >
          </Select>
        </div>
        <div class="field p-2">

          <label for="longMarkingsSelect">{{ $t('pitchReport.longMarkings') }}:</label><br>
          <Select
              id="longMarkingsSelect"
              v-model="pitch.longMeterMarkings"
              :class="{
                'to-be-filled':pitch.longMeterMarkings===undefined
            }"
              :options="store.enabledPitchVariables?.markingsOptions"
              class="markings-options"
              option-label="name"
              :placeholder="$t('pitchReport.longMarkings')"
          >
          </Select>
        </div>
      </div>
      <div class="flex-row flex-wrap flex justify-center">
        <div class="field p-2">

          <label for="goalpostsSelect">{{ $t('pitchReport.goalPosts') }}:</label><br>
          <Select
              id="goalpostsSelect"
              v-model="pitch.goalPosts"
              :class="{
                'to-be-filled':pitch.goalPosts===undefined
            }"
              :options="store.enabledPitchVariables?.goalPosts"
              option-label="name"
              :placeholder="$t('pitchReport.goalPosts')"
          >
          </Select>
        </div>
        <div class="field p-2">

          <label for="goalDimsSelect">{{ $t('pitchReport.goalDimensions') }}:</label><br>
          <Select
              id="goalDimsSelect"
              v-model="pitch.goalDimensions"
              :class="{
                'to-be-filled':pitch.goalDimensions===undefined
            }"
              :options="store.enabledPitchVariables?.goalDimensions"
              option-label="name"
              :placeholder="$t('pitchReport.goalDimensions')"
          >
          </Select>
        </div>
      </div>
      <div class="flex-row flex-wrap flex justify-center">
        <div class="field p-2">
          <label for="additionalInfo">{{ $t('pitchReport.additionalInformation') }}:</label><br>

          <InputText
              id="additionalInfo"
              v-model="pitch.additionalInformation"
              class="w-80"
              placeholder=""
          />
        </div>

      </div>
      <div class="flex-row flex justify-center mt-4">
        <Button
            @click="deleteThisReport(store.selectedPitchReport)"
            class="p-button-danger"
          >{{ $t('pitchReport.delete') }}</Button>
      </div>
    </div>
  </div>
</template>


<style scoped>
.markings-options {
  @apply min-w-[13rem]
}
</style>
