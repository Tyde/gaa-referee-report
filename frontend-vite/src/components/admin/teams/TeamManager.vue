<script lang="ts" setup>
import {allTeams} from "@/utils/api/teams_api";
import {computed, onMounted, ref} from "vue";
import type {Team} from "@/types";
import TeamList from "@/components/admin/teams/TeamList.vue";

const teams = ref<Team[]>([])
const teamsNoAmalgamations = computed(() => teams.value.filter(it => !it.isAmalgamation))
const amalgamtions = computed(() => teams.value.filter(it => it.isAmalgamation))
function fetchAllTeams() {
  allTeams()
      .then(data => teams.value = data)
      .catch(error => console.log(error))
}


function onTeamUpdated(team: Team) {
  let slot = teams.value.findIndex(it => it.id === team.id)
  if(slot) {
    teams.value[slot] = team
  }
  fetchAllTeams()
}

onMounted(() => {
  fetchAllTeams()
})
</script>

<template>

  <div class="flex flex-row">
    <div class="basis-1/2">
      <h3>Teams</h3>
      <TeamList :teams="teamsNoAmalgamations" :amalgamation-display="false" @team-updated="onTeamUpdated"/>
    </div>
    <div class="basis-1/2">
      <h3>Amalgamations</h3>
      <TeamList :teams="amalgamtions" :amalgamation-display="true" @team-updated="onTeamUpdated"/>
    </div>
  </div>

</template>



<style scoped>
h3{
  @apply m-2;
  @apply text-xl font-bold;
  @apply text-center;
}
</style>