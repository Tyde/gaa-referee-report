<script lang="ts" setup>

import {useDashboardStore} from "@/utils/dashboard_store";
import UserReportList from "@/components/dashboard/UserReportList.vue";
import {computed} from "vue";

const store = useDashboardStore()

const unsubmittedReports = computed(() => {
  return store.myReports.filter(report => !report.isSubmitted)
})
const submittedReports = computed(() => {
  return store.myReports.filter(report => report.isSubmitted)
})


</script>
<template>
  <div v-if="store.isLoading">
    <h1>Loading...</h1>
    <!-- Spinner -->
    <div class="flex justify-center">
      <div class="animate-spin rounded-full h-32 w-32 border-b-2 border-gray-900"></div>
    </div>


  </div>
  <div v-else>
    <h1>Unsubmitted Reports</h1>
    <UserReportList :reports="unsubmittedReports"></UserReportList>
    <h1>Submitted Reports</h1>
    <UserReportList :reports="submittedReports"></UserReportList>

  </div>

</template>


<style scoped>

</style>
