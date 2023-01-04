<script lang="ts" setup>

import type {GameType} from "@/types";
import {computed, ref} from "vue";
import {uploadNewGameType} from "@/utils/api/game_report_api";

const newGameType = ref<string>("");
const props = defineProps<{
  visible: boolean,
  gameTypes: Array<GameType>,
}>()
const emits = defineEmits<{
  (e: 'update:visible', value: boolean): void,
  (e: 'newGameType', value: GameType): void
}>()
const localVisible = computed({
  get() {
    return props.visible
  },
  set(newValue) {
    emits('update:visible', newValue)
  }
})

function storeGameType() {

  let gtDB = uploadNewGameType(newGameType.value).catch((err) => {
    console.log(err)
  }).then((gt) => {
    emits('newGameType', gt as GameType)
  })

  emits('update:visible', false)

}

const proposedExistingAlternatives = computed(() => {
  return props.gameTypes.filter(
      gameType =>
          gameType.name.toLowerCase().includes(newGameType.value.toLowerCase())
  )
})
</script>
<template>

  <Dialog
      v-model:visible="localVisible"
      :closable="true"
      :close-on-escape="true"
      :modal="true"
      header="Add game type"
  >
    <div class="grid grid-cols-2">
      <div>
        <InputText
            v-model="newGameType"
            class="p-2"
            placeholder="New game type"
        />
        <Button
            class="p-2"
            @click="storeGameType"
        >
          Save
        </Button>
      </div>
      <div v-if="newGameType.length>2">
        Existing alternatives:
        <ul>
          <li
              v-for="gameType in proposedExistingAlternatives"
              class="p-4 hover:bg-gray-200 cursor-pointer"
          >
            {{ gameType.name }}
          </li>
        </ul>
      </div>
    </div>
  </Dialog>
</template>


<style scoped>

</style>