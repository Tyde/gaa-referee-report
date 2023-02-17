<script lang="ts" setup>
import {onMounted, ref, watch} from "vue";
import CreateTournament from "@/components/tournament/CreateTournament.vue";
import type {DatabaseTournament} from "@/types";
import {loadAllTournaments, loadTournamentsOnDate} from "@/utils/api/tournament_api";
import {DateTime} from "luxon";
import {useReportStore} from "@/utils/edit_report_store";

/*
const emit = defineEmits<{
  (e: 'update:modelValue', tournament: Tournament): void
}>()

const props = defineProps<{
  modelValue?: DatabaseTournament
}>()
*/

const store = useReportStore()

const date = ref<Date>()
const found_tournaments = ref(<DatabaseTournament[]>[])
//const selected_tournament = ref<DatabaseTournament | undefined>()
const show_create_new_tournament = ref(false)
const loading = ref(false)

const completeTournamentList = ref(<DatabaseTournament[]>[])
const showingRecentTournaments = ref(false)

function select_tournament(tournament: DatabaseTournament) {
  //selected_tournament.value = tournament
  store.report.tournament = tournament
}

watch(date, async () => {
  if (date.value) {
    showingRecentTournaments.value = false
    loading.value = true
    found_tournaments.value = await loadTournamentsOnDate(DateTime.fromJSDate(date.value))
    loading.value = false
  } else {

  }
})

function on_tournament_created(tournament: DatabaseTournament) {
  select_tournament(tournament)
  date.value = tournament.date.toJSDate()
  show_create_new_tournament.value = false
  loadTournamentsOnDate(DateTime.fromJSDate(date.value))
}

function showNewTournamentDialogWithoutDate() {
  date.value = DateTime.now().minus({days: DateTime.now().weekday + 1}).toJSDate()
  show_create_new_tournament.value = true

}

onMounted(() => {
  loadAllTournaments()
      .then((tournaments) => {
        completeTournamentList.value = tournaments
        found_tournaments.value = completeTournamentList.value.filter((tournament) => {
          return Math.abs(tournament.date.diffNow().as('days')) < 14
        })
        showingRecentTournaments.value = true
      })
      .catch((error) => {
        store.newError(error)
      })
})

</script>
<template>
  <div class="mx-auto w-5/12">

    <div v-if="!show_create_new_tournament">
      <h2>
        Select a tournament
      </h2>
      <div v-if="store.report.tournament" class="text-center mb-2">

        Selected tournament: {{ store.report.tournament.name }} - {{ store.report.tournament.location }} -
        {{ store.report.tournament.date.toISODate() }}
      </div>

      <div class="flex flex-row justify-center">
        <div class="pr-3 flex items-center"><label for="dateformat">Date of Tournament: </label></div>
        <div>
          <Calendar id="dateformat" v-model="date" dateFormat="yy-mm-dd"/>
        </div>
      </div>
      <div class="p-listbox">
        <ul class="p-listbox-list">
          <li
              v-if="loading"
              class="p-listbox-item">
            Loading...
          </li>
          <li
              v-if="showingRecentTournaments && found_tournaments.length >0"
              class=" italic bg-amber-200 hover:bg-amber-200 p-listbox-item"
          >
            Showing recent tournaments
          </li>
          <li
              v-for="tournament in found_tournaments"
              :class="{
                'selected-tournament': store.report.tournament && tournament.id === store.report.tournament.id
              }"
              class="p-listbox-item"
              @click="select_tournament(tournament)"
          >
            {{ tournament.name }} - {{ tournament.location }} - {{ tournament.date.toISODate() }}
          </li>
          <li
              v-if="date"
              class="p-listbox-item tournamentselect-add-option"
              @click="show_create_new_tournament=true"

          >
            Tournament not in list? Create new ...
          </li>
          <li
              v-else
              class="p-listbox-item tournamentselect-add-option"
              @click="showNewTournamentDialogWithoutDate">
            Tournament not in list? Create new ...
          </li>
        </ul>
      </div>
    </div>


    <CreateTournament
        v-else
        :preselected-date="date ?? new Date()"
        @tournament_created="on_tournament_created"
    />
  </div>
</template>


<style scoped>
h2 {
  @apply text-2xl font-bold text-gray-700 text-center mt-2 mb-2;
}
.tournamentselect-add-option {
  font-style: italic;
  font-weight: bold;
  color: darkcyan !important;
}

.selected-tournament {
  background-color: cornflowerblue;
}
</style>