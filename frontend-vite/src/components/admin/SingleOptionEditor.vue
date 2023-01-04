<script lang="ts" setup>

import type {PitchProperty, PitchPropertyType} from "@/types";
import {useAdminStore} from "@/utils/admin_store";
import {computed, ref, watch} from "vue";
import type {ComponentPublicInstance} from "vue";
import InputText from "primevue/inputtext";
import {useConfirm} from "primevue/useconfirm";

const props = defineProps<{
  type: PitchPropertyType
}>()

const store = useAdminStore()
const editingOption = ref<PitchProperty|undefined>()
const confirm = useConfirm()

const options = computed(() => {
  return store.getVariablesByType(props.type)
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
      //Confirmed delete
    },
    reject: () => {
      //Rejected delete

    }

  })
}



async function storeOption(option:PitchProperty) {
  await store.updatePitchVariable(option, props.type)
  editingOption.value = undefined
}

</script>

<template>
  <div>

      <div class="flex flex-col m-2">
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
            {{ option.name }}
            <div class="group-hover:visible invisible float-right">
              <vue-feather type="trash" @click="deleteOption(option)"/>
              <vue-feather type="edit" @click="showInputFor(option)"/>
            </div>
          </template>
        </div>
      </div>
    <ConfirmDialog :group="dialogOption"></ConfirmDialog>
  </div>
</template>


<style scoped>
.option-label {
  @apply p-2;
  @apply m-1;
  @apply w-96;
  @apply rounded;
  @apply bg-gray-100;
  @apply hover:bg-gray-200;
}

</style>