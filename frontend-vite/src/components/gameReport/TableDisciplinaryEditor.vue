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

    <table class="disciplinary-action-table">
      <thead>
        <tr>
          <th>First Name</th>
          <th>Last Name</th>
          <th>Number</th>
          <th><span v-tooltip.bottom="'Team Official'">T.O</span></th>
          <th>Rule</th>
          <th>Description</th>
        </tr>
      </thead>
      <tr
          v-for="dAction in selectedDisciplinaryActions"
          :key="dAction.id"
      >
        <td>{{dAction.firstName}}</td>
        <td>{{dAction.lastName}}</td>
        <td>{{dAction.number}}</td>
        <td>
          <Checkbox
              v-model="dAction.forTeamOfficial"
              class="checkbox-rule-card"
          />
        </td>
        <td>{{dAction.rule?.description}}</td>
        <td>{{dAction.details}}</td>
      </tr>
    </table>
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

.disciplinary-action-table {
  @apply table-auto border-collapse;

}
.disciplinary-action-table thead {
  @apply bg-surface-600;
}
.disciplinary-action-table th {
  @apply text-left;
  @apply p-2;
  @apply font-bold;
  @apply border-surface-400 border;

}
.disciplinary-action-table td {
  @apply p-2;
  @apply border-surface-400 border;

}
</style>
