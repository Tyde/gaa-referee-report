<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import {computed, ref} from "vue";
import {NewReferee, Referee} from "@/types";
import {addRefereeOnServer} from "@/utils/api/admin_api";

const store = useAdminStore()
const props = defineProps<{
  visible: boolean
}>()
const emits = defineEmits<{
  (e: 'update:visible', value: boolean): void,
}>()

const localVisible = computed({
  get() {
    return props.visible
  },
  set(newValue) {
    emits('update:visible', newValue)
  }
})

function closeDialog() {
  emits('update:visible', false)
}


const newUser = ref<NewReferee>({})


function addNewReferee() {
  if (newUser.value.firstName && newUser.value.lastName && newUser.value.mail) {
    addRefereeOnServer(newUser.value)
        .then((referee: Referee) => {
          closeDialog()
        })
        .catch((error) => {
          store.newError(error)
          closeDialog()
        })
  }
}
</script>

<template>
  <Dialog
      v-model:visible="localVisible"
      :closable="false"
      :close-on-escape="false"
      header="Add new Referee"
      :modal="true"
  >
    <h5>First Name</h5>
    <InputText v-model="newUser.firstName" />
    <h5>Last Name</h5>
    <InputText v-model="newUser.lastName" />
    <h5>Email</h5>
    <InputText v-model="newUser.mail" />

    <div class="flex justify-end">
      <Button
          label="Cancel"
          class="p-button-secondary"
          @click="closeDialog"
      />
      <Button
          label="Add"
          class="p-button-primary"
          @click="addNewReferee"
      />
    </div>
  </Dialog>
</template>



<style scoped>

</style>