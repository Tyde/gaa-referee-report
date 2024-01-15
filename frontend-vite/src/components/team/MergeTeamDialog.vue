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

watch(() => props.visible, (newValue) => {
  if (newValue) {
    teamsToMerge.value = []
  }
})

async function mergeTeams() {
  let mergeInto = props.selectedTeam
  let mergeFrom = teamsToMerge.value
  mergeTeamsOnServer(mergeInto, mergeFrom)
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
        <span v-if="teamsToMerge.length == 0"  class="inline-block w-16  bg-gray-200 rounded-xl  border-dashed border-[1px] border-black">&nbsp;</span>
        <span v-else class="inline-block bg-gray-200 rounded-lg">
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
              :hide_amalgamations="!amalgamationMode"
              @team_selected="team => teamsToMerge.push(team)"
              :only_amalgamations="amalgamationMode"
          />
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
