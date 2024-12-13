<script setup lang="ts">
import {ref} from "vue";
import {useTeamsheetStore} from "@/utils/teamsheet_store";

const store = useTeamsheetStore();
const isLoading = ref<Boolean>(true)

{
  let promiseAux = store.publicStore.loadAuxiliaryInformationFromSerer()
  let promiseTurn = store.publicStore.loadTournaments()
  Promise.all([promiseAux, promiseTurn])
      .then(() => {
        isLoading.value = false
      })
      .catch((e) => {
        store.newError(e)
      })
}

</script>

<template>
  <transition-group name="p-message" tag="div">
    <Message v-for="msg in store.publicStore.currentErrors" severity="error" :key="msg.timestamp">{{msg.message}}</Message>
  </transition-group>
<div class="container md:w-[450px] mx-auto ">
  <h1 class="text-2xl font-bold text-center mb-2">GGE Teamsheet Dashboard</h1>

  <router-view></router-view>
</div>
</template>

<style scoped>

</style>
