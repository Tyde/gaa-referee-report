<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import {onMounted, ref} from "vue";
import {GameReport, Pitch, Report} from "@/types";
import ShowFullReport from "@/components/showReport/ShowFullReport.vue";

const store = useAdminStore();
const props = defineProps<{
  id: string
}>();
const isLoading = ref(true)
const currentReport = ref<Report>({} as Report)
const allGameReports = ref<Array<GameReport>>([])
const allPitchReports = ref<Array<Pitch>>([])

onMounted(() => {
  isLoading.value = true
  store.getCompleteReport(parseInt(props.id))
      .then(reportData => {
        currentReport.value = reportData.report
        allGameReports.value = reportData.gameReports
        allPitchReports.value = reportData.pitchReports
        isLoading.value = false
      } )
      .catch(e => store.newError(e))


})

</script>

<template>
  <div v-if="isLoading" class="z-[10000] flex relative">
    <span>Loading</span>
  </div>
  <template v-else>
    <ShowFullReport
        :current-report="currentReport"
        :all-game-reports="allGameReports"
        :all-pitch-reports="allPitchReports"
    />
  </template>
</template>



<style scoped>

</style>