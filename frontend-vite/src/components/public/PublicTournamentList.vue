<script setup lang="ts">
import {usePublicStore} from "@/utils/public_store";
import {RegionDEO} from "@/types/tournament_types";
import {computed, ref} from "vue";
import {useRouter} from "vue-router";

const store = usePublicStore();
function regionIDToRegion(regionID: number): RegionDEO {
  return store.regions.filter(it => it.id == regionID)[0]
}


const tournamentsSortedByDate = computed(() => {
  return store.tournaments.sort((a, b) => {
    return a.date.diff(b.date).milliseconds < 0 ? 1 : -1
  }).filter(tournament => {
    if (selectedRegion.value) {
      return tournament.region == selectedRegion.value?.id
    } else {
      return true
    }
  })
})

const router = useRouter()
function navigateToReport(reportID: number) {
  let id = reportID.toString()
  router.push({path: `/tournament/${id}`})
}

const selectedRegion = ref<RegionDEO | undefined>(undefined)
</script>

<template>
  <div class="flex-col w-full flex items-center">
<div class="flex flex-col w-full lg:w-7/12">
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
  <div
      v-for="tournament in tournamentsSortedByDate"
      key="tournament.id"
      class="single-tournament-row"
      @click="navigateToReport(tournament.id)"
  >
    <div class="flex flex-row">
      <div class="grow">{{ tournament.name }}</div>
      <div>{{ tournament.location }}</div>
    </div>
    <div class="flex flex-row">
      <div class="grow">{{ tournament.date.toISODate() }}</div>
      <div>{{ regionIDToRegion(tournament.region).name }}</div>
    </div>
  </div>

</div>
  </div>
</template>

<style scoped>
.single-tournament-row {
  @apply flex flex-col;
  @apply p-2 m-2;
  @apply rounded-lg bg-gray-100;
  @apply hover:bg-gray-200;
  @apply cursor-pointer;
}


</style>