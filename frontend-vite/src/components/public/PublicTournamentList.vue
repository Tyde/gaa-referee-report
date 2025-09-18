<script setup lang="ts">
import {usePublicStore} from "@/utils/public_store";
import {type DatabaseTournament, PublicTournamentWithTeamsDEO, RegionDEO} from "@/types/tournament_types";
import {computed, onBeforeMount, ref} from "vue";
import {useRouter} from "vue-router";
import {loadAllTournamentsWithTeams} from "@/utils/api/tournament_api";
import type {Team} from "@/types/team_types";
import RangedDateTimePicker from "@/components/util/RangedDateTimePicker.vue";
import {DateTime} from "luxon";

const store = usePublicStore();

function regionIDToRegion(regionID: number): RegionDEO {
  return store.regions.filter(it => it.id == regionID)[0]
}

const tournamentsWithTeams = ref<PublicTournamentWithTeamsDEO[]>([])


const tournamentsSortedByDate = computed(() => {
  return store.tournaments.toSorted((a, b) => {
    return a.date.diff(b.date).milliseconds < 0 ? 1 : -1
  }).filter(tournament => {
    if (selectedRegion.value) {
      return tournament.region == selectedRegion.value?.id
    } else {
      return true
    }
  }).filter(teamFilterPredicate)
      .filter(tournament => {
        let tDate = tournament.date
        let isLaterThanStart = tDate.diff(dateTimeRange.value[0]).milliseconds > 0
        let isEarlierThanEnd = tDate.diff(dateTimeRange.value[1]).milliseconds < 0
        return isLaterThanStart && isEarlierThanEnd
      })
})

const teamFilterPredicate = (tournament: DatabaseTournament) => {
  if (selectedTeam.value) {
    const teamsInTournament = tournamentsWithTeams.value.find(it => it.tournament.id == tournament.id)?.teams
    if (!teamsInTournament) {
      return false
    }
    return teamsInTournament.filter(it => {
      if (it.isAmalgamation) {
        return (it.amalgamationTeams
            ?.filter(amalgamationTeam => amalgamationTeam.id == selectedTeam.value?.id).length ?? 0) > 0
      } else {
        return it.id == selectedTeam.value?.id
      }
    }).length > 0
  } else {
    return true
  }
}

const router = useRouter()

function navigateToReport(reportID: number) {
  let id = reportID.toString()
  router.push({path: `/tournament/${id}`})
}

function loadTournamentsWithTeams() {
  loadAllTournamentsWithTeams().then(response => {
    tournamentsWithTeams.value = response.tournaments
  })
      .catch(error => {
        store.newError(error)
      })
}

onBeforeMount(() => {
  loadTournamentsWithTeams()
})
const selectedTeam = ref<Team | undefined>(undefined)
const allTeams = computed(() => {
  return store.teams.filter(it => !it.isAmalgamation)
      .sort((a, b) => {
    return a.name.localeCompare(b.name)
  })
})

const selectedRegion = ref<RegionDEO | undefined>(undefined)
const dateRange = ref<Date[]>([DateTime.now().minus({days: 180}).toJSDate(), new Date()])
const dateTimeRange = computed(() => {
  return dateRange.value.map(it => DateTime.fromJSDate(it))
})
</script>

<template>
  <div class="flex-col w-full flex items-center">
    <div class="flex flex-col w-full">
      <RangedDateTimePicker v-model:date-range="dateRange" class="m-2"/>
      <div class="flex flex-row justify-center content-center">


        <div>
          <SelectButton
              v-model="selectedRegion"
              :options="store.regions"
              optionLabel="name"
          />
        </div>
        <div v-if="selectedRegion">
          <Button @click="selectedRegion = undefined">X</Button>
        </div>
      </div>
      <div class="flex flex-row justify-center content-center m-2">
        <Select
            v-model="selectedTeam"
            :options="allTeams"
            optionLabel="name"
            placeholder="Filter by team"
            filter
        />
        <div v-if="selectedTeam" class="ml-2">
          <Button @click="selectedTeam = undefined">X</Button>
        </div>
      </div>
      <div
          v-for="tournament in tournamentsSortedByDate"
          key="tournament.id"
          class="single-tournament-row"
          @click="navigateToReport(tournament.id)"
      >
        <div class="flex flex-row">
          <div class="grow font-bold text-xl mb-4">{{ tournament.name }}</div>
          <div><vue-feather type="map-pin" class="h-3"></vue-feather> {{ tournament.location }}</div>
        </div>
        <div class="flex flex-row">
          <div class="grow"><vue-feather type="calendar" class="h-3"></vue-feather> {{ tournament.date.toISODate() }}</div>
          <div>{{ regionIDToRegion(tournament.region).name }}</div>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.single-tournament-row {
  @apply flex flex-col;
  @apply p-4 m-2;
  @apply rounded-lg bg-surface-700 border border-surface-600;
  @apply hover:bg-surface-600;
  @apply cursor-pointer;
}


</style>
