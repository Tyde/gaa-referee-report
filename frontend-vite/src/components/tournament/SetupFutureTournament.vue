<script setup lang="ts">

import {ref} from "vue";
import type {DatabaseTournament} from "@/types/tournament_types";
import TournamentSelector from "@/components/tournament/TournamentSelector.vue";
import PreselectTeam from "@/components/team/PreselectTeam.vue";
import type {Team} from "@/types/team_types";
import {setTournamentPreselectedTeams} from "@/utils/api/tournament_api";
import {usePublicStore} from "@/utils/public_store";

const tournament = ref<DatabaseTournament>()
const store = usePublicStore()
const isLoading = ref(false)
async function submit(teams: Team[]) {
  if (tournament.value === undefined) {
    return
  }
  isLoading.value = true
  setTournamentPreselectedTeams(tournament.value, teams)
      .then(() => {
        tournament.value = undefined
      })
      .catch((e) => {
        store.newError(e.message)
      })
      .finally(() => {
        isLoading.value = false
      })
}
</script>

<template>
  <h1>Setup Tournament</h1>
  <template v-if="!isLoading">
    <TournamentSelector
      v-if="tournament === undefined"
      v-model="tournament"
    />
    <div v-else>
      <h2>{{tournament.name}} in {{tournament.location}}</h2>
      <h3>{{tournament.date.toISODate()}}</h3>
      <PreselectTeam
          @submit="teams => submit(teams)"
          :tournament="tournament"
      />
    </div>
  </template>
  <template v-else>
    <p>Loading...</p>
  </template>
</template>

<style scoped>
h1 {
  @apply text-2xl font-semibold text-center mb-2;
}
h2 {
  @apply text-xl font-semibold text-center mb-1;
}
h3 {
  @apply text-lg font-semibold text-center mb-1;
}
</style>
