<script lang="ts" setup>
import {ref} from "vue";
import type {Team} from "@/types/team_types";
import {addTeamAlias, deleteTeamAlias} from "@/utils/api/teams_api";

const props = defineProps<{
  team: Team
}>()

const emit = defineEmits<{
  (e: 'aliasesChanged'): void
}>()

const newAlias = ref("")
const errorMessage = ref("")
const busy = ref(false)

function errorText(error: unknown): string {
  return error instanceof Error ? error.message : String(error)
}

async function addAlias() {
  if (!newAlias.value.trim() || busy.value) {
    return
  }
  busy.value = true
  errorMessage.value = ""
  try {
    await addTeamAlias(props.team.id, newAlias.value)
    newAlias.value = ""
    emit('aliasesChanged')
  } catch (error) {
    errorMessage.value = errorText(error)
  } finally {
    busy.value = false
  }
}

async function removeAlias(id: number) {
  if (busy.value) {
    return
  }
  busy.value = true
  errorMessage.value = ""
  try {
    await deleteTeamAlias(id)
    emit('aliasesChanged')
  } catch (error) {
    errorMessage.value = errorText(error)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="flex flex-col gap-2">
    <div class="flex flex-row flex-wrap items-center gap-1">
      <span class="text-sm opacity-70">Alternative spellings:</span>
      <span v-if="!(team.aliases?.length)" class="text-sm italic opacity-60">none</span>
      <span
          v-for="alias in team.aliases ?? []"
          :key="alias.id"
          class="bg-surface-500 rounded-xl px-2 py-1 text-sm flex flex-row items-center gap-1"
      >
        <span>{{ alias.alias }}</span>
        <Button
            icon="pi pi-times"
            text
            rounded
            size="small"
            :aria-label="`Remove alias ${alias.alias}`"
            :disabled="busy"
            @click="removeAlias(alias.id)"
        />
      </span>
    </div>
    <div class="flex flex-row items-center gap-2">
      <InputText
          v-model="newAlias"
          placeholder="Add spelling"
          aria-label="Add alternative spelling"
          :disabled="busy"
          @keyup.enter="addAlias"
      />
      <Button label="Add" :disabled="busy" @click="addAlias"/>
    </div>
    <div v-if="errorMessage" class="text-sm text-red-500" role="alert">{{ errorMessage }}</div>
  </div>
</template>
