<script setup lang="ts">

import type {Team} from "@/types/team_types";
import TeamSelectField from "@/components/team/TeamSelectField.vue";
import {computed, onMounted, ref, watch} from "vue";
import {useReportStore} from "@/utils/edit_report_store";
import {editTeamOnServer, mergeTeamsOnServer} from "@/utils/api/teams_api";
import {useAdminStore} from "@/utils/admin_store";

const localVisible = defineModel('visible')
const props = defineProps<{
  selectedTeam: Team,
}>()

const emits = defineEmits<{
  (e: 'conversionDone', mergeInto: Team): void
}>()

const excludeTeamList = computed(() => {
  if(!props.selectedTeam) return []
  return [props.selectedTeam]
})

const store = useAdminStore()

const noSquadAmalgamationExcludes = computed(() => {
  return store.publicStore.teams.filter(it => it.isAmalgamation && it.amalgamationTeams?.length && it.amalgamationTeams?.length > 1)
})

const baseTeamForNewSquad = ref<Team>()
const conversionTarget = ref<Team>()

function onConversionDone(resultTeam:Team) {
  store.publicStore.loadTeams()
  localVisible.value = false
  emits('conversionDone', resultTeam)
}

async function convertTeam() {
  if (baseTeamForNewSquad.value && props.selectedTeam) {
    const teamCopy = JSON.parse(JSON.stringify(props.selectedTeam))
    teamCopy.isAmalgamation = true
    teamCopy.amalgamationTeams = [baseTeamForNewSquad.value]
    editTeamOnServer(teamCopy)
        .then((resTeam) => {
          onConversionDone(resTeam)
        })
        .catch(reason => store.newError(reason))
  }
}

async function mergeIntoSquad() {
  if (conversionTarget.value) {
    mergeTeamsOnServer(
        conversionTarget.value,
        [props.selectedTeam]
    )
        .then((resTeam) => {
          onConversionDone(resTeam)
        })
        .catch(reason => store.newError(reason)
    )
  }
}

const existingSquadListForWarning = computed(() => {
  if (baseTeamForNewSquad.value == null) return []
  return store.publicStore.teams.filter(
      it => it.isAmalgamation &&
          it.amalgamationTeams?.length &&
          it.amalgamationTeams?.length == 1 &&
          it.amalgamationTeams[0].id == baseTeamForNewSquad.value?.id
  ).sort((a, b) => a.name.localeCompare(b.name))
})

const tabActiveIndex = ref(0)
function switchModeAsSquadAlreadyExists(chosenSquad: Team) {
  baseTeamForNewSquad.value = undefined
  conversionTarget.value = chosenSquad
  tabActiveIndex.value = 1
}


onMounted(() => {
  tabActiveIndex.value = 0
})
</script>

<template>
  <Dialog
      v-model:visible="localVisible"
      :closable="true"
      :close-on-escape="true"

      :pt="{ root: { class: 'merge-diag'}}"
      :modal="true"
  >
    <TabView v-model:active-index="tabActiveIndex">
      <TabPanel header="Convert to new Squad">
        <div class="flex flex-col">

          <div>Convert
            <span class="font-bold" v-if="selectedTeam">{{ selectedTeam.name }}</span>
            into squad with base team:</div>
          <div class="align-middle" v-if="!baseTeamForNewSquad">
            <TeamSelectField
                :show_new_amalgamate="false"
                :show_add_new_team="false"
                :exclude_team_list="excludeTeamList"
                :force_hide_exclude_team_list="true"
                :allow_unselect="false"
                :hide_amalgamations="true"
                @team_selected="team => baseTeamForNewSquad = team"
                @team_unselected="team => baseTeamForNewSquad = undefined"
                :only_amalgamations="false"
                :show_hide_squad_box="false"
            />
          </div>
          <template v-else>
            <div
                class="p-2 bg-slate-100 font-bold rounded-lg ml-8 mr-8 mb-4 hover:cursor-pointer hover:bg-red-400"
                @click="baseTeamForNewSquad = undefined"
            >{{baseTeamForNewSquad?.name}}
            </div>
            <div v-if="existingSquadListForWarning.length > 0">
              <div class="font-bold text-red-600">Warning: This team already has these existing squads. Click on one to merge:</div>
              <div class="flex flex-col items-center">
                  <div
                      v-for="team in existingSquadListForWarning"
                      :key="team.id"
                      class="bg-gray-300 rounded-xl m-1 p-2 text-sm"
                      @click="switchModeAsSquadAlreadyExists(team)"
                  >{{ team.name }}</div>
              </div>
            </div>
          </template>


          <div class="flex flex-col justify-end m-2">
            <Button label="Convert" @click="convertTeam"/>
          </div>
        </div></TabPanel>
      <TabPanel header="Merge with existing Squad">
        <div class="flex flex-col">

          <div>Merge
            <span class="font-bold" v-if="selectedTeam">{{ selectedTeam.name }}</span>
            into squad:</div>
          <div class="align-middle" v-if="!conversionTarget">
            <TeamSelectField
                :show_new_amalgamate="false"
                :show_add_new_team="false"
                :exclude_team_list="noSquadAmalgamationExcludes"
                :force_hide_exclude_team_list="true"
                :allow_unselect="false"
                :hide_amalgamations="false"
                @team_selected="team => conversionTarget = team"
                @team_unselected="team => conversionTarget = undefined"
                :only_amalgamations="true"
                :show_hide_squad_box="false"
            />
          </div>
          <div
              v-else
              class="p-2 bg-slate-100 font-bold rounded-lg ml-8 mr-8 mb-4 hover:cursor-pointer hover:bg-red-400"
              @click="conversionTarget = undefined"
          >{{conversionTarget?.name}}
          </div>

          <div class="flex flex-col justify-end m-2">
            <Button label="Merge" @click="mergeIntoSquad"/>
          </div>
        </div>
      </TabPanel>
    </TabView>

  </Dialog>
</template>

<style scoped>
.merge-diag {
  @apply w-1/3;
}
</style>
