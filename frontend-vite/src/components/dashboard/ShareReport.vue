<script lang="ts" setup>
import {ref, watch} from "vue";
import type {CompactTournamentReportDEO} from "@/types/report_types";
import {shareReportOnServer} from "@/utils/api/report_api";
import useClipboard from "vue-clipboard3";

const props = defineProps<{
  report?: CompactTournamentReportDEO
}>()

const emit = defineEmits<{
  (event: 'onError', error:any): void
  (event: 'onSuccess'): void
}>()

watch(() => props.report, (newReport, oldReport) => {
  console.log("watch", newReport, oldReport)
  if (newReport && oldReport === undefined) {
    shareReport(newReport)
  }
})

const shareReportLink = ref('')
const shareReportDialog = ref(false)
function shareReport(report: CompactTournamentReportDEO) {
  shareReportOnServer(report.id)
      .then((resp) => {
        shareReportLink.value = location.host +"/report/share/"+ resp.uuid
        shareReportDialog.value = true
        emit('onSuccess')
      })
      .catch((err) => {
        emit('onError', err)
      })
}
const {toClipboard} = useClipboard()

async function copyShareLinkToClipboard() {
  await toClipboard(shareReportLink.value)
}
</script>

<template>

  <Dialog header="Shared report" v-model:visible="shareReportDialog">
    <p>Share this link:</p>
    <InputText v-model="shareReportLink" readonly/>
    <Button label="Copy link" icon="pi pi-copy" class="p-button-raised p-button-text"
            @click="copyShareLinkToClipboard"/>
  </Dialog>
</template>



<style scoped>

</style>