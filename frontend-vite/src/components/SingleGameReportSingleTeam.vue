<script lang="ts" setup>

import type {DisciplinaryAction, Injury, Rule, SingleTeamGameReport, Team} from "@/types";
import {computed, onMounted, onUpdated, ref, watch} from "vue";
import InjuryEditor from "@/components/InjuryEditor.vue";
import DisciplinaryEditor from "@/components/DisciplinaryEditor.vue";

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
        :filter="true"
        :options="selectedTeams"
        :show-clear="true"
        class="col-span-4"
        option-label="name"
        placeholder="Select a Team"
        :class="{
                'to-be-filled':modelValue.team==undefined
            }"
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
              buttonLayout="vertical" class="w-16 text-sm" decrement-button-class="score-button-base"
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
              buttonLayout="vertical" class="w-16 text-sm" decrement-button-class="score-button-base"
              increment-button-class="score-button-base"
              input-class="text-sm"
              showButtons
              type="text"

          />
        </div>
      </div>
      <div class="text-lg col-span-2 p-2">
        Total: {{ modelValue.goals * 3 + modelValue.points }}
      </div>

      <Button
          class="col-span-2 p-2"
          @click="openDisciplinaryDialog"
          :disabled="!props.reportPassesMinimalRequirements"
      >
        Edit Disciplinary Actions ({{ modelValue.disciplinaryActions.length - 1 }})
      </Button>
      <Button
          class="col-span-2 p-2"
          @click="openInjuryDialog"
          :disabled="!props.reportPassesMinimalRequirements"
      >
        Edit Injuries ({{ modelValue.injuries.length - 1 }})
      </Button>

    </template>
    <DisciplinaryEditor
        v-model:visible="displayDisciplinary"
        v-model="modelValue.disciplinaryActions"
        :team="modelValue.team"
        :rules="rules"
        :game-report-id="reportId"

    />
    <InjuryEditor
        v-model:visible="displayInjuries"
        v-model="modelValue.injuries"
        :team="modelValue.team"
        :game-report-id="reportId"
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
  margin-top: .2rem;
  margin-right: .2rem;
  float: left;
  height: 0.5rem;
  width: 0.3rem;
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