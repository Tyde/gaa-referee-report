<script setup lang="ts">

import type {Pitch} from "@/types/pitch_types";
import {onMounted} from "vue";

let props = defineProps<{
  pitchReport: Pitch,
  showRefereeName?: Boolean
}>()


</script>

<template>
  <div>
    <!-- Options: length, width, surface, penaltySquareMarkings, smallSquareMarkings
    thirteenMeterMarkings, twentyMeterMarkings, longMeterMarkings, goalPosts, goalDimensions -->
    <h3>{{ pitchReport.name }}</h3>
    <h4 v-if="showRefereeName">Report by {{pitchReport.report.referee.firstName}} {{pitchReport.report.referee.lastName}} </h4>
    <div class="flex flex-wrap break-inside-avoid">
      <div class="pitch-report-option">
        <h5>Length</h5>
        {{ pitchReport.length?.name }}
      </div>
      <div class="pitch-report-option">
        <h5>Width</h5>
        {{ pitchReport.width?.name }}
      </div>
      <div class="pitch-report-option">
        <h5>Surface</h5>
        {{ pitchReport.surface?.name }}
      </div>
    </div>
    <div class="flex flex-wrap break-inside-avoid">
      <div class="pitch-report-option">
        <h5>Penalty Square Markings</h5>
        {{ pitchReport.penaltySquareMarkings?.name }}
      </div>
      <div class="pitch-report-option">
        <h5>Small Square Markings</h5>
        {{ pitchReport.smallSquareMarkings?.name }}
      </div>
    </div>
    <div class="flex flex-wrap break-inside-avoid">
      <div class="pitch-report-option">
        <h5>Thirteen Meter Markings</h5>
        {{ pitchReport.thirteenMeterMarkings?.name }}
      </div>
      <div class="pitch-report-option">
        <h5>Twenty Meter Markings</h5>
        {{ pitchReport.twentyMeterMarkings?.name }}
      </div>
      <div class="pitch-report-option">
        <h5>45m/65m Markings</h5>
        {{ pitchReport.longMeterMarkings?.name }}
      </div>
    </div>
    <div class="flex flex-wrap break-inside-avoid">
      <div class="pitch-report-option">
        <h5>Goal Posts</h5>
        {{ pitchReport.goalPosts?.name }}
      </div>
      <div class="pitch-report-option">
        <h5>Goal Dimensions</h5>
        {{ pitchReport.goalDimensions?.name }}
      </div>
    </div>
    <div v-if="pitchReport.additionalInformation.length > 0" class="pitch-report-option break-inside-avoid ">
      <h5>Additional Information</h5>
      {{ pitchReport.additionalInformation }}
    </div>
  </div>


</template>


<style scoped>
.pitch-report-option {
  @apply p-2 m-2;
  @apply border-2 border-gray-200 rounded-md;
}

h5 {
  @apply text-center;
  @apply font-bold;
}
h3 {
  @apply text-center text-lg;
}
h4 {
  @apply my-4;
  @apply text-center italic;
}
</style>
