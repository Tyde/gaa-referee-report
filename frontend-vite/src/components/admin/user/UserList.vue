<script lang="ts" setup>

import {useAdminStore} from "@/utils/admin_store";
import {getAllUsers, updateUserOnServer} from "@/utils/api/admin_api";
import {onMounted, ref} from "vue";
import type {DataTableRowEditSaveEvent} from "primevue/datatable";
import NewUserDialog from "@/components/admin/user/NewUserDialog.vue";
import type { RefereeWithRoleDEO} from "@/types/referee_types";
import {updateUserRole} from "@/utils/api/referee_api";
import {RefereeRole, SetRefereeRoleDEO} from "@/types/referee_types";

const store = useAdminStore();
const users = ref<RefereeWithRoleDEO[]>([]);

const editingUsers = ref<RefereeWithRoleDEO[]>([]);
const showNewUserDialog = ref(false);
onMounted(() => {
  updateUserList()
})

function updateUserList() {
  getAllUsers()
      .then(it => users.value = it)
      .catch(err => store.newError(err))
}

function editUser(event: DataTableRowEditSaveEvent) {

  let {newData, index } = event
  updateUserOnServer(newData)
      .then(() => {
        users.value[index] = newData
      })
      .catch(err => store.newError(err))

}

function setRefereeRole(user: RefereeWithRoleDEO, role: RefereeRole) {
  updateUserRole(SetRefereeRoleDEO.parse({
    id: user.id,
    role: role
  }))
      .then(() => updateUserList())
      .catch(err => store.newError(err))
}
function deactivateUser(user: RefereeWithRoleDEO) {
  setRefereeRole(user, RefereeRole.Enum.INACTIVE)
}

function activateDeactivatedUser(user: RefereeWithRoleDEO) {
  setRefereeRole(user, RefereeRole.Enum.REFEREE)
}

function makeAdmin(user: RefereeWithRoleDEO) {
  setRefereeRole(user, RefereeRole.Enum.ADMIN)
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
      <template #body="{data}:{data: RefereeWithRoleDEO}">
        <Button
            v-if="data.role === RefereeRole.Enum.REFEREE"
            icon="pi pi-user-minus"
            label="Deactivate"
            class="mr-2 p-button-rounded p-button-danger"
            @click="() => deactivateUser(data)"></Button>
        <Button
            v-if="data.role === RefereeRole.Enum.INACTIVE"
            icon="pi pi-user-plus"
            label="Activate User"
            class="mr-2 p-button-rounded p-button-success"
            @click="() => activateDeactivatedUser(data)"></Button>
        <Button
            v-if="data.role === RefereeRole.Enum.REFEREE"
            icon="pi pi-lock-open"
            label="Make Admin"
            class="mr-2 p-button-rounded p-button-warning"
            @click="() => makeAdmin(data)"></Button>

        <span v-if="data.role === RefereeRole.Enum.WAITING_FOR_ACTIVATION"> Did not set password yet. </span>
      </template>
    </Column>
    <Column :rowEditor="true" headerStyle="width:7rem" bodyStyle="text-align:center"></Column>
  </DataTable>

  <NewUserDialog
    v-model:visible="showNewUserDialog"
    @newRefereeAdded="() => updateUserList()"
    />
</template>



<style scoped>

</style>