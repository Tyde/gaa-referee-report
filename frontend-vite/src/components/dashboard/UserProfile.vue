<script lang="ts" setup>
import {useDashboardStore} from "@/utils/dashboard_store";
import {computed, ref, watch} from "vue";
import {updatePasswordOnServer} from "@/utils/api/referee_api";
import {Referee, UpdateRefereePasswordDAO} from "@/types/referee_types";
import {usePublicStore} from "@/utils/public_store";

const store = useDashboardStore()
const publicStore = usePublicStore()
const meShadowCopy = ref<Referee>({...store.me})

watch(() => store.me, (newVal) => {
  console.log("Store me changed")
  meShadowCopy.value = {...newVal}
})

const hasChanges = computed(() => {
  return store.me.firstName !== meShadowCopy.value.firstName ||
      store.me.lastName !== meShadowCopy.value.lastName ||
      store.me.mail !== meShadowCopy.value.mail
})

const passwordChange = ref({
  oldPassword: "",
  newPassword: "",
  newPasswordRepeat: ""
})

const repeatPasswordMatches = computed(() => {
  return passwordChange.value.newPassword === passwordChange.value.newPasswordRepeat ||
      passwordChange.value.newPassword === ""
})

function updatePassword() {
  //First, check for validity of the change
  if (!repeatPasswordMatches.value) {
    alert("New password and repeat password do not match")
    return
  }
  if (passwordChange.value.newPassword === "") {
    alert("New password is empty")
    return
  }

  let updateDAO = UpdateRefereePasswordDAO.parse({
    id: store.me.id,
    oldPassword: passwordChange.value.oldPassword,
    newPassword: passwordChange.value.newPassword
  })
  updatePasswordOnServer(updateDAO)
      .then((res) => {
        if(res.success) {
          alert("Password successfully changed")
          passwordChange.value = {
            oldPassword: "",
            newPassword: "",
            newPasswordRepeat: ""
          }
        } else {
          publicStore.newError("Password change failed: " + res.message)
        }
      })
      .catch((error) => {
        publicStore.newError(error)
      })
  //store.updatePassword(passwordChange.value.oldPassword, passwordChange.value.newPassword)
}

const changePasswordDisabled = computed(() => {
  return !repeatPasswordMatches.value || passwordChange.value.newPassword === "" || passwordChange.value.oldPassword === ""
})
</script>

<template>
  <div class="w-96 mx-auto">
    <h2>Profile</h2>
    <!-- Table for Inputs: Firstname Lastname Email -->
    <div class="grid grid-cols-2 text-lg">
      <div class="profile-label">
        <label for="firstname">First name:</label>
      </div>
      <div>
        <input class="profile-input" type="text" id="firstname" v-model="meShadowCopy.firstName">
      </div>
      <div class="profile-label">
        <label for="firstname">Last name:</label>
      </div>
      <div>
        <input class="profile-input" type="text" id="firstname" v-model="meShadowCopy.lastName">
      </div>
      <div class="profile-label">
        <label for="firstname">Mail:</label>
      </div>
      <div>
        <input class="profile-input" type="text" id="firstname" v-model="meShadowCopy.mail">
      </div>
      <div></div>
      <div>
        <Button
            class="p-button"
            @click="store.updateMe(meShadowCopy)"
            :disabled="!hasChanges"
        >Save</Button>
      </div>
      <div>
        <h4>Change password</h4>
      </div>
      <div></div>
      <div class="profile-label">
        <label for="oldPassword">Old password:</label>
      </div>
      <div>
        <input class="profile-input" type="password" id="oldPassword" v-model="passwordChange.oldPassword">
      </div>
      <div class="profile-label">
        <label for="newPassword">New password:</label>
      </div>
      <div>
        <input class="profile-input" type="password" id="newPassword" v-model="passwordChange.newPassword">
      </div>
      <div class="profile-label">
        <label for="newPasswordRepeat">Repeat new password:</label>
      </div>
      <div>
        <input class="profile-input" :class="{'input-invalid':!repeatPasswordMatches}" type="password" id="newPasswordRepeat" v-model="passwordChange.newPasswordRepeat">
      </div>
      <div></div>
      <div>
        <Button
            class="p-button"
            @click="updatePassword()"
            :disabled="changePasswordDisabled"
        >Change password</Button>
      </div>
    </div>
  </div>

</template>


<style scoped>
h2 {
  @apply text-2xl;
  @apply font-bold;
  @apply text-gray-900;
  @apply text-center;
  @apply m-2;
}
.profile-input {
  @apply border-2 border-gray-300;
  @apply rounded-lg;
  @apply w-56;
  @apply shadow;
  @apply p-2 m-2;
}
.profile-label{
  @apply p-2;
}
.input-invalid {
  @apply border-2 border-red-500;
  @apply shadow-red-500
}
</style>
