<script setup lang="ts">

import TeamSelectField from "@/components/team/TeamSelectField.vue";
import {ref, watch} from "vue";
import type {Team} from "@/types/team_types";
import type {DatabaseTournament} from "@/types/tournament_types";
import {loadTournamentPreselectedTeams} from "@/utils/api/tournament_api";
import {usePublicStore} from "@/utils/public_store";

const props = defineProps<{
  tournament: DatabaseTournament
}>()

const emit = defineEmits<{
  (e: 'submit', teams: Team[]): void
}>()

const selectedTeams = ref<Team[]>([])

const store = usePublicStore()

watch(props.tournament, (tournament) => {
  console.log(tournament)
  if (tournament) {
    selectedTeams.value = []
    loadTournamentPreselectedTeams(tournament.id)
        .then((teams) => {
          for(const id of teams.teamIds) {
            const team = store.findTeamById(id)
            if(team) {
              selectedTeams.value.push(team)
            }
          }
        })
  }
}, {immediate: true})
function teamUnselected(team: Team) {
  selectedTeams.value = selectedTeams.value.filter((t) => t.id !== team.id)
}
function teamSelected(team: Team) {
  selectedTeams.value.push(team)
}

function submit() {
  emit('submit', selectedTeams.value)
  selectedTeams.value = []
}
</script>

<template>
<div class="flex flex-col items-center">
  <h3>Preselect Teams</h3>
  <TeamSelectField
      :show_new_amalgamate="true"
      :show_add_new_team="true"
      :exclude_team_list="selectedTeams"
      :force_hide_exclude_team_list="true"
      :allow_unselect="true"
      :show_hide_squad_box="true"
      @team_unselected="team => teamUnselected(team)"
      @team_selected="team => teamSelected(team)"
      class="w-[800px]"
      />
  <div>
    <ul class="flex flex-row flex-wrap max-w-[800px] justify-center">
      <li
          v-for="team in selectedTeams"
          :key="team.id"
          class="bg-surface-600 rounded-lg p-2 m-2 cursor-pointer"
          @click="teamUnselected(team)"
      >
        {{team.name}}
      </li>
    </ul>
  </div>
  <Button label="Submit" @click="submit"/>
</div>
</template>

<style scoped>

</style>
