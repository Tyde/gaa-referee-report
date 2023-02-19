<script lang="ts" setup>
import {computed, onMounted, ref} from "vue";
import CreateTeam from "@/components/team/CreateTeam.vue";
import CreateAmalgamation from "@/components/team/CreateAmalgamation.vue";
import {allTeams} from "@/utils/api/teams_api";
import {useReportStore} from "@/utils/edit_report_store";
import type {Team} from "@/types/team_types";

interface SearchResultTeam {
  team: Team,
  search_score: number
}

const store = useReportStore()
const props = defineProps<{
  show_amalgamate: Boolean
  show_add_new_team: Boolean
  exclude_team_list?: Team[]
  force_hide_exclude_team_list: Boolean
  allow_unselect?: Boolean
}>()


const emit = defineEmits<{
  (e: 'team_selected', team: Team): void
  (e: 'team_unselected', team: Team): void
}>()

const searchTerm = ref("")
const showSelect = ref(true)
const showNewTeam = ref(false)
const showNewAmalgamation = ref(false)
const teamsAvailable = ref(<Team[]>[])
const isLoading = ref(false)

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
  try {
    teamsAvailable.value = await allTeams()
        .catch((error) => {
          store.newError(error)
          return []
        })
  } catch (e) {
    console.error(e)
  } finally {
    isLoading.value = false
  }
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

const filtered_list = computed(() => {
  let preparedlist = teamsAvailable.value.sort((a, b) => {
    return a.name.localeCompare(b.name)
  })
  if (props.force_hide_exclude_team_list && props.exclude_team_list !== undefined) {

    let excludelist = props.exclude_team_list
    //console.log("Checking excludes")
    preparedlist = preparedlist.filter(value => {
      return excludelist.findIndex(exclude_val => {
        return exclude_val.id == value.id
      }) == -1
    })
  }
  if (searchTerm.value) {
    return preparedlist.filter(value => {
      let isInName = value.name.toLowerCase().search(searchTerm.value.toLowerCase()) != -1
      let isInAmalgamations = value.amalgamationTeams?.reduce(function (pv, cv) {
        let isInLocalName = cv.name.toLowerCase().search(searchTerm.value.toLowerCase()) != -1
        return pv || isInLocalName
      }, false)
      return isInName || isInAmalgamations
    }).map(value => {
      return {
        team: value,
        search_score: 1
      } as SearchResultTeam
    })
  } else {
    return preparedlist.map(value => {
      return {team: value, search_score: 1} as SearchResultTeam
    })
  }
})


function unselect_team(team: Team) {
  emit("team_unselected", team)
}


</script>

<template>

  <div>
    <h2 v-if="showSelect">Select teams:</h2>
    <template v-if="showSelect">

      <div class="mx-auto w-min">
        <span class="p-float-label">
              <InputText id="search_term" v-model="searchTerm"/>
              <label for="search_term">Enter team name to search</label>
            </span>
      </div>
      <div class="listbox team-selector-box">
        <ul class="p-listbox-list">
          <li
              v-if="props.show_add_new_team "
              class="p-listbox-item teamselect-add-option"
              @click="on_add_team_click()"
          >
            Add new team <span v-if="searchTerm.length>0">"{{ searchTerm }}"</span> ...
          </li>
          <li
              v-if="props.show_amalgamate"
              class="teamselect-add-option p-listbox-item"
              @click="on_start_amalgamation_click()"
          >
            Add new amalgamation
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
              :key="srt.search_score"
              :class="[{
                  amalgamation_item: srt.team.isAmalgamation,
                },classForTeam(srt.team)]"
              @click="on_team_click(srt.team)"
          >
            <template v-if="srt.team.isAmalgamation">
              {{ srt.team.name }} - Amalgamation
              <p v-if="thisTeamInExludedList(srt.team)" class="already-selected-subtitle">
                Already in selection
              </p>
              <!-- unselect icon -->
              <p v-if="srt.team.amalgamationTeams" class="amalgamation_subtitle">
                {{ srt.team.amalgamationTeams.map(value => value.name).join(" - ") }}
              </p>

            </template>
            <template v-else>
              {{ srt.team.name }}
              <p v-if="thisTeamInExludedList(srt.team)" class="already-selected-subtitle">
                Already in selection <i
                  v-if="thisTeamInExludedList(srt.team) && props.allow_unselect"
                  class="pi pi-times hover:cursor-pointer mr-2"
                  @click.stop="emit('team_unselected', srt.team)"/>
              </p>

            </template>
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
      <CreateAmalgamation
          :rough_amalgamation_name="searchTerm"
          @on_cancel="showNewAmalgamation = false; showSelect = true"
          @on_new_amalgamation="new_team_created"
      />
    </template>

  </div>
</template>

<style scoped>
h2{
  @apply text-xl;
  @apply text-center;
  @apply font-bold;
  @apply mb-2;
}
.listbox {
  @apply m-2;
  @apply border-2 border-gray-400 rounded;
}

.listbox li{
  @apply pt-3 pb-3 pl-2 pr-2;
  @apply hover:bg-gray-200 hover:cursor-pointer;
}
.team-selector-box {
  @apply h-96 overflow-scroll;
}

.amalgamation_item {
  background-color: burlywood;
}

.amalgamation_subtitle {
  font-size: smaller;
  margin-top: 4px;
  margin-bottom: 0;
  @apply mr-2;
}

.already-selected-subtitle {
  @apply float-right;
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