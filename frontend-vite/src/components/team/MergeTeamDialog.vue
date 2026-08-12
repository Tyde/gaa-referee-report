<script setup lang="ts">

import {useAdminStore} from "@/utils/admin_store";
import type {Team} from "@/types/team_types";
import {computed, watch} from "vue";
import TeamSelectField from "@/components/team/TeamSelectField.vue";
import {ref} from "vue";
import {mergeTeamsOnServer} from "@/utils/api/teams_api";

const props = defineProps<{
  visible: boolean,
  selectedTeam: Team,
}>()

const emits = defineEmits<{
  (e: 'update:visible', value: boolean): void,
  (e: 'teamMerged', mergeInto: Team): void
}>()

const localVisible = computed({
  get() {
    return props.visible
  },
  set(newValue) {
    emits('update:visible', newValue)
  }
})

const amalgamationMode = computed(() => props.selectedTeam.isAmalgamation)
const store = useAdminStore()

const teamsToMerge = ref<Team[]>([])
const changeDate = ref<Date>(new Date())
const aliasKeep = ref<Record<number, boolean>>({})
const aliasText = ref<Record<number, string>>({})

function trackAlias(team: Team) {
  aliasKeep.value[team.id] = true
  aliasText.value[team.id] = team.name
}

function untrackAlias(team: Team) {
  delete aliasKeep.value[team.id]
  delete aliasText.value[team.id]
}

watch(() => props.visible, (newValue) => {
  if (newValue) {
    teamsToMerge.value = []
    changeDate.value = new Date()
    aliasKeep.value = {}
    aliasText.value = {}
  }
})

async function mergeTeams() {
  const mergeInto = props.selectedTeam
  const mergeFrom = teamsToMerge.value
  const changeDateIso = changeDate.value ? new Date(changeDate.value).toISOString().slice(0, 10) : undefined
  const aliasesToCreate: Record<string, string> = {}
  for (const team of mergeFrom) {
    const alias = aliasText.value[team.id]?.trim()
    if (aliasKeep.value[team.id] && alias) {
      aliasesToCreate[String(team.id)] = alias
    }
  }
  mergeTeamsOnServer(mergeInto, mergeFrom, changeDateIso, aliasesToCreate)
      .catch(reason => store.newError(reason))
      .then(resTeam => {
        if (resTeam) {
          emits('teamMerged', resTeam)
        }
      })
}

const excludeTeamList = computed(() => {
  return [props.selectedTeam, ...teamsToMerge.value]
})
</script>

<template>
  <Dialog
      v-model:visible="localVisible"
      :closable="true"
      :close-on-escape="true"
      header="Merge Teams"
      :modal="true"
      :pt="{ root: { class: 'merge-diag'}}"
  >
    <div class="flex flex-col">
      <div>
        Merge
        <span v-if="teamsToMerge.length == 0"  class="inline-block w-16  bg-surface-500 rounded-xl  border-dashed border-[1px] border-black">&nbsp;</span>
        <span v-else class="inline-block bg-surface-500 rounded-lg p-1">
          {{teamsToMerge.map(team => team.name).join(", ")}}
        </span>
        into team <b>{{selectedTeam.name}}</b>
      </div>
      <div class="flex flex-col align-middle">
        <div class="m-2">
          <TeamSelectField
              :show_new_amalgamate="false"
              :show_add_new_team="false"
              :exclude_team_list="excludeTeamList"
              :force_hide_exclude_team_list="true"
              :allow_unselect="true"
              @team_selected="team => { teamsToMerge.push(team); trackAlias(team) }"
              @team_unselected="team => { teamsToMerge = teamsToMerge.filter(it => it.id !== team.id); untrackAlias(team) }"
              :show_hide_squad_box="false"
              :show_teams="!amalgamationMode"
              :show_amalgamations="amalgamationMode"
              :show_squads="amalgamationMode"
          />
        </div>
        <div v-if="teamsToMerge.length" class="flex flex-col gap-2 m-2">
          <div class="text-sm opacity-80">
            Keep the merged names as alternative spellings so referees can still find this team:
          </div>
          <div v-for="team in teamsToMerge" :key="team.id" class="flex flex-row items-center gap-2">
            <Checkbox v-model="aliasKeep[team.id]" :input-id="'keep_alias_' + team.id" binary/>
            <label :for="'keep_alias_' + team.id">Keep "{{ team.name }}" as an alternative spelling</label>
            <InputText
                v-model="aliasText[team.id]"
                :id="'alias_text_' + team.id"
                :aria-label="'Alternative spelling for ' + team.name"
                :disabled="!aliasKeep[team.id]"
                class="flex-1"
            />
          </div>
        </div>
        <div class="flex flex-row items-center gap-2 m-2">
          <label>Date of change</label>
          <DatePicker v-model="changeDate" dateFormat="yy-mm-dd" showIcon iconDisplay="input" class="w-full"/>
        </div>
        <div class="flex flex-col justify-end m-2">
          <Button label="Merge" @click="mergeTeams"/>
        </div>
      </div>
    </div>
  </Dialog>

</template>

<style scoped>

</style>

<style>
.merge-diag {
  @apply w-1/3;
}
</style>
