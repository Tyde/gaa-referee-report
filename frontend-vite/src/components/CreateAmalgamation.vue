<script setup lang="ts">
import {ref} from "vue";
import TeamSelectField from "@/components/TeamSelectField.vue";
import type {Team} from "@/types";

const props = defineProps<{
  rough_amalgamation_name?:string
}>()

const emit = defineEmits<{
  (e: 'on_new_amalgamation', team: Team): void,
  (e: 'on_cancel'):void
}>()

const amalgamation_name = ref("")
const selected_teams = ref(<Team[]>[])
const is_loading = ref(false)

function on_team_selected(team: Team) {
  selected_teams.value.push(team)
}


async function create_amalgamation() {
  if (amalgamation_name) {
    if(selected_teams.value.length > 1) {
      const requestOptions = {
        method: "POST",
        headers: {
          'Content-Type': 'application/json;charset=utf-8'
        },
        body: JSON.stringify({"name": amalgamation_name.value, "teams":selected_teams.value})
      };
      is_loading.value = true
      const response = await fetch("/api/new_amalgamation", requestOptions)
      const data = await response.json()
      is_loading.value = false
      emit('on_new_amalgamation', data as Team)
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
</script>

<template>
  <Card>
    <template #title>Create new Amalgamation</template>
    <template #content>
      <span class="p-float-label">
        <InputText v-model="amalgamation_name" type="text" id="amalgamation_name"/>
        <label for="amalgamation_name">Enter amalgamation name</label>
      </span>
      <ul>
        <li v-for="team in selected_teams">
          {{team.name}}
        </li>
      </ul>
      <br>
      <TeamSelectField
       :show_add_new_team="true"
       :show_amalgamate="false"
       :exclude_team_list="selected_teams"
       @team_selected="on_team_selected"
      />
      <Button class="p-button-rounded" @click="create_amalgamation">Send</Button>
      <Button class="p-button-secondary p-button-rounded" @click="cancel">Cancel</Button>
    </template>


  </Card>
</template>

<style scoped>

</style>