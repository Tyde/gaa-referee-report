<script setup lang="ts">

import {computed, ref, watch} from "vue";
import {getTeamHistory} from "@/utils/api/teams_api";
import type {Team, TeamHistoryEventDEO} from "@/types/team_types";
import {useAdminStore} from "@/utils/admin_store";
import {DateTime} from "luxon";

const store = useAdminStore()

const props = defineProps<{
  visible: boolean,
  selectedTeam: Team,
}>()

const emits = defineEmits<{
  (e: 'update:visible', value: boolean): void,
}>()

const localVisible = computed({
  get() {
    return props.visible
  },
  set(newValue) {
    emits('update:visible', newValue)
  }
})

const events = ref<TeamHistoryEventDEO[]>([])
const loading = ref(false)

watch(() => props.visible, (newValue) => {
  if (newValue && props.selectedTeam) {
    loading.value = true
    getTeamHistory(props.selectedTeam.id)
        .then((evts) => {
          events.value = evts
        })
        .catch((reason) => store.newError(reason))
        .finally(() => {
          loading.value = false
        })
  }
})

function eventDescription(event: TeamHistoryEventDEO): string {
  switch (event.changeType) {
    case "CREATED":
      return `Team was created`
    case "RENAMED":
      return `Renamed from "${event.oldValue}" to "${event.newValue}"`
    case "CONVERTED_TO_AMALGAMATION":
      return `Converted to an amalgamation`
    case "CONVERTED_TO_TEAM":
      return `Converted to a regular team`
    case "MEMBER_ADDED":
      return `"${event.newValue}" joined this amalgamation`
    case "MEMBER_REMOVED":
      return `"${event.oldValue}" left this amalgamation`
    case "MERGED_INTO":
      return `Merged into "${event.newValue}"`
    default:
      return event.changeType
  }
}

function formatDate(iso: string): string {
  const dt = DateTime.fromISO(iso)
  return dt.isValid ? dt.toFormat("dd.MM.yyyy") : iso
}

function formatRecordedAt(iso: string): string {
  const dt = DateTime.fromISO(iso)
  return dt.isValid ? dt.toFormat("dd.MM.yyyy HH:mm") : iso
}

</script>

<template>
  <Dialog
      v-model:visible="localVisible"
      :closable="true"
      :close-on-escape="true"
      header="Team History"
      :modal="true"
      :pt="{ root: { class: 'history-diag'}}"
  >
    <div class="flex flex-col">
      <div class="text-lg font-bold m-2">{{ selectedTeam?.name }}</div>
      <div v-if="loading" class="m-2">Loading history...</div>
      <div v-else-if="events.length === 0" class="m-2">
        No history recorded for this team.
      </div>
      <ol v-else class="m-2 border-l border-surface-300">
        <li v-for="(event, index) in [...events].reverse()" :key="index" class="relative ml-4 pb-4">
          <span class="absolute -left-[10px] top-1 h-4 w-4 rounded-full bg-primary"></span>
          <div class="text-sm font-semibold">{{ eventDescription(event) }}</div>
          <div class="text-xs text-surface-500">
            {{ formatDate(event.changeDate) }}
            <template v-if="event.changeDate !== event.recordedAt.slice(0, 10)">
              · recorded {{ formatRecordedAt(event.recordedAt) }}
            </template>
          </div>
        </li>
      </ol>
    </div>
  </Dialog>
</template>

<style scoped>
.history-diag {
  @apply w-1/2;
}
</style>
