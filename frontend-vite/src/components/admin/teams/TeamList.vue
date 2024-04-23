<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import {editTeamOnServer} from "@/utils/api/teams_api";
import {ref} from "vue";
import type {DataTableRowEditSaveEvent} from "primevue/datatable";
import {FilterMatchMode} from "primevue/api";
import TeamSelectField from "@/components/team/TeamSelectField.vue";
import type {Team} from "@/types/team_types";
import MergeTeamDialog from "@/components/team/MergeTeamDialog.vue";
import ConvertTeamToAmalgamtionDialog from "@/components/admin/teams/ConvertTeamToAmalgamtionDialog.vue";

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
</script>

<template>
  <div>
    <DataTable
        :value="props.teams"
        edit-mode="row"
        v-model:editing-rows="editingTeams"
        @row-edit-save="editTeam"
        v-model:filters="filters"
        filter-display="row"
    >
      <Column field="name" header="Name" sortable>
        <template #editor="slotProps">
          <InputText v-model="slotProps.data[slotProps.field]"/>
          <template v-if="slotProps.data.isAmalgamation">
            <TeamSelectField
                :exclude_team_list="slotProps.data.amalgamationTeams"
                :force_hide_exclude_team_list="false"
                :show_add_new_team="false"
                :show_new_amalgamate="false"
                :allow_unselect="true"
                @team_selected="(team) => slotProps.data.amalgamationTeams.push(team)"
                @team_unselected="(team) => slotProps.data.amalgamationTeams = slotProps.data.amalgamationTeams.filter((it:Team) => it.id !== team.id)"
            />
          </template>

        </template>
        <template #filter="{filterModel,filterCallback}">
          <InputText v-model="filterModel.value" @input="filterCallback()"/>
        </template>
        <template #body="{data}">
          <template v-if="data.isAmalgamation">
            <div class="grid grid-cols-2 gap-2 items-center">
              <div>{{ data.name }}</div>
              <div class="flex justify-end">
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
                <Button text label="Convert to Squad" @click="() => startAmalgamationConvert(data)"/>
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

  </div>

</template>


<style scoped>

</style>
