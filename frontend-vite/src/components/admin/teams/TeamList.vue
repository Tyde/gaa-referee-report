<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import {editTeamOnServer} from "@/utils/api/teams_api";
import {computed, ref} from "vue";
import type {DataTableRowEditSaveEvent, DataTableRowEditInitEvent} from "primevue/datatable";
import { FilterMatchMode } from '@primevue/core/api';
import type {Team} from "@/types/team_types";
import MergeTeamDialog from "@/components/team/MergeTeamDialog.vue";
import ConvertTeamToAmalgamtionDialog from "@/components/admin/teams/ConvertTeamToAmalgamtionDialog.vue";
import EditAmalgamationDialog from "@/components/admin/teams/EditAmalgamationDialog.vue";
import TeamHistoryDialog from "@/components/admin/teams/TeamHistoryDialog.vue";
import TeamAliasEditor from "@/components/admin/teams/TeamAliasEditor.vue";

const store = useAdminStore()

//const teams = ref<Team[]>([])

const props = defineProps<{
  teams: Team[]
}>()

const emit = defineEmits<{
  (event: 'teamUpdated', team: Team): void
}>()





const editingTeams = ref<Team[]>([])

function toIsoDate(value: Date | undefined | null): string | undefined {
  if (!value) {
    return undefined
  }
  const date = new Date(value)
  if (isNaN(date.getTime())) {
    return undefined
  }
  return date.toISOString().slice(0, 10)
}

function onRowEditInit(event: DataTableRowEditInitEvent) {
  //Initialize the change date for the editor (defaults to today)
  event.data.changeDateValue = event.data.changeDate ? new Date(event.data.changeDate) : new Date()
  event.data.originalName = event.data.name
  event.data.keepOldName = true
  event.data.oldNameAlias = event.data.name
}

function editTeam(event: DataTableRowEditSaveEvent) {
  const {newData} = event
  const payload: Team = {
    name: newData.name,
    id: newData.id,
    isAmalgamation: newData.isAmalgamation,
    amalgamationTeams: newData.amalgamationTeams ?? null,
    changeDate: toIsoDate(newData.changeDateValue)
  }
  const nameChanged = newData.originalName !== undefined && newData.originalName !== newData.name
  const keepOldName = nameChanged && newData.keepOldName && newData.oldNameAlias?.trim()
      ? newData.oldNameAlias.trim()
      : undefined
  editTeamOnServer(payload, keepOldName)
      .then((dbTeam) => {
        emit('teamUpdated', dbTeam)
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

const historyDialogVisible = ref(false)
const historyTeam = ref<Team>()
function showHistory(team: Team) {
  historyTeam.value = team
  historyDialogVisible.value = true
}

const orderedTeamsList = computed(() => {
  return [...props.teams].sort((a, b) => a.name > b.name ? 1 : -1)
})
</script>

<template>
  <div>
    <DataTable
        :value="orderedTeamsList"
        edit-mode="row"
        v-model:editing-rows="editingTeams"
        @row-edit-init="onRowEditInit"
        @row-edit-save="editTeam"
        v-model:filters="filters"
        filter-display="row"
    >
      <Column field="name" header="Name" sortable>
        <template #editor="slotProps">
          <div class="flex flex-col gap-2">
            <InputText v-model="slotProps.data.name"/>
            <div class="flex flex-row items-center gap-2">
              <label class="text-xs">Date of change</label>
              <DatePicker v-model="slotProps.data.changeDateValue" dateFormat="yy-mm-dd" showIcon iconDisplay="input" class="w-full"/>
            </div>
            <div v-if="slotProps.data.name !== slotProps.data.originalName" class="flex flex-col gap-1">
              <label :for="`keep-old-name-${slotProps.data.id}`" class="flex flex-row items-center gap-2 text-xs">
                <Checkbox :input-id="`keep-old-name-${slotProps.data.id}`" v-model="slotProps.data.keepOldName" binary/>
                Keep old name as alternative spelling
              </label>
              <label :for="`old-name-alias-${slotProps.data.id}`" class="sr-only">Alternative spelling</label>
              <InputText :id="`old-name-alias-${slotProps.data.id}`" v-model="slotProps.data.oldNameAlias" :disabled="!slotProps.data.keepOldName"/>
            </div>
          </div>
        </template>
        <template #filter="{filterModel,filterCallback}">
          <InputText v-model="filterModel.value" @input="filterCallback()"/>
        </template>
        <template #body="{data}">
          <template v-if="data.isAmalgamation">
            <div class="grid grid-cols-2 gap-2 items-center">
              <div>{{ data.name }}</div>
              <div class="flex justify-end">
                <Button text label="History" @click="() => showHistory(data)"/>
                <Button text label="Edit teams" @click="() => startEditAmalgamation(data)"/>
                <Button text label="Merge with..." @click="() => startMergeTeam(data)"/>
              </div>
              <div class="col-span-2 flex flex-row">
                <div class="bg-surface-500 rounded-xl m-1 p-2 text-sm" v-for="team in data.amalgamationTeams" :key="team.id">
                  <span>{{ team.name }}</span>
                </div>
              </div>
              <div class="col-span-2">
                <TeamAliasEditor :team="data" @aliases-changed="() => emit('teamUpdated', data)"/>
              </div>
            </div>

          </template>
          <template v-else>
            <div class="flex flex-col">
              <div class="flex flex-row items-center">
                <div class="flex-1 align-middle inline-block">{{ data.name }}</div>
                <div>
                  <Button text label="History" @click="() => showHistory(data)"/>
                  <Button text label="Merge with..." @click="() => startMergeTeam(data)"/>
                  <Button text label="Convert" @click="() => startAmalgamationConvert(data)"/>
                </div>
              </div>
              <TeamAliasEditor :team="data" @aliases-changed="() => emit('teamUpdated', data)"/>
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
    <TeamHistoryDialog
        v-if="historyTeam"
        v-model:visible="historyDialogVisible"
        :selected-team="historyTeam"
        />

  </div>

</template>


<style scoped>

</style>
