<script setup lang="ts">

import type {GameType} from "@/types";
import {ref, watch} from "vue";

const props = defineProps<{
  gameType: GameType
}>()
const emit = defineEmits<{
  (e: 'on_save', gameType: GameType): void,
  (e: 'on_cancel', gameType: GameType): void
}>()

// Edit on a local copy so typing does not mutate the shared store object
// directly (which previously also meant Cancel could not revert changes).
const localGameType = ref<GameType>({...props.gameType})
watch(() => props.gameType, (gameType) => {
  localGameType.value = {...gameType}
})

function onSaveEdit() {
  emit('on_save', localGameType.value)
}
function onCancelEdit() {
  emit('on_cancel', localGameType.value)
}
</script>
<template>
  <div class="flex flex-row">
    <div class="p-1 flex-grow">
      <InputText
          v-model="localGameType.name"
          class="w-full"
          @keyup.enter="onSaveEdit()"
      />
    </div>
    <div class="p-2">

      <vue-feather class="hover:cursor-pointer" type="x" @click="onCancelEdit()"/>
    </div>
  </div>
  <div class="p-1">
    <Button @click="onSaveEdit()" class="p-button-sm p-button-success m-1">Save</Button>
  </div>
</template>

