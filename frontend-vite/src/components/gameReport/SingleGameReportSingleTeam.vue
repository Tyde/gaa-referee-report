<script lang="ts" setup>

import type {DisciplinaryAction, Injury, Rule, SingleTeamGameReport, Team} from "@/types";
import {computed, onMounted, onUpdated, ref, watch} from "vue";
import InjuryEditor from "@/components/gameReport/InjuryEditor.vue";
import DisciplinaryEditor from "@/components/gameReport/DisciplinaryEditor.vue";

const props = defineProps<{
  modelValue: SingleTeamGameReport,
  selectedTeams: Array<Team>,
  rules: Array<Rule>,
  reportId?: number,
  reportPassesMinimalRequirements: boolean,
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: SingleTeamGameReport): void,
  (e: 'triggerUpdate'): void
}>()

const displayDisciplinary = ref(false)
const displayInjuries = ref(false)


function openDisciplinaryDialog() {
  emit('triggerUpdate')
  displayDisciplinary.value = true;
}

function closeDisciplinaryDialog() {
  displayDisciplinary.value = false;
}

function openInjuryDialog() {
  emit('triggerUpdate')
  displayInjuries.value = true;
}

function closeInjuryDialog() {
  displayInjuries.value = false;
}

</script>

<template>
  <div class="grid grid-cols-4">

    <Dropdown
        v-model="modelValue.team"
        :class="{
                'to-be-filled':modelValue.team===undefined
            }"
        :filter="true"
        :options="selectedTeams"
        :show-clear="true"
        class="col-span-4"
        option-label="name"
        placeholder="Select a Team"
    >
    </Dropdown>
    <template v-if="modelValue.team">
      <div class="flex justify-center p-2">
        <div>
          <label for="goals_team">Goals</label><br>
          <InputNumber
              id="goals_team"
              v-model="modelValue.goals"
              :step="1"
              buttonLayout="vertical" class="w-16 text-sm"
              decrement-button-class="score-button-base"
              increment-button-class="score-button-base"
              input-class="text-sm"
              showButtons
              type="text"
          />
        </div>
      </div>
      <div class="flex justify-center p-2">
        <div>
          <label for="points_team">Points</label><br>
          <InputNumber
              id="points_team"
              v-model="modelValue.points"
              :step="1"
              buttonLayout="vertical" class="w-16 text-sm"
              decrement-button-class="score-button-base"
              increment-button-class="score-button-base"
              input-class="text-sm"
              showButtons
              type="text"

          />
        </div>
      </div>
      <div class="text-xl col-span-2 p-2 flex flex-col justify-center place-content-center">
        <div class="flex-shrink">Total: {{ modelValue.goals * 3 + modelValue.points }}</div>
      </div>


      <div class="col-span-2 p-1 flex">
        <Button
            :disabled="!props.reportPassesMinimalRequirements"
            @click="openDisciplinaryDialog"
            class="flex-grow"
        >
          Edit Disciplinary Actions ({{ modelValue.disciplinaryActions.length - 1 }})
        </Button>
      </div>

      <div class="col-span-2 p-1 flex">
        <Button
            :disabled="!props.reportPassesMinimalRequirements"
            class="flex-grow"
            @click="openInjuryDialog"
        >
          Edit Injuries ({{ modelValue.injuries.length - 1 }})
        </Button>
      </div>


    </template>
    <DisciplinaryEditor
        v-model="modelValue.disciplinaryActions"
        v-model:visible="displayDisciplinary"
        :game-report-id="reportId"
        :rules="rules"
        v-if="modelValue.team"
        :team="modelValue.team"

    />
    <InjuryEditor
        v-model="modelValue.injuries"
        v-model:visible="displayInjuries"
        :game-report-id="reportId"
        :team="modelValue.team"
    />
  </div>


</template>


<style>
.p-disciplinary {
  font-size: 1rem !important;
}

.dropdown-disciplinary {
  width: 22rem;
  max-width: 22rem;
}

.rule-card {
  margin-top: .25rem;
  margin-right: .3rem;
  float: left;
  height: 1rem;
  width: 0.5rem;
  clear: both;
}

.card-yellow {
  background-color: gold;
}

.card-black {
  background-color: black;
}

.card-red {
  background-color: red;
}

.score-button-base {
  padding: 0px !important;
  margin: 0px !important;
}
</style>