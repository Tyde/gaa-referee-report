<script setup lang="ts">
import type {TeamTournamentCountDEO} from "@/types/stats_types";
import { ref } from "vue";

defineProps<{
    data: TeamTournamentCountDEO[];
}>();

const first = ref(0);
</script>

<template>
    <DataTable :value="data" :rows="25" paginator rowHover stripedRows v-model:first="first">
        <Column header="Rank" style="width: 4rem">
            <template #body="{ index }">{{ first + index + 1 }}</template>
        </Column>
        <Column field="teamName" header="Team" sortable />
        <Column field="tournamentCount" header="Tournaments" sortable style="width: 8rem" />
    </DataTable>
</template>
