<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import {allTeams, editTeamOnServer} from "@/utils/api/teams_api";
import {computed, onMounted, ref} from "vue";
import type {Team} from "@/types";
import type {DataTableRowEditSaveEvent} from "primevue/datatable";
import {FilterMatchMode} from "primevue/api";
import TeamSelectField from "@/components/team/TeamSelectField.vue";

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

const filters = ref({
  global: {value: null, matchMode: FilterMatchMode.CONTAINS},
  name: {value: null, matchMode: FilterMatchMode.CONTAINS},
})
</script>

<template>
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
                :show_amalgamate="false"
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
            {{data.name}}
            <div class="flex flex-row">
              <div class="bg-gray-300 rounded-xl m-1 p-2 text-sm" v-for="(team, index) in data.amalgamationTeams" :key="team.id">
                <span>{{ team.name }}</span>
              </div>
            </div>
          </template>
          <template v-else>
            {{data.name}}
          </template>
        </template>
      </Column>
      <Column :rowEditor="true" headerStyle="width:7rem" bodyStyle="text-align:center"></Column>
    </DataTable>


</template>


<style scoped>

</style>