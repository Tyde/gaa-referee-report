<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import type {GameCode} from "@/types";
import {computed, onMounted, ref, watch} from "vue";
import type {NewRuleDEO} from "@/types/rules_types";

const store = useAdminStore()

const props = defineProps<{
  code?: GameCode,
  visible: boolean
}>()
const rule = ref<NewRuleDEO>( {} as NewRuleDEO)

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

function reset() {
  rule.value = {
    code: props.code?.id ?? 0,
    isCaution: false,
    isBlack: false,
    isRed: false,
    description: "",
    isDisabled: false,
  }
}
onMounted(() => {
 reset()
})


const cards = [
  {label: "No card"},
  {label: "Caution"},
  {label: "Black card"},
  {label: "Red card"},
]

const selectedCardForButton = ref(cards[0])

watch(rule, () => {
  if (rule.value) {
    let selectedCard = 0
    if (rule.value.isCaution) {
      selectedCard = 1
    } else if (rule.value.isBlack) {
      selectedCard = 2
    } else if (rule.value.isRed) {
      selectedCard = 3
    }
    selectedCardForButton.value = cards[selectedCard]
  } else {
    selectedCardForButton.value = cards[0]
  }
  console.log(selectedCardForButton.value)
})

watch(() => props.code, () => {
  if (props.code) {
    rule.value.code = props.code.id
  }
})

const isLoading = ref(false)

async function saveRule() {
  if (rule.value) {
    isLoading.value = true
    store.addRule(rule.value)
        .then(() => {
          reset()
          isLoading.value = false
          localVisible.value = false
        })
        .catch((error) => {
          store.newError(error)
          isLoading.value = false
        })
  }
}

function cancelEdit() {
  localVisible.value = false
  reset()
}

</script>
<template>
  <Dialog
      v-model:visible="localVisible"
      :closable="false"
      :close-on-escape="false"
      header="Create new rule"
      :modal="true"

  >
    <h5>Rule:</h5>
    <Textarea
        class="m-1 w-[100%]"
        ref="ruleEditInput"
        v-model="rule.description"
        :disabled="isLoading"
        :autoResize="true"
        rows="5"
        cols="40"
    />
    <h5>Code:</h5>
    <Dropdown v-model="rule.code" :options="store.publicStore.codes" optionLabel="name" optionValue="id" :disabled="isLoading"/>
    <h5>Card:</h5>
    <SelectButton v-model="selectedCardForButton" :options="cards" class="m-1" optionLabel="label" :disabled="isLoading"/>
    <br>
    <div class="flex flex-row">
      <div class="p-2">
        <Button class="m-2 p-button-success" label="Save" icon="pi pi-check" @click="saveRule()" :disabled="isLoading"/>
      </div>
      <div class="p-2">
        <Button class="m-2 p-button-danger" label="Cancel" icon="pi pi-times" @click="cancelEdit()"
                :disabled="isLoading"/>
      </div>
    </div>

  </Dialog>

</template>


<style scoped>

</style>
