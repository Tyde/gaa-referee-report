<script setup lang="ts">

import {useAdminStore} from "@/utils/admin_store";
import {computed, onMounted, ref} from "vue";
import type {DatabaseTournament, ReportDEO, Tournament} from "@/types";
import {loadAllTournaments} from "@/utils/api/tournament_api";
import {Report} from "@/types";
import {CompactTournamentReportDEO, CompleteReportDEO, loadAllReports} from "@/utils/api/report_api";
import {FilterMatchMode, FilterOperator} from "primevue/api";
import {useRouter} from "vue-router";

const store = useAdminStore()
const router = useRouter()
const isLoading = ref(true)

const tournaments = ref<DatabaseTournament[]>([])
const tournamentsSortedByDate = computed(() => {
  return tournaments.value.sort((a, b) => {
    return a.date.diff(b.date).milliseconds < 0 ? 1 : -1
  })
})

const reports = ref<Array<CompactTournamentReportDEO>>([])
const filters = ref({
  global: { value: null, matchMode: FilterMatchMode.CONTAINS},
  refereeName: {
    operator: FilterOperator.AND,
    constraints: [{ value: null, matchMode: FilterMatchMode.CONTAINS }]
  },
  codeName: {
    operator: FilterOperator.OR,
    constraints: [{ value: null, matchMode: FilterMatchMode.EQUALS }]
  },
  isSubmitted: {
    operator: FilterOperator.OR,
    constraints: [{ value: true, matchMode: FilterMatchMode.EQUALS }]
  },
})
function reportsByTournament(tournament: DatabaseTournament) {
  let filteed =  reports.value.filter(report => report.tournament == tournament.id)
  let transformed = filteed.map(report => {
    let code = store.findCodeById(report.code)
    return { ...report, codeName: (code?.name ?? '')}
  })
  return transformed

}

onMounted(() => {
  loadAllTournaments()
      .then(it => tournaments.value = it)
      .catch(e => store.newError(e))
  loadAllReports(tournaments.value)
      .then(it => {
        reports.value = it
        isLoading.value = false
      })
      .catch(e => store.newError(e))
})

</script>

<template>
  <div v-if="isLoading" class="z-[10000] flex flex-row justify-center">
    <div class="text-center text-2xl">
      <!-- spinning icon -->
      <i class="pi pi-spin pi-spinner"></i><br>
      <span>Loading</span>
    </div>
  </div>
  <div
      v-else
      v-for="tournament in tournamentsSortedByDate"
      :key="tournament.id"
  >
    <h3>{{tournament.date.toISODate()}} - {{tournament.name}}</h3>
    <h4>{{tournament.location}}</h4>
    <!--<table class="table-auto">
      <tr>
        <th>Referee</th>
        <th>Code</th>
        <th># Games</th>
        <th>Teams</th>
        <th>Submitted</th>
      </tr>
      <tr v-for="report in reportsByTournament(tournament)" :key="report.id">
        <td>{{report.referee.firstName}} {{report.referee.lastName}}</td>
        <td>{{store.findCodeById(report.code).name}}</td>
        <td>{{report.gameReports.length}}</td>
        <td>{{report.selectedTeams.length}}</td>
        <td>{{report.isSubmitted}}</td>
      </tr>
    </table>-->
    <DataTable
        :value="reportsByTournament(tournament)"
        filterDisplay = "menu"
        v-model:filters="filters"
    >
      <Column field="refereeName" header="Referee" :sortable="true" >
        <template #filter="{filterModel,filterCallback}">
          <InputText type="text" v-model="filterModel.value" @keydown.enter="filterCallback()" class="p-column-filter" :placeholder="`Search by name - ${filterModel.matchMode}`"/>
        </template>
      </Column>
      <Column field="codeName" header="Code" :sortable="true" :filterMatchModeOptions="[{label: 'is', value:FilterMatchMode.EQUALS}]">
        <template #filter="{filterModel,filterCallback}">
          <!--<InputText type="text" v-model="filterModel.value" @keydown.enter="filterCallback()" class="p-column-filter" :placeholder="`Search by name - ${filterModel.matchMode}`"/>-->
          <SelectButton
              v-model="filterModel.value"
              :options="store.codes"
              optionLabel="name"
              optionValue="name"
              @change="filterCallback()" />
        </template>
      </Column>
      <Column field="numGameReports" header="# Games" :sortable="true"/>
      <Column field="numTeams" header="# Teams" :sortable="true"/>
      <Column field="isSubmitted"
              header="Submitted"
              :sortable="true"
      >
        <template #filter="{filterModel,filterCallback}">
          Submitted: <Checkbox
              input-id="isSubmitBox"
              v-model="filterModel.value"
              @input="filterCallback()"
              :binary="true"
          />
        </template>
      </Column>
      <Column>
        <template #body="{data}">
          <Button
              label="Open"
              icon="pi pi-folder-open"
              class="p-button-raised p-button-text"
              @click="router.push({path: `/tournament-reports/${data.id}`})"
          />
        </template>

      </Column>
    </DataTable>
  </div>

</template>



<style scoped>
h3 {
  @apply text-2xl;
  @apply font-bold;
  @apply mt-2;
  @apply text-center;
}
h4 {
  @apply text-xl;
  @apply font-bold;
  @apply mb-2;
  @apply text-center;
}
</style>