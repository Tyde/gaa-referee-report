<script setup lang="ts">

import type {GameType} from "@/types";

const props = defineProps<{
  gameType: GameType
}>()
const emit = defineEmits<{
  (e: 'on_save', gameType: GameType): void,
  (e: 'on_cancel', gameType: GameType): void
}>()

function onSaveEdit(gameType: GameType) {
  emit('on_save', gameType)
}
function onCancelEdit(gameType: GameType) {
  emit('on_cancel', gameType)
}
</script>
<template>
  <div class="flex flex-row">
    <div class="p-1 flex-grow">
      <InputText
          v-model="gameType.name"
          class="w-full"
          @keyup.enter="onSaveEdit(gameType)"
      />
    </div>
    <div class="p-2">

      <vue-feather class="hover:cursor-pointer" type="x" @click="onCancelEdit(gameType)"/>
    </div>
  </div>
  <div class="p-1">
    <Button @click="onSaveEdit(gameType)" class="p-button-sm p-button-success m-1">Save</Button>
  </div>
</template>

