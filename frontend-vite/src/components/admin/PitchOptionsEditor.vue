<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import SingleOptionEditor from "@/components/admin/SingleOptionEditor.vue";
import {PitchPropertyType} from "@/types/pitch_types";

const store = useAdminStore()
</script>

<template>
  <h2>Pitch options</h2>
  <hr class="h-px my-8 bg-surface-600 border-0 mx-32">

  <div class="flex flex-wrap flex-row">
    <div>
      <h3>Pitch markings:</h3>
      <SingleOptionEditor
          :type="PitchPropertyType.markingsOptions"
      />
    </div>
    <div>
      <h3>Pitch lenghts:</h3>
      <SingleOptionEditor :type="PitchPropertyType.length"/>
    </div>
    <div>
      <h3>Pitch widths:</h3>
      <SingleOptionEditor :type="PitchPropertyType.width"/>
    </div>
    <div>
      <h3>Pitch surfaces:</h3>
      <SingleOptionEditor :type="PitchPropertyType.surface" />
    </div>
    <div>
      <h3>Goal dimensions</h3>
      <SingleOptionEditor :type="PitchPropertyType.goalDimensions"/>
    </div>
    <div>
      <h3>Goalposts</h3>
      <SingleOptionEditor :type="PitchPropertyType.goalPosts"/>
    </div>
  </div>
</template>


<style scoped>


</style>
