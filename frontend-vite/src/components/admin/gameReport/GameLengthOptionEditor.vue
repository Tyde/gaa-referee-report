<script setup lang="ts">
import {ref} from "vue";
import {useAdminStore} from "@/utils/admin_store";
import type {GameLengthOption} from "@/types";

const store = useAdminStore()
const newOption = ref<GameLengthOption>({id: -1, name: "", minutes: 0})

function addNew() {
  if (newOption.value.name.trim().length > 0 && newOption.value.minutes > 0) {
    store.createGameLength({...newOption.value}).then(() => {
      newOption.value = {id: -1, name: "", minutes: 0}
    })
  }
}

</script>

<template>
  <div class="p-4">
    <div class="mb-4">
      <div class="flex flex-row gap-2 items-end">
        <span class="w-2/3">
          <label class="block mb-1">Name</label>
          <InputText v-model="newOption.name" class="w-full" placeholder="e.g. 30 min (2x15 min)"/>
        </span>
        <span class="w-1/3">
          <label class="block mb-1">Minutes</label>
          <InputNumber v-model="newOption.minutes" class="w-full" :min="1" :use-grouping="false"/>
        </span>
        <Button class="p-button-success" @click="addNew">Add</Button>
      </div>
    </div>

    <DataTable :value="store.publicStore.gameLengthOptions" striped-rows>
      <Column field="name" header="Name">
        <template #body="slotProps">
          <InputText v-model="slotProps.data.name" class="w-full" @blur="store.updateGameLength(slotProps.data)"/>
        </template>
      </Column>
      <Column field="minutes" header="Minutes">
        <template #body="slotProps">
          <InputNumber v-model="slotProps.data.minutes" :min="1" :use-grouping="false"
                       @blur="store.updateGameLength(slotProps.data)"/>
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<style scoped>

</style>

