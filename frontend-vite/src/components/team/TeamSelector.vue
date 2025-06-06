<script setup lang="ts">
import {onMounted, ref, watch} from "vue";
import TeamSelectField from "@/components/team/TeamSelectField.vue";
import {useReportStore} from "@/utils/edit_report_store";
import type {Team} from "@/types/team_types";
import CreateSplitTeam from "@/components/team/CreateSplitTeam.vue";
import {useI18n} from "vue-i18n";

const emit = defineEmits(['submit-teams'])

const store = useReportStore()
const {t} = useI18n()
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

const showSelect = ref(true)
const selectedSplitBaseTeam = ref<Team>()
function onSplitCanceled() {
  showSelect.value = true
  selectedSplitBaseTeam.value = undefined
}

function onSplitSuccessful(teams: Team[]) {
  store.publicStore.loadTeams()
  showSelect.value = true
  if(selectedSplitBaseTeam.value) {
    removeTeam(selectedSplitBaseTeam.value)
  }
  selectedSplitBaseTeam.value = undefined
  teams.forEach((team) => {
    addTeam(team)
  })
  emit('submit-teams',teams_added.value)
}

function onSplitClick(team: Team) {
  showSelect.value = false
  selectedSplitBaseTeam.value = team
}

function calculateSplitButtonModel(team: Team) {
  let list =  [{
    label:t('teamSelect.removeFromList'),
    command: () => {
      removeTeam(team)
    }
  }]


  if(!team.isAmalgamation) {
    list.unshift({
      label: t('teamSelect.newSquadsButton')+' ('+team.name+' A, .. B, ...)',
      command: () => {
        onSplitClick(team)
      }
    })
  }
  let squads = store.findSquadsForTeam(team)
  squads.reverse()
  squads.forEach((squad) => {
    list.unshift({
      label: t('teamSelect.useSquadPrefix')+' '+squad.name,
      command: () => {
        swapTeam(team,squad)
      }
    })
  })

  return list
}

function swapTeam(beforeTeam:Team, afterTeam:Team) {
  let index = teams_added.value.indexOf(beforeTeam)
  if(index >= 0) {
    teams_added.value.splice(
        index,
        1,
        afterTeam
    )
    emit('submit-teams',teams_added.value)
  }
}


/**
 *
 * Bug here is:sometimes the store.report.selectedTeams comes first and sometimes the preselected teams comes first
 *
 * Depending on what is first, the submit-teams might be submitted without the correct dataset
 *
 * I tried adding this to onMounted but that lead to teams being duplicated once, which seems to be a "race-condition" where
 * in the end both emits and sets are called
 */
const isInitialized = ref(false)

onMounted(() => {
  // First, set from report.selectedTeams
  teams_added.value = [...store.report.selectedTeams]

  // Then, combine with any pre-selected teams
  if (store.tournamentPreSelectedTeams) {
    teams_added.value = [...new Set([...teams_added.value, ...store.tournamentPreSelectedTeams])]
  }

  // Only emit after we have the complete list
  emit('submit-teams', teams_added.value)

  // Mark as initialized
  isInitialized.value = true
})

// This watch will run after initialization is complete
watch(() => store.tournamentPreSelectedTeams, (newVal) => {
  if (newVal && isInitialized.value) {
    teams_added.value = [...new Set([...teams_added.value, ...newVal])]
    emit('submit-teams', teams_added.value)
  }
})
</script>

<template>
  <div class="mx-auto m-2 w-full md:w-10/12 2xl:w-5/12">
    <h2>{{ $t('teamSelect.selectReportTeams') }}</h2>
    <div>



      <TeamSelectField
          :show_add_new_team="true"
          :show_new_amalgamate="true"
          :exclude_team_list="teams_added"
          @team_selected="addTeam"
          @team_unselected="removeTeam"
          :force_hide_exclude_team_list="false"
          :allow_unselect="false"
          :show_hide_squad_box="true"
      />
      <template v-if="selectedSplitBaseTeam != undefined">
        <CreateSplitTeam
            :base-team="selectedSplitBaseTeam"
            @on_new_team_split="onSplitSuccessful"
            @on_cancel="onSplitCanceled"
        />
      </template>


      <template v-if="teams_added.length>0">
        {{ $t('teamSelect.selectedTeams') }}:<br>
        <ul class="selected-teams-list">
          <li v-for="team in teams_added.toSorted((a,b) => a.name.localeCompare(b.name))">
            <SplitButton
                @click="removeTeam(team)"
                :label = "team.name"
                :model="calculateSplitButtonModel(team)"
            />
          </li>
        </ul>
      </template>




    </div>
  </div>
</template>



<style scoped>

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
