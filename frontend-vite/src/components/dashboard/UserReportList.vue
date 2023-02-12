<script lang="ts" setup>

import {useDashboardStore} from "@/utils/dashboard_store";
import type {CompactTournamentReportDEO} from "@/utils/api/report_api";
import {computed, ref} from "vue";
import {FilterMatchMode, FilterOperator} from "primevue/api";
import {DateTime} from "luxon";
import {useConfirm} from "primevue/useconfirm";


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
      tournamentDate: (tournament?.date ?? ''),
      tournamentLocation: tournament?.location ?? '',
    }
  })
})

function editReport(report: CompactTournamentReportDEO) {
  location.href = "/report/edit/" + report.id
}

function showReport(report: CompactTournamentReportDEO) {
  location.href = "/report/show/" + report.id
}


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
            v-if="!data.isSubmitted"
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
      </template>

    </Column>
  </DataTable>


</template>


<style scoped>

</style>