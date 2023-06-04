<script setup lang="ts">

import {usePublicStore} from "@/utils/public_store";
import {ref} from "vue";

const store = usePublicStore();
const isLoading = ref<Boolean>(true)

{
  let promiseAux = store.loadAuxiliaryInformationFromSerer()
  let promiseTurn = store.loadTournaments()
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
    <Message v-for="msg in store.currentErrors" severity="error" :key="msg.timestamp">{{msg.message}}</Message>
  </transition-group>
  <router-view></router-view>

</template>

<style scoped>

</style>