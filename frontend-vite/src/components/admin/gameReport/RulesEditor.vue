<script setup lang="ts">

import {useAdminStore} from "@/utils/admin_store";
import type {GameCode} from "@/types";
import SingleRuleEditor from "@/components/admin/gameReport/SingleRuleEditor.vue";
import NewRuleEditor from "@/components/admin/gameReport/NewRuleEditor.vue";
import {computed, ref} from "vue";

const store = useAdminStore()

function rulesByCode(code:GameCode) {
  const out = store.rules.filter(r => r.code == code.id)
  console.log(out)
  return out
}

const activeCodeIndex = ref(0)

const currentCode = computed(() => {
  return store.codes[activeCodeIndex.value]
})

const newRuleVisible = ref(false)


function addRule(code:GameCode) {
  newRuleVisible.value = true
}
</script>

<template>
<div class="flex flex-row justify-center">
  <div class="container">
    <TabView v-model:active-index="activeCodeIndex">
      <TabPanel v-for="code in store.codes" key="id" :header="code.name">
        <div class="flex flex-col">
          <div class="flex flex-row justify-center">
            <Button label="Add Rule" icon="pi pi-plus" class="p-button-success m-2" @click="addRule(code)"/>
          </div>
          <SingleRuleEditor v-for="rule in rulesByCode(code)" :rule-id="rule.id" :key="rule.id"/>
        </div>
      </TabPanel>
    </TabView>
    <NewRuleEditor :code="currentCode" v-model:visible="newRuleVisible"/>

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