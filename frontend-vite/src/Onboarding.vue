<script lang="ts" setup>


import {computed, onMounted, ref} from "vue";
import {activateUser, validateActivationToken} from "@/utils/api/referee_api";
import {useAdminStore} from "@/utils/admin_store";
import type {Referee} from "@/types/referee_types";
import {isApiError} from "@/types";

const currentReferee = ref<Referee | null>(null)
const newPassword = ref("")
const confirmPassword = ref("")
const store = useAdminStore()
const token = ref("")
const tokenInvalid = ref(false)
onMounted(() => {
  let loc = new URL(location.href)
  //URL schema https://referee.gaelicgames.eu/user/activate/:token  - get token from URL
  token.value = loc.pathname.split("/")[3]
  if (token.value && token.value.length > 0) {
    //Validate token on server
    validateActivationToken(token.value)
        .then(data => currentReferee.value = data)
        .catch(err => {
          if (isApiError(err) && err.error == "insertionFailed") {
            tokenInvalid.value = true
          } else {
            store.newError(err)
          }
        })
  }
})

function sendPassword() {
  activateUser(token.value, newPassword.value)
      .then(() => {
        location.href = "/login"
      })
      .catch(err => store.newError(err))
}

const passwordsMatch = computed(() => {
  return newPassword.value.length > 0 && newPassword.value === confirmPassword.value
})

const passwordMinLength = computed(() => {
  return newPassword.value.length >= 8
})
</script>

<template>
  <transition-group name="p-message" tag="div">
    <Message v-for="msg in store.currentErrors" severity="error" :key="msg.timestamp">{{ msg.message }}</Message>
  </transition-group>
  <div class="flex justify-center flex-row">
    <div v-if="currentReferee" class="text-center">
      <h1>Hi {{ currentReferee.firstName }} {{ currentReferee.lastName }}!</h1>
      <p>You can now activate your account by setting a password.</p>
      <h5>Password</h5>
      <Password v-model="newPassword" toggle-mask/>
      <p class="text-red-700" v-if="!passwordMinLength && newPassword.length>0">Please use at least 8 characters</p>
      <h5>Confirm Password</h5>
      <Password v-model="confirmPassword" :feedback="false" toggle-mask/>
      <p class="text-red-700" v-if="!passwordsMatch && confirmPassword.length>0">Passwords do not match</p>

      <div class="p-2">
        <Button
            label="Submit"
            @click="sendPassword()"
            :disabled="!(passwordsMatch && passwordMinLength)"/>
      </div>
    </div>
    <div v-if="tokenInvalid" class="text-red-700 text-2xl text-center font-bold m-4">
      Invalid token. Either you already activated your account or the used link is incorrect.<br>
      <a class="underline italic" href="/login">Go back to login</a>
    </div>
    <div v-if="!currentReferee && !tokenInvalid" class="text-center">Loading...</div>
  </div>
</template>


<style scoped>

</style>
