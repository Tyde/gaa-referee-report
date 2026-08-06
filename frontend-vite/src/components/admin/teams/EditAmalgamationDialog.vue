<script setup lang="ts">
import type {Team} from "@/types/team_types";
import {computed, ref, watch} from "vue";
import TeamSelectField from "@/components/team/TeamSelectField.vue";
import {editTeamOnServer} from "@/utils/api/teams_api";
import {useAdminStore} from "@/utils/admin_store";
// EditAmalgamationDialog.vue

const store = useAdminStore()

const props = defineProps<{
  visible: boolean,
  selectedTeam: Team,
}>()

const emits = defineEmits<{
  (e: 'update:visible', value: boolean): void,
  (e: 'teamUpdated', team: Team): void
}>()

const localVisible = computed({
  get() {
    return props.visible
  },
  set(newValue) {
    emits('update:visible', newValue)
  }
})

const teamEditCopy = ref<Team>(JSON.parse(JSON.stringify(props.selectedTeam)))
const changeDate = ref<Date>(new Date())
watch(() => props.visible, (newValue) => {
  if (newValue) {
    teamEditCopy.value = JSON.parse(JSON.stringify(props.selectedTeam))
    changeDate.value = teamEditCopy.value.changeDate ? new Date(teamEditCopy.value.changeDate) : new Date()
  }
})

function saveEdit() {
  const payload: Team = {
    ...teamEditCopy.value,
    changeDate: changeDate.value ? changeDate.value.toISOString().slice(0, 10) : undefined
  }
  editTeamOnServer(payload)
      .then((dbTeam) => {
        emits('teamUpdated', dbTeam)
      })
      .catch((error) => {
        store.newError(error)
      })

}

</script>

<template>
  <Dialog
      v-model:visible="localVisible"
      :closable="true"
      :close-on-escape="true"
      header="Edit Amalgamation Teams"
      :modal="true"
      :pt="{ root: { class: 'amalgamation-diag'}}"
  >
    <div class="flex flex-col">
      <div>
        Name: <InputText v-model="teamEditCopy.name" />
      </div>
      <div class="flex flex-row items-center gap-2 m-2">
        <label>Date of change</label>
        <DatePicker v-model="changeDate" dateFormat="yy-mm-dd" showIcon iconDisplay="input" class="w-full"/>
      </div>
      <div v-if="teamEditCopy.amalgamationTeams">
        Teams:
        <ul>
          <li
              v-for="team in teamEditCopy.amalgamationTeams"
              :key="team.id"
              @click="teamEditCopy.amalgamationTeams = teamEditCopy.amalgamationTeams?.filter((it:Team) => it.id !== team.id)"
              class="bg-gray-300 rounded-xl m-1 p-2 text-sm hover:cursor-pointer"
          >
            <vue-feather type="x" class="mr-2"/>
                {{team.name}}
          </li>
        </ul>
        <TeamSelectField
            :exclude_team_list="teamEditCopy.amalgamationTeams"
            :show_add_new_team="false"
            :show_new_amalgamate="false"
            :forcefully_hidden_teams="[selectedTeam]"
            :force_hide_exclude_team_list="true"
            @team_selected="(team) => teamEditCopy.amalgamationTeams?.push(team)"
            @team_unselected="(team) => teamEditCopy.amalgamationTeams = teamEditCopy.amalgamationTeams?.filter((it:Team) => it.id !== team.id)"
            :show_amalgamations="false"
        />

      </div>
      <div class="flex flex-row justify-end m-2">
        <Button label="Save" @click="saveEdit" class="m-2"/>
        <Button label="Cancel" @click="localVisible = false" severity="Secondary" class="m-2"/>
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.amalgamation-diag {
  @apply w-3/6;
}
</style>
