<script lang="ts" setup>
import {computed, onMounted, onUpdated, watch} from "vue";
import {deleteInjuryOnServer, injuryIsBlank} from "@/utils/api/injuries_api";
import {useReportStore} from "@/utils/edit_report_store";
import type {Injury} from "@/types/game_report_types";
import {useI18n} from "vue-i18n";

const store = useReportStore()
const props = defineProps<{
  visible: boolean
  isTeamA: boolean
}>()
const emits = defineEmits<{
  (e: 'update:visible', value: boolean): void,
}>()

const selectedInjuryArray = computed(() => {
  if (props.isTeamA) {
    return store.selectedGameReport!!.teamAReport.injuries
  } else {
    return store.selectedGameReport!!.teamBReport.injuries
  }
})
const selectedTeam = computed(() => {
  if (props.isTeamA) {
    return store.selectedGameReport!!.teamAReport.team
  } else {
    return store.selectedGameReport!!.teamBReport.team
  }
})
async function uploadInjuriesToServer() {
  if (store.selectedGameReport?.id != undefined) {
    await store.waitForAllTransfersDone()
    for (let injury of selectedInjuryArray.value) {
      store.sendInjury(injury,store.selectedGameReport,true)
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

  uploadInjuriesToServer()
  emits('update:visible', false)

}

const {t} = useI18n()
const injuryDialogTitle = computed(() => {
  return t("gameReport.injuriesTitle") + " " + selectedTeam.value?.name
})
watch(()=>selectedInjuryArray, () => {
  //Always add empty injury if there is no empty row
  generateEmptyInjury()
},{deep:true,immediate:true})

function generateEmptyInjury() {
  let newInjury = selectedInjuryArray.value
  if (newInjury.length == 0) {
    addEmptyInjury()
  } else if (!injuryIsBlank(newInjury[newInjury.length - 1])) {
    addEmptyInjury()
  }
}

function addEmptyInjury() {
  if(selectedTeam.value) {
    //console.log("Adding empty injury with team id " + selectedTeam.value.id)
    selectedInjuryArray.value.push({
      firstName: "",
      lastName: "",
      details: "",
      team: selectedTeam.value
    } as Injury)
  } else {
    console.log("Cant add empty injury - no team")
  }
}

function deleteInjury(injury: Injury) {
  if (injury.id) {
    deleteInjuryOnServer(injury)
        .catch((error) => {
          store.newError(error)
        })
  }
  selectedInjuryArray.value.splice(selectedInjuryArray.value.indexOf(injury), 1)
}

onUpdated(() => {
  //Always add empty injury if there is no empty row
  generateEmptyInjury()
})
onMounted(() => {
  //Always add empty injury if there is no empty row
  generateEmptyInjury()
})
</script>
<template>

  <Dialog
      v-model:visible="localVisible"
      :closable="false"
      :close-on-escape="false"
      :header="injuryDialogTitle"
      :modal="true"
  >
    <div v-for="injury in selectedInjuryArray">
      <div class="flex flex-row flex-wrap">
        <div class="p-2 w-1/2 md:w-auto">
          <InputText
              v-model="injury.firstName"
              class="w-full md:w-52"
              :placeholder="$t('gameReport.player.firstName')"
          />
        </div>
        <div class="p-2 w-1/2 md:w-auto">
          <InputText
              v-model="injury.lastName"
              class="w-full md:w-52"

              :placeholder="$t('gameReport.player.lastName')"
          />
        </div>
        <div class="p-2">
          <InputText
              v-model="injury.details"

              :placeholder="$t('gameReport.description')"
          />
        </div>
        <div class="p-2" v-if="!injuryIsBlank(injury)">
          <Button class="p-button-danger p-button-text"
                  @click="deleteInjury(injury)">
            <vue-feather
                class="p-1"
                type="x"
            />
          </Button>
        </div>
      </div>
      <div class="flex-grow h-px bg-gray-300 m-4"></div>
    </div>
    <template #footer>

      <Button autofocus icon="pi pi-check" label="Ok" @click="closeDialog"/>
    </template>
  </Dialog>
</template>

<style scoped>

</style>
