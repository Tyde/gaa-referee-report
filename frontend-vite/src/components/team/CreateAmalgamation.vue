<script lang="ts" setup>
import {ref, onBeforeMount, computed} from "vue";
import TeamSelectField from "@/components/team/TeamSelectField.vue";
import {allTeams, createAmalgamationOnServer} from "@/utils/api/teams_api";
import {useReportStore} from "@/utils/edit_report_store";
import type {Team} from "@/types/team_types";

const store = useReportStore();
const props = defineProps<{
  rough_amalgamation_name?: string
}>()

const emit = defineEmits<{
  (e: 'on_new_amalgamation', team: Team): void,
  (e: 'on_cancel'): void
}>()

const amalgamation_name = ref("")
const selected_teams = ref(<Team[]>[])
const is_loading = ref(false)

function on_team_selected(team: Team) {
  selected_teams.value.push(team)
}

const cachedTeams = ref<Team[]>([])

onBeforeMount(() => {
  allTeams().catch((error) => {
    store.newError(error)
  }).then((teams) => {
    if(teams) {
      cachedTeams.value = teams
    }
  })
})

const duplicates = computed(() => {
  return cachedTeams.value.filter(team => {
    if(team.isAmalgamation && team.amalgamationTeams) {
      if(team.amalgamationTeams.length === selected_teams.value.length) {

        let allTeamsInAmalgamation = true
        for (let otherAmalgamationTeam of team.amalgamationTeams) {
          let contained = selected_teams.value
              .filter(it => it.id === otherAmalgamationTeam.id).length > 0
          allTeamsInAmalgamation = allTeamsInAmalgamation && contained

        }

        return allTeamsInAmalgamation
      }
    }
    return false
  })
})

async function create_amalgamation() {
  if (amalgamation_name.value && amalgamation_name.value.length > 0) {
    if (selected_teams.value.length > 1) {
      is_loading.value = true
      const team = await createAmalgamationOnServer(
          amalgamation_name.value,
          selected_teams.value
      )
          .catch((error) => {
            store.newError(error)
            return undefined
          })
      is_loading.value = false
      if (team) {
        emit('on_new_amalgamation', team)
      }
    } else {
      alert("Amalgamations have to consist of at least two teams")
    }
  } else {
    alert("Please enter an amalgamation name")
  }
}

function cancel() {
  emit('on_cancel')
}

function unselectTeam(team: Team) {
  selected_teams.value = selected_teams.value.filter(t => t.id !== team.id)
}
</script>

<template>
  <div class="w-fill shadow-xl p-2 bg-gray-100 border-gray-500 rounded-xl">
    <h3>Create new Amalgamation</h3>
    <div class="w-fill">
      <div class="mt-2 text-center">
        <label for="amalgamation_name">Enter amalgamation name</label>
        <div class="m-1">
          <InputText id="amalgamation_name" v-model="amalgamation_name" type="text"/>
        </div>

      </div>
      <ul class="flex flex-row flex-wrap justify-center list-none w-full min-h-[4rem]">
        <li v-for="team in selected_teams"
            class="p-2 m-2 h-min rounded-2xl bg-gray-300 hover:bg-gray-200 hover:cursor-pointer inline-block"
            @click="unselectTeam(team)"
        >
          {{ team.name }}
        </li>
      </ul>
      <br>
      <TeamSelectField
          :exclude_team_list="selected_teams"
          :force_hide_exclude_team_list="false"
          :show_add_new_team="true"
          :show_new_amalgamate="false"
          @team_selected="on_team_selected"
      />
      <div v-if="duplicates.length > 0" class="text-lg font-bold p-2 m-2 bg-red-400 rounded-lg">
        WARNING: The amalgamation with your selection already exists:
        {{ duplicates.map(it => it.name).join(", ") }}
      </div>
      <div class="flex flex-row justify-center">
        <div class="m-2">
          <Button class="p-button-rounded" @click="create_amalgamation">Save Amalgamation</Button>
        </div>
        <div class="m-2">
          <Button class="p-button-secondary p-button-rounded" @click="cancel">Cancel creating Amalgamation</Button>
        </div>
      </div>
    </div>


  </div>
</template>

<style scoped>
h3 {
  @apply text-xl font-bold text-gray-700 text-center mt-2 mb-2;
}
</style>
