<script setup lang="ts">

import {useAdminStore} from "@/utils/admin_store";
import type {GameCode} from "@/types";
import SingleRuleEditor from "@/components/admin/gameReport/SingleRuleEditor.vue";
import NewRuleEditor from "@/components/admin/gameReport/NewRuleEditor.vue";
import {computed, onMounted, ref} from "vue";

const store = useAdminStore()

function rulesByCode(code:GameCode) {
  const out = store.publicStore.rules.filter(r => r.code == code.id)
  return out
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
        <Tab v-for="(code, index) in store.publicStore.codes" :key="code.id" :value="code.id">
          {{ code.name }}
        </Tab>
      </TabList>
      <TabPanels>
        <TabPanel v-for="code in store.publicStore.codes" key="id" :value="code.id" class="bg-surface-700">
          <div class="flex flex-col">
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
