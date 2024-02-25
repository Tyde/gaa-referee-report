<script setup lang="ts">

import {computed, onMounted, ref} from "vue";
import {loadAllTournaments} from "@/utils/api/tournament_api";
import {loadAllReports} from "@/utils/api/report_api";
import {FilterMatchMode, FilterOperator} from "primevue/api";
import {useRouter} from "vue-router";
import type {DatabaseTournament} from "@/types/tournament_types";
import {RegionDEO} from "@/types/tournament_types";
import type {CompactTournamentReportDEO} from "@/types/report_types";
import ShareReport from "@/components/dashboard/ShareReport.vue";
import {usePublicStore} from "@/utils/public_store";
import {DateTime} from "luxon";
import RangedDateTimePicker from "@/components/util/RangedDateTimePicker.vue";


const publicStore = usePublicStore()
const router = useRouter()
const isLoading = ref(true)

const first = ref(0)
const rows = ref(10)

const tournaments = ref<DatabaseTournament[]>([])
const tournamentsSortedByDate = computed(() => {
  return tournaments.value.toSorted((a, b) => {
    return a.date.diff(b.date).milliseconds < 0 ? 1 : -1
  })
})

const tournamentsFiltered = computed(() => {
  return tournamentsSortedByDate.value.filter(tournament => {
    if (selectedRegion.value) {
      return tournament.region == selectedRegion.value?.id
    } else {
      return true
    }
  }).filter(tournament => {
    let tDate = tournament.date
    let isLaterThanStart = tDate.diff(dateTimeRange.value[0]).milliseconds > 0
    let isEarlierThanEnd = tDate.diff(dateTimeRange.value[1]).milliseconds < 0
    return isLaterThanStart && isEarlierThanEnd
  })
})

const tournamentsPaginated = computed(() => {
  return tournamentsFiltered.value.slice(first.value, first.value + rows.value)
})

const reports = ref<Array<CompactTournamentReportDEO>>([])
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
  isSubmitted: {
    operator: FilterOperator.OR,
    constraints: [{value: true, matchMode: FilterMatchMode.EQUALS}]
  },
})

function reportsByTournament(tournament: DatabaseTournament) {
  let filteed = reports.value.filter(report => report.tournament == tournament.id)
  return filteed.map(report => {
    let code = publicStore.findCodeById(report.code)
    return {...report, codeName: (code?.name ?? '')}
  })

}

const reportToShare = ref<CompactTournamentReportDEO | undefined>(undefined)
const selectedRegion = ref<RegionDEO | undefined>(undefined)

onMounted(() => {
  loadAllTournaments()
      .then(it => tournaments.value = it)
      .catch(e => publicStore.newError(e))
  loadAllReports()
      .then(it => {
        console.log("reports loaded")
        reports.value = it
        isLoading.value = false
      })
      .catch(e => publicStore.newError(e))
})
const dateRange = ref<Date[]>([DateTime.now().minus({days: 180}).toJSDate(), new Date()])
const dateTimeRange = computed(() => {
  return dateRange.value.map(it => DateTime.fromJSDate(it))
})

function shareSingleReport(report: CompactTournamentReportDEO) {
  console.log(report)
  reportToShare.value = report
}


</script>

<template>
  <ShareReport
      :report="reportToShare"
      @on-error="(err) => publicStore.newError(err)"
      @on-success="() => reportToShare = undefined"
  ></ShareReport>
  <div v-if="isLoading" class="z-[10000] flex flex-row justify-center">
    <div class="text-center text-2xl">
      <!-- spinning icon -->
      <i class="pi pi-spin pi-spinner"></i><br>
      <span>Loading</span>
    </div>
  </div>
  <div
      v-else>
    <RangedDateTimePicker v-model:date-range="dateRange" class="m-2"/>
    <div class="flex flex-row justify-center content-center m-2">
      <div>
        <SelectButton
            v-model="selectedRegion"
            :options="publicStore.regions"
            optionLabel="name"
        />
      </div>
      <div v-if="selectedRegion">
        <Button @click="selectedRegion = undefined">X</Button>
      </div>
    </div>
    <div
        v-for="tournament in tournamentsPaginated"
        :key="tournament.id"
    >
      <div class="flex flex-row">
        <div class="grow">
          <h3>{{ tournament.date.toISODate() }} - {{ tournament.name }}</h3>
          <h4>{{ tournament.location }}</h4>
        </div>
        <div>
          <Button
              label="Full report"
              icon="pi pi-folder-open"
              class="m-2"
              @click="router.push({path: `/tournament-reports/complete/${tournament.id}`})"
              />
        </div>
      </div>
      <DataTable
          :value="reportsByTournament(tournament)"
          filterDisplay="menu"
          v-model:filters="filters"
      >
        <Column field="refereeName" header="Referee" :sortable="true">
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
                :options="publicStore.codes"
                optionLabel="name"
                optionValue="name"
                @change="filterCallback()"/>
          </template>
        </Column>
        <Column field="numGameReports" header="# Games" :sortable="true"/>
        <Column field="numTeams" header="# Teams" :sortable="true"/>
        <Column field="isSubmitted"
                header="Submitted"
                :sortable="true"
        >
          <template #filter="{filterModel,filterCallback}">
            Submitted:
            <Checkbox
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
            <!-- share button: -->
            <Button
                label="Share"
                icon="pi pi-share-alt"
                class="p-button-raised p-button-text"
                @click="() => shareSingleReport(data)"
            />
          </template>

        </Column>
      </DataTable>


    </div>
    <Paginator
        v-model:first="first"
        v-model:rows="rows"
        :total-records="tournamentsFiltered.length"
        :rowsPerPageOptions="[10, 20, 50, 100]"
    />
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
