<script lang="ts" setup>

import type {Pitch, Report} from "@/types";
import type {PitchVariables} from "@/utils/api/pitch_api";
import {onMounted, ref} from "vue";
import SinglePitchReport from "@/components/SinglePitchReport.vue";

const props = defineProps<{
  modelValue: Array<Pitch>,
  report: Report,
  pitchReportOptions: PitchVariables
}>()

const selectedPitchReport = ref<Pitch | undefined>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: Array<Pitch>): void
}>()

function newPitch() {
  let newPitch = <Pitch>{
    report: props.report,
    name: "",
    additionalInformation: "",
  }
  props.modelValue.push(newPitch)
  emit('update:modelValue', props.modelValue)
  selectedPitchReport.value = props.modelValue[props.modelValue.length-1]
}

onMounted(() => {
  if(props.modelValue.length == 0) {
    newPitch()
  } else {
    selectedPitchReport.value = props.modelValue[0]
  }
})

</script>
<template>
  <div>
    <Toolbar>
      <template #start>
        <SelectButton v-model="selectedPitchReport" :options="modelValue">
          <template #option="slotProps">
            <span
                v-if="slotProps.option.name">{{ slotProps.option.name }}</span>
            <span v-else>Unnamed Pitch</span>

          </template>
        </SelectButton>
      </template>
      <template #end>
        <Button class="p-button-success" @click="newPitch"><i class="pi pi-plus"></i></Button>
      </template>
    </Toolbar>
    <SinglePitchReport
        v-if="selectedPitchReport"
        v-model="selectedPitchReport"
        :report="report"
        :pitch-report-options="props.pitchReportOptions"
    />
  </div>
</template>


<style scoped>

</style>