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
watchEffect(() => {
  if (props.baseTeam) {
    splitTeamDrafts.value = []
    for (let i = 0; i < numSplitTeams.value; i++) {
      splitTeamDrafts.value.push(
          {
            suffix: String.fromCharCode(i + 65),
            internalId: i
          }
      )
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
    )
  }) ?? []
  let singlePromise =  Promise.all(promises)
  singlePromise.catch((err) => {

    store.newError(err)
  }).then((teams) => {
    if(teams) {
      emit('on_new_team_split', teams)
    } else {
      store.newError("Server returned void")
    }
  }).finally(() => isLoading.value = false)
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
          <li v-for="draft in splitTeamDrafts" :key="draft.internalId">
            <template v-if="!modifyNames">{{baseTeam.name}} {{ draft.suffix }}</template>
            <template v-else>
              {{baseTeam.name}} <InputText v-model="draft.suffix"/>
            </template>
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
            :disabled="baseTeam === undefined"
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
