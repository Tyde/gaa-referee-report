<script lang="ts" setup>
import {onMounted, ref, watch} from "vue";
import CreateTournament from "@/components/tournament/CreateTournament.vue";
import {loadAllTournaments, loadTournamentsOnDate} from "@/utils/api/tournament_api";
import {DateTime} from "luxon";
import {useReportStore} from "@/utils/edit_report_store";
import type {DatabaseTournament} from "@/types/tournament_types";
import { TrophyIcon } from "@heroicons/vue/20/solid";
import { ListBulletIcon } from "@heroicons/vue/20/solid";


const tournamentModel = defineModel<DatabaseTournament>()


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
  tournamentModel.value = tournament
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
          if(tournament.isLeague && tournament.endDate) {
            let isInSpan = tournament.date.diffNow().as('days') < 0 &&
                tournament.endDate.diffNow().as('days') > 0
            let isJustAfterEnd = tournament.endDate.diffNow().as('days') < 0 &&
                tournament.endDate.diffNow().as('days') > -14
            return isInSpan || isJustAfterEnd
          }
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

  <div class="mx-auto w-full md:w-10/12 xl:w-5/12">
    <div v-if="!show_create_new_tournament">


      <h2>
        {{ $t('tournament.select') }}
      </h2>
      <div v-if="tournamentModel" class="text-center mb-2">

        {{ $t('tournament.selected') }}: {{ tournamentModel.name }} - {{ tournamentModel.location }} -
        {{ tournamentModel.date.toISODate() }}
      </div>

      <div class="flex flex-row justify-center">
        <div class="pr-3 flex items-center"><label for="dateformat">{{ $t('tournament.date') }}: </label></div>
        <div>
          <Calendar id="dateformat" v-model="date" dateFormat="yy-mm-dd"/>
        </div>
      </div>
      <div class="p-listbox">
        <ul class="p-listbox-list">
          <li
              v-if="loading"
              class="p-listbox-item">
            {{ $t('navigation.loading') }}
          </li>
          <li
              v-if="showingRecentTournaments && found_tournaments.length >0"
              class=" italic bg-amber-200 hover:bg-amber-200 p-listbox-item"
          >
            {{ $t('tournament.recent') }}
          </li>
          <li
              v-for="tournament in found_tournaments"
              :class="{
                'selected-tournament': tournamentModel && tournament.id === tournamentModel.id
              }"
              class="p-listbox-item flex flex-row items-center"
              @click="select_tournament(tournament)"
          >
            <div>
              <TrophyIcon v-if="!tournament.isLeague" class="h-4 w-4 mr-1 text-gray-500" />
              <ListBulletIcon v-else class="h-4 w-4 mr-1 text-gray-500" />
            </div>

            <div>
              {{ tournament.name }} | {{ tournament.location }} |
              <template v-if="!tournament.isLeague"> {{ tournament.date.toISODate() }}</template>
              <template v-else> {{ tournament.date.toISODate() }} - {{ tournament.endDate?.toISODate() }} | League
              </template>
            </div>
          </li>
          <li
              v-if="date"
              class="p-listbox-item tournamentselect-add-option"
              @click="show_create_new_tournament=true"

          >
            {{ $t('tournament.notInListCreateQuestion') }}
          </li>
          <li
              v-else
              class="p-listbox-item tournamentselect-add-option"
              @click="showNewTournamentDialogWithoutDate">
            {{ $t('tournament.notInListCreateQuestion') }}
          </li>
        </ul>
      </div>

    </div>



    <CreateTournament
        v-else
        :preselected-date="date ?? new Date()"
        @tournament_created="on_tournament_created"
        @canceled="show_create_new_tournament=false"
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
