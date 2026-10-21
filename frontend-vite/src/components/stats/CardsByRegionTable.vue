<script setup lang="ts">
import type {CardStatsByRegionDEO} from "@/types/stats_types";

defineProps<{
    data: CardStatsByRegionDEO[];
}>();
</script>

<template>
    <DataTable :value="data" rowHover stripedRows>
        <Column field="regionName" header="Region" sortable />
        <Column field="cautionCount" header="Yellow Cards" sortable />
        <Column field="blackCardCount" header="Black Cards" sortable />
        <Column field="redCardCount" header="Red Cards" sortable />
        <Column field="totalGames" header="Total Games" sortable />
        <Column header="Cards/Game" sortable>
            <template #body="{ data: row }">
                {{ row.totalGames > 0 ? ((row.cautionCount + row.blackCardCount + row.redCardCount) / row.totalGames).toFixed(2) : '-' }}
            </template>
        </Column>
    </DataTable>
</template>
