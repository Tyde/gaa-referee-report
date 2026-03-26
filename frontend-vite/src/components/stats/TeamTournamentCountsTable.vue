<script setup lang="ts">
import type {TeamTournamentCountDEO} from "@/types/stats_types";

defineProps<{
    data: TeamTournamentCountDEO[];
}>();
</script>

<template>
    <DataTable :value="data" :rows="10" paginator rowHover stripedRows>
        <Column header="Rank" style="width: 4rem">
            <template #body="{ index }">{{ index + 1 }}</template>
        </Column>
        <Column field="teamName" header="Team" sortable />
        <Column field="tournamentCount" header="Tournaments" sortable style="width: 8rem" />
    </DataTable>
</template>
