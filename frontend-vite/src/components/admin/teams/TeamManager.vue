<script lang="ts" setup>
import {loadAllTeams} from "@/utils/api/teams_api";
import {computed, onMounted, ref} from "vue";
import TeamList from "@/components/admin/teams/TeamList.vue";
import type {Team} from "@/types/team_types";
import {useAdminStore} from "@/utils/admin_store";

//const teams = ref<Team[]>([])
const store = useAdminStore()
const teamsNoAmalgamations = computed(() => store.publicStore.teams.filter(it => !it.isAmalgamation))
const amalgamtions = computed(() => store.publicStore.teams.filter(it => it.isAmalgamation))
function fetchAllTeams() {
  store.publicStore.loadTeams()
}


function onTeamUpdated(team: Team) {
  let slot = store.publicStore.teams.findIndex(it => it.id === team.id)
  if(slot) {
    store.publicStore.teams[slot] = team
  }
  fetchAllTeams()
}

onMounted(() => {
  fetchAllTeams()
})
</script>

<template>

  <div class="flex flex-col lg:flex-row">
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
