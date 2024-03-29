<script setup lang="ts">

import {useReportStore} from "@/utils/edit_report_store";
import type {Team} from "@/types/team_types";
import {computed, ref, watch, watchEffect} from "vue";
import TeamSelectField from "@/components/team/TeamSelectField.vue";
import {createAmalgamationOnServer} from "@/utils/api/teams_api";

const store = useReportStore();

const emit = defineEmits<{
  (e: 'on_new_team_split', teams: Team[]): void,
  (e: 'on_cancel'): void
}>()

const props = defineProps<{
  baseTeam:Team
}>()

const numSplitTeams = ref(2)
const isLoading = ref(false)
const modifyNames = ref(false)
const splitTeamDrafts = ref<{suffix:string, internalId:number}[]>()

const anyTeamNameExists = computed(() => {
  return splitTeamDrafts.value?.some(it => teamNameAlreadyExists(it.suffix)) ?? false
})
function teamNameAlreadyExists(suffix: string) {
  console.log("Checking for team name: ", props.baseTeam+ " " + suffix)
  return store.publicStore.teams.filter(it => it.name === props.baseTeam.name+ " " + suffix).length > 0
}
watchEffect(() => {
  if (props.baseTeam && !modifyNames.value) {
    splitTeamDrafts.value = []
    for (let i = 0; i < numSplitTeams.value; i++) {
      splitTeamDrafts.value.push(
          {
            suffix: String.fromCharCode(i + 65),
            internalId: i
          }
      )
    }
  } else if (props.baseTeam && modifyNames.value) {
    for (let i = splitTeamDrafts.value?.length ?? 0; i < numSplitTeams.value; i++) {
      splitTeamDrafts.value?.push(
          {
            suffix: String.fromCharCode(i + 65),
            internalId: i
          }
      )
    }
    if(splitTeamDrafts.value?.length??0 > numSplitTeams.value) {
      splitTeamDrafts.value = splitTeamDrafts.value?.slice(0, numSplitTeams.value)
    }
  } else {
    return []
  }
})


function createSplitTeams() {
  isLoading.value = true


  let promises = splitTeamDrafts.value?.map((draft) => {
    return createAmalgamationOnServer(
        props.baseTeam.name + " "+ draft.suffix,
        [props.baseTeam]
    ).catch((err) => {
      store.newError(err)
    })
  }) ?? []
  let singlePromise =  Promise.all(promises)
  singlePromise.catch((err) => {
    store.newError(err)
  }).then((teams) => {
    let allTeamsDefined = teams?.every(it => it !== undefined)
    if(allTeamsDefined) {
      let allTeams = teams as Team[]
      emit('on_new_team_split', allTeams)
    } else {
      store.newError("SAt least one of the teams could not be created.")
    }
  }).finally(() => isLoading.value = false)
}

function removeDraftAtIndex(index: number) {
  modifyNames.value = true
  splitTeamDrafts.value?.splice(index, 1)
  numSplitTeams.value = splitTeamDrafts.value?.length ?? 0
}

</script>

<template>
  <div>
    <h3>{{ $t('teamSelect.splitTeam.title') }}</h3>
    <div class="flex flex-col items-center">
      <div>
        {{ $t('teamSelect.splitTeam.infoSelected') }}: {{ baseTeam.name }}
      </div>
      <div class="content-center">
        {{$t('teamSelect.splitTeam.selectNumberOf')}}:<br>
        <InputNumber
            v-model="numSplitTeams"
            :step="1"
            :max="4"
            :min="1"
            showButtons buttonLayout="horizontal"
            :pt="{
              input: {
                class: 'w-12'
              }
            }"
        />
      </div>
      <div class="mt-2">
        {{$t('teamSelect.splitTeam.wouldCreateFollowing')}}:
        <ul>
          <li class="m-1 ml-3" v-for="(draft,index) in splitTeamDrafts" :key="draft.internalId">
            <template v-if="!modifyNames">{{baseTeam.name}} {{ draft.suffix }}</template>
            <template v-else>
              {{baseTeam.name}} <InputText v-model="draft.suffix"/>
            </template>
            <span
                v-if="teamNameAlreadyExists(draft.suffix)"
                class="bg-red-600 text-white p-1 ml-2 rounded font-bold hover:cursor-pointer"
                @click="removeDraftAtIndex(index)"
            >{{$t('teamSelect.splitTeam.nameExists')}} <i class="fas fa-times"></i>
            </span>
          </li>
        </ul>
        <Checkbox v-model="modifyNames" input-id="modify_names" :binary="true" />
        <label for="modify_names">{{$t('teamSelect.splitTeam.modifyNames')}}</label>
      </div>

    </div>
    <div class="flex flex-row justify-end">
      <div class="m-2">
        <Button
            class="p-button-rounded"
            @click="createSplitTeams"
            :disabled="baseTeam === undefined || anyTeamNameExists"
        >{{ $t('general.submit') }}</Button>
      </div>
      <div class="m-2">
        <Button class="p-button-secondary p-button-rounded" @click="emit('on_cancel')">{{ $t('general.cancel') }}</Button>
      </div>
    </div>
  </div>
</template>

<style scoped>
h3 {
  @apply text-xl;
  @apply font-bold;
  @apply mt-2;
  @apply mb-2;
  @apply content-center;
}
</style>
