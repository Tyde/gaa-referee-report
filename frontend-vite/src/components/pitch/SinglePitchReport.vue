<script lang="ts" setup>

import type {Pitch} from "@/types";
import {computed, onBeforeUnmount, watch} from "vue";
import {useReportStore} from "@/utils/edit_report_store";

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

watch(() => store.selectedPitchReport, (value, oldValue) => {
  console.log("Switched report, oldReport is ")
  asyncUpload(value!!)
})
onBeforeUnmount(() => {
  asyncUpload(store.selectedPitchReport!!)
})

</script>

<template>
  <div class="flex flex-col">
    <div v-if="pitch.name?.trim().length===0" class="rounded-xl border border-amber-400
            bg-amber-200 text-center text-lg font-sans text-gray-700
            p-4 m-4">
      Please enter a name for the pitch! Otherwise the data wont be stored
    </div>
    <div class="flex-col flex grow">
      <div class="flex-row flex-wrap flex justify-center">
        <div class="field p-2">
          <label for="pitchName">Name:</label><br>
          <div
              :class="{
                'to-be-filled':pitch.name.trim().length===0
            }"
          >
            <InputText
                id="pitchName"
                v-model="pitch.name"
                placeholder="Pitch #X"
            />
          </div>
        </div>
        <div class="field p-2">

          <label for="surfaceSelect">Surface:</label><br>

          <Dropdown
              id="surfaceSelect"
              v-model="pitch.surface"
              :class="{
                'to-be-filled':pitch.surface===undefined
            }"
              :options="store.pitchVariables?.surfaces"
              option-label="name"
              placeholder="Surface"
          >
          </Dropdown>
        </div>
      </div>
      <div class="flex-row flex-wrap flex justify-center">
        <div class="field p-2">

          <label for="lengthSelect">Length:</label><br>
          <Dropdown
              id="lengthSelect"
              v-model="pitch.length"
              :class="{
                'to-be-filled':pitch.length===undefined
            }"
              :options="store.pitchVariables?.lengths"
              option-label="name"
              placeholder="Length"
          >
          </Dropdown>
        </div>
        <div class="field p-2">

          <label for="widthSelect">Width:</label><br>
          <Dropdown
              id="widthSelect"
              v-model="pitch.width"
              :class="{
                'to-be-filled':pitch.width===undefined
            }"
              :options="store.pitchVariables?.widths"
              option-label="name"
              placeholder="Width"
          >
          </Dropdown>
        </div>
      </div>
      <div class="flex-row flex-wrap flex justify-center">
        <div class="field p-2">

          <label for="smallSquareSelect">Small square markings:</label><br>
          <Dropdown
              id="smallSquareSelect"
              v-model="store.selectedPitchReport.smallSquareMarkings"
              :class="{
                'to-be-filled':pitch.smallSquareMarkings===undefined
            }"
              :options="store.pitchVariables?.markingsOptions"
              class="markings-options"
              option-label="name"
              placeholder="Small square markings"
          >
          </Dropdown>
        </div>
        <div class="field p-2">

          <label for="penaltySquareSelect">Penalty square markings:</label><br>
          <Dropdown
              id="penaltySquareSelect"
              v-model="pitch.penaltySquareMarkings"
              :class="{
                'to-be-filled':pitch.penaltySquareMarkings===undefined
            }"
              :options="store.pitchVariables?.markingsOptions"
              class="markings-options"
              option-label="name"
              placeholder="Penalty square markings"
          >
          </Dropdown>
        </div>
        <div class="field p-2">

          <label for="thirteenMarkignsSelect">13m markings:</label><br>
          <Dropdown
              id="thirteenMarkignsSelect"
              v-model="pitch.thirteenMeterMarkings"
              :class="{
                'to-be-filled':pitch.thirteenMeterMarkings===undefined
            }"
              :options="store.pitchVariables?.markingsOptions"
              class="markings-options"
              option-label="name"
              placeholder="13m markings"
          >
          </Dropdown>
        </div>
        <div class="field p-2">

          <label for="twentyMarkignsSelect">20m markings:</label><br>
          <Dropdown
              id="twentyMarkignsSelect"
              v-model="pitch.twentyMeterMarkings"
              :class="{
                'to-be-filled':pitch.twentyMeterMarkings===undefined
            }"
              :options="store.pitchVariables?.markingsOptions"
              class="markings-options"
              option-label="name"
              placeholder="20m markings"
          >
          </Dropdown>
        </div>
        <div class="field p-2">

          <label for="longMarkingsSelect">45m/65m markings:</label><br>
          <Dropdown
              id="longMarkingsSelect"
              v-model="pitch.longMeterMarkings"
              :class="{
                'to-be-filled':pitch.longMeterMarkings===undefined
            }"
              :options="store.pitchVariables?.markingsOptions"
              class="markings-options"
              option-label="name"
              placeholder="45m/65m markings"
          >
          </Dropdown>
        </div>
      </div>
      <div class="flex-row flex-wrap flex justify-center">
        <div class="field p-2">

          <label for="goalpostsSelect">Goalposts:</label><br>
          <Dropdown
              id="goalpostsSelect"
              v-model="pitch.goalPosts"
              :class="{
                'to-be-filled':pitch.goalPosts===undefined
            }"
              :options="store.pitchVariables?.goalPosts"
              option-label="name"
              placeholder="Goalposts"
          >
          </Dropdown>
        </div>
        <div class="field p-2">

          <label for="goalDimsSelect">Goal dimensions:</label><br>
          <Dropdown
              id="goalDimsSelect"
              v-model="pitch.goalDimensions"
              :class="{
                'to-be-filled':pitch.goalDimensions===undefined
            }"
              :options="store.pitchVariables?.goalDimensions"
              option-label="name"
              placeholder="Goalposts"
          >
          </Dropdown>
        </div>
      </div>
      <div class="flex-row flex-wrap flex justify-center">
        <div class="field p-2">
          <label for="additionalInfo">Additional info:</label><br>

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
            @click="emit('deleteThisReport',store.selectedPitchReport)"
            class="p-button-danger"
          >Delete this pitch report</Button>
      </div>
    </div>
  </div>
</template>


<style scoped>
.markings-options {
  @apply min-w-[13rem]
}
</style>