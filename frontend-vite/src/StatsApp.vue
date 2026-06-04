<script setup lang="ts">
import {ref, onMounted} from "vue";
import {useRouter} from "vue-router";
import {loadStats} from "@/utils/api/stats_api";
import type {StatsDEO} from "@/types/stats_types";
import TeamTournamentCountsTable from "@/components/stats/TeamTournamentCountsTable.vue";
import CardsByYearTable from "@/components/stats/CardsByYearTable.vue";
import CardsByRegionTable from "@/components/stats/CardsByRegionTable.vue";
import EloTable from "@/components/stats/EloTable.vue";
import AverageCardsPanel from "@/components/stats/AverageCardsPanel.vue";

const router = useRouter();
const stats = ref<StatsDEO | null>(null);
const isLoading = ref(true);
const errorMessage = ref<string | null>(null);

onMounted(async () => {
    try {
        stats.value = await loadStats();
    } catch (e) {
        errorMessage.value = String(e);
    } finally {
        isLoading.value = false;
    }
});
</script>

<template>
    <div class="flex flex-row justify-center">
        <div class="referee-container">
            <div class="header no-print items-center">
                <img src="./assets/logo.png" alt="Logo" class="w-12 h-12 m-2 cursor-pointer" @click="router.push('/')">
                <div class="grow font-bold text-xl">GGE Referee Report System</div>
                <div class="mr-2">
                    <a href="/public" class="text-blue-400 hover:underline">Public Reports</a>
                </div>
            </div>

            <div class="p-4">
                <h1 class="text-2xl font-bold mb-6">Statistics</h1>

                <Message v-if="errorMessage" severity="error">{{ errorMessage }}</Message>

                <div v-if="isLoading" class="flex justify-center p-8">
                    <span>Loading statistics...</span>
                </div>

                <div v-else-if="stats" class="flex flex-col gap-6">
                    <!-- Team Tournament Attendance -->
                    <Panel header="Team Tournament Attendance">
                        <p class="text-sm text-surface-400 mb-3">Teams ranked by number of tournaments attended (from submitted reports)</p>
                        <TeamTournamentCountsTable :data="stats.teamTournamentCounts" />
                    </Panel>

                    <!-- Cards Statistics -->
                    <Panel header="Card Statistics">
                        <div class="flex flex-col gap-4">
                            <AverageCardsPanel :data="stats.averageCardsPerGame" />

                            <div>
                                <h3 class="font-semibold mb-2">Cards by Year</h3>
                                <CardsByYearTable :data="stats.cardsByYear" />
                            </div>

                            <div>
                                <h3 class="font-semibold mb-2">Cards by Region</h3>
                                <CardsByRegionTable :data="stats.cardsByRegion" />
                            </div>
                        </div>
                    </Panel>

                    <!-- ELO Ratings -->
                    <Panel header="Team ELO Ratings">
                        <p class="text-sm text-surface-400 mb-3">ELO ratings calculated from all submitted game results (starting rating: 1000)</p>
                        <EloTable :data="stats.teamElos" />
                    </Panel>
                </div>
            </div>
        </div>
    </div>
</template>

<style>
@media print {
  .no-print, .no-print * {
    display: none !important;
  }
}
</style>
