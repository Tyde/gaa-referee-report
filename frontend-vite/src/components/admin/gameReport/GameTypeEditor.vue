<script setup lang="ts">

import {useAdminStore} from "@/utils/admin_store";
import {onMounted, ref} from "vue";
import type {GameType} from "@/types";
import SingleGameTypeEditor from "@/components/admin/gameReport/SingleGameTypeEditor.vue";

const store = useAdminStore()

onMounted(() => {
})

const editingTypes = ref<GameType[]>([])

function showInputFor(gameType: GameType) {
  //Add a clone into our editing list
  console.log(gameType)
  editingTypes.value.push({...gameType})
  console.log(editingTypes.value)
}

function onSaveEdit(gameType: GameType) {

  //Remove the clone from our editing list
  onCancelEdit(gameType)
  //Save the changes
  if(gameType.id === -1) {
    store.createGameType(gameType)
  } else {
    store.updateGameType(gameType)
  }
}

function onCancelEdit(gameType: GameType) {
  //Remove the clone from our editing list
  if(gameType.id === -1) {
    showNewGameTypeInput.value = false
  } else {

    editingTypes.value = editingTypes.value.filter(it => it.id !== gameType.id)
  }
}

const showNewGameTypeInput = ref(false)
const newGameType = ref<GameType>({
  id: -1,
  name: ""
})
function addNewGameTypeEditor() {
  newGameType.value = {
    id: -1,
    name: ""
  }
  showNewGameTypeInput.value = true
}
</script>

<template>

  <div class="flex flex-col justify-start w-full">
    <div
        v-for="gameType in store.publicStore.gameTypes" :key="gameType.id"
        class="single-game-type-option group">
      <template v-if="editingTypes.filter(it => it.id === gameType.id).length === 1">
        <SingleGameTypeEditor :game-type="gameType" @on_save="onSaveEdit" @on_cancel="onCancelEdit"/>
      </template>
      <template v-else>
      {{gameType.name}}
      <div class="group-hover:visible invisible float-right">
        <!--<vue-feather type="trash"  class="hover:cursor-pointer ml-1" @click="deleteType(gameType)"/>-->
        <vue-feather type="edit" class="hover:cursor-pointer ml-1" @click="showInputFor(gameType)"/>
      </div>
      </template>
    </div>
    <div v-if="showNewGameTypeInput"  class="single-game-type-option">
      <SingleGameTypeEditor :game-type="newGameType" @on_save="onSaveEdit" @on_cancel="onCancelEdit"/>
    </div>
    <div v-else class="flex flex-row justify-center m-2 rounded bg-gray-300">
      <Button
          @click="addNewGameTypeEditor"
          class="p-button-sm p-button-secondary m-1"
          :disabled="editingTypes.filter(it => it.id === -1).length === 1"
      >Add new</Button>
    </div>
  </div>

</template>


<style>
.single-game-type-option {
  @apply rounded bg-blue-100 border-gray-50;
  @apply border-2 m-2 p-2 text-lg;
}
</style>
