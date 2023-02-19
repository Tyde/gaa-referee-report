<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import SingleOptionEditor from "@/components/admin/SingleOptionEditor.vue";
import {PitchPropertyType} from "@/types/pitch_types";

const store = useAdminStore()
</script>

<template>
  <h2>Pitch options</h2>
  <div class="flex flex-wrap flex-row">
    <div>
      <h4>Pitch markings:</h4>
      <SingleOptionEditor
          :type="PitchPropertyType.markingsOptions"
      />
    </div>
    <div>
      <h4>Pitch lenghts:</h4>
      <SingleOptionEditor :type="PitchPropertyType.length"/>
    </div>
    <div>
      <h4>Pitch widths:</h4>
      <SingleOptionEditor :type="PitchPropertyType.width"/>
    </div>
    <div>
      <h4>Pitch surfaces:</h4>
      <SingleOptionEditor :type="PitchPropertyType.surface" />
    </div>
    <div>
      <h4>Goal dimensions</h4>
      <SingleOptionEditor :type="PitchPropertyType.goalDimensions"/>
    </div>
    <div>
      <h4>Goalposts</h4>
      <SingleOptionEditor :type="PitchPropertyType.goalPosts"/>
    </div>
  </div>
</template>


<style scoped>
h2 {
  @apply text-center text-2xl;
  @apply font-bold;
  @apply mb-2;
}
h4 {
  @apply text-center text-lg;
  @apply font-bold;
  @apply mb-2;
}
</style>