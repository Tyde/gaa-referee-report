<script setup lang="ts">

import {computed, ref} from "vue";
import {ObjectUtils} from "primevue/utils";

const props = defineProps({
  options: {
    type: Array<any>,
    required: true
  },
  optionLabel: {
    type: String,
    required: false
  },
  dataKey: {
    type: String,
    required: false
  },
  filterFields: {
    type: Array<string>,
    required: false
  },
  placeholder: {
    type: String,
    required: false
  },
})

const model = defineModel()



const isOpen = ref(false)
const searchTerm = ref('')

const filteredOptions = computed(() => {
  if (searchTerm.value) {
    if (props.filterFields) {
      return props.options.filter((option:any) => {
        for (let field of props.filterFields!!) {
          let match = ObjectUtils.resolveFieldData(option, field).toLowerCase().includes(searchTerm.value.toLowerCase())
          if (match) {
            return true
          }
        }
      })
    } else {
      return props.options?.filter((option:any) =>
          getOptionLabel(option).toLowerCase().includes(searchTerm.value.toLowerCase())
      )
    }
  } else {
    return props.options
  }
})

function getOptionLabel(option:any) {
  return props.optionLabel ? ObjectUtils.resolveFieldData(option, props.optionLabel) : option;
}
function getOptionRenderKey(option:any, index:number) {
  return (props.dataKey ? ObjectUtils.resolveFieldData(option, props.dataKey) : getOptionLabel(option)) + '_' + index;
}
function selectOption(option:any) {
  model.value = option;
  isOpen.value = false;
}
</script>

<template>
<div class="w-full">
  <button
      @click="isOpen = !isOpen"
      class="border rounded border-gray-400 bg-white p-2 m-2 w-full"
  >
    <span v-if="model">
      <slot name="option" :option="model" :index="-1">
            {{getOptionLabel(model)}}
          </slot>
    </span>
    <span v-else class="text-gray-700">
      {{placeholder || 'Select'}}
    </span>
  </button>
  <div
      v-if="isOpen"
      class="mobile-dropdown-overlay"
  >
    <div v-if="placeholder" class="font-bold text-lg text-center">{{placeholder}}</div>
    <div class="flex flex-row">
      <IconField iconPosition="left" class="m-2 grow w-full">
        <InputIcon class="pi pi-search"></InputIcon>
        <InputText v-model="searchTerm" class="w-full" placeholder="Search"/>
      </IconField>
      <Button
          @click="isOpen = false"
          class="p-button-danger p-button-text">
        <vue-feather
            class="p-1"
            type="x"
        />
      </Button>
    </div>
    <div
        class="w-full h-full overflow-y-auto"
    >
      <ul>
        <li
            v-for="(option,i) of filteredOptions"
            :key="getOptionRenderKey(option,i)"
            class="mobile-dropdown-option"
            @click="() => selectOption(option)"
        >
          <slot name="option" :option="option" :index="i">
            {{getOptionLabel(option)}}
          </slot>
        </li>
      </ul>
    </div>
  </div>
</div>
</template>

<style scoped>
.mobile-dropdown-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  @apply rounded-lg border border-gray-300 bg-white;
  @apply p-4 flex flex-col;
}

.mobile-dropdown-option {
  @apply p-2;
  @apply border-b border-gray-300;
  @apply hover:bg-gray-100;
}
</style>
