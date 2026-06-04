<script setup lang="ts">
import type {TeamEloDEO} from "@/types/stats_types";
import { ref } from "vue";

defineProps<{
    data: TeamEloDEO[];
}>();

const first = ref(0);
</script>

<template>
    <DataTable :value="data" :rows="25" paginator rowHover stripedRows v-model:first="first">
        <Column header="Rank" style="width: 4rem">
            <template #body="{ index }">{{ first + index + 1 }}</template>
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
