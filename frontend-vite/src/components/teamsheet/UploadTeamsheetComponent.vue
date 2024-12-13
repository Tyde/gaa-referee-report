<script setup lang="ts">

import type {FileUploadBeforeUploadEvent, FileUploadProgressEvent, FileUploadUploadEvent} from "primevue/fileupload";
import {ref} from "vue";
import {onTeamsheetUploadComplete} from "@/utils/api/teamsheet_api";
import {useTeamsheetStore} from "@/utils/teamsheet_store";
import {TeamsheetUploadSuccessDEO} from "@/types/teamsheet_types";

const uploading = ref<boolean>(false)
const uploadProgress = ref<number>(0)

const store = useTeamsheetStore()

const emit = defineEmits<{
  (e: 'uploadComplete', response: TeamsheetUploadSuccessDEO): void,
  (e: 'uploadStart'): void,
  (e: 'uploadFailed', error: Error): void
}>()

function onUpload(event: FileUploadUploadEvent) {
  emit('uploadStart')
  onTeamsheetUploadComplete(event)
      .then((response) => {
        uploading.value = false
        store.uploadSuccessDEO = response
        emit('uploadComplete', response)
      })
      .catch((e) => {
        emit('uploadFailed', e)
        store.newError(e)
        uploading.value = false
      })
}

function onBeforeUpload(event: FileUploadBeforeUploadEvent) {
  uploading.value = true
}

function onProgress(event: FileUploadProgressEvent) {
  uploadProgress.value = event.progress
}
</script>

<template>


  <div class="mx-auto">
    <FileUpload
        mode="basic"
        name="teamsheet"
        url="/api/teamsheet/upload"
        accept=".pdf"
        @upload="onUpload"
        :max-file-size="5000000"
        :auto="true"
        @before-upload="onBeforeUpload"
        :class="{ 'p-disabled': uploading }"
        class="mx-auto"
        @progress="onProgress"
    ></FileUpload>
  </div>
  <ProgressBar v-if="uploading" :value="uploadProgress">
  </ProgressBar>
</template>

<style scoped>

</style>
