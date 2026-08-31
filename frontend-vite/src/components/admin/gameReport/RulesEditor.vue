<script setup lang="ts">

import {useAdminStore} from "@/utils/admin_store";
import type {GameCode} from "@/types";
import SingleRuleEditor from "@/components/admin/gameReport/SingleRuleEditor.vue";
import NewRuleEditor from "@/components/admin/gameReport/NewRuleEditor.vue";
import {computed, onMounted, ref} from "vue";

const store = useAdminStore()

function rulesByCode(code:GameCode) {
  return store.publicStore.rules
      .filter(r => r.code == code.id)
      .sort((a, b) => (a.ruleNumberSortKey ?? '').localeCompare(b.ruleNumberSortKey ?? '') || a.id - b.id)
}

const rulesWithoutNumber = computed(() => {
  return store.publicStore.rules.filter(r => r.ruleNumber == null)
})

function rulesWithoutNumberForCode(code:GameCode) {
  return rulesWithoutNumber.value.filter(r => r.code == code.id)
}

const duplicateRuleNumbers = computed(() => {
  const counts = new Map<string, number>()
  for (const r of store.publicStore.rules) {
    if (r.ruleNumber != null) {
      const key = `${r.code}:${r.ruleNumber}`
      counts.set(key, (counts.get(key) ?? 0) + 1)
    }
  }
  const byCode = new Map<string, string[]>()
  for (const [key, count] of counts) {
    if (count > 1) {
      const [code, ...rest] = key.split(':')
      const number = rest.join(':')
      byCode.set(code, [...(byCode.get(code) ?? []), number])
    }
  }
  return byCode
})

function duplicateNumbersForCode(code:GameCode): string[] {
  return duplicateRuleNumbers.value.get(String(code.id)) ?? []
}

const activeCodeIndex = ref(0)

const currentCode = computed(() => {
  return store.publicStore.codes[activeCodeIndex.value]
})

const newRuleVisible = ref(false)


function addRule() {
  newRuleVisible.value = true
}

onMounted(() => {
store.publicStore.waitForAllVariablesPresent().then(() => {
    activeCodeIndex.value = store.publicStore.codes[0].id
  })
})
</script>

<template>
<div class="flex flex-row justify-center">
  <div class="container">
    <Tabs :value="activeCodeIndex">
      <TabList>
        <Tab v-for="code in store.publicStore.codes" :key="code.id" :value="code.id">
          {{ code.name }}
        </Tab>
      </TabList>
      <TabPanels>
        <TabPanel v-for="code in store.publicStore.codes" :key="code.id" :value="code.id" class="bg-surface-700">
          <div class="flex flex-col">
            <template v-if="rulesWithoutNumberForCode(code).length > 0">
              <div class="m-2 border-yellow-400 rounded border-2 p-2">
                <p>{{ rulesWithoutNumberForCode(code).length }} rule(s) have no rule number and fall at the end of the list.</p>
              </div>
            </template>
            <template v-if="duplicateNumbersForCode(code).length">
              <div class="m-2 border-red-400 rounded border-2 p-2">
                <p>Duplicate rule numbers for this code:
                  <b>{{ duplicateNumbersForCode(code).join(', ') }}</b>
                </p>
              </div>
            </template>
            <div class="flex flex-row justify-center">
              <Button label="Add Rule" icon="pi pi-plus" class="p-button-success m-2" @click="addRule()"/>
            </div>
            <SingleRuleEditor v-for="rule in rulesByCode(code)" :rule-id="rule.id" :key="rule.id"/>
          </div>
        </TabPanel>
      </TabPanels>
    </Tabs>
    <NewRuleEditor :code="currentCode" v-model:visible="newRuleVisible"/>

  </div>
</div>

</template>

<style scoped>

</style>
