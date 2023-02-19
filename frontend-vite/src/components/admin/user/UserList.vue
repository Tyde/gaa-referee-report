<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import {getAllUsers, updateUserOnServer} from "@/utils/api/admin_api";
import {onMounted, ref} from "vue";
import type {DataTableRowEditSaveEvent} from "primevue/datatable";
import NewUserDialog from "@/components/admin/user/NewUserDialog.vue";
import type {Referee} from "@/types/referee_types";

const store = useAdminStore();
const users = ref<Referee[]>([]);

const editingUsers = ref<Referee[]>([]);
const showNewUserDialog = ref(false);
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

function deactivateUser(user: Referee) {
    //TODO
}
</script>

<template>
  <!-- Button for new user -->
  <div class="flex flex-row justify-center w-full">
    <div>
      <Button
          label="Register new Referee"
          icon="pi pi-plus"
          class="p-button-success p-mr-2"
          @click="showNewUserDialog = true"
      />
    </div>
  </div>
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
        <Button icon="pi pi-user-minus" title="Deactivate User" class="mr-2 p-button-rounded p-button-danger" @click="() => deactivateUser(data)"></Button>
      </template>
    </Column>
    <Column :rowEditor="true" headerStyle="width:7rem" bodyStyle="text-align:center"></Column>
  </DataTable>

  <NewUserDialog
    v-model:visible="showNewUserDialog"
    />
</template>



<style scoped>

</style>