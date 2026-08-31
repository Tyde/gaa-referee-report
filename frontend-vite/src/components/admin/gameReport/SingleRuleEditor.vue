<script setup lang="ts">

import {useAdminStore} from "@/utils/admin_store";
import {computed, ref, watch} from "vue";
import {
  checkIfRuleDeletable,
  createNewRuleVersionOnServer,
  deleteRuleOnServer,
  getRuleHistory,
  toggleRuleStateOnServer, translateRule,
} from "@/utils/api/admin_api";
import {getRules} from "@/utils/api/disciplinary_action_api";
import type {NewRuleVersionDEO, Rule, RuleHistoryDEO} from "@/types/rules_types";


const store = useAdminStore()
const props = defineProps<{
  ruleId: number
}>()
const editing = ref(false)
const askDeleteOrDisable = ref(false)
const askDisableOnly = ref(false)

const isLoading = ref(false)
const rule = computed(() => {
  return store.publicStore.rules.find(r => r.id == props.ruleId)
})

const shadowCopyRule = ref<Rule | undefined>()

const hasDuplicateRuleNumber = computed(() => {
  const current = rule.value
  if (!current || current.ruleNumber == null) return false
  return store.publicStore.rules.some(r =>
      r.id != current.id &&
      r.code == current.code &&
      r.ruleNumber == current.ruleNumber
  )
})


function editRule() {
  if (rule.value) {
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
  checkIfRuleDeletable(rule.value!)
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
  toggleRuleStateOnServer(rule.value!)
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
  toggleRuleStateOnServer(rule.value!)
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
  deleteRuleOnServer(rule.value!)
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
  if (shadowCopyRule.value && rule.value) {
    const sc = shadowCopyRule.value
    const newVersion: NewRuleVersionDEO = {
      parentId: rule.value.id,
      code: sc.code,
      isCaution: selectedCardInCopy.value.id == 1,
      isBlack: selectedCardInCopy.value.id == 2,
      isRed: selectedCardInCopy.value.id == 3,
      description: sc.description,
      isDisabled: sc.isDisabled,
      ruleNumber: sc.ruleNumber?.trim() || undefined,
      descriptionFr: sc.descriptionFr,
      descriptionDe: sc.descriptionDe,
      descriptionEs: sc.descriptionEs,
    }
    createNewRuleVersionOnServer(newVersion)
        .then(async () => {
          editing.value = false
          shadowCopyRule.value = undefined
          store.publicStore.rules = await getRules()
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
    translateRule(shadowCopyRule.value!.description)
        .then((translated) => {
          shadowCopyRule.value!.descriptionFr = translated.ruleFr
          shadowCopyRule.value!.descriptionEs = translated.ruleEs
          shadowCopyRule.value!.descriptionDe = translated.ruleDe
        })
        .catch((e) => {
          store.newError(e)
        })
        .finally(() => {
          waitingForTranslation.value = false
        })
  }
}

const showHistory = ref(false)
const historyData = ref<RuleHistoryDEO | undefined>()

function toggleHistory() {
  showHistory.value = !showHistory.value
  if (showHistory.value && rule.value) {
    getRuleHistory(rule.value.id)
        .then((history) => {
          historyData.value = history
        })
        .catch((e) => {
          store.newError(e)
        })
  }
}

function revertVersion(v: Rule) {
  if (!rule.value) return
  const newVersion: NewRuleVersionDEO = {
    parentId: rule.value.id,
    code: v.code,
    isCaution: v.isCaution,
    isBlack: v.isBlack,
    isRed: v.isRed,
    description: v.description,
    isDisabled: v.isDisabled,
    ruleNumber: v.ruleNumber?.trim() || undefined,
    descriptionFr: v.descriptionFr,
    descriptionDe: v.descriptionDe,
    descriptionEs: v.descriptionEs,
  }
  createNewRuleVersionOnServer(newVersion)
      .then(async () => {
        showHistory.value = false
        historyData.value = undefined
        store.publicStore.rules = await getRules()
      })
      .catch((e) => {
        store.newError(e)
      })
}

function formatHistoryDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  // createdAt is stored as LocalDateTime.toString() e.g. "2024-05-06T12:34:56.123456"
  // Strip fractional seconds and replace T with space
  return iso.replace('T', ' ').replace(/\.\d+$/, '')
}

</script>

<template>
  <div class="rule-text-card group" :class="{loading:isLoading}">
    <template v-if="!editing">
      <div>
        <div class="float-left">
          <h4>
            <span class="rule-number-badge">{{ rule?.ruleNumber ?? '—' }}</span>
            <span class="latest-tag" v-if="rule?.isLatest">Latest</span>
            <span class="disabled-tag" v-if="rule?.isDisabled">Disabled</span>
            {{rule?.description }}
          </h4>
          <p><b>FR:</b>{{rule?.descriptionFr}}</p>
          <p><b>ES:</b>{{rule?.descriptionEs}}</p>
          <p><b>DE:</b>{{rule?.descriptionDe}}</p>
          <p v-if="rule?.isCaution">Caution</p>
          <p v-else-if="rule?.isBlack">Black</p>
          <p v-else-if="rule?.isRed">Red</p>
          <p v-if="hasDuplicateRuleNumber" class="duplicate-warning">Duplicate rule number in this code</p>
        </div>
        <div class="group-hover:visible invisible float-right h-buttons">
          <vue-feather v-if="!rule?.isDisabled" type="trash" @click="startDeleteProcess()"/>
          <vue-feather v-else type="check" @click="enableRule()"/>
          <vue-feather type="edit" @click="editRule()"/>
          <Button link class="m-1" label="History" @click="toggleHistory()"/>
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
      <template v-if="showHistory">
        <div class="clear-both m-2 border-blue-400 rounded border-2 p-2">
          <div class="flex flex-row justify-between items-center">
            <h5>Rule history</h5>
            <vue-feather class="m-2" type="x" @click="showHistory = false"/>
          </div>
          <div v-if="historyData">
            <div v-for="version in historyData.versions" :key="version.id" class="history-entry"
                 :class="{'history-entry-active': version.id === rule?.id, changed: version.description !== rule?.description}">
              <div class="flex flex-row justify-between items-center">
                <b><span class="rule-number-badge">{{ version.ruleNumber ?? '—' }}</span>
                  <span v-if="version.id === rule?.id" class="active-tag">Active</span>
                  <span v-else-if="version.isLatest" class="latest-tag">Latest</span></b>
                <span class="history-date">{{ formatHistoryDate(version.createdAt) }}</span>
                <Button v-if="version.id !== rule?.id" label="Revert" class="p-button-info m-1"
                        @click="revertVersion(version)"/>
              </div>
              <p>{{ version.description }}</p>
              <p><b>FR:</b>{{ version.descriptionFr }}</p>
              <p><b>ES:</b>{{ version.descriptionEs }}</p>
              <p><b>DE:</b>{{ version.descriptionDe }}</p>
            </div>
          </div>
          <p v-else>Loading history...</p>
        </div>
      </template>
    </template>
    <template v-else>
      <template v-if="shadowCopyRule !== undefined">
      <label class="m-1"><b>Rule Number:</b></label><InputText class="m-1 w-[100%]"
                 v-model="shadowCopyRule.ruleNumber" :disabled="waitingForTranslation"/>
      <InputText class="m-1 w-[100%]" ref="ruleEditInput"
                 v-model="shadowCopyRule.description" :disabled="waitingForTranslation"/>
        <b>FR:</b><InputText class="m-1 w-[100%]" ref="ruleEditInput"
                  v-model="shadowCopyRule.descriptionFr" :disabled="waitingForTranslation"/>
        <b>ES:</b><InputText class="m-1 w-[100%]" ref="ruleEditInput"
                  v-model="shadowCopyRule.descriptionEs" :disabled="waitingForTranslation"/>
        <b>DE:</b><InputText class="m-1 w-[100%]" ref="ruleEditInput"
                  v-model="shadowCopyRule.descriptionDe" :disabled="waitingForTranslation"/>
      <div class="float-left">
        <SelectButton v-model="selectedCardInCopy" :options="cards" class="m-1" optionLabel="label"/>
      </div>
      <div class="float-right h-buttons m-1 align-bottom">
        <Button
            class="m-2 p-button-info"
            link
            :disabled="waitingForTranslation"
            @click="tryTranslateRule()"
        ><vue-feather v-if="!waitingForTranslation" class="mr-2" type="feather" />
          <vue-feather v-else class="mr-2 animate-spin" type="loader" />
          {{ waitingForTranslation ? 'Translating...' : 'Translate Rules with AI' }}</Button>
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
.rule-number-badge {
  @apply bg-blue-800;
  @apply text-white;
  @apply rounded;
  @apply p-1;
  @apply m-1;
}
.latest-tag {
  @apply bg-green-800;
  @apply text-white;
  @apply rounded;
  @apply p-1;
  @apply m-1;
}
.duplicate-warning {
  @apply text-yellow-300;
  @apply font-bold;
}
.history-entry {
  @apply border-b border-surface-400;
  @apply py-1;
}
.history-entry.changed p:first-of-type {
  @apply text-yellow-300;
}
.history-date {
  @apply text-sm;
  @apply text-surface-300;
}
.active-tag {
  @apply bg-emerald-600;
  @apply text-white;
  @apply rounded;
  @apply p-1;
  @apply m-1;
  @apply border border-emerald-300;
}
.history-entry-active {
  @apply bg-surface-500;
  @apply border border-emerald-500;
  @apply rounded;
  @apply px-2;
}
</style>
