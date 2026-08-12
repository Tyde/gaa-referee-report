<script lang="ts" setup>
import {computed, ref, watch} from "vue";
import {useRouter} from "vue-router";
import {DateTime} from "luxon";
import {useAdminStore} from "@/utils/admin_store";
import {getTeamGames} from "@/utils/api/teams_api";
import type {TeamGamesDEO} from "@/types/team_types";
import {formatGaaScore, groupAndSortTeamGames, playedAsOutcome} from "@/utils/team_games";

const props = defineProps<{
  teamId: string
}>();

const router = useRouter();
const store = useAdminStore();
const teamGames = ref<TeamGamesDEO>();
const includeAmalgamatedTeams = ref(false);
const isLoading = ref(false);
const hasRequestError = ref(false);
let latestRequest = 0;

const tournamentGroups = computed(() => groupAndSortTeamGames(teamGames.value?.games ?? []));
const gameCount = computed(() => teamGames.value?.games.length ?? 0);
const tournamentCount = computed(() => tournamentGroups.value.length);
const selectedTeam = computed(() => teamGames.value?.selectedTeam);
const canShowAmalgamatedTeams = computed(() => selectedTeam.value !== undefined && !selectedTeam.value.isAmalgamation);

function formatTournamentDates(tournament: TeamGamesDEO["games"][number]["tournament"]): string {
  const startDate = tournament.date.toLocaleString(DateTime.DATE_MED);
  if (tournament.isLeague && tournament.endDate) {
    return `${startDate} – ${tournament.endDate.toLocaleString(DateTime.DATE_MED)}`;
  }
  return startDate;
}

function formatGameStartTime(startTime: TeamGamesDEO["games"][number]["startTime"]): string | undefined {
  return startTime?.toLocaleString(DateTime.DATETIME_MED) ?? undefined;
}

async function loadGames() {
  const request = ++latestRequest;
  isLoading.value = true;
  hasRequestError.value = false;

  try {
    const result = await getTeamGames(Number(props.teamId), includeAmalgamatedTeams.value);
    if (request === latestRequest) {
      teamGames.value = result;
    }
  } catch (error) {
    if (request === latestRequest) {
      hasRequestError.value = true;
      store.newError(String(error));
    }
  } finally {
    if (request === latestRequest) {
      isLoading.value = false;
    }
  }
}

function openCompleteTournamentReport(tournamentId: number) {
  router.push({path: `/tournament-reports/complete/${tournamentId}`});
}

watch(() => props.teamId, () => {
  if (includeAmalgamatedTeams.value) {
    includeAmalgamatedTeams.value = false;
  } else {
    loadGames();
  }
}, {immediate: true});

watch(includeAmalgamatedTeams, loadGames);
</script>

<template>
  <div class="mx-auto flex w-full max-w-5xl flex-col gap-5 p-4">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <Button text icon="pi pi-arrow-left" label="Back to teams" @click="router.push({path: '/teams'})"/>
      <div class="flex items-center gap-5 text-sm text-surface-600 dark:text-surface-300">
        <span>{{ gameCount }} {{ gameCount === 1 ? 'game' : 'games' }}</span>
        <span>{{ tournamentCount }} {{ tournamentCount === 1 ? 'tournament' : 'tournaments' }}</span>
      </div>
    </div>

    <div class="flex flex-col gap-3 rounded-lg bg-surface-100 p-4 dark:bg-surface-800">
      <div>
        <h1 class="text-2xl font-bold">Games for {{ selectedTeam?.name ?? 'team' }}</h1>
        <p v-if="selectedTeam" class="text-sm text-surface-600 dark:text-surface-300">
          Submitted games and tournaments where this team played.
        </p>
      </div>
      <label v-if="canShowAmalgamatedTeams" class="flex w-fit items-center gap-2">
        <Checkbox v-model="includeAmalgamatedTeams" binary :disabled="isLoading"/>
        <span>Also show amalgamated teams</span>
      </label>
    </div>

    <div v-if="isLoading" class="flex items-center justify-center gap-3 rounded-lg border border-surface-200 p-10 dark:border-surface-700">
      <i class="pi pi-spin pi-spinner text-2xl"/>
      <span>Loading games…</span>
    </div>

    <div v-else-if="hasRequestError" class="rounded-lg border border-red-300 bg-red-50 p-4 text-red-800 dark:border-red-700 dark:bg-red-950 dark:text-red-200">
      Unable to load this team's games. Please try again.
    </div>

    <div v-else-if="teamGames && gameCount === 0" class="rounded-lg border border-surface-200 p-6 text-center dark:border-surface-700">
      No submitted games have been recorded for this team.
    </div>

    <div v-else-if="teamGames" class="flex flex-col gap-5">
      <section v-for="group in tournamentGroups" :key="group.tournament.id" class="overflow-hidden rounded-lg border border-surface-200 dark:border-surface-700">
        <header class="flex flex-wrap items-center justify-between gap-3 bg-surface-100 p-4 dark:bg-surface-800">
          <div>
            <h2 class="text-xl font-semibold">{{ group.tournament.name }}</h2>
            <p class="text-sm text-surface-600 dark:text-surface-300">
              {{ formatTournamentDates(group.tournament) }} · {{ group.tournament.location }}
            </p>
          </div>
          <Button text label="Full tournament report" icon="pi pi-external-link" @click="openCompleteTournamentReport(group.tournament.id)"/>
        </header>

        <div class="divide-y divide-surface-200 dark:divide-surface-700">
          <article v-for="game in group.games" :key="game.gameId" class="grid gap-3 p-4 md:grid-cols-[minmax(9rem,auto)_minmax(0,1fr)_minmax(0,1fr)_auto] md:items-center">
            <div class="text-sm text-surface-600 dark:text-surface-300">
              <div v-if="formatGameStartTime(game.startTime)">{{ formatGameStartTime(game.startTime) }}</div>
              <div v-else>No start time recorded</div>
              <div>{{ game.codeName }}<span v-if="game.gameTypeName"> · {{ game.gameTypeName }}</span></div>
            </div>

            <div>
              <div class="text-xs font-medium uppercase text-surface-500">Played as</div>
              <div class="font-semibold">{{ game.playedAsTeam.name }}</div>
              <div class="text-lg">{{ formatGaaScore(game.playedAsGoals, game.playedAsPoints) }}</div>
            </div>

            <div>
              <div class="text-xs font-medium uppercase text-surface-500">Opponent</div>
              <div class="font-semibold">{{ game.opponentTeam.name }}</div>
              <div class="text-lg">{{ formatGaaScore(game.opponentGoals, game.opponentPoints) }}</div>
            </div>

            <div class="flex flex-col items-start gap-1 md:items-end">
              <Tag :value="playedAsOutcome(game)" :severity="playedAsOutcome(game) === 'W' ? 'success' : playedAsOutcome(game) === 'L' ? 'danger' : 'secondary'"/>
              <span class="text-sm text-surface-600 dark:text-surface-300">Referee: {{ game.refereeName }}</span>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>
