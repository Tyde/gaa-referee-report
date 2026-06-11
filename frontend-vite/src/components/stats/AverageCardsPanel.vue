<script setup lang="ts">
import type {AverageCardsPerGameDEO} from "@/types/stats_types";

defineProps<{
    data: AverageCardsPerGameDEO;
}>();
</script>

<template>
    <div class="flex flex-row gap-4 flex-wrap mb-4">
        <div class="stat-card">
            <div class="stat-label">Total Games</div>
            <div class="stat-value">{{ data.totalGames }}</div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Avg Yellow Cards / Game</div>
            <div class="stat-value">{{ data.averageCautions.toFixed(2) }}</div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Avg Black Cards / Game</div>
            <div class="stat-value">{{ data.averageBlackCards.toFixed(2) }}</div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Avg Red Cards / Game</div>
            <div class="stat-value">{{ data.averageRedCards.toFixed(2) }}</div>
        </div>
    </div>
</template>

<style scoped>
.stat-card {
    @apply flex flex-col items-center p-4 rounded-lg bg-surface-700 border border-surface-600 min-w-32;
}
.stat-label {
    @apply text-sm text-surface-400 text-center;
}
.stat-value {
    @apply text-2xl font-bold mt-1;
}
</style>
