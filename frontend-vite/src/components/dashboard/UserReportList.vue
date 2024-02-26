<script lang="ts" setup>

import {useDashboardStore} from "@/utils/dashboard_store";
import {computed, ref} from "vue";
import {FilterMatchMode, FilterOperator} from "primevue/api";
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
    }
  })
})

function editReport(report: CompactTournamentReportDEO) {
  location.href = "/report/edit/" + report.id
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


  <DataTable
      :value="transformedReports"
      filterDisplay="menu"
      v-model:filters="filters"
      class="hidden md:block"
  >
    <Column field="tournamentName" header="Tournament" :sortable="true">
      <template #filter="{filterModel,filterCallback}">
        <InputText type="text" v-model="filterModel.value" @keydown.enter="filterCallback()" class="p-column-filter"
                   :placeholder="`Search by name - ${filterModel.matchMode}`"/>
      </template>
    </Column>
    <Column field="tournamentLocation" header="Location" :sortable="true">
      <template #filter="{filterModel,filterCallback}">
        <InputText type="text" v-model="filterModel.value" @keydown.enter="filterCallback()" class="p-column-filter"
                   :placeholder="`Search by name - ${filterModel.matchMode}`"/>
      </template>
    </Column>
    <Column field="tournamentDate" header="Date" :sortable="true">
      <template #body="{data}">
        {{ data.tournamentDate.toISODate() }}
      </template>
      <template #filter="{filterModel,filterCallback}">
        <InputText type="text" v-model="filterModel.value" @keydown.enter="filterCallback()" class="p-column-filter"
                   :placeholder="`Search by name - ${filterModel.matchMode}`"/>
      </template>
    </Column>
    <Column field="codeName" header="Code" :sortable="true"
            :filterMatchModeOptions="[{label: 'is', value:FilterMatchMode.EQUALS}]">
      <template #filter="{filterModel,filterCallback}">
        <!--<InputText type="text" v-model="filterModel.value" @keydown.enter="filterCallback()" class="p-column-filter" :placeholder="`Search by name - ${filterModel.matchMode}`"/>-->
        <SelectButton
            v-model="filterModel.value"
            :options="store.codes"
            optionLabel="name"
            optionValue="name"
            @change="filterCallback()"/>
      </template>
    </Column>
    <Column field="numGameReports" header="# Games" :sortable="true"/>
    <Column field="numTeams" header="# Teams" :sortable="true"/>
    <Column field="isSubmitted"
            header="Submitted"
    >
    </Column>
    <Column>
      <template #body="{data}">
        <Button
            label="Edit"
            icon="pi pi-pencil"
            class="p-button-raised p-button-text"
            @click="() => editReport(data)"></Button>
        <Button
            :label="data.isSubmitted ? 'View' : 'Preview'"
            icon="pi pi-folder-open"
            class="p-button-raised p-button-text"
            @click="() => showReport(data)"
        />
        <!-- delete button: -->
        <Button
            v-if="!data.isSubmitted"
            label="Delete"
            icon="pi pi-trash"
            class="p-button-raised p-button-text"
            @click="askDeleteReport(data)"
        />
        <!-- share button: -->
        <Button
            label="Share"
            icon="pi pi-share-alt"
            class="p-button-raised p-button-text"
            @click="() => reportToShare = data"
        />
      </template>

    </Column>
  </DataTable>

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
