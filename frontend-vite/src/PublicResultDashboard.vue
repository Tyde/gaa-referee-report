<script setup lang="ts">

import {usePublicStore} from "@/utils/public_store";
import {ref} from "vue";
import {useRouter} from "vue-router";

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
const router = useRouter()

</script>

<template>
  <transition-group name="p-message" tag="div">
    <Message v-for="msg in store.currentErrors" severity="error" :key="msg.timestamp">{{msg.message}}</Message>
  </transition-group>
  <div class="flex flex-row justify-center">
    <div class="referee-container">
      <div class="header no-print items-center">

        <img src="./assets/logo.png" alt="Logo" class="w-12 h-12 m-2 cursor-pointer" @click="router.push('/')">
        <div class="grow font-bold text-xl cursor-pointer" @click="router.push('/')">GGE Referee Report System</div>
        <div class="mr-2">Public Reports</div>
      </div>
  <router-view></router-view>
    </div>
  </div>

</template>

<style>
@media print {
  .no-print, .no-print * {
    display: none !important;
  }
}
</style>
