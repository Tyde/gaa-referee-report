<script setup lang="ts">
import {onMounted, ref} from "vue";
import type {Team} from "@/types";
import TeamSelectField from "@/components/team/TeamSelectField.vue";
import {allTeams} from "@/utils/api/teams_api";
import {useReportStore} from "@/utils/edit_report_store";

const emit = defineEmits(['submit-teams'])

const store = useReportStore()

enum TeamSelectorModal {
  NoModal,
  CreateTeamModal,
  CreateAmalgamationModal
}


const teams_added = ref(<Team[]>[])


function addTeam(team: Team) {
  teams_added.value.push(team)
  emit('submit-teams',teams_added.value)
}


function removeTeam(team:Team) {
  teams_added.value = teams_added.value.filter(lteam => { return lteam !== team })
  emit('submit-teams',teams_added.value)
}




onMounted(() => {
  teams_added.value.length = 0
  teams_added.value = teams_added.value.concat(store.report.selectedTeams)
})
</script>

<template>
  <Card>
    <template #title>Select the clubs, that will be part of this report</template>
    <template #content>



      <TeamSelectField
          :show_add_new_team="true"
          :show_amalgamate="true"
          :exclude_team_list="teams_added"
          @team_selected="addTeam"
          :force_hide_exclude_team_list="false"
      />


      <template v-if="teams_added.length>0">
        Selected teams:<br>
        <ul class="selected-teams-list">
          <li v-for="team in teams_added.sort((a,b) => a.name.localeCompare(b.name))">
            <Button
                class="p-button-rounded p-button-secondary p-button-sm"
                @click="removeTeam(team)"
            >
              {{team.name}}  <i class="pi pi-times"></i>
            </Button>
          </li>
        </ul>
      </template>


    </template>
  </Card>
</template>



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