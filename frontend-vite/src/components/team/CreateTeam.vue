<script setup lang="ts">
import {computed, onMounted, ref, watch} from "vue";
import {createTeam} from "@/utils/api/teams_api";
import {useReportStore} from "@/utils/edit_report_store";
import type {Team} from "@/types/team_types";
import {useI18n} from "vue-i18n";
import {watchDebounced} from "@vueuse/core";

const store = useReportStore()
const props = defineProps<{
  rough_team_name?: string
}>()

const emit = defineEmits<{
  (e: 'on_new_team', team: Team): void,
  (e: 'on_cancel'): void
}>()

onMounted(() => {
  new_team_name.value = props.rough_team_name || ""
})
const new_team_name = ref("")
const is_loading = ref(false)

async function send_new_team_to_server() {
  if (new_team_name.value != "") {
    is_loading.value = true
    createTeam(new_team_name.value)
        .catch((error) => {
          store.newError(error)
        })
        .then((data) => {
          emit('on_new_team', data as Team)

        })
        .finally(() => {
          is_loading.value = false
        })

  }
}

class TeamNameWarning {
  ignore: boolean = false
  constructor(public message: string) {
  }
}
const {t} = useI18n()
class TeamNameTooShortWarning extends TeamNameWarning {
  constructor() {
    super(t('teamSelect.errorNameTooShort'))
  }
}

class TeamNameIndicatesAmalgamationWarning extends TeamNameWarning {
  constructor() {
    super(t('teamSelect.errorNameIndicatesAmalgamation'))
  }
}

class TeamNameIndicatesSquadWarning extends TeamNameWarning {
  constructor() {
    super(t('teamSelect.errorNameIndicatesSquad'))
  }
}


const invalidTeamNameWarnings = ref<TeamNameWarning[]>([])

watchDebounced(new_team_name,() => {
  const teamNameTooShort = new_team_name.value.length < 3;
  if (teamNameTooShort &&
      invalidTeamNameWarnings.value.findIndex(it => it instanceof TeamNameTooShortWarning) === -1) {
    invalidTeamNameWarnings.value.push(new TeamNameTooShortWarning())
  } else if (new_team_name.value.length >= 3) {
    invalidTeamNameWarnings.value = invalidTeamNameWarnings.value.filter(it => !(it instanceof TeamNameTooShortWarning))
  }
  const teamNameMatchesAmalgamation = new_team_name.value.match(/\//)
  if (teamNameMatchesAmalgamation &&
      invalidTeamNameWarnings.value.findIndex(it => it instanceof TeamNameIndicatesAmalgamationWarning) === -1) {
    invalidTeamNameWarnings.value.push(new TeamNameIndicatesAmalgamationWarning())
  } else if (!teamNameMatchesAmalgamation) {
    invalidTeamNameWarnings.value = invalidTeamNameWarnings.value.filter(it => !(it instanceof TeamNameIndicatesAmalgamationWarning))
  }

  const teamNameMatchesSquad = new_team_name.value.match(/\s[a-zA-Z]($|\s+$)/);
  if (teamNameMatchesSquad &&
      invalidTeamNameWarnings.value.findIndex(it => it instanceof TeamNameIndicatesSquadWarning) === -1) {
    invalidTeamNameWarnings.value.push(new TeamNameIndicatesSquadWarning())
  } else if (!teamNameMatchesSquad) {
    invalidTeamNameWarnings.value = invalidTeamNameWarnings.value.filter(it => !(it instanceof TeamNameIndicatesSquadWarning))
  }
}, {debounce: 200, maxWait:400})
</script>

<template>
  <div class="w-fill shadow-xl p-2 bg-gray-100 border-gray-500 rounded-xl flex-col text-center mb-4">
    <label for="new_team_name">{{ $t('teamSelect.newTeamName') }}</label><br>
    <InputText id="new_team_name" :disabled="is_loading" v-model="new_team_name"/>
    <br>
    <div v-if="invalidTeamNameWarnings.length > 0" class="text-red-500">
      <ul>
        <li v-for="warning in invalidTeamNameWarnings">{{ warning.message }}</li>
      </ul>
    </div>
    <div class="flex flex-row justify-center">
      <div class="m-2">
        <Button @click="send_new_team_to_server"
                :disabled="invalidTeamNameWarnings.length > 0"
        >{{ $t('general.submit') }}</Button>
      </div>
      <div class="m-2">
        <Button @click="emit('on_cancel')">{{ $t('general.cancel') }}</Button>
      </div>
    </div>
  </div>
</template>


<style scoped>

</style>
