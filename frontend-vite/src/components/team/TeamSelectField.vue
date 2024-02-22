<script lang="ts" setup>
import {computed, onMounted, ref, watch} from "vue";
import CreateTeam from "@/components/team/CreateTeam.vue";
import CreateAmalgamation from "@/components/team/CreateAmalgamation.vue";
import {loadAllTeams} from "@/utils/api/teams_api";
import {useReportStore} from "@/utils/edit_report_store";
import type {Team} from "@/types/team_types";

interface SearchResultTeam {
  team: Team,
  search_score: number
}

const store = useReportStore()
const props = defineProps<{
  /**
   * Show the option to create a new amalgamation
   */
  show_new_amalgamate: Boolean
  /**
   * Show the option to create a new team
   */
  show_add_new_team: Boolean
  /**
   * Teams included in this list will not appear in the select list
   */
  exclude_team_list?: Team[]
  /**
   * The exclude team list will only be excluded when this is set to true
   */
  force_hide_exclude_team_list: Boolean
  /**
   * Allows the option to deselect selected teams
   */
  allow_unselect?: Boolean
  /**
   * Hides all amalgamations
   */
  hide_amalgamations?: Boolean
  /**
   * Only shows amalgamations
   */
  only_amalgamations?: Boolean
  /**
   * Allow the option to split a team that is selected
   */
  show_hide_squad_box?: Boolean
}>()


const emit = defineEmits<{
  (e: 'team_selected', team: Team): void
  (e: 'team_unselected', team: Team): void
}>()

const searchTerm = ref("")
const showSelect = ref(true)
const showNewTeam = ref(false)
const showNewAmalgamation = ref(false)
//const teamsAvailable = ref(<Team[]>[])
const isLoading = ref(false)
const hide_squads = ref(props.show_hide_squad_box)

watch(() => props.show_hide_squad_box, (value, oldValue, onCleanup) => {
  console.log("Changed props value from ",oldValue," to ",value)
  hide_squads.value = value
}, {immediate: true})

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
  store.loadAllTeamsFromServer()
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

const filtered_list = computed(() => {
  let preparedlist = store.allTeams.sort((a, b) => {
    return a.name.localeCompare(b.name)
  })
  if (props.only_amalgamations) {
    preparedlist = preparedlist.filter(value => {
      return value.isAmalgamation
    })
  }
  if (props.hide_amalgamations) {
    preparedlist = preparedlist.filter(value => {
      return !value.isAmalgamation
    })
  }
  if (hide_squads.value) {
    preparedlist = preparedlist.filter(value => {
      if (value.isAmalgamation && value.amalgamationTeams && value.amalgamationTeams.length == 1) {
        return false
      }
      return true
    })
  }
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

      <div class="flex flex-row justify-evenly items-stretch">
        <div class="md:w-32 grow"></div>
        <div class="self-center w-min ">
          <span class="p-float-label">
            <InputText id="search_term" v-model="searchTerm"/>
            <label for="search_term">{{ $t('teamSelect.enterNameForSearch') }}</label>
          </span>
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
              :key="srt.search_score"
              :class="[{
                  amalgamation_item: srt.team.isAmalgamation &&
                                      srt.team.amalgamationTeams &&
                                      srt.team.amalgamationTeams.length > 1,
                  squad_item: srt.team.isAmalgamation &&
                                      srt.team.amalgamationTeams &&
                                      srt.team.amalgamationTeams.length == 1
                },classForTeam(srt.team)]"
              @click="on_team_click(srt.team)"
          >
            <template v-if="srt.team.isAmalgamation">
              {{ srt.team.name }} -
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
            <template v-else>
              {{ srt.team.name }}
              <div class="float-right flex flex-row justify-end">
                <p v-if="thisTeamInExludedList(srt.team)" class="already-selected-subtitle">
                  {{ $t('teamSelect.alreadyInSelection') }} <i
                    v-if="thisTeamInExludedList(srt.team) && props.allow_unselect"
                    class="pi pi-times hover:cursor-pointer mr-2"
                    @click.stop="emit('team_unselected', srt.team)"/>
                </p>
              </div>

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
h2 {
  @apply text-xl;
  @apply text-center;
  @apply font-bold;
  @apply mb-2;
}

.listbox {
  @apply m-2;
  @apply border-2 border-gray-400 rounded;
}

.listbox li {
  @apply pt-3 pb-3 pl-2 pr-2;
  @apply hover:bg-gray-200 hover:cursor-pointer;
}

.team-selector-box {
  @apply h-96 overflow-scroll;
}

.amalgamation_item {
  background-color: burlywood;
}

.squad_item {
  @apply bg-blue-300
}

.amalgamation_subtitle {
  font-size: smaller;
  margin-top: 4px;
  margin-bottom: 0;
  @apply mr-2;
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
