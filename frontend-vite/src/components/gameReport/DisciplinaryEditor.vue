<script lang="ts" setup>
import {computed, onMounted, watch} from "vue";
import {deleteDisciplinaryActionOnServer, disciplinaryActionIsBlank,} from "@/utils/api/disciplinary_action_api";
import {useReportStore} from "@/utils/edit_report_store";
import type {DisciplinaryAction} from "@/types/game_report_types";
import {useI18n} from "vue-i18n";
import MobileDropdown from "@/components/util/MobileDropdown.vue";
import type {Rule} from "@/types/rules_types";

const store = useReportStore()
const props = defineProps<{
  visible: boolean,
  isTeamA: boolean,
//  modelValue: Array<DisciplinaryAction>,
//  team: Team,
//  gameReportId?: number,
//  rules: Array<Rule>,
}>()

const selectedDisciplinaryActions = computed(() => {
  if (props.isTeamA) {
    return store.selectedGameReport!!.teamAReport.disciplinaryActions
  } else {
    return store.selectedGameReport!!.teamBReport.disciplinaryActions
  }
})

const selectedTeam = computed(() => {
  if (props.isTeamA) {
    return store.selectedGameReport!!.teamAReport.team
  } else {
    return store.selectedGameReport!!.teamBReport.team
  }
})

const emits = defineEmits<{
  (e: 'update:visible', value: boolean): void,
}>()


const localVisible = computed({
  get() {
    return props.visible
  },
  set(newValue) {
    emits('update:visible', newValue)
  }
})


async function uploadActionsToServer() {
  if (store.selectedGameReport?.id) {
    await store.waitForAllTransfersDone()
    for (let action of selectedDisciplinaryActions.value) {
      store.sendDisciplinaryAction(action, store.selectedGameReport, true)
          .catch((error) => {
            store.newError(error)
          })
    }
  } else {
    console.log("No report id - deferring upload")
  }
}

function closeDialog() {
  //('update:modelValue', props.modelValue ?? [])
  emits('update:visible', false)
  uploadActionsToServer()
      .catch((error) => {
        store.newError(error)
      })
}

const {t} = useI18n()

const disciplinaryDialogTitle = computed(() => {
  return t("gameReport.disciplinaryActionTitle") + " " + selectedTeam.value?.name
})


watch(() => selectedDisciplinaryActions, () => {
  generateEmptydAFields()
}, {deep: true, immediate: true})

function generateEmptydAFields() {
  let newActions = selectedDisciplinaryActions.value

  if (newActions.length == 0) {
    addEmptyDisciplinaryAction()
  } else if (!disciplinaryActionIsBlank(newActions[newActions.length - 1])) {
    addEmptyDisciplinaryAction()
  }
}


function addEmptyDisciplinaryAction() {
  if (selectedTeam.value) {
    selectedDisciplinaryActions.value.push({
      id: undefined,
      team: selectedTeam.value,
      firstName: "",
      lastName: "",
      number: undefined,
      rule: undefined,
      details: "",
      redCardIssued: false,
      forTeamOfficial: false,
    })
  }
}

function deleteDAction(dAction: DisciplinaryAction) {
  if (dAction.id) {
    deleteDisciplinaryActionOnServer(dAction)
        .catch((error) => {
          store.newError(error)
        })
  }
  selectedDisciplinaryActions.value.splice(selectedDisciplinaryActions.value.indexOf(dAction), 1)
}

const i8n = useI18n()
function localizedDescription(rule: Rule) {
  switch (i8n.locale.value) {
    case "en":
      return rule.description
    case "fr":
      return stringOrDefault(rule.descriptionFr, rule.description)
    case "es":
      return stringOrDefault(rule.descriptionEs, rule.description)
    case "de":
      return stringOrDefault(rule.descriptionDe, rule.description)
    default:
      console.log("Unknown locale", i8n.locale.value)
      return rule.description
  }
}

const ruleFilterFields = computed(() => {
  const fields = ['description'];
  switch (i8n.locale.value) {
    case "fr":
      fields.unshift('descriptionFr');
      break;
    case "es":
      fields.unshift('descriptionEs');
      break;
    case "de":
      fields.unshift('descriptionDe');
      break;
  }
  return fields;
})


function stringOrDefault(s: string | undefined, def: string) {
  if (s && s.length > 0) {
    return s
  } else {
    return def
  }
}

const filteredRules = computed(() => {
  return store.publicStore.rules.filter((rule) => {
    return rule.isDisabled == false && rule.code == store.report.gameCode.id
  })
})
/*
onUpdated(() => {
  console.log("DA model value:",props.modelValue)
  generateEmptydAFields()
})*/

onMounted(() => {
  generateEmptydAFields()
})
</script>
<template>
  <Dialog
      v-model:visible="localVisible"
      :closable="false"
      :close-on-escape="false"
      :header="disciplinaryDialogTitle"
      :modal="true"
  >
    <div v-for="dAction in selectedDisciplinaryActions">
      <div class="flex flex-row flex-wrap">
        <div class="p-2 w-1/2 md:w-auto">
          <InputText
              v-model="dAction.firstName"
              class="w-full md:w-52"
              :placeholder="$t('gameReport.player.firstName')"
          />
        </div>
        <div class="p-2 w-1/2 md:w-auto">
          <InputText
              v-model="dAction.lastName"
              class="w-full md:w-52"
              :placeholder="$t('gameReport.player.lastName')"
          />
        </div>

        <div class="p-2">
          <InputNumber
              v-model="dAction.number"
              class="w-20"
              input-class="w-20"
              :placeholder="$t('gameReport.player.number')"
              :disabled="dAction.forTeamOfficial"
              :input-props="{inputmode: 'numeric'}"
          />
        </div>
        <div class="p-2 flex flex-col">
          <div class="flex flex-row items-center h-12">
            <div class="mr-2">T.O.?</div>
            <Checkbox
                v-model="dAction.forTeamOfficial"
                :binary="true"
            />
          </div>
        </div>
        <!-- Rule: {{dAction.rule?.id}}-->
        <div class="w-full md:w-auto">


          <div class="hidden md:flex md:flex-col">
            <Dropdown
                v-model="dAction.rule"
                :options="filteredRules"
                :show-clear="true"
                class="dropdown-disciplinary m-2"
                input-class="dropdown-disciplinary"
                :placeholder="$t('gameReport.rule')"
                :filter="true"
                :filter-fields="ruleFilterFields"
                :pt="{item: {class: 'whitespace-break-spaces md:whitespace-nowrap'}}"
            >
              <template #value="slotProps">
                <div v-if="slotProps.value" class="p-disciplinary ">
                  <div v-if="slotProps.value.isCaution" class="rule-card card-yellow"></div>
                  <div v-if="slotProps.value.isBlack" class="rule-card card-black"></div>
                  <div v-if="slotProps.value.isRed" class="rule-card card-red"></div>
                  {{ localizedDescription(slotProps.value).substring(0, 20) }} ...
                </div>
                <span v-else class="p-disciplinary">{{ slotProps.placeholder }}</span>
              </template>
              <template #option="slotProps">

                <div>
                  <div v-if="slotProps.option.isCaution" class="rule-card card-yellow"></div>
                  <div v-if="slotProps.option.isBlack" class="rule-card card-black"></div>
                  <div v-if="slotProps.option.isRed" class="rule-card card-red"></div>
                  {{ localizedDescription(slotProps.option) }}
                </div>

              </template>
            </Dropdown>
            <div v-if="dAction.rule" class="w-[22rem] p-2">
              {{ localizedDescription(dAction.rule) }}
            </div>
          </div>
          <MobileDropdown
              :options="filteredRules"
              v-model="dAction.rule"
              optionLabel="description"
              optionValue="id"
              :filter-fields="ruleFilterFields"
              class="block md:hidden"
              :placeholder="$t('gameReport.rule')"
          >
            <template #option="slotProps:{option:Rule}">

              <div>
                <div v-if="slotProps.option.isCaution" class="rule-card card-yellow"></div>
                <div v-if="slotProps.option.isBlack" class="rule-card card-black"></div>
                <div v-if="slotProps.option.isRed" class="rule-card card-red"></div>
                {{ localizedDescription(slotProps.option) }}
              </div>

            </template>
          </MobileDropdown>

        </div>
        <div class="p-2 flex flex-col"
             v-if="dAction.rule?.isCaution || dAction.rule?.isBlack">
          <div class="flex flex-row items-center h-12">
            <div class="checkbox-rule-card card-red"></div>
            <Checkbox
                v-model="dAction.redCardIssued"
                :binary="true"
            />
          </div>
        </div>
        <div class="p-2">
          <InputText
              v-model="dAction.details"
              :placeholder="$t('gameReport.description')"
          />
        </div>
        <div v-if="!disciplinaryActionIsBlank(dAction)">
          <Button class="p-button-danger p-button-text"
                  @click="deleteDAction(dAction)">
            <vue-feather
                class="p-1"
                type="x"
            />
          </Button>
        </div>
      </div>
      <div class="flex-grow h-px bg-gray-300 m-4"></div>
    </div>
    <template #footer>

      <Button autofocus icon="pi pi-check" label="Ok" @click="closeDialog"/>
    </template>
  </Dialog>
</template>


<style scoped>
.checkbox-rule-card {
  @apply mr-1;
  float: left;
  height: 1.5rem;
  width: 0.75rem;
  clear: both;
}
</style>
