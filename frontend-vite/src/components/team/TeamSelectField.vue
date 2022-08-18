<script lang="ts" setup>
import {computed, onMounted, ref} from "vue";
import type {Team} from "@/types";
import CreateTeam from "@/components/team/CreateTeam.vue";
import CreateAmalgamation from "@/components/team/CreateAmalgamation.vue";
import Card from 'primevue/card';
import {allTeams} from "@/utils/api/teams_api";

interface SearchResultTeam {
  team: Team,
  search_score: number
}

const props = defineProps<{
  show_amalgamate: Boolean
  show_add_new_team: Boolean
  exclude_team_list?: Team[]
}>()

const emit = defineEmits<{
  (e: 'team_selected', team: Team): void
}>()

const searchTerm = ref("")
const showSelect = ref(true)
const showNewTeam = ref(false)
const showNewAmalgamation = ref(false)
const teamsAvailable = ref(<Team[]>[])
const isLoading = ref(false)

function on_team_click(team: Team) {
  emit("team_selected", team)
  searchTerm.value = ""
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
  } catch (e) {
    console.error(e)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetch_available_teams()
})


const filtered_list = computed(() => {
  let preparedlist = teamsAvailable.value.sort((a, b) => {
    return a.name.localeCompare(b.name)
  })
  if (props.exclude_team_list !== undefined) {
    let excludelist = props.exclude_team_list
    console.log("Checking excludes")
    preparedlist = preparedlist.filter(value => {
      return excludelist.findIndex(exclude_val => {
        return exclude_val.id == value.id
      }) == -1
    })
  }
  if (searchTerm.value) {
    let foundList = preparedlist.filter(value => {
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
    return foundList
  } else {
    return preparedlist.map(value => {
      return {team: value, search_score: 1} as SearchResultTeam
    })
  }
})

</script>

<template>

  <Card style="width:35em; margin-bottom: 2em">
    <template v-if="showSelect" #title>Select teams:</template>
    <template #content>
      <template v-if="showSelect">
      <span class="p-float-label">
        <InputText id="search_term" v-model="searchTerm"/>
        <label for="search_term">Enter team name</label>
      </span>
        <div class="p-listbox team-selector-box">
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
                v-if="isLoading && filtered_list.length == 0"
                class="p-listbox-item"
            >
              <i
                  class="pi pi-spin pi-spinner"
                  style="font-size: 2rem"></i>
            </li>
            <li
                v-for="srt in filtered_list"
                :key="srt.search_score"
                :class="{
                  amalgamation_item: srt.team.isAmalgamation
                }"
                class="p-listbox-item"
                @click="on_team_click(srt.team)"
            >
              <template v-if="srt.team.isAmalgamation">
                {{ srt.team.name }} - Amalgamation
                <p v-if="srt.team.amalgamationTeams" class="amalgamation_subtitle">
                  {{ srt.team.amalgamationTeams.map(value => value.name).join(" - ") }}
                </p>
              </template>
              <template v-else>
                {{ srt.team.name }}
              </template>
            </li>
          </ul>
        </div>
      </template>

      <template v-if="showNewTeam">
        <CreateTeam
            :rough_team_name="searchTerm"
            @on_new_team="new_team_created"
        />
      </template>
      <template v-if="showNewAmalgamation">
        <CreateAmalgamation
            :rough_amalgamation_name="searchTerm"
            @on_cancel="showNewAmalgamation = false; showSelect = true"
            @on_new_amalgamation="new_team_created"
        />
      </template>

    </template>
  </Card>
</template>

<style scoped>
.team-selector-box {
  height: 20em;
  overflow: scroll;
}

.amalgamation_item {
  background-color: burlywood;
}

.amalgamation_subtitle {
  font-size: smaller;
  margin-top: 4px;
  margin-bottom: 0px;
}

.teamselect-add-option {
  font-style: italic;
  font-weight: bold;
  color: darkcyan !important;
}
</style>