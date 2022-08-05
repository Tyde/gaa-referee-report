<script setup lang="ts">
import {onMounted, ref} from "vue";
import type Team from "@/types";
import TeamSelectField from "@/components/TeamSelectField.vue";

const emit = defineEmits(['submit-teams'])
const props = defineProps<{
  alreadySelectedTeams:Array<Team>
}>()

enum TeamSelectorModal {
  NoModal,
  CreateTeamModal,
  CreateAmalgamationModal
}


const show_modal = ref(<TeamSelectorModal>TeamSelectorModal.NoModal)
const selected = ref(<Team>{})
const teams_available = ref(<Team[]>[])
const teams_added = ref(<Team[]>[])

function add_team(team: Team) {

  teams_added.value.push(team)
  emit('submit-teams',teams_added.value)
}

function create_team() {
  show_modal.value = TeamSelectorModal.CreateTeamModal
}

function new_team_created(newTeam: Team) {
  teams_added.value.push(newTeam)
  show_modal.value = TeamSelectorModal.NoModal
  fetch_available_teams()
}

function create_amalgamation() {

}

async function fetch_available_teams() {
  const response = await fetch("/api/teams_available")
  teams_available.value = await response.json()
}

onMounted(() => {
  teams_added.value.length = 0
  console.log("already selected: ")
  console.log(props.alreadySelectedTeams)
  teams_added.value = teams_added.value.concat(props.alreadySelectedTeams)
  console.log(teams_added.value)
  fetch_available_teams()
})
</script>

<template>
  <Card>
    <template #title>Select the clubs, that will be part of this report</template>
    <template #content>

      <!--
      <select v-model="selected">
        <option v-for="team in teams_available" :value="team">
          {{team.name}}
        </option>
      </select>
      <br>
      <br>-->

      <TeamSelectField
          :show_add_new_team="true"
          :show_amalgamate="true"
          :exclude_team_list="teams_added"
          @team_selected="add_team"
      />

      <template v-if="teams_added.length>0">
        Selected:
        <ul class="selected-teams-list">
          <li v-for="team in teams_added">
            <Button
                class="p-button-rounded p-button-secondary p-button-sm"
                @click="teams_added = teams_added.filter(lteam => { return lteam !== team })"
            >
              {{team.name}}  <i class="pi pi-times"></i>
            </Button>
          </li>
        </ul>
      </template>

      <!--<Button label="submit" @click="$emit('submit-teams',teams_added)">Submit</Button>-->
      <!--
          <button v-on:click="add_team">Add</button>
          <br>
          <button @click="create_team">Add new Team</button> <button @click="create_amalgamation">Add new Amalgamation</button>
          <br>


          <div v-if="show_modal!==TeamSelectorModal.NoModal">
            <CreateTeam
                v-if="show_modal===TeamSelectorModal.CreateTeamModal"
                @on_new_team="new_team_created"
            />
          </div>
          -->
    </template>
  </Card>
</template>

<script lang="ts">
export default {
  name: "TeamSelector.vue",
  data() {
    return {
      count: 0
    }
  }
}
</script>

<style scoped>
.selected-teams-list {
  max-width: 35em;
  list-style: none;
  padding:0em;
}
.selected-teams-list li {
  display: inline-block;
  padding: 8px;
}
</style>