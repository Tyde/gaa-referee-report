<script lang="ts" setup>

import type {Pitch, Report} from "@/types";
import {type PitchVariables, uploadPitch} from "@/utils/api/pitch_api";
import {onBeforeUnmount, watch} from "vue";

const props = defineProps<{
  modelValue: Pitch,
  report: Report,
  pitchReportOptions: PitchVariables
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: Pitch): void
}>()

watch(() => props.modelValue, (value, oldValue) => {
  console.log("Switched report, oldReport is ")
  uploadPitch(oldValue)
})
onBeforeUnmount(() => {
  uploadPitch(props.modelValue)
})

</script>

<template>
  <div class="flex flex-col">
    <div v-if="props.modelValue.name.trim().length===0" class="rounded-xl border border-amber-400
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
                'to-be-filled':props.modelValue.name.trim().length===0
            }"
          >
            <InputText
                id="pitchName"
                v-model="modelValue.name"
                placeholder="Pitch #X"
            />
          </div>
        </div>
        <div class="field p-2">

          <label for="surfaceSelect">Surface:</label><br>

          <Dropdown
              id="surfaceSelect"
              v-model="props.modelValue.surface"
              :class="{
                'to-be-filled':props.modelValue.surface===undefined
            }"
              :options="props.pitchReportOptions.surfaces"
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
              v-model="props.modelValue.length"
              :class="{
                'to-be-filled':props.modelValue.length===undefined
            }"
              :options="props.pitchReportOptions.lengths"
              option-label="name"
              placeholder="Length"
          >
          </Dropdown>
        </div>
        <div class="field p-2">

          <label for="widthSelect">Width:</label><br>
          <Dropdown
              id="widthSelect"
              v-model="props.modelValue.width"
              :class="{
                'to-be-filled':props.modelValue.width===undefined
            }"
              :options="props.pitchReportOptions.widths"
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
              v-model="props.modelValue.smallSquareMarkings"
              :class="{
                'to-be-filled':props.modelValue.smallSquareMarkings===undefined
            }"
              :options="props.pitchReportOptions.markingsOptions"
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
              v-model="props.modelValue.penaltySquareMarkings"
              :class="{
                'to-be-filled':props.modelValue.penaltySquareMarkings===undefined
            }"
              :options="props.pitchReportOptions.markingsOptions"
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
              v-model="props.modelValue.thirteenMeterMarkings"
              :class="{
                'to-be-filled':props.modelValue.thirteenMeterMarkings===undefined
            }"
              :options="props.pitchReportOptions.markingsOptions"
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
              v-model="props.modelValue.twentyMeterMarkings"
              :class="{
                'to-be-filled':props.modelValue.twentyMeterMarkings===undefined
            }"
              :options="props.pitchReportOptions.markingsOptions"
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
              v-model="props.modelValue.longMeterMarkings"
              :class="{
                'to-be-filled':props.modelValue.longMeterMarkings===undefined
            }"
              :options="props.pitchReportOptions.markingsOptions"
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
              v-model="props.modelValue.goalPosts"
              :class="{
                'to-be-filled':props.modelValue.goalPosts===undefined
            }"
              :options="props.pitchReportOptions.goalPosts"
              option-label="name"
              placeholder="Goalposts"
          >
          </Dropdown>
        </div>
        <div class="field p-2">

          <label for="goalDimsSelect">Goal dimensions:</label><br>
          <Dropdown
              id="goalDimsSelect"
              v-model="props.modelValue.goalDimensions"
              :class="{
                'to-be-filled':props.modelValue.goalDimensions===undefined
            }"
              :options="props.pitchReportOptions.goalDimensions"
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
              v-model="modelValue.additionalInformation"
              class="w-80"
              placeholder=""
          />
        </div>

      </div>
    </div>
  </div>
</template>


<style scoped>
.markings-options {
  @apply min-w-[13rem]
}
</style>