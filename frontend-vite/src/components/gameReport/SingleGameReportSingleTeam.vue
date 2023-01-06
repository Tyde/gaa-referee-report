<script lang="ts" setup>

import {computed, ref} from "vue";
import InjuryEditor from "@/components/gameReport/InjuryEditor.vue";
import DisciplinaryEditor from "@/components/gameReport/DisciplinaryEditor.vue";
import {useReportStore} from "@/utils/edit_report_store";


const store = useReportStore()
const props = defineProps<{
  isTeamA: boolean,
}>()



const displayDisciplinary = ref(false)
const displayInjuries = ref(false)


const currentSingleTeamGameReport = computed(() => {
  if (props.isTeamA) {
    return store.selectedGameReport!!.teamAReport
  } else {
    return store.selectedGameReport!!.teamBReport
  }
})

function openDisciplinaryDialog() {
  if (store.selectedGameReport) {
    store.sendGameReport(store.selectedGameReport)
  }
  displayDisciplinary.value = true;
}

function closeDisciplinaryDialog() {
  displayDisciplinary.value = false;
}

function openInjuryDialog() {
  if (store.selectedGameReport) {
    store.sendGameReport(store.selectedGameReport)
  }
  displayInjuries.value = true;
}

function closeInjuryDialog() {
  displayInjuries.value = false;
}


</script>

<template>
  <div class="grid grid-cols-4">

    <Dropdown
        v-model="currentSingleTeamGameReport.team"
        :class="{
                'to-be-filled':currentSingleTeamGameReport.team===undefined
            }"
        :filter="true"
        :options="store.report.selectedTeams"
        :show-clear="true"
        class="col-span-4"
        option-label="name"
        placeholder="Select a Team"
    >
    </Dropdown>
    <template v-if="currentSingleTeamGameReport.team">
      <div class="flex justify-center p-2">
        <div>
          <label for="goals_team">Goals</label><br>
          <InputNumber
              id="goals_team"
              v-model="currentSingleTeamGameReport.goals"
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
              v-model="currentSingleTeamGameReport.points"
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
        <div class="flex-shrink">Total: {{
            currentSingleTeamGameReport.goals * 3 + currentSingleTeamGameReport.points
          }}
        </div>
      </div>


      <div class="col-span-2 p-1 flex">
        <Button
            :disabled="!store.selectedGameReportPassesMinimalRequirements"
            @click="openDisciplinaryDialog"
            class="flex-grow"
        >
          Edit Disciplinary Actions ({{ currentSingleTeamGameReport.disciplinaryActions.length - 1 }})
        </Button>
      </div>

      <div class="col-span-2 p-1 flex">
        <Button
            :disabled="!store.selectedGameReportPassesMinimalRequirements"
            class="flex-grow"
            @click="openInjuryDialog"
        >
          Edit Injuries ({{ currentSingleTeamGameReport.injuries.length - 1 }})
        </Button>
      </div>


    </template>

    <DisciplinaryEditor
        :is-team-a="props.isTeamA"
        v-model:visible="displayDisciplinary"
        v-if="currentSingleTeamGameReport.team"
    />

    <InjuryEditor
        :is-team-a="props.isTeamA"
        v-model:visible="displayInjuries"
        v-if="currentSingleTeamGameReport.team"
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