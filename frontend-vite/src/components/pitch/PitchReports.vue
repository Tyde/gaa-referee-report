<script lang="ts" setup>

import type {Pitch, Report} from "@/types";
import type {PitchVariables} from "@/utils/api/pitch_api";
import {deletePitchOnServer} from "@/utils/api/pitch_api";
import {onMounted, ref} from "vue";
import SinglePitchReport from "@/components/pitch/SinglePitchReport.vue";

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
  selectedPitchReport.value = props.modelValue[props.modelValue.length - 1]
}

onMounted(() => {
  if (props.modelValue.length == 0) {
    //newPitch()
  } else {
    selectedPitchReport.value = props.modelValue[0]
  }
})


const deletePitchDialogVisible = ref(false)
const pitchToDelete = ref<Pitch | undefined>()

function startDeletePitchDialog(pitch: Pitch) {
  pitchToDelete.value = pitch
  deletePitchDialogVisible.value = true
}

function cancelDeletePitchDialog() {
  deletePitchDialogVisible.value = false
  pitchToDelete.value = undefined
}

async function deletePitch() {
  if (pitchToDelete.value) {
    if (pitchToDelete.value.id) {
      await deletePitchOnServer(pitchToDelete.value)
    }
    let index = props.modelValue.indexOf(pitchToDelete.value)
    if (index >= 0) {
      props.modelValue.splice(index, 1)
      if (props.modelValue.length > 0) {
        selectedPitchReport.value = props.modelValue[Math.min(index, props.modelValue.length - 1)]
      } else {
        selectedPitchReport.value = undefined
      }
      emit('update:modelValue', props.modelValue)
    }
  }
  cancelDeletePitchDialog()
}

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
        :pitch-report-options="props.pitchReportOptions"
        :report="report"
        @delete-this-report="(pitch)=>startDeletePitchDialog(pitch)"
    />

    <Dialog
        v-model:visible="deletePitchDialogVisible"
        :modal="true"
    >
      <template #header>
        <span>Delete Pitch Report</span>
      </template>

      <p>Are you sure you want to delete this pitch report?</p>
      <span v-if="pitchToDelete.name">{{ pitchToDelete.name }}</span>

      <template #footer>
        <Button class="p-button-danger" @click="deletePitch">Delete</Button>
        <Button class="p-button-secondary" @click="cancelDeletePitchDialog">Cancel</Button>
      </template>
    </Dialog>
  </div>
</template>


<style scoped>

</style>