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
const originalName = ref(props.selectedTeam.name)
const keepOldName = ref(true)
const oldNameAlias = ref(props.selectedTeam.name)
watch(() => props.visible, (newValue) => {
  if (newValue) {
    teamEditCopy.value = JSON.parse(JSON.stringify(props.selectedTeam))
    changeDate.value = teamEditCopy.value.changeDate ? new Date(teamEditCopy.value.changeDate) : new Date()
    originalName.value = teamEditCopy.value.name
    keepOldName.value = true
    oldNameAlias.value = teamEditCopy.value.name
  }
})

function saveEdit() {
  const payload: Team = {
    name: teamEditCopy.value.name,
    id: teamEditCopy.value.id,
    isAmalgamation: teamEditCopy.value.isAmalgamation,
    amalgamationTeams: teamEditCopy.value.amalgamationTeams ?? null,
    changeDate: changeDate.value ? changeDate.value.toISOString().slice(0, 10) : undefined
  }
  const nameChanged = originalName.value !== teamEditCopy.value.name
  const keepOldNameAsAlias = nameChanged && keepOldName.value && oldNameAlias.value.trim()
      ? oldNameAlias.value.trim()
      : undefined
  editTeamOnServer(payload, keepOldNameAsAlias)
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
      <div v-if="teamEditCopy.name !== originalName" class="flex flex-col gap-1 m-2">
        <label :for="`keep-old-name-amalgamation-${selectedTeam.id}`" class="flex flex-row items-center gap-2 text-xs">
          <Checkbox :input-id="`keep-old-name-amalgamation-${selectedTeam.id}`" v-model="keepOldName" binary/>
          Keep old name as alternative spelling
        </label>
        <label :for="`old-name-alias-amalgamation-${selectedTeam.id}`" class="sr-only">Alternative spelling</label>
        <InputText
            :id="`old-name-alias-amalgamation-${selectedTeam.id}`"
            v-model="oldNameAlias"
            :disabled="!keepOldName"
        />
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
