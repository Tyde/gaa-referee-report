<script lang="ts" setup>

import {useDashboardStore} from "@/utils/dashboard_store";
import {computed, ref} from "vue";
import { FilterMatchMode,FilterOperator } from '@primevue/core/api';
import {useConfirm} from "primevue/useconfirm";
import {useRouter} from "vue-router";
import type {CompactTournamentReportDEO} from "@/types/report_types";
import ShareReport from "@/components/dashboard/ShareReport.vue";
import {usePublicStore} from "@/utils/public_store";
import {DateTime} from "luxon";

const publicStore = usePublicStore()
const store = useDashboardStore()

const props = defineProps<{
  reports: Array<CompactTournamentReportDEO>
}>()
const filters = ref({
  global: {value: null, matchMode: FilterMatchMode.CONTAINS},
  refereeName: {
    operator: FilterOperator.AND,
    constraints: [{value: null, matchMode: FilterMatchMode.CONTAINS}]
  },
  codeName: {
    operator: FilterOperator.OR,
    constraints: [{value: null, matchMode: FilterMatchMode.EQUALS}]
  },

})

type TransformedTournamentReport = CompactTournamentReportDEO & {
  codeName: string,
  tournamentName: string,
  tournamentDate: DateTime,
  tournamentLocation: string,
}
const transformedReports = computed(() => {
  return props.reports.map(report => {
    let code = store.findCodeById(report.code)
    let tournament = store.findTournamentById(report.tournament)
    return {
      ...report,
      codeName: (code?.name ?? ''),
      tournamentName: (tournament?.name ?? ''),
      tournamentDate: (tournament?.date ?? DateTime.now()),
      tournamentLocation: tournament?.location ?? '',
    } as TransformedTournamentReport
  })
})

function editReport(report: CompactTournamentReportDEO) {
  location.href = "/report/edit/" + report.id
}


function confirmEditReport(report: CompactTournamentReportDEO) {
  if (report.isSubmitted && report.submitDate && report.submitDate.plus({days: 3}) < DateTime.now()) {
    confirm.require({
      message: `This report is already submitted. Are you sure you want to edit it?`,
      header: 'Edit Report',
      icon: 'pi pi-exclamation-triangle',
      accept: () => editReport(report),
      reject: () => {
        console.log("reject")
      }
    })
  } else {

  }
}

const router = useRouter()
function showReport(report: CompactTournamentReportDEO) {
  router.push("/report/" + report.id)
}

const reportToShare = ref<CompactTournamentReportDEO | undefined>(undefined)
const confirm = useConfirm()

function askDeleteReport(report: CompactTournamentReportDEO) {
  console.log("askDeleteReport", report)
  const tournament = store.findTournamentById(report.tournament)
  confirm.require({
    message: `Are you sure you want to delete this report?
            ${tournament?.name} - ${tournament?.location}
             | ${report.numTeams} teams | ${report.numGameReports} game reports`,
    header: 'Delete Report',
    icon: 'pi pi-exclamation-triangle',
    accept: () => store.deleteReport(report),
    reject: () => {
      console.log("reject")
    }
  })

}



</script>
<template>

  <div class="flex-col justify-start hidden md:flex">
    <div v-for="report in transformedReports" key="report.id">
      <div class="bg-surface-700 rounded-lg p-4 m-2">
        <div class="flex flex-row justify-between">
          <div class="flex flex-col grow">
            <div class="flex flex-row justify-between mb-2">
              <div class="text-xl font-bold">{{report.tournamentName}} - {{report.codeName}}</div>
              <div class="text-xl mr-2">
                <template v-if="report.isSubmitted">Submitted</template>
                <template v-else>Not submitted</template>
              </div>
            </div>
            <div class="grow"></div>
            <div class="flex flex-row">
              <div class="mr-4"><vue-feather type="map-pin" class="h-3"></vue-feather> {{report.tournamentLocation}}</div>
              <div class="mr-4"><vue-feather type="calendar" class="h-3"></vue-feather> {{report.tournamentDate.toISODate()}}</div>
              <div class="mr-4"><vue-feather type="bar-chart-2" class="h-3"></vue-feather> {{report.numTeams}} Teams</div>
              <div class="mr-4"><vue-feather type="triangle" class="h-3"></vue-feather> {{report.numGameReports}}
                <template v-if="report.numGameReports==1">Game</template>
                <template v-else>Games</template>
              </div>
            </div>
          </div>
          <div class="flex flex-col ml-4">
            <Button label="Edit"
                    unstyled
                    pt:root="bg-surface-600 p-1 rounded-lg hover:bg-surface-500 w-24 m-1"
                    pt:label="text-primary"
                    @click="() => editReport(report)"></Button>
            <Button
                :label="report.isSubmitted ? 'View' : 'Preview'"
                unstyled
                pt:root="bg-surface-600 p-1 rounded-lg hover:bg-surface-500 w-24 m-1"
                pt:label="text-primary"
                @click="() => showReport(report)" />
            <Button
                v-if="!report.isSubmitted"
                label="Delete"
                unstyled
                pt:root="bg-surface-600 p-1 rounded-lg hover:bg-surface-500 w-24 m-1"
                pt:label="text-primary"
                @click="askDeleteReport(report)" />
            <Button
                label="Share"
                unstyled
                pt:root="bg-surface-600 p-1 rounded-lg hover:bg-surface-500 w-24 m-1"
                pt:label="text-primary"
                @click="() => reportToShare = report" />
          </div>
        </div>
      </div>
    </div>
  </div>

  <div
      class="block m-2 md:hidden"
  >
    <div v-for="report in transformedReports">

          <div>
            <span class="text-lg font-bold">{{report.tournamentName}} </span><br>
            {{report.tournamentLocation}} - {{report.tournamentDate.toISODate()}}  <br>
            {{report.codeName}} <br>
            {{report.numTeams}} teams, {{report.numGameReports}} game reports
          </div>
          <div class="text-lg">{{report.isSubmitted ? 'Submitted' : 'Not submitted'}}</div>
          <div class="flex flex-row justify-end">
            <Button
                label="Edit"
                icon="pi pi-pencil"
                class="p-button-raised p-button-text"
                @click="() => editReport(report)"></Button>
            <Button
                :label="report.isSubmitted ? 'View' : 'Preview'"
                icon="pi pi-folder-open"
                class="p-button-raised p-button-text"
                @click="() => showReport(report)"
            />
            <!-- delete button: -->
            <Button
                v-if="!report.isSubmitted"
                label="Delete"
                icon="pi pi-trash"
                class="p-button-raised p-button-text"
                @click="askDeleteReport(report)"
            />
            <!-- share button: -->
            <Button
                label="Share"
                icon="pi pi-share-alt"
                class="p-button-raised p-button-text"
                @click="() => reportToShare = report"
            />
          </div>

      <Divider />
    </div>
  </div>
  <ShareReport
    :report="reportToShare"
    @on-error="(err) => publicStore.newError(err)"
    @on-success="() => reportToShare = undefined"
    ></ShareReport>


</template>


<style scoped>

</style>
