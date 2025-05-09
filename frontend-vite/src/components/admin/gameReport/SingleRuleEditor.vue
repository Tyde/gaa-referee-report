<script setup lang="ts">

import {useAdminStore} from "@/utils/admin_store";
import {computed, ref, watch} from "vue";
import {
  checkIfRuleDeletable,
  deleteRuleOnServer,
  toggleRuleStateOnServer, translateRule,
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
  return store.publicStore.rules.find(r => r.id == props.ruleId)
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
    const ruleForUpdate = JSON.parse(JSON.stringify(shadowCopyRule.value))
    ruleForUpdate.isCaution = selectedCardInCopy.value.id == 1
    ruleForUpdate.isBlack = selectedCardInCopy.value.id == 2
    ruleForUpdate.isRed = selectedCardInCopy.value.id == 3
    updateRuleOnServer(ruleForUpdate)
        .then(() => {
          editing.value = false

          let index = store.publicStore.rules.indexOf(rule.value!!)
          store.publicStore.rules[index] = ruleForUpdate
          shadowCopyRule.value = undefined
        })
        .catch(e => {
          store.newError(e)
        })
  }
}

const cards = [
  {label: "No card", id: 0},
  {label: "Caution", id: 1},
  {label: "Black card", id: 2},
  {label: "Red card", id: 3}
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
})

const waitingForTranslation = ref(false)
async function tryTranslateRule() {
  if(shadowCopyRule.value) {
    waitingForTranslation.value = true
    translateRule(shadowCopyRule.value!!.description)
        .then((translated) => {
          shadowCopyRule.value!!.descriptionFr = translated.ruleFr
          shadowCopyRule.value!!.descriptionEs = translated.ruleEs
          shadowCopyRule.value!!.descriptionDe = translated.ruleDe
        })
        .catch((e) => {
          store.newError(e)
        })
        .finally(() => {
          waitingForTranslation.value = false
        })
  }
}

</script>

<template>
  <div class="rule-text-card group" :class="{loading:isLoading}">
    <template v-if="!editing">
      <div>
        <div class="float-left">
          <h4><span class="disabled-tag" v-if="rule?.isDisabled">Disabled</span>  {{rule?.description }}</h4>
          <p><b>FR:</b>{{rule?.descriptionFr}}</p>
          <p><b>ES:</b>{{rule?.descriptionEs}}</p>
          <p><b>DE:</b>{{rule?.descriptionDe}}</p>
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
          <Button label="Cancel" class="m-2 p-button-info" @click="askDeleteOrDisable = false"/>
        </div>
      </template>
      <template v-if="askDisableOnly">
        <div class="clear-both m-2 border-red-400 rounded border-2 p-2">
          <p>This Rule can't be completely deleted from the Database as it is used in some reports.
            However it can be disabled so that it isn't selectable for reports.
            Are you sure you want to disable this rule?</p>
          <Button label="Yes" class="m-2 p-button-danger" @click="disableRule()"/>
          <Button label="Cancel" class="m-2 p-button-info" @click="askDisableOnly = false"/>
        </div>
      </template>
    </template>
    <template v-else>
      <template v-if="shadowCopyRule !== undefined">
      <InputText class="m-1 w-[100%]" ref="ruleEditInput"
                 v-model="shadowCopyRule.description"/>
        <b>FR:</b><InputText class="m-1 w-[100%]" ref="ruleEditInput"
                  v-model="shadowCopyRule.descriptionFr"/>
        <b>ES:</b><InputText class="m-1 w-[100%]" ref="ruleEditInput"
                  v-model="shadowCopyRule.descriptionEs"/>
        <b>DE:</b><InputText class="m-1 w-[100%]" ref="ruleEditInput"
                  v-model="shadowCopyRule.descriptionDe"/>
      <div class="float-left">
        <SelectButton v-model="selectedCardInCopy" :options="cards" class="m-1" optionLabel="label"/>
      </div>
      <div class="float-right h-buttons m-1 align-bottom">
        <Button
            class="m-2 p-button-info"
            link
            :disabled="waitingForTranslation"
            @click="tryTranslateRule()"
        ><vue-feather class="mr-2" type="feather" />
          Translate Rules with AI...</Button>
        <vue-feather class="m-2" type="x" @click="cancelEdit()"/>
        <vue-feather class="m-2" type="check" @click="saveRule()"/>
      </div>
      </template>
    </template>

  </div>

</template>


<style scoped>
.rule-text-card {
  @apply p-2 m-1;
  @apply bg-surface-600;
  @apply rounded;
  @apply w-full;
}

.rule-text-card h4 {
  @apply text-lg;
  @apply font-bold;
}

.h-buttons i {
  @apply p-1;
  @apply hover:cursor-pointer;
}
.loading {
  @apply bg-surface-500;
}
.disabled-tag {
  @apply bg-red-800;
  @apply text-white;
  @apply rounded;
  @apply p-1;
  @apply m-1;
}
</style>
