<script lang="ts" setup>
import type { DisciplinaryAction} from "@/types";
import {computed, onMounted, watch} from "vue";
import {
  deleteDisciplinaryActionOnServer,
  disciplinaryActionIsBlank,
} from "@/utils/api/disciplinary_action_api";
import {useReportStore} from "@/utils/edit_report_store";

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
      store.sendDisciplinaryAction(action,store.selectedGameReport,true)
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

const disciplinaryDialogTitle = computed(() => {
  return "Disciplinary Action for " +  selectedTeam.value?.name
})



watch(()=>selectedDisciplinaryActions, () => {
  generateEmptydAFields()
},{deep:true,immediate:true})

function generateEmptydAFields() {
  let newActions = selectedDisciplinaryActions.value

  if (newActions.length == 0) {
    addEmptyDisciplinaryAction()
  } else if (!disciplinaryActionIsBlank(newActions[newActions.length - 1])) {
    addEmptyDisciplinaryAction()
  }
}


function addEmptyDisciplinaryAction() {
  if(selectedTeam.value) {
    console.log("adding empty disciplinary action")
    selectedDisciplinaryActions.value.push({
      id: undefined,
      team: selectedTeam.value,
      firstName: "",
      lastName: "",
      number: undefined,
      rule: undefined,
      details: ""
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

const filteredRules = computed(() => {
  return store.rules.filter((rule) => {
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
        <div class="p-2">
          <InputText
              v-model="dAction.firstName"
              class="w-52"
              placeholder="First name"
          />
        </div>
        <div class="p-2">
          <InputText
              v-model="dAction.lastName"
              class="w-52"
              placeholder="Last name"
          />
        </div>

        <div class="p-2">
          <InputNumber
              v-model="dAction.number"
              class="w-20"
              input-class="w-20"
              placeholder="Number"
          />
        </div>

        <!-- Rule: {{dAction.rule?.id}}-->

        <Dropdown
            v-model="dAction.rule"
            :options="filteredRules"
            :show-clear="true"
            class="dropdown-disciplinary m-2"
            input-class="dropdown-disciplinary"
            placeholder="Rule:"
            :filter="true"
            :filter-fields="['description']"
        >
          <template #value="slotProps">
            <div v-if="slotProps.value" class="p-disciplinary">
              <div v-if="slotProps.value.isCaution" class="rule-card card-yellow"></div>
              <div v-if="slotProps.value.isBlack" class="rule-card card-black"></div>
              <div v-if="slotProps.value.isRed" class="rule-card card-red"></div>
              {{ slotProps.value.description }}
            </div>
            <span v-else class="p-disciplinary">{{ slotProps.placeholder }}</span>
          </template>
          <template #option="slotProps">

            <div>
              <div v-if="slotProps.option.isCaution" class="rule-card card-yellow"></div>
              <div v-if="slotProps.option.isBlack" class="rule-card card-black"></div>
              <div v-if="slotProps.option.isRed" class="rule-card card-red"></div>
              {{ slotProps.option.description }}
            </div>
          </template>
        </Dropdown>
        <div class="p-2">
          <InputText
              v-model="dAction.details"

              placeholder="Description"
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

</style>