<script setup lang="ts">

import SingleTournamentEditor from "@/components/admin/tournaments/SingleTournamentEditor.vue";
import {useAdminStore} from "@/utils/admin_store";
import {computed, onMounted, ref} from "vue";
import {DatabaseTournament, RegionDEO} from "@/types/tournament_types";
import {loadAllTournaments} from "@/utils/api/tournament_api";
import {DateTime} from "luxon";
import RangedDateTimePicker from "@/components/util/RangedDateTimePicker.vue";
import {useRouter} from "vue-router";

let store = useAdminStore()

const tournaments = ref<DatabaseTournament[]>([])
const tournamentsSortedByDate = computed(() => {
  return tournaments.value.sort((a, b) => {
    return a.date.diff(b.date).milliseconds < 0 ? 1 : -1
  })
})

const tournamentsFiltered = computed(() => {
  return tournamentsSortedByDate.value.filter(tournament => {
    if (selectedRegion.value) {
      return tournament.region == selectedRegion.value?.id
    } else {
      return true
    }
  }).filter(tournament => {
    let tDate = tournament.date
    let isLaterThanStart = tDate.diff(dateTimeRange.value[0]).milliseconds > 0
    let isEarlierThanEnd = tDate.diff(dateTimeRange.value[1]).milliseconds < 0
    return isLaterThanStart && isEarlierThanEnd
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
const dateRange = ref<Date[]>([DateTime.now().minus({days: 180}).toJSDate(), new Date()])
const dateTimeRange = computed(() => {
  return dateRange.value.map(it => DateTime.fromJSDate(it))
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
  <div v-else class="flex flex-col">
    <RangedDateTimePicker v-model:date-range="dateRange" class="m-2" />
    <div class="flex flex-row justify-center content-center m-2">
      <div>
        <SelectButton
            v-model="selectedRegion"
            :options="store.publicStore.regions"
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
            v-for="tournament in tournamentsFiltered"
            :key="tournament.id"
            :tournament="tournament"
        />

      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
