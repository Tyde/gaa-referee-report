<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import {computed, ref} from "vue";
import type {ComponentPublicInstance} from "vue";
import InputText from "primevue/inputtext";
import {useConfirm} from "primevue/useconfirm";
import type {PitchProperty} from "@/types/pitch_types";
import type {PitchPropertyType} from "@/types/pitch_types";

const props = defineProps<{
  type: PitchPropertyType
}>()

const store = useAdminStore()
const editingOption = ref<PitchProperty|undefined>()
const confirm = useConfirm()

const options = computed(() => {
  return store.publicStore.getVariablesByType(props.type)
})
const optionEditInput = ref<Array<ComponentPublicInstance> | null>(null)
async function waitForInputAvailable() {
  let start_time = new Date().getTime()
  while (true) {
    if (optionEditInput.value) {
      return
    }
    if ((new Date()).getTime() > start_time + 3000) {
      throw new Error("Timeout waiting for variables")
    }
    await new Promise(resolve => setTimeout(resolve, 100))
  }
}
async function showInputFor(option:PitchProperty) {
  editingOption.value = option
  await waitForInputAvailable()
  if(optionEditInput.value) {
    optionEditInput.value[0].$el.focus()
  }
}

const dialogOption = computed(() => {
  return "option_"+props.type
})

async function deleteOption(option:PitchProperty) {
  confirm.require({
    group: dialogOption.value,
    message: `Are you sure you want to delete this option (${option.name})?`,
    header: 'Delete Confirmation',
    icon: 'pi pi-info-circle',
    acceptClass: 'p-button-danger',
    accept: () => {
      store.deletePitchVariable(option, props.type)
    },
    reject: () => {
      //Rejected delete

    }

  })
}

async function enableOption(option:PitchProperty) {
  await store.enablePitchVariable(option, props.type)
}

function newOption() {
  let newOption = <PitchProperty>{
    id: -1,
    name: "",
    type: props.type
  }
  console.log("bfeore push", options.value)
  options.value.push(newOption)
  console.log("after push", options.value)
  editingOption.value = newOption
}

async function storeOption(option:PitchProperty) {
  await store.updatePitchVariable(option, props.type)
  editingOption.value = undefined
}

</script>

<template>
  <div>

      <div class="flex flex-col m-2 shadow-xl bg-gray-100">
        <div v-for="option in options" class="group option-label">
          <template v-if="option === editingOption">

            <div class="flex flex-row">
              <div class="p-1 flex-grow">
                <InputText class="m-1" ref="optionEditInput" v-model="option.name"/>
              </div>
              <div class="p-2">

                <vue-feather type="x" @click="editingOption = undefined"/>
              </div>
            </div>
            <div class="p-1">
              <Button @click="storeOption(option)" class="p-button-sm p-button-success m-1">Save</Button>
            </div>
          </template>
          <template v-else>
            <span v-if="option.disabled" class="tag-disabled">Disabled</span>{{ option.name }}
            <div class="group-hover:visible invisible float-right">
              <vue-feather v-if="!option.disabled" type="trash" @click="deleteOption(option)"/>
              <vue-feather v-else type="check" @click="enableOption(option)"/>
              <vue-feather type="edit" @click="showInputFor(option)"/>
            </div>
          </template>
        </div>
        <div class="flex flex-row justify-center m-2 rounded w-96 bg-gray-300">
          <Button @click="newOption" class="p-button-sm p-button-primary m-1">Add new</Button>
        </div>
      </div>
    <ConfirmDialog :group="dialogOption"></ConfirmDialog>
  </div>
</template>


<style scoped>
.option-label {
  @apply p-2;
  @apply m-2;
  @apply w-96;
  @apply rounded;
  @apply bg-gray-200;
  @apply hover:bg-gray-300;
}

.tag-disabled {
  @apply bg-red-600;
  @apply text-white;
  @apply rounded-lg;
  @apply p-1 mr-1;
}

</style>
