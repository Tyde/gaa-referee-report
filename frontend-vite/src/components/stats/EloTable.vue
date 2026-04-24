<script setup lang="ts">
import type {TeamEloDEO} from "@/types/stats_types";

defineProps<{
    data: TeamEloDEO[];
}>();
</script>

<template>
    <DataTable :value="data" :rows="10" paginator rowHover stripedRows>
        <Column header="Rank" style="width: 4rem">
            <template #body="{ index }">{{ index + 1 }}</template>
        </Column>
        <Column field="teamName" header="Team" sortable />
        <Column field="eloScore" header="ELO Rating" sortable style="width: 8rem">
            <template #body="{ data: row }">
                {{ row.eloScore.toFixed(1) }}
            </template>
        </Column>
        <Column field="gamesPlayed" header="Games Played" sortable style="width: 8rem" />
    </DataTable>
</template>
