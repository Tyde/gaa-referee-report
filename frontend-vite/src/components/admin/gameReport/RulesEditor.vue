<script setup lang="ts">

import {useAdminStore} from "@/utils/admin_store";
import type {GameCode} from "@/types";
import SingleRuleEditor from "@/components/admin/gameReport/SingleRuleEditor.vue";

const store = useAdminStore()

function rulesByCode(code:GameCode) {
  const out = store.rules.filter(r => r.code == code.id)
  console.log(out)
  return out
}


</script>

<template>
<div class="flex flex-row justify-center">
  <div class="container">
    <TabView>
      <TabPanel v-for="code in store.codes" key="id" :header="code.name">
        <div class="flex flex-col">


          <SingleRuleEditor v-for="rule in rulesByCode(code)" :rule-id="rule.id" :key="rule.id"/>
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