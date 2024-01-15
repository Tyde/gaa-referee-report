<script setup lang="ts">
import {onMounted, ref} from "vue";
import TeamSelectField from "@/components/team/TeamSelectField.vue";
import {useReportStore} from "@/utils/edit_report_store";
import type {Team} from "@/types/team_types";

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
  <div class="mx-auto m-2 w-full md:w-10/12 xl:w-5/12">
    <h2>Select the clubs, that will be part of this report</h2>
    <div>



      <TeamSelectField
          :show_add_new_team="true"
          :show_new_amalgamate="true"
          :exclude_team_list="teams_added"
          @team_selected="addTeam"
          :force_hide_exclude_team_list="false"
          :allow_unselect="false"
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


    </div>
  </div>
</template>



<style scoped>
  h2 {
    @apply text-2xl font-bold text-gray-700 text-center mt-2 mb-2;
  }
.selected-teams-list {
  @apply w-full;
  list-style: none;
  padding:0;
  @apply flex flex-row justify-center items-center flex-wrap;
}
.selected-teams-list li {
  display: inline-block;
  padding: 8px;

}
</style>
