<script setup lang="ts">

import UploadTeamsheetComponent from "@/components/teamsheet/UploadTeamsheetComponent.vue";
import type {TeamsheetUploadSuccessDEO} from "@/types/teamsheet_types";
import {useRouter} from "vue-router";
const router = useRouter()

function onUploadComplete(response: TeamsheetUploadSuccessDEO) {
  console.log("Upload complete")
  router.push({name: "augment-data", params: {fileKey: response.fileKey}})
}
</script>

<template>
  <div class="m-2">
    Please upload your teamsheet for any upcoming tournaments. The teamsheet should be a PDF file exported from Foireann.
  </div>
<UploadTeamsheetComponent
   @upload-complete="onUploadComplete"
></UploadTeamsheetComponent>
</template>

<style scoped>

</style>
