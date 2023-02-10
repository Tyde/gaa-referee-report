<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import {getAllUsers, updateUserOnServer} from "@/utils/api/admin_api";
import {onMounted, ref} from "vue";
import {Referee} from "@/types";
import type {DataTableRowEditSaveEvent} from "primevue/datatable";

const store = useAdminStore();
const users = ref<Referee[]>([]);

const editingUsers = ref<Referee[]>([]);

onMounted(() => {
  getAllUsers()
      .then(it => users.value = it)
      .catch(err => store.newError(err))
})

function editUser(event: DataTableRowEditSaveEvent) {

  let {newData, index } = event
  updateUserOnServer(newData)
      .then(() => {
        users.value[index] = newData
      })
      .catch(err => store.newError(err))

}
</script>

<template>
  <DataTable
      :value="users"
      edit-mode="row"
      v-model:editing-rows="editingUsers"
      @row-edit-save="editUser"
  >
    <Column field="firstName" header="First Name" sortable>
      <template #editor="slotProps">
        <InputText v-model="slotProps.data[slotProps.field]" />
      </template>
    </Column>
    <Column field="lastName" header="Last Name" sortable>
      <template #editor="slotProps">
        <InputText v-model="slotProps.data[slotProps.field]" />
      </template>
    </Column>
    <Column field="mail" header="Email" sortable>
      <template #editor="slotProps">
        <InputText v-model="slotProps.data[slotProps.field]" />
      </template>
    </Column>
    <Column>
      <template #body="{data}">
        <Button icon="pi pi-user-minus" title="Deactivate User" class="mr-2 p-button-rounded p-button-danger" @click="deactivateUser(slotProps.data)"></Button>
      </template>
    </Column>
    <Column :rowEditor="true" headerStyle="width:7rem" bodyStyle="text-align:center"></Column>
  </DataTable>
</template>



<style scoped>

</style>