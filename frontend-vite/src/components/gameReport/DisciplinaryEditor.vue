<script lang="ts" setup>
import {DisciplinaryAction, Rule, Team} from "@/types";
import {computed, onMounted, onUpdated, watch} from "vue";
import {disciplinaryActionIsBlank, uploadDisciplinaryAction} from "@/utils/api/disciplinary_action_api";

const props = defineProps<{
  visible: boolean,
  modelValue: Array<DisciplinaryAction>,
  team: Team,
  gameReportId?: number,
  rules: Array<Rule>,
}>()
const emits = defineEmits<{
  (e: 'update:visible', value: boolean): void,
  (e: 'update:modelValue', value: Array<DisciplinaryAction>): void
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
  if (props.gameReportId != undefined) {
    for (let action of props.modelValue) {
      await uploadDisciplinaryAction(action, props.gameReportId)
    }
  } else {
    console.log("No report id - deferring upload")
  }
}

function closeDialog() {
  emits('update:modelValue', props.modelValue ?? [])
  emits('update:visible', false)
  uploadActionsToServer()
}

const disciplinaryDialogTitle = computed(() => {
  return "Disciplinary Action for " + props.team?.name
})
watch(props.modelValue, (newActions) => {
  generateEmptydAFields()
})


function generateEmptydAFields() {
  let newActions = props.modelValue
  if (newActions.length == 0) {
    addEmptyDisciplinaryAction()
  } else if (!disciplinaryActionIsBlank(newActions[newActions.length - 1])) {
    addEmptyDisciplinaryAction()
  }
}


function addEmptyDisciplinaryAction() {
  props.modelValue.push({
    team: props.team,
    firstName: "",
    lastName: "",
    number: undefined,
    rule: undefined,
    details: ""
  })
}

function deleteDAction(dAction: DisciplinaryAction) {
  props.modelValue.splice(props.modelValue.indexOf(dAction), 1)
}

onUpdated(() => {
  generateEmptydAFields()
})

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
    <div v-for="dAction in modelValue">
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
            :options="rules"
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