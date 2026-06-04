<script lang="ts" setup>
import {computed, onMounted, onUpdated, watch} from "vue";
import {deleteSubstitutionOnServer, substitutionIsBlank} from "@/utils/api/substitutions_api";
import {useReportStore} from "@/utils/edit_report_store";
import type {Substitution} from "@/types/game_report_types";
import {useI18n} from "vue-i18n";

const store = useReportStore()
const props = defineProps<{
  visible: boolean
  isTeamA: boolean
}>()
const emits = defineEmits<{
  (e: 'update:visible', value: boolean): void,
}>()

const selectedSubstitutionArray = computed(() => {
  if (props.isTeamA) {
    return store.selectedGameReport!.teamAReport.substitutions
  } else {
    return store.selectedGameReport!.teamBReport.substitutions
  }
})
const selectedTeam = computed(() => {
  if (props.isTeamA) {
    return store.selectedGameReport!.teamAReport.team
  } else {
    return store.selectedGameReport!.teamBReport.team
  }
})

async function uploadSubstitutionsToServer() {
  if (store.selectedGameReport?.id != undefined) {
    await store.waitForAllTransfersDone()
    for (const substitution of selectedSubstitutionArray.value) {
      store.sendSubstitution(substitution, store.selectedGameReport, true)
          .catch((error) => {
            store.newError(error)
          })
    }
  } else {
    console.log("No report id - deferring upload")
  }
}

const localVisible = computed({
  get() {
    return props.visible
  },
  set(newValue) {
    emits('update:visible', newValue)
  }
})

function closeDialog() {
  uploadSubstitutionsToServer()
  emits('update:visible', false)
}

const {t} = useI18n()
const substitutionDialogTitle = computed(() => {
  return t("gameReport.substitutionsTitle") + " " + selectedTeam.value?.name
})
watch(() => selectedSubstitutionArray, () => {
  //Always add empty substitution if there is no empty row
  generateEmptySubstitution()
}, {deep: true, immediate: true})

function generateEmptySubstitution() {
  const subs = selectedSubstitutionArray.value
  if (subs.length == 0) {
    addEmptySubstitution()
  } else if (!substitutionIsBlank(subs[subs.length - 1])) {
    addEmptySubstitution()
  }
}

function addEmptySubstitution() {
  if (selectedTeam.value) {
    selectedSubstitutionArray.value.push({
      playerOnFirstName: "",
      playerOnLastName: "",
      playerOnNumber: undefined,
      playerOffFirstName: "",
      playerOffLastName: "",
      playerOffNumber: undefined,
      minute: undefined,
      team: selectedTeam.value
    } as Substitution)
  } else {
    console.log("Cant add empty substitution - no team")
  }
}

function deleteSubstitution(substitution: Substitution) {
  if (substitution.id) {
    deleteSubstitutionOnServer(substitution)
        .catch((error) => {
          store.newError(error)
        })
  }
  selectedSubstitutionArray.value.splice(selectedSubstitutionArray.value.indexOf(substitution), 1)
}

onUpdated(() => {
  generateEmptySubstitution()
})
onMounted(() => {
  generateEmptySubstitution()
})
</script>
<template>

  <Dialog
      v-model:visible="localVisible"
      :closable="false"
      :close-on-escape="false"
      :header="substitutionDialogTitle"
      :modal="true"
  >
    <div
        v-for="substitution in selectedSubstitutionArray"
        class="flex flex-wrap items-end gap-4 border-b border-gray-300 py-3"
    >
      <div class="flex flex-col gap-1">
        <label class="text-sm text-gray-500">{{ $t('gameReport.substitution.minute') }}</label>
        <InputNumber
            v-model="substitution.minute"
            class="w-20"
            input-class="w-20"
            :use-grouping="false"
            :placeholder="$t('gameReport.substitution.minute')"
        />
      </div>

      <div class="flex flex-col gap-1">
        <label class="text-sm font-medium text-green-700">{{ $t('gameReport.substitution.playerOn') }}</label>
        <div class="flex flex-wrap gap-2">
          <InputNumber
              v-model="substitution.playerOnNumber"
              class="w-16"
              input-class="w-16"
              :use-grouping="false"
              :placeholder="$t('gameReport.player.number')"
          />
          <InputText
              v-model="substitution.playerOnFirstName"
              class="w-36"
              :placeholder="$t('gameReport.player.firstName')"
          />
          <InputText
              v-model="substitution.playerOnLastName"
              class="w-36"
              :placeholder="$t('gameReport.player.lastName')"
          />
        </div>
      </div>

      <div class="pb-2 text-2xl text-gray-400">&rarr;</div>

      <div class="flex flex-col gap-1">
        <label class="text-sm font-medium text-red-700">{{ $t('gameReport.substitution.playerOff') }}</label>
        <div class="flex flex-wrap gap-2">
          <InputNumber
              v-model="substitution.playerOffNumber"
              class="w-16"
              input-class="w-16"
              :use-grouping="false"
              :placeholder="$t('gameReport.player.number')"
          />
          <InputText
              v-model="substitution.playerOffFirstName"
              class="w-36"
              :placeholder="$t('gameReport.player.firstName')"
          />
          <InputText
              v-model="substitution.playerOffLastName"
              class="w-36"
              :placeholder="$t('gameReport.player.lastName')"
          />
        </div>
      </div>

      <div class="pb-1 ml-auto" v-if="!substitutionIsBlank(substitution)">
        <Button class="p-button-danger p-button-text"
                @click="deleteSubstitution(substitution)">
          <vue-feather
              class="p-1"
              type="x"
          />
        </Button>
      </div>
    </div>
    <template #footer>
      <Button autofocus icon="pi pi-check" label="Ok" @click="closeDialog"/>
    </template>
  </Dialog>
</template>

<style scoped>

</style>
