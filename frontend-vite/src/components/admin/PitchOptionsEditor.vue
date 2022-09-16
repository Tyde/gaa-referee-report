<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import SingleOptionEditor from "@/components/admin/SingleOptionEditor.vue";
import {PitchPropertyType} from "@/types";

const store = useAdminStore()
</script>

<template>
<SingleOptionEditor
  :type="PitchPropertyType.markingsOptions"
  />
</template>


<style scoped>

</style>