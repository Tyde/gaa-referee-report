<script setup lang="ts">

import {useAdminStore} from "@/utils/admin_store";
import type {GameCode, Rule} from "@/types";
import {computed, ref, watch} from "vue";

const store = useAdminStore()
const editingRule = ref<Rule|undefined>()
const shadowCopyEditingRule = ref<Rule|undefined>()
function rulesByCode(code:GameCode) {
  return store.rules.filter(r => r.code == code.id)
}

function editRule(rule:Rule) {
  editingRule.value = rule
  shadowCopyEditingRule.value = JSON.parse(JSON.stringify(rule))
}
const cards = [
  {label: "No card"},
  {label: "Caution"},
  {label: "Black card"},
  {label: "Red card"},
]
const selectedCardInCopy =ref()
watch(shadowCopyEditingRule, (newVal, oldVal) => {
  console.log("Changed to ", newVal)
  if(shadowCopyEditingRule.value) {
    let selectedCard = 0
    if(shadowCopyEditingRule.value.isCaution) {
      selectedCard = 1
    } else if(shadowCopyEditingRule.value.isBlack) {
      selectedCard = 2
    } else if(shadowCopyEditingRule.value.isRed) {
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
<div class="flex flex-row justify-center">
  <div class="container">
    <TabView>
      <TabPanel v-for="code in store.codes" key="id" :header="code.name">
        <div class="flex flex-col">
          <div v-for="rule in rulesByCode(code)" key="id" class="rule-card group">
            <template v-if="editingRule!==rule">
              <div class="float-left">
                <h4>{{rule.description}}</h4>
                <p v-if="rule.isCaution">Caution</p>
                <p v-else-if="rule.isBlack">Black</p>
                <p v-else-if="rule.isRed">Red</p>
              </div>
              <div class="group-hover:visible invisible float-right h-buttons">
                <vue-feather type="trash" @click="disableRule(rule)"/>
                <vue-feather type="edit" @click="editRule(rule)"/>
              </div>
            </template>
            <template v-else>
              <InputText class="m-1 w-[100%]" ref="ruleEditInput" v-model="shadowCopyEditingRule.description"/>
              <SelectButton v-model="selectedCardInCopy" :options="cards" class="m-1" optionLabel="label"/>
            </template>
          </div>
        </div>
      </TabPanel>
    </TabView>

  </div>
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