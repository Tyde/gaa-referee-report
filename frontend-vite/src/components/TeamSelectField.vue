<script setup lang="ts">
import {computed, onMounted, ref} from "vue";
import type Team from "@/types";
import CreateTeam from "@/components/CreateTeam.vue";
import CreateAmalgamation from "@/components/CreateAmalgamation.vue";
import Card from 'primevue/card';

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

const search_term = ref("")
const show_select = ref(true)
const show_new_team = ref(false)
const show_new_amalgamation = ref(false)
const teams_available = ref(<Team[]>[])

function on_team_click(team: Team) {
  console.log("Clicked on team " + team.name)
  emit("team_selected", team)
  search_term.value = ""
}

function on_add_team_click() {
  show_select.value = false
  show_new_team.value = true
}

function new_team_created(team: Team) {
  emit("team_selected", team)
  fetch_available_teams()
  show_select.value = true
  show_new_team.value = false
  show_new_amalgamation.value = false
  search_term.value = ""
}

function on_start_amalgamation_click() {
  show_select.value = false
  show_new_amalgamation.value = true
}

async function fetch_available_teams() {
  const response = await fetch("/api/teams_available")
  teams_available.value = await response.json()
}

onMounted(() => {
  fetch_available_teams()
})


const filtered_list = computed(() => {
  let preparedlist = teams_available.value
  if (props.exclude_team_list !== undefined) {
    let excludelist = props.exclude_team_list
    console.log("Checking excludes")
    preparedlist = preparedlist.filter(value => {
      return excludelist.findIndex(exclude_val => {
        return exclude_val.id == value.id
      }) == -1
    })
  }
  if (search_term.value) {
    let foundList = preparedlist.filter(value => {
      let isInName = value.name.toLowerCase().search(search_term.value.toLowerCase()) != -1
      let isInAmalgamations = value.amalgamationTeams?.reduce(function (pv,cv) {
        let isInLocalName = cv.name.toLowerCase().search(search_term.value.toLowerCase()) != -1
        return pv||isInLocalName
      },false)
      return isInName||isInAmalgamations
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
    <template v-if="show_select" #title>Select teams:</template>
    <template #content>
      <template v-if="show_select">
      <span class="p-float-label">
        <InputText v-model="search_term" id="search_term"/>
        <label for="search_term">Enter team name</label>
      </span>
        <div class="p-listbox team-selector-box">
          <ul class="p-listbox-list">
            <li
                v-if="props.show_add_new_team "
                @click="on_add_team_click()"
                class="p-listbox-item teamselect-add-option"
            >
              Add new team <span v-if="search_term.length>0">"{{ search_term }}"</span> ...
            </li>
            <li
                v-if="props.show_amalgamate"
                @click="on_start_amalgamation_click()"
                class="teamselect-add-option p-listbox-item"
            >
              Add new amalgamation
              <span v-if="search_term.length>0">"{{ search_term }}"</span>
              ...
            </li>
            <li
                v-for="srt in filtered_list"
                :key="srt.search_score"
                @click="on_team_click(srt.team)"
                class="p-listbox-item"
                :class="{
                  amalgamation_item: srt.team.isAmalgamation
                }"
            >
              <template v-if="srt.team.isAmalgamation">
                {{srt.team.name}} - Amalgamation
                <p class="amalgamation_subtitle" v-if="srt.team.amalgamationTeams">
                  {{srt.team.amalgamationTeams.map(value => value.name).join(" - ")}}
                </p>
              </template>
              <template v-else>
                {{srt.team.name}}
              </template>
            </li>
          </ul>
        </div>
      </template>

      <template v-if="show_new_team">
        <CreateTeam
            :rough_team_name="search_term"
            @on_new_team="new_team_created"
        />
      </template>
      <template v-if="show_new_amalgamation">
        <CreateAmalgamation
            :rough_amalgamation_name="search_term"
            @on_new_amalgamation="new_team_created"
            @on_cancel="show_new_amalgamation = false; show_select = true"
        />
      </template>

    </template>
  </Card>
</template>

<style scoped>
.team-selector-box {
  max-height: 14em;
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