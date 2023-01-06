<script lang="ts" setup>
import {ref} from "vue";
import TeamSelectField from "@/components/team/TeamSelectField.vue";
import type {Team} from "@/types";
import {createAmalgamationOnServer} from "@/utils/api/teams_api";
import {useReportStore} from "@/utils/edit_report_store";

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


async function create_amalgamation() {
  if (amalgamation_name) {
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
</script>

<template>
  <Card>
    <template #title>Create new Amalgamation</template>
    <template #content>
      <span class="p-float-label">
        <InputText id="amalgamation_name" v-model="amalgamation_name" type="text"/>
        <label for="amalgamation_name">Enter amalgamation name</label>
      </span>
      <ul>
        <li v-for="team in selected_teams">
          {{ team.name }}
        </li>
      </ul>
      <br>
      <TeamSelectField
          :exclude_team_list="selected_teams"
          :force_hide_exclude_team_list="false"
          :show_add_new_team="true"
          :show_amalgamate="false"
          @team_selected="on_team_selected"
      />
      <Button class="p-button-rounded" @click="create_amalgamation">Send</Button>
      <Button class="p-button-secondary p-button-rounded" @click="cancel">Cancel</Button>
    </template>


  </Card>
</template>

<style scoped>

</style>