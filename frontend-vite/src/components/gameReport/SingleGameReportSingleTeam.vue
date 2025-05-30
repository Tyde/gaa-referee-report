<script lang="ts" setup>

import {computed, ref} from "vue";
import InjuryEditor from "@/components/gameReport/InjuryEditor.vue";
import DisciplinaryEditor from "@/components/gameReport/DisciplinaryEditor.vue";
import {useReportStore} from "@/utils/edit_report_store";
import MobileDropdown from "@/components/util/MobileDropdown.vue";
import GAAScoreInput from "@/components/gameReport/GAAScoreInput.vue";
import TableDisciplinaryEditor from "@/components/gameReport/TableDisciplinaryEditor.vue";


const store = useReportStore()
const props = defineProps<{
  isTeamA: boolean,
}>()


const displayDisciplinary = ref(false)
const displayDisciplinaryTable = ref(false)
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

function stripRuleCardsFromDescription(description?: string) {
  return description?.replace(/(CAUTION:|ORDER OFF:|BLACK CARD:)/gm, '')
}



</script>

<template>
  <div class="grid grid-cols-4">


    <div class="md:block hidden col-span-4">
    <Select
        v-model="currentSingleTeamGameReport.team"
        :class="{
                'to-be-filled':currentSingleTeamGameReport.team===undefined
            }"
        :filter="true"
        :options="store.report.selectedTeams"
        :show-clear="true"
        class="w-full"
        option-label="name"
        :placeholder="$t('gameReport.selectTeam')"
        data-key="id"
        :reset-filter-on-hide="true"
    >
    </Select>
    </div>

    <MobileDropdown
        :options="store.report.selectedTeams"
        v-model="currentSingleTeamGameReport.team"
        class="col-span-4 block md:hidden"
        option-label="name"
        :placeholder="$t('gameReport.selectTeam')"
        :filter-fields="['name']"
        data-key="id"
    />
    <template v-if="currentSingleTeamGameReport.team">

      <div class="flex justify-center p-2">
        <GAAScoreInput
            v-model:goals="currentSingleTeamGameReport.goals"
            v-model:points="currentSingleTeamGameReport.points"
            />
      </div>
      <div class="text-xl col-span-2 p-2 flex flex-col justify-center place-content-center">
        <div class="flex-shrink">{{ $t('gameReport.total') }}: {{
            (currentSingleTeamGameReport?.goals ?? 0) * 3 + (currentSingleTeamGameReport?.points ?? 0)
          }}
        </div>
      </div>


      <div class="col-span-2 p-1 flex flex-col">
        <Button
            :disabled="!store.selectedGameReportPassesMinimalRequirements"
            @click="openDisciplinaryDialog"
            class="flex-shrink"
        >
          {{ $t('gameReport.editDisciplinaryActions') }} ({{ currentSingleTeamGameReport.disciplinaryActions.length - 1 }})
        </Button>
        <div class="text-sm flex flex-col">
          <div
              v-for="action in currentSingleTeamGameReport.disciplinaryActions"
              :key="action.id"
          >
            <template v-if="action.id">
              <div v-if="action.rule?.isCaution" class="rule-card card-yellow"></div>
              <div v-if="action.rule?.isRed" class="rule-card card-red"></div>
              <div v-if="action.rule?.isBlack" class="rule-card card-black"></div>
              <div v-if="action.redCardIssued" class="rule-card-clear-none card-red"></div>
              <template v-if="action.number && action.number >= 0">{{ action.number }}</template>
              <template v-else-if="action.forTeamOfficial"> T.O. </template> - {{ action.firstName }} {{ action.lastName }} -
              {{ stripRuleCardsFromDescription(action.rule?.description)?.substring(0, 25) }} ...
            </template>
          </div>
        </div>
      </div>

      <div class="col-span-2 p-1 flex flex-col">
        <Button
            :disabled="!store.selectedGameReportPassesMinimalRequirements"
            class="flex-shrink"
            @click="openInjuryDialog"
        >
          {{ $t('gameReport.editInjuries') }} ({{ currentSingleTeamGameReport.injuries.length - 1 }})
        </Button>
        <div class="text-sm flex flex-col">
          <div
              v-for="injury in currentSingleTeamGameReport.injuries"
              :key="injury.id"
          >
            <template v-if="injury.id">
              {{ injury.firstName }} {{ injury.lastName }} - {{ injury.details.substring(0, 25) }} ...
            </template>
          </div>
        </div>
      </div>
      <Button v-if="false"
        @click="displayDisciplinaryTable = true">Open Table Dis</Button>


    </template>

    <DisciplinaryEditor
        :is-team-a="props.isTeamA"
        v-model:visible="displayDisciplinary"
        v-if="currentSingleTeamGameReport.team"
    />

    <TableDisciplinaryEditor :is-team-a="props.isTeamA"
                             v-model:visible="displayDisciplinaryTable"
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


@media (min-width: 1024px) {
  .score-button-base {
    padding: 0px !important;
    margin: 0px !important;
  }
}
</style>
