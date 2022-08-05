<script setup lang="ts">
import {computed, onMounted, ref, watch} from "vue";
import {fromDateToDateString} from "@/utils/gobal_functions";
import CreateTournament from "@/components/CreateTournament.vue";
import type {DatabaseTournament, Tournament} from "@/types";
const emit = defineEmits<{
  (e:'tournament_selected', tournament:Tournament): void
}>()

const props = defineProps<{
  alreadySelectedTournament?:DatabaseTournament
}>()

const date = ref<Date>()
const found_tournaments = ref(<DatabaseTournament[]>[])
const selected_tournament = ref<DatabaseTournament|undefined>()
const show_create_new_tournament = ref(false)
const dateString = computed(() => {
  let newdate = date.value
  return fromDateToDateString(newdate)
  //return Intl.DateTimeFormat('en-US',{calendar: "iso8601"}).format(date.value)
})

function select_tournament(tournament:DatabaseTournament) {
  selected_tournament.value = tournament
  emit('tournament_selected',tournament)
}
async function loadTournamenmentsOnDate() {
  const res = await(fetch("/api/tournament/find_by_date/"+dateString.value))
  let tempResponse:Array<{id:number,name:string,location:string,date:string }> = await res.json()
  found_tournaments.value = tempResponse.map(value => {
    return {
      id:value.id,
      name:value.name,
      date:new Date(value.date),
      location:value.location
    } as DatabaseTournament
  })

}
watch(dateString, async (newDateString,oldDateString) => {
  loadTournamenmentsOnDate()
})

function on_tournament_created(tournament:DatabaseTournament) {
  selected_tournament.value = tournament
  date.value = tournament.date
  show_create_new_tournament.value = false
  loadTournamenmentsOnDate()
}

onMounted(()=> {
  if(props.alreadySelectedTournament) {
    selected_tournament.value = props.alreadySelectedTournament
    console.log(selected_tournament)
    console.log(selected_tournament.value.date)
    date.value = selected_tournament.value.date
  }
})
</script>
<template>
  <template v-if="!show_create_new_tournament">
  <Card>
    <template #content>
      <template v-if="selected_tournament">

        Selected tournament: {{selected_tournament.name}} - {{selected_tournament.location}} - {{selected_tournament.date}}
      </template>

      <div class="field col-12 md:col-4">
        <label for="dateformat">Date of Tournament:</label>
        <Calendar id="dateformat" v-model="date"  dateFormat="yy-mm-dd" />
      </div>
      <div class="p-listbox" v-if="dateString">
        <ul class="p-listbox-list">
          <li
              v-for="tournament in found_tournaments"
              class="p-listbox-item"
              @click="select_tournament(tournament)"
              :class="{
                'selected-tournament': selected_tournament && tournament.id === selected_tournament.id
              }"
          >
            {{tournament.name}} - {{tournament.location}} - {{fromDateToDateString(tournament.date)}}
          </li>
          <li
              class="p-listbox-item tournamentselect-add-option"
              v-if="dateString"
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