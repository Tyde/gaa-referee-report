<script setup lang="ts">

import {useAdminStore} from "@/utils/admin_store";
import GameTypeEditor from "@/components/admin/gameReport/GameTypeEditor.vue";
import RulesEditor from "@/components/admin/gameReport/RulesEditor.vue";
import ExtraTimeOptionEditor from "@/components/admin/gameReport/ExtraTimeOptionEditor.vue";

const store = useAdminStore()


</script>

<template>
  <Accordion>
    <AccordionTab header="Game Types">
      <GameTypeEditor />
    </AccordionTab>
    <AccordionTab header="Rules">
      <RulesEditor />
    </AccordionTab>
    <AccordionTab header="Extra Time Options">
      <ExtraTimeOptionEditor />
    </AccordionTab>
  </Accordion>

</template>

<style scoped>

</style>