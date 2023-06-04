<script setup lang="ts">

import SingleTournamentEditor from "@/components/admin/tournaments/SingleTournamentEditor.vue";
import {useAdminStore} from "@/utils/admin_store";
import {computed, onMounted, ref} from "vue";
import {DatabaseTournament, RegionDEO} from "@/types/tournament_types";
import {loadAllTournaments} from "@/utils/api/tournament_api";
import {loadAllReports} from "@/utils/api/report_api";
let store = useAdminStore()

const tournaments = ref<DatabaseTournament[]>([])
const tournamentsSortedByDate = computed(() => {
  return tournaments.value.sort((a, b) => {
    return a.date.diff(b.date).milliseconds < 0 ? 1 : -1
  }).filter(tournament => {
    if (selectedRegion.value) {
      return tournament.region == selectedRegion.value?.id
    } else {
      return true
    }
  })
})

const selectedRegion = ref<RegionDEO | undefined>(undefined)
const isLoading = ref(true)


onMounted(() => {
  loadAllTournaments()
      .then(it => tournaments.value = it)
      .catch(e => store.newError(e))
      .finally(() => isLoading.value = false)
})

</script>

<template>
  <div v-if="isLoading" class="z-[10000] flex flex-row justify-center">
    <div class="text-center text-2xl">
      <!-- spinning icon -->
      <i class="pi pi-spin pi-spinner"></i><br>
      <span>Loading</span>
    </div>
  </div>
  <div v-else>
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

    <div class="flex justify-center">
      <div class="flex flex-col w-screen lg:w-7/12 xl:w-5/12 ">
        <SingleTournamentEditor
            v-for="tournament in tournamentsSortedByDate"
            :key="tournament.id"
            :tournament="tournament"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>