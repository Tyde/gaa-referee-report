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
  <hr class="h-px my-8 bg-surface-600 border-0 mx-32">
  <template v-if="!isLoading">
    <TournamentSelector
      v-if="tournament === undefined"
      v-model="tournament"
    />
    <div v-else>
      <div class="tournament-info-bubble">{{tournament.name}} in {{tournament.location}}
      </div>
      <div class="font-bold primary-300 text-center">{{tournament.date.toISODate()}}</div>
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
.tournament-info-bubble {
  @apply text-2xl font-bold bg-surface-600 rounded-lg shadow py-4 text-center mx-auto text-primary-200 w-auto
}
</style>
