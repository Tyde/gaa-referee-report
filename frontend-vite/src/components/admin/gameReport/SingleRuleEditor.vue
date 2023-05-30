<script setup lang="ts">

import {useAdminStore} from "@/utils/admin_store";
import {computed, ref, watch} from "vue";
import {
  checkIfRuleDeletable,
  deleteRuleOnServer,
  toggleRuleStateOnServer,
  updateRuleOnServer
} from "@/utils/api/admin_api";
import type {Rule} from "@/types/rules_types";


const store = useAdminStore()
const props = defineProps<{
  ruleId: Number
}>()
const editing = ref(false)
const askDeleteOrDisable = ref(false)
const askDisableOnly = ref(false)

const isLoading = ref(false)
const rule = computed(() => {
  return store.rules.find(r => r.id == props.ruleId)
})

const shadowCopyRule = ref<Rule | undefined>()


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

function startDeleteProcess() {
  isLoading.value = true
  checkIfRuleDeletable(rule.value!!)
      .then((isDeletable) => {
        if (isDeletable) {
          askDeleteOrDisable.value = true
        } else {
          askDisableOnly.value = true
        }
        isLoading.value = false
      })
      .catch((e) => {
            store.newError(e)
            isLoading.value = false
          }
      )
}

function enableRule() {
  toggleRuleStateOnServer(rule.value!!)
      .then((rule) => {
        store.updateRuleInStore(rule)
        askDeleteOrDisable.value = false
        askDisableOnly.value = false
      })
      .catch((e) => {
        store.newError(e)
      })
}

function disableRule() {
  toggleRuleStateOnServer(rule.value!!)
      .then((rule) => {
        store.updateRuleInStore(rule)
        askDeleteOrDisable.value = false
        askDisableOnly.value = false
      })
      .catch((e) => {
        store.newError(e)
      })
}
function deleteRule() {
  deleteRuleOnServer(rule.value!!)
      .then((ruleId) => {

        askDeleteOrDisable.value = false
        askDisableOnly.value = false
        store.deleteRuleInStore(ruleId.id)
      })
      .catch((e) => {
        store.newError(e)
      })
}

function saveRule() {
  if (shadowCopyRule.value) {
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

const selectedCardInCopy = ref(cards[0])

watch(shadowCopyRule, () => {
  if (shadowCopyRule.value) {
    let selectedCard = 0
    if (shadowCopyRule.value.isCaution) {
      selectedCard = 1
    } else if (shadowCopyRule.value.isBlack) {
      selectedCard = 2
    } else if (shadowCopyRule.value.isRed) {
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
  <div class="rule-card group" :class="{loading:isLoading}">
    <template v-if="!editing">
      <div>
        <div class="float-left">
          <h4><span class="disabled-tag" v-if="rule?.isDisabled">Disabled</span>  {{rule?.description }}</h4>
          <p v-if="rule?.isCaution">Caution</p>
          <p v-else-if="rule?.isBlack">Black</p>
          <p v-else-if="rule?.isRed">Red</p>
        </div>
        <div class="group-hover:visible invisible float-right h-buttons">
          <vue-feather v-if="!rule?.isDisabled" type="trash" @click="startDeleteProcess()"/>
          <vue-feather v-else type="check" @click="enableRule()"/>
          <vue-feather type="edit" @click="editRule()"/>
        </div>
      </div>
      <div v-if="isLoading">
        <vue-feather type="loader" class="animate-spin"/>
      </div>
      <template v-if="askDeleteOrDisable">
        <div class="clear-both m-2 border-red-400 rounded border-2 p-2">
          <p>This rule is not used in any report, therefore it can be completely deleted.
            Otherwise you can just disable the rule so that it isn't selectable for reports, but
            still is in the database for later reactivation.</p>
          <Button label="Delete" class="m-2 p-button-danger" @click="deleteRule()"/>
          <Button label="Disable" class="m-2 p-button-warning" @click="disableRule()"/>
          <Button label="Cancel" class="m-2 p-button-secondary" @click="askDeleteOrDisable = false"/>
        </div>
      </template>
      <template v-if="askDisableOnly">
        <div class="clear-both m-2 border-red-400 rounded border-2 p-2">
          <p>This Rule can't be completely deleted from the Database as it is used in some reports.
            However it can be disabled so that it isn't selectable for reports.
            Are you sure you want to disable this rule?</p>
          <Button label="Yes" class="m-2 p-button-danger" @click="disableRule()"/>
          <Button label="Cancel" class="m-2 p-button-secondary" @click="askDisableOnly = false"/>
        </div>
      </template>
    </template>
    <template v-else>
      <template v-if="shadowCopyRule !== undefined">
      <InputText class="m-1 w-[100%]" ref="ruleEditInput"
                 v-model="shadowCopyRule.description"/>
      <div class="float-left">
        <SelectButton v-model="selectedCardInCopy" :options="cards" class="m-1" optionLabel="label"/>
      </div>
      <div class="float-right h-buttons m-1 align-bottom">
        <vue-feather class="m-2" type="x" @click="cancelEdit()"/>
        <vue-feather class="m-2" type="check" @click="saveRule()"/>
      </div>
      </template>
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
.loading {
  @apply bg-gray-800;
}
.disabled-tag {
  @apply bg-red-400;
  @apply text-white;
  @apply rounded;
  @apply p-1;
  @apply m-1;
}
</style>