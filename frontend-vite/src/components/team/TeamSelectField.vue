<script lang="ts" setup>
import type {PropType} from 'vue';
import {computed, onMounted, ref} from "vue";
import CreateTeam from "@/components/team/CreateTeam.vue";
import SmartCreateAmalgamation from "@/components/team/SmartCreateAmalgamation.vue";
import {useReportStore} from "@/utils/edit_report_store";
import type {Team} from "@/types/team_types";
import CreateSplitTeam from "@/components/team/CreateSplitTeam.vue";
import {searchTeams, type TeamSearchResult} from "@/utils/team_search";

const store = useReportStore()


const props = defineProps({
  /**
   * Show the option to create a new amalgamation
   */
  show_new_amalgamate: Boolean,
  /**
   * Show the option to create a new team
   */
  show_add_new_team: Boolean,
  /**
   * Teams included in this list will not appear in the select list
   */
  exclude_team_list: Array as PropType<Team[] | undefined>,
  /**
   * The exclude team list will only be excluded when this is set to true
   */
  force_hide_exclude_team_list: Boolean,
  /**
   * Allows the option to deselect selected teams
   */
  allow_unselect: Boolean as PropType<boolean | undefined>,
  /**
   * Allow the option to split a team that is selected
   */
  show_hide_squad_box: Boolean as PropType<boolean | undefined>,

  /**
   * Teams that are not shown in the list, even if the unselect option is enabled
   */
  forcefully_hidden_teams: Array as PropType<Team[] | undefined>,
  /**
   * Show teams that are not amalgamations
   */
  show_teams: {
    type: Boolean,
    default: true,
    required: false
  },
  /**
   * Show amalgamations (this does not include squads)
   */
  show_amalgamations: {
    type: Boolean,
    default: true,
    required: false
  },
  /**
   * Show amalgamations nested under their member teams instead of as
   * flat top-level entries. Requires show_amalgamations.
   */
  nest_amalgamations: {
    type: Boolean,
    default: true,
    required: false
  },
  /**
   * Show squads (this does not include amalgamations).
   * Will be overriden by the hide_squad_checkbox" value, if it is shown
   */
  show_squads: {
    type: Boolean,
    default: true,
    required: false
  }
});

const emit = defineEmits<{
  (e: 'team_selected', team: Team): void
  (e: 'team_unselected', team: Team): void
}>()

const searchTerm = ref("")
const showSelect = ref(true)
const showNewTeam = ref(false)
const showNewAmalgamation = ref(false)
const newSquadSelectedTeam = ref<Team | undefined>(undefined)
//const teamsAvailable = ref(<Team[]>[])
const isLoading = ref(false)
const hide_squads = ref<boolean | undefined>(false)

const expandedTeamIds = ref(new Set<number>())
const collapsedTeamIds = ref(new Set<number>())

interface TeamSelectResult extends TeamSearchResult {
  /**
   * Amalgamations of this team matching the current search term
   * (all of its amalgamations when the search term is empty).
   */
  matchingAmalgamations: TeamSearchResult[],
  /**
   * True when the team itself does not match the search term and it only
   * appears in the list because one of its amalgamations matches.
   */
  matchedOnlyViaAmalgamation: boolean
}

function isMultiMemberAmalgamation(team: Team): boolean {
  return !!team.isAmalgamation && !!team.amalgamationTeams && team.amalgamationTeams.length > 1
}

const nestAmalgamations = computed(() => props.nest_amalgamations && props.show_amalgamations)

const amalgamationsByBaseTeamId = computed(() => {
  const map = new Map<number, Team[]>()
  if (!nestAmalgamations.value) {
    return map
  }
  for (const team of store.publicStore.teams) {
    if (isMultiMemberAmalgamation(team)) {
      for (const member of team.amalgamationTeams!) {
        const list = map.get(member.id) ?? []
        list.push(team)
        map.set(member.id, list)
      }
    }
  }
  for (const list of map.values()) {
    list.sort((a, b) => a.name.localeCompare(b.name))
  }
  return map
})

/*
watch(() => props.show_hide_squad_box, (value, oldValue, onCleanup) => {
  console.log("Changed props value from ",oldValue," to ",value)
  hide_squads.value = value
}, {immediate: true})*/

function on_team_click(team: Team) {
  if (!thisTeamInExludedList(team)) {
    emit("team_selected", team)
    searchTerm.value = ""
  }
}

function on_add_team_click() {
  showSelect.value = false
  showNewTeam.value = true
}

function new_team_created(team: Team) {
  emit("team_selected", team)
  fetch_available_teams()
  showSelect.value = true
  showNewTeam.value = false
  showNewAmalgamation.value = false
  searchTerm.value = ""
}

function on_start_amalgamation_click() {
  showSelect.value = false
  showNewAmalgamation.value = true
}


async function fetch_available_teams() {
  isLoading.value = true
  store.publicStore.loadTeams()
      .finally(() => isLoading.value = false)

}

onMounted(() => {

  fetch_available_teams()
})

function thisTeamInExludedList(team: Team): boolean {
  return !!props.exclude_team_list?.find(t => t.id === team.id)
}

function classForTeam(team: Team): string {
  return thisTeamInExludedList(team) ? "already-selected-item" : "p-listbox-item"
}

const nestedSearchResults = computed(() => {
  const map = new Map<number, TeamSearchResult[]>()
  for (const [baseTeamId, amalgamations] of amalgamationsByBaseTeamId.value) {
    map.set(baseTeamId, searchTeams(amalgamations, searchTerm.value))
  }
  return map
})

const filtered_list = computed<TeamSelectResult[]>(() => {
  let preparedlist = store.publicStore.teams.toSorted((a, b) => {
    return a.name.localeCompare(b.name)
  })
  if ((props.show_hide_squad_box && hide_squads.value) || (!props.show_hide_squad_box && !props.show_squads)) {
    preparedlist = preparedlist.filter(value => {
      if (value.isAmalgamation && value.amalgamationTeams && value.amalgamationTeams.length == 1) {
        return false
      }
      return true
    })
  }
  if (!props.show_teams) {
    preparedlist = preparedlist.filter(value => {
      return value.isAmalgamation
    })
  }
  if (nestAmalgamations.value) {
    preparedlist = preparedlist.filter(value => !isMultiMemberAmalgamation(value))
  } else if (!props.show_amalgamations) {
    preparedlist = preparedlist.filter(value => !isMultiMemberAmalgamation(value))
  }

  if (props.forcefully_hidden_teams) {
    preparedlist = preparedlist.filter(value => {
      return !props.forcefully_hidden_teams?.find(t => t.id === value.id)
    })
  }
  if (props.force_hide_exclude_team_list && props.exclude_team_list !== undefined) {

    const excludelist = props.exclude_team_list
    //console.log("Checking excludes")
    preparedlist = preparedlist.filter(value => {
      return excludelist.findIndex(exclude_val => {
        return exclude_val.id == value.id
      }) == -1
    })
  }

  const results = new Map<number, TeamSelectResult>()
  for (const result of searchTeams(preparedlist, searchTerm.value)) {
    results.set(result.team.id, {
      ...result,
      matchingAmalgamations: nestAmalgamations.value
          ? nestedSearchResults.value.get(result.team.id) ?? []
          : [],
      matchedOnlyViaAmalgamation: false
    })
  }

  // Surface base teams that do not match themselves but have a matching amalgamation
  if (nestAmalgamations.value && searchTerm.value.length > 0) {
    for (const [baseTeamId, nested] of nestedSearchResults.value) {
      if (nested.length === 0 || results.has(baseTeamId)) {
        continue
      }
      const baseTeam = preparedlist.find(t => t.id === baseTeamId)
      if (baseTeam) {
        results.set(baseTeamId, {
          team: baseTeam,
          score: nested[0].score,
          matchingAmalgamations: nested,
          matchedOnlyViaAmalgamation: true
        })
      }
    }
  }

  return [...results.values()]
      .sort((a, b) => b.score - a.score || a.team.name.localeCompare(b.team.name))
})


function isRowExpanded(result: TeamSelectResult): boolean {
  if (result.matchedOnlyViaAmalgamation && searchTerm.value.length > 0) {
    return !collapsedTeamIds.value.has(result.team.id)
  }
  return expandedTeamIds.value.has(result.team.id)
}

function setRowExpanded(result: TeamSelectResult, expanded: boolean) {
  const expandedSet = new Set(expandedTeamIds.value)
  const collapsedSet = new Set(collapsedTeamIds.value)
  expandedSet.delete(result.team.id)
  collapsedSet.delete(result.team.id)
  if (result.matchedOnlyViaAmalgamation && searchTerm.value.length > 0) {
    if (!expanded) {
      collapsedSet.add(result.team.id)
    }
  } else if (expanded) {
    expandedSet.add(result.team.id)
  }
  expandedTeamIds.value = expandedSet
  collapsedTeamIds.value = collapsedSet
}


function unselect_team(team: Team) {
  emit("team_unselected", team)
}

function createSquad(team: Team) {
  showSelect.value = false
  newSquadSelectedTeam.value = team
}

function onSquadSuccessful(teams: Team[]) {
  fetch_available_teams().then(
      () => {
        isLoading.value = false
        showSelect.value = true
        newSquadSelectedTeam.value = undefined
        expandedTeamIds.value = new Set()
        for (const team of teams) {
          emit("team_selected", team)
        }
      }
  )
}

function onSquadCancel() {
  showSelect.value = true
  newSquadSelectedTeam.value = undefined
  expandedTeamIds.value = new Set()
}


</script>

<template>

  <div>
    <h2 v-if="showSelect">Select teams:</h2>
    <template v-if="showSelect">

      <div class="flex flex-row justify-evenly items-stretch">
        <div class="md:w-32 grow"></div>
        <div class="self-center w-min flex flex-col">
          <label for="search_term">{{ $t('teamSelect.enterNameForSearch') }}</label>
          <InputText id="search_term" v-model="searchTerm"/>
        </div>


        <label v-if="show_hide_squad_box" for="hide_squads" class="flex justify-center items-center md:w-32 grow">
          <Checkbox
              v-model="hide_squads"
              input-id="hide_squads"
              binary
              class="mr-2"
          />
          Hide squads (.. A, .. B)</label>
        <div v-else class="md:w-32 grow"></div>

      </div>
      <div class="listbox team-selector-box">
        <ul class="p-listbox-list">
          <li
              v-if="props.show_add_new_team "
              class="p-listbox-item teamselect-add-option"
              @click="on_add_team_click()"
          >
            {{ $t('teamSelect.addNewTeam') }} <span v-if="searchTerm.length>0">"{{ searchTerm }}"</span> ...
          </li>
          <li
              v-if="props.show_new_amalgamate"
              class="teamselect-add-option p-listbox-item"
              @click="on_start_amalgamation_click()"
          >
            {{ $t('teamSelect.addNewAmalgamation') }}
            <span v-if="searchTerm.length>0">"{{ searchTerm }}"</span>
            ...
          </li>
          <li
              v-if="isLoading && filtered_list.length === 0"
              class="p-listbox-item"
          >
            <i
                class="pi pi-spin pi-spinner"
                style="font-size: 2rem"></i>
          </li>
          <li
              v-for="srt in filtered_list"
              :key="srt.team.id"
              :class="[{
                  amalgamation_item: srt.team.isAmalgamation &&
                                      srt.team.amalgamationTeams &&
                                      srt.team.amalgamationTeams.length > 1,
                  squad_item: srt.team.isAmalgamation &&
                                      srt.team.amalgamationTeams &&
                                      srt.team.amalgamationTeams.length == 1
                },classForTeam(srt.team)]"
              @click="on_team_click(srt.team)"
              class="group"
          >
            <template v-if="srt.team.isAmalgamation">
              {{ srt.team.name }} -
              <p v-if="srt.matchedAlias" class="matched-alias-subtitle">
                {{ $t('teamSelect.matchedAlias', {alias: srt.matchedAlias}) }}
              </p>
              <template v-if="srt.team.amalgamationTeams!!.length > 1">Amalgamation</template>
              <template v-else>Squad</template>
              <p v-if="thisTeamInExludedList(srt.team)" class="already-selected-subtitle">
                {{ $t('teamSelect.alreadyInSelection') }}
              </p>
              <!-- unselect icon -->
              <p v-if="srt.team.amalgamationTeams" class="amalgamation_subtitle">
                {{ srt.team.amalgamationTeams.map(value => value.name).join(" - ") }}
              </p>

            </template>
            <div class="flex flex-col" v-else>
              <div class="flex flex-row justify-between items-center">
                <div>{{ srt.team.name }}</div>
                <div class="flex flex-row justify-end items-center">
                  <p v-if="thisTeamInExludedList(srt.team)" class="already-selected-subtitle">
                    {{ $t('teamSelect.alreadyInSelection') }} <i
                      v-if="thisTeamInExludedList(srt.team) && props.allow_unselect"
                      class="pi pi-times hover:cursor-pointer mr-2"
                      @click.stop="emit('team_unselected', srt.team)"/>
                  </p>
                  <template v-if="srt.matchingAmalgamations.length === 0">
                    <Button v-if="!isRowExpanded(srt)" unstyled
                            class="hidden group-hover:block touch:block "
                            pt:root="touch:border touch:rounded aspect-square"
                            @click.stop="setRowExpanded(srt, true)">
                      <vue-feather type="chevron-down" class="h-4 m-1 touch:h-6 leading-none"></vue-feather>
                    </Button>
                    <Button v-if="isRowExpanded(srt)"
                            unstyled
                            pt:root="touch:border touch:rounded aspect-square"
                            @click.stop="setRowExpanded(srt, false)">
                      <vue-feather type="chevron-up" class="h-4 m-1 touch:h-6 leading-none"></vue-feather>
                    </Button>
                  </template>
                </div>
              </div>
              <div v-if="srt.matchedAlias" class="matched-alias-subtitle">
                {{ $t('teamSelect.matchedAlias', {alias: srt.matchedAlias}) }}
              </div>
              <div v-else-if="srt.matchedOnlyViaAmalgamation" class="matched-alias-subtitle">
                {{ $t('teamSelect.matchingAmalgamation', {count: srt.matchingAmalgamations.length}) }}
              </div>
              <div
                  v-if="srt.matchingAmalgamations.length > 0"
                  class="amalgamation-toggle-row"
                  @click.stop="setRowExpanded(srt, !isRowExpanded(srt))"
              >
                <vue-feather
                    :type="isRowExpanded(srt) ? 'chevron-down' : 'chevron-right'"
                    class="h-4 w-4"
                ></vue-feather>
                <span>{{ srt.matchingAmalgamations.length }} Amalgamations</span>
              </div>
              <!-- grid 0fr/1fr trick: animates the real content height, so
                   collapsing short lists does not wait through empty space -->
              <div
                  class="grid transition-all duration-300 ease-in-out"
                  :class="isRowExpanded(srt)
                      ? 'grid-rows-[1fr] opacity-100'
                      : 'grid-rows-[0fr] opacity-0'"
              >
                <div class="overflow-hidden pt-2 w-full flex flex-col">
                  <ul v-if="srt.matchingAmalgamations.length > 0" class="amalgamation-sublist">
                    <li
                        v-for="sub in srt.matchingAmalgamations"
                        :key="sub.team.id"
                        :class="classForTeam(sub.team)"
                        @click.stop="on_team_click(sub.team)"
                    >
                      <div class="flex flex-row justify-between items-center">
                        <div>{{ sub.team.name }}</div>
                        <p v-if="thisTeamInExludedList(sub.team)" class="already-selected-subtitle">
                          {{ $t('teamSelect.alreadyInSelection') }}
                        </p>
                      </div>
                      <div v-if="sub.matchedAlias" class="matched-alias-subtitle">
                        {{ $t('teamSelect.matchedAlias', {alias: sub.matchedAlias}) }}
                      </div>
                      <p v-if="sub.team.amalgamationTeams" class="amalgamation_subtitle">
                        {{ sub.team.amalgamationTeams.map(value => value.name).join(" - ") }}
                      </p>
                    </li>
                  </ul>
                  <div class="w-full flex flex-row justify-between">
                    <div class="grow"></div>
                    <div>
                      <Button
                          unstyled
                          pt:root="bg-primary-600 p-2 rounded-lg hover:bg-primary-500 m-1"
                          pt:label="text-primary-contrast"
                          @click.stop="createSquad(srt.team)"
                      >
                        Create Squad
                      </Button>
                    </div>
                  </div>
                </div>
              </div>

            </div>
          </li>
        </ul>
      </div>
    </template>

    <template v-if="showNewTeam">
      <CreateTeam
          :rough_team_name="searchTerm"
          @on_new_team="new_team_created"
          @on_cancel="showNewTeam = false; showSelect = true"
      />
    </template>
    <template v-if="showNewAmalgamation">
      <SmartCreateAmalgamation
          :rough_amalgamation_name="searchTerm"
          @on_cancel="showNewAmalgamation = false; showSelect = true"
          @on_new_amalgamation="new_team_created"
      />
    </template>
    <template v-if="newSquadSelectedTeam != undefined">

      <CreateSplitTeam
          :base-team="newSquadSelectedTeam"
          @on_new_team_split="onSquadSuccessful"
          @on_cancel="onSquadCancel"
      />

    </template>

  </div>
</template>

<style scoped>


.listbox {
  @apply m-2;
  @apply border border-surface-400 rounded;
}

.listbox li {
  /* pr-6 keeps row content clear of the container's overlay scrollbar */
  @apply pt-3 pb-3 pl-2 pr-6;
  @apply hover:bg-surface-600 hover:cursor-pointer;
}

.team-selector-box {
  @apply h-96 overflow-y-scroll;
}

.amalgamation_item {
  @apply bg-primary text-primary-contrast;
  @apply hover:!bg-primary-600
}

.squad_item {
  @apply bg-surface-500
}

.amalgamation_subtitle {
  font-size: smaller;
  margin-top: 4px;
  margin-bottom: 0;
  @apply mr-2;
}

.amalgamation-toggle-row {
  @apply flex flex-row items-center;
  @apply mt-1 pt-1 pb-1 pl-2 pr-2 rounded;
  @apply bg-surface-500 hover:bg-surface-400;
  @apply text-sm;
  @apply cursor-pointer select-none;
}

.amalgamation-sublist {
  @apply border border-surface-400 rounded;
  @apply overflow-y-auto max-h-48;
  margin: 0;
  padding: 0;
  list-style: none;
}

.amalgamation-sublist li {
  @apply pt-2 pb-2 pl-2 pr-6;
  @apply hover:bg-surface-600 hover:cursor-pointer;
  @apply border-b border-surface-400 last:border-b-0;
}

.matched-alias-subtitle {
  @apply text-sm italic opacity-80;
}

.already-selected-subtitle {
  @apply text-sm;
  @apply mt-1;
}

.teamselect-add-option {
  font-style: italic;
  font-weight: bold;
  color: darkcyan !important;
}

.already-selected-item {
  margin: 0;
  padding: .75rem 1rem;
  border: 0 none;
  @apply text-gray-600;
  transition: none;
  border-radius: 0;
  @apply bg-gray-200
}
</style>
