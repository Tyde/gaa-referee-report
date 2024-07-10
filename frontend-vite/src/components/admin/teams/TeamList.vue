<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import {editTeamOnServer} from "@/utils/api/teams_api";
import {computed, ref} from "vue";
import type {DataTableRowEditSaveEvent} from "primevue/datatable";
import {FilterMatchMode} from "primevue/api";
import TeamSelectField from "@/components/team/TeamSelectField.vue";
import type {Team} from "@/types/team_types";
import MergeTeamDialog from "@/components/team/MergeTeamDialog.vue";
import ConvertTeamToAmalgamtionDialog from "@/components/admin/teams/ConvertTeamToAmalgamtionDialog.vue";
import EditAmalgamationDialog from "@/components/admin/teams/EditAmalgamationDialog.vue";

const store = useAdminStore()

//const teams = ref<Team[]>([])

const props = defineProps<{
  teams: Team[]
}>()

const emit = defineEmits<{
  (event: 'teamUpdated', team: Team): void
}>()





const editingTeams = ref<Team[]>([])

function editTeam(event: DataTableRowEditSaveEvent) {
  let {newData, index } = event
  editTeamOnServer(newData)
      .then((dbTeam) => {
        props.teams[index] = dbTeam
        emit('teamUpdated', newData)
      })
      .catch((error) => {
        store.newError(error)
      })
}

function teamMerged(mergeInto: Team) {
  emit('teamUpdated', mergeInto)
  mergeTeamDialogVisible.value = false
}

const filters = ref({
  global: {value: null, matchMode: FilterMatchMode.CONTAINS},
  name: {value: null, matchMode: FilterMatchMode.CONTAINS},
})

const mergeTeamDialogVisible = ref(false);
const mergeSelectedTeam = ref<Team>();

function startMergeTeam(team: Team) {
  mergeSelectedTeam.value = team
  mergeTeamDialogVisible.value = true
}

const convertToAmalgamationDialogVisibale = ref(false);
const convertToAmalgamationTeam = ref<Team>();
function startAmalgamationConvert(team: Team) {
  convertToAmalgamationTeam.value = team
  convertToAmalgamationDialogVisibale.value = true
}


const editAmalgamationDialogVisible = ref(false)
const editAmalgamation = ref<Team>()
function startEditAmalgamation(team: Team) {
  editAmalgamation.value = team
  editAmalgamationDialogVisible.value = true
}

function onAmalgamationEdited(team: Team) {
  emit('teamUpdated', team)
  editAmalgamationDialogVisible.value = false
  editAmalgamation.value = undefined
}

const orderedTeamsList = computed(() => {
  return props.teams.sort((a, b) => a.name > b.name ? 1 : -1)
})
</script>

<template>
  <div>
    <DataTable
        :value="orderedTeamsList"
        edit-mode="row"
        v-model:editing-rows="editingTeams"
        @row-edit-save="editTeam"
        v-model:filters="filters"
        filter-display="row"
    >
      <Column field="name" header="Name" sortable>
        <template #editor="slotProps">
          <InputText v-model="slotProps.data.name"/>

        </template>
        <template #filter="{filterModel,filterCallback}">
          <InputText v-model="filterModel.value" @input="filterCallback()"/>
        </template>
        <template #body="{data}">
          <template v-if="data.isAmalgamation">
            <div class="grid grid-cols-2 gap-2 items-center">
              <div>{{ data.name }}</div>
              <div class="flex justify-end">
                <Button text label="Edit teams" @click="() => startEditAmalgamation(data)"/>
                <Button text label="Merge with..." @click="() => startMergeTeam(data)"/>
              </div>
              <div class="col-span-2 flex flex-row">
                <div class="bg-gray-300 rounded-xl m-1 p-2 text-sm" v-for="(team, index) in data.amalgamationTeams" :key="team.id">
                  <span>{{ team.name }}</span>
                </div>
              </div>
            </div>

          </template>
          <template v-else>
            <div class="flex flex-row items-center">
              <div class="flex-1 align-middle inline-block">{{ data.name }}</div>
              <div>
                <Button text label="Merge with..." @click="() => startMergeTeam(data)"/>
                <Button text label="Convert" @click="() => startAmalgamationConvert(data)"/>
              </div>
            </div>
          </template>
        </template>
      </Column>
      <Column :rowEditor="true" headerStyle="width:7rem" bodyStyle="text-align:center"></Column>
    </DataTable>
    <MergeTeamDialog
        v-if="mergeSelectedTeam"
        v-model:visible="mergeTeamDialogVisible"
        :selected-team="mergeSelectedTeam"
        @teamMerged="teamMerged"
    />
    <ConvertTeamToAmalgamtionDialog
        v-if="convertToAmalgamationTeam"
        v-model:visible="convertToAmalgamationDialogVisibale"
        :selected-team="convertToAmalgamationTeam"
        />
    <EditAmalgamationDialog
        v-if="editAmalgamation"
        v-model:visible="editAmalgamationDialogVisible"
        :selected-team="editAmalgamation"
        @team-updated="onAmalgamationEdited"
        />

  </div>

</template>


<style scoped>

</style>
