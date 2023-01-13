
<script setup lang="ts" >

import {useAdminStore} from "@/utils/admin_store";
import {computed, onMounted, ref, watch} from "vue";
import type {Rule} from "@/types";
import {updateRuleOnServer} from "@/utils/api/admin_api";

const store = useAdminStore()
const props = defineProps<{ruleId:Number}>()
const editing = ref(false)
const rule = computed(() => {
  const rule = store.rules.find(r => r.id == props.ruleId)
  return rule
})

const shadowCopyRule = ref<Rule|undefined>()


function editRule() {
  if (rule) {
    editing.value = true
    shadowCopyRule.value = JSON.parse(JSON.stringify(rule.value))
  }
}

function cancelEdit() {
  editing.value = false
  shadowCopyRule.value = undefined
}


function disableRule() {
  console.log("disable rule")
}

function saveRule() {
  if(shadowCopyRule.value) {
    updateRuleOnServer(shadowCopyRule.value)
        .then(() => {
          editing.value = false

          let index = store.rules.indexOf(rule.value!!)
          store.rules[index] = shadowCopyRule.value!!
          shadowCopyRule.value = undefined
        })
        .catch(e => {
          store.newError(e)
        })
  }
}

const cards = [
  {label: "No card"},
  {label: "Caution"},
  {label: "Black card"},
  {label: "Red card"},
]

const selectedCardInCopy =ref(cards[0])

watch(shadowCopyRule, (newVal, oldVal) => {
  if(shadowCopyRule.value) {
    let selectedCard = 0
    if(shadowCopyRule.value.isCaution) {
      selectedCard = 1
    } else if(shadowCopyRule.value.isBlack) {
      selectedCard = 2
    } else if(shadowCopyRule.value.isRed) {
      selectedCard = 3
    }
    selectedCardInCopy.value = cards[selectedCard]
  } else {
    selectedCardInCopy.value = cards[0]
  }
  console.log(selectedCardInCopy.value)
})

</script>

<template>
  <div class="rule-card group">
    <template v-if="!editing">
      <div class="float-left">
        <h4>{{rule.description}}</h4>
        <p v-if="rule.isCaution">Caution</p>
        <p v-else-if="rule.isBlack">Black</p>
        <p v-else-if="rule.isRed">Red</p>
      </div>
      <div class="group-hover:visible invisible float-right h-buttons">
        <vue-feather type="trash" @click="disableRule()"/>
        <vue-feather type="edit" @click="editRule()"/>
      </div>
    </template>
    <template v-else>
      <InputText class="m-1 w-[100%]" ref="ruleEditInput" v-model="shadowCopyRule.description"/>
      <div class="float-left">
        <SelectButton v-model="selectedCardInCopy" :options="cards" class="m-1" optionLabel="label"/>
      </div>
      <div class="float-right h-buttons m-1 align-bottom">
        <vue-feather class="m-2" type="x" @click="cancelEdit()"/>
        <vue-feather class="m-2" type="check" @click="saveRule()"/>
      </div>
    </template>
  </div>

</template>


<style scoped>
.rule-card {
  @apply p-2 m-1;
  @apply bg-gray-100;
  @apply rounded;
}
.rule-card h4 {
  @apply font-bold;
}
.h-buttons i {
  @apply p-1;
  @apply hover:cursor-pointer;
}
</style>