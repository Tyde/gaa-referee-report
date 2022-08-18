<script lang="ts" setup>
import type {Injury, Team} from "@/types";
import {computed, onMounted, onUpdated, watch} from "vue";
import {injuryIsBlank, uploadInjury} from "@/utils/api/injuries_api";

const props = defineProps<{
  visible: boolean
  modelValue: Array<Injury>
  team?: Team
  gameReportId?: number
}>()
const emits = defineEmits<{
  (e: 'update:visible', value: boolean): void,
  (e: 'update:modelValue', value: Array<Injury>): void
}>()

async function uploadInjuriesToServer() {
  if (props.gameReportId != undefined) {
    for (let injury of props.modelValue) {
      console.log(injury)
      await uploadInjury(injury, props.gameReportId)
    }
  } else {
    console.log("No report id - deferring upload")
  }
}

const localVisible = computed({
  get() {
    return props.visible
  },
  set(newValue) {
    emits('update:visible', newValue)
  }
})

function closeDialog() {

  emits('update:modelValue', props.modelValue)
  uploadInjuriesToServer()
  emits('update:visible', false)

}

const injuryDialogTitle = computed(() => {
  return "Injuries for " + props.team?.name
})
watch(props.modelValue, (newInjuries) => {
  generateEmptyInjury()
})

function generateEmptyInjury() {
  let newInjury = props.modelValue
  if (newInjury.length == 0) {
    addEmptyInjury()
  } else if (!injuryIsBlank(newInjury[newInjury.length - 1])) {
    addEmptyInjury()
  }
}

function addEmptyInjury() {
  props.modelValue.push({
    firstName: "",
    lastName: "",
    details: "",
    team: props.team
  } as Injury)
}

function deleteInjury(injury: Injury) {
  props.modelValue.splice(props.modelValue.indexOf(injury), 1)
}

onUpdated(() => {
  generateEmptyInjury()
})
onMounted(() => {
  generateEmptyInjury()
})
</script>
<template>

  <Dialog
      v-model:visible="localVisible"
      :closable="false"
      :close-on-escape="false"
      :header="injuryDialogTitle"
      :modal="true"
  >
    <div v-for="injury in props.modelValue">
      <div class="flex flex-row flex-wrap">
        <div class="p-2">
          <InputText
              v-model="injury.firstName"
              placeholder="First name"
          />
        </div>
        <div class="p-2">
          <InputText
              v-model="injury.lastName"
              placeholder="Last name"
          />
        </div>
        <div class="p-2">
          <InputText
              v-model="injury.details"

              placeholder="Description"
          />
        </div>
        <div class="p-2" v-if="!injuryIsBlank(injury)">
          <Button class="p-button-danger p-button-text"
                  @click="deleteInjury(injury)">
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