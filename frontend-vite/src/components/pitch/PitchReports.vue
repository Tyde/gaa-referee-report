<script lang="ts" setup>

import type {Pitch} from "@/types";
import {computed, onMounted, ref} from "vue";
import SinglePitchReport from "@/components/pitch/SinglePitchReport.vue";
import {useReportStore} from "@/utils/edit_report_store";

const store = useReportStore()


function newPitch() {
  let newPitch = <Pitch>{
    report: store.report,
    name: "",
    additionalInformation: "",
  }
  store.pitchReports.push(newPitch)
  store.selectedPitchReportIndex = store.pitchReports.length - 1
}

onMounted(() => {
  if (store.pitchReports.length == 0) {
    //newPitch() -- We dont do that anymore because some might not want to send a report
  } else {
    store.selectedPitchReportIndex = 0
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
    await store.deletePitchReport(pitchToDelete.value)
  }
  cancelDeletePitchDialog()
}

const pitchReportIndices = computed(() => {
  return Array.from(Array(store.pitchReports.length).keys())
})

</script>
<template>
  <div>
    <Toolbar v-if="store.pitchReports.length>0">
      <template #start>
        <SelectButton v-model="store.selectedPitchReportIndex" :options="pitchReportIndices">
          <template #option="slotProps">
            <span
                v-if="store.pitchReports[slotProps.option].name">{{ store.pitchReports[slotProps.option].name }}</span>
            <span v-else>Unnamed Pitch</span>

          </template>
        </SelectButton>
      </template>
      <template #end>
        <Button class="p-button-success" @click="newPitch"><i class="pi pi-plus"></i></Button>
      </template>
    </Toolbar>
    <div v-else class="flex flex-row justify-center mt-2 p-4 bg-gray-300 ">
      <div class="text-center" v-if="store.pitchReports.length===0">
        <p class="text-lg">No pitch reports yet. Create the first:</p>
        <Button class="p-button-success" @click="newPitch"><i class="pi pi-plus"></i></Button>
      </div>
    </div>
    <!--
    <SinglePitchReport
        v-if="store.selectedPitchReport"
        v-model="selectedPitchReport"
        :pitch-report-options="props.pitchReportOptions"
        :report="report"
        @delete-this-report="(pitch)=>startDeletePitchDialog(pitch)"
    />-->
    <SinglePitchReport
        v-if="store.selectedPitchReport"
        @delete-this-report="startDeletePitchDialog"
    />

    <Dialog
        v-model:visible="deletePitchDialogVisible"
        :modal="true"
    >
      <template #header>
        <span>Delete Pitch Report</span>
      </template>

      <p>Are you sure you want to delete this pitch report?</p>
      <span v-if="pitchToDelete?.name">{{ pitchToDelete.name }}</span>

      <template #footer>
        <Button class="p-button-danger" @click="deletePitch">Delete</Button>
        <Button class="p-button-secondary" @click="cancelDeletePitchDialog">Cancel</Button>
      </template>
    </Dialog>
  </div>
</template>


<style scoped>

</style>