<script lang="ts" setup>
import {onMounted, onUpdated, ref, watch} from "vue";
import {fromDateToDateString} from "@/utils/gobal_functions";
import CreateTournament from "@/components/CreateTournament.vue";
import type {DatabaseTournament, Tournament} from "@/types";
import {loadTournamentsOnDate} from "@/utils/api/tournament_api";
import {DateTime} from "luxon";

const emit = defineEmits<{
  (e: 'update:modelValue', tournament: Tournament): void
}>()

const props = defineProps<{
  modelValue?: DatabaseTournament
}>()

const date = ref<Date>()
const found_tournaments = ref(<DatabaseTournament[]>[])
//const selected_tournament = ref<DatabaseTournament | undefined>()
const show_create_new_tournament = ref(false)
const loading = ref(false)

/*
const dateString = computed(() => {
  let newdate = date.value
  return fromDateToDateString(newdate)
  //return Intl.DateTimeFormat('en-US',{calendar: "iso8601"}).format(date.value)
})*/

function select_tournament(tournament: DatabaseTournament) {
  //selected_tournament.value = tournament
  emit('update:modelValue', tournament)
}

watch(date, async () => {
  if (date.value) {
    loading.value = true
    found_tournaments.value = await loadTournamentsOnDate(DateTime.fromJSDate(date.value))
    loading.value = false
  }
})

function on_tournament_created(tournament: DatabaseTournament) {
  select_tournament(tournament)
  date.value = tournament.date.toJSDate()
  show_create_new_tournament.value = false
  loadTournamentsOnDate(DateTime.fromJSDate(date.value))
}

</script>
<template>
  <template v-if="!show_create_new_tournament">
    <Card>
      <template #content>
        <template v-if="modelValue">

          Selected tournament: {{ modelValue.name }} - {{ modelValue.location }} -
          {{ modelValue.date.toISODate() }}
        </template>

        <div class="field col-12 md:col-4">
          <label for="dateformat">Date of Tournament:</label>
          <Calendar id="dateformat" v-model="date" dateFormat="yy-mm-dd"/>
        </div>
        <div v-if="date" class="p-listbox">
          <ul class="p-listbox-list">
            <li
                v-if="loading"
                class="p-listbox-item">
              Loading...
            </li>
            <li
                v-for="tournament in found_tournaments"
                :class="{
                'selected-tournament': modelValue && tournament.id === modelValue.id
              }"
                class="p-listbox-item"
                @click="select_tournament(tournament)"
            >
              {{ tournament.name }} - {{ tournament.location }} - {{ fromDateToDateString(tournament.date) }}
            </li>
            <li
                v-if="date"
                class="p-listbox-item tournamentselect-add-option"
                @click="show_create_new_tournament=true"

            >
              Tournament not in list? Create new ...
            </li>
          </ul>
        </div>
      </template>
      <template #title>
        Select a tournament
      </template>

    </Card>
  </template>
  <CreateTournament
      v-else
      :preselected-date="date"
      @tournament_created="on_tournament_created"
  />
</template>


<style scoped>
.tournamentselect-add-option {
  font-style: italic;
  font-weight: bold;
  color: darkcyan !important;
}

.selected-tournament {
  background-color: cornflowerblue;
}
</style>