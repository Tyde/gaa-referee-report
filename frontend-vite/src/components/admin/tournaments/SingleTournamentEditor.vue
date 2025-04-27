<script setup lang="ts">

import {DatabaseTournament, databaseTournamentToTournamentDAO, RegionDEO} from "@/types/tournament_types";
import {computed, ref, watch} from "vue";
import {useAdminStore} from "@/utils/admin_store";
import {updateTournamentOnServer} from "@/utils/api/admin_api";
import {DateTime} from "luxon";
import {useRouter} from "vue-router";
import {useConfirm} from "primevue/useconfirm";

let props = defineProps<{
  tournament: DatabaseTournament
}>();


let emit = defineEmits<{
  (e: 'tournament_updated', tournament: DatabaseTournament): void,
  (e: 'canceled'): void
}>()

let store = useAdminStore()

function regionIDToRegion(regionID: number): RegionDEO {
  return store.publicStore.regions.filter(it => it.id == regionID)[0]
}

function startEdit() {
  if (!currentlyEditing.value) {
    // Weird hack to deep clone the object
    editedTournament.value = DatabaseTournament.parse(
        databaseTournamentToTournamentDAO(props.tournament)
    )
    currentlyEditing.value = true
  }
}

function save() {


  if (editedTournament.value) {
    loading.value = true
    updateTournamentOnServer(editedTournament.value)
        .then(() => {
          Object.keys(props.tournament).forEach((key) => {
            // @ts-ignore
            props.tournament[key] = editedTournament.value[key]
          })
          emit('tournament_updated', props.tournament)

          currentlyEditing.value = false
        })
        .catch((e) => {
          store.newError(e)
        })
        .finally(() => {
          loading.value = false
        })
  }


}

function cancel() {
  currentlyEditing.value = false
  emit('canceled')
}

let loading = ref(false)
let currentlyEditing = ref(false)
let editedTournament = ref<DatabaseTournament | undefined>()
const router = useRouter()

function goToCompleteTournamentReport(id: number) {
  router.push({path: "/tournament-reports/complete/" + id})
}

const confirm = useConfirm()
const mergeDialogVisible = ref(false)
const mergeWithTournament = ref<DatabaseTournament | undefined>(undefined)
const tournamentsExceptThis = computed(() => {
  return store.publicStore.tournaments.filter(it => it.id != props.tournament.id)
})

function createTournamentButtonModel(tournament: DatabaseTournament) {
  return [
    {
      label: 'Delete Tournament',
      command: () => {
        console.log("to be deleted: ", tournament)
        confirm.require({
          message: 'Are you sure you want to delete the tournament '
              + tournament.name
              + ' in '
              + tournament.location
              + " on "
              + tournament.date.toISODate() + "?\n This will delete all reports for that tournament.",
          header: "Delete tournament?",
          icon: "pi pi-exlamation-triangle",
          rejectClass: 'p-button-secondary p-button-outlined',
          rejectLabel: 'Cancel',
          acceptLabel: 'Save',
          accept() {
            store.deleteTournament(tournament)
          },
          reject() {
          },
        })
      },
    }, {
      label: 'Merge Tournament into...',
      command: () => {
        mergeDialogVisible.value = true
      }
    }
  ]
}


watch(() => editedTournament.value?.isLeague, (newVal) => {
  console.log("triggered watcher")
  if (newVal && editedTournament.value && !editedTournament.value?.endDate) {
    editedTournament.value.endDate = editedTournament.value?.date.plus({days: 1})
    console.log("Setting end date to ", editedTournament.value.endDate)
  }
})

function mergeTournaments() {
  if (mergeWithTournament.value) {
    console.log("Merging ", props.tournament, " into ", mergeWithTournament.value)
    store.mergeTournament(props.tournament, mergeWithTournament.value)
    mergeDialogVisible.value = false
  }
}
</script>

<template>
  <div class="flex flex-row">
    <div
        class="single-tournament-row grow"
        :class="{ 'cursor-pointer': !currentlyEditing , 'league': !currentlyEditing && props.tournament.isLeague }"
        v-on:click="startEdit"
    >
      <template v-if="!currentlyEditing">
        <div class="flex flex-row">
          <div class="grow">{{ props.tournament.name }}</div>
          <div>{{ props.tournament.location }}</div>
        </div>
        <div class="flex flex-row">
          <div class="grow">{{ props.tournament.date.toISODate() }}<template
              v-if="props.tournament.isLeague">&nbsp;-&nbsp;{{ props.tournament.endDate?.toISODate() }}
            </template>
          </div>
          <div>{{ regionIDToRegion(props.tournament.region).name }}</div>
        </div>
      </template>
      <template v-else-if="editedTournament">
        <div class="tournament-edit-row">
          <div class="grow">
            <FloatLabel>
                <InputText
                    v-model="editedTournament.name"
                    :inputId="'tournament-name-' + props.tournament.id"
                />
              <label :for="'tournament-name-' + props.tournament.id">Name</label>
            </FloatLabel>
          </div>
          <div>
            <FloatLabel>
              <InputText
                  v-model="editedTournament.location"
                  :inputId="'tournament-location-' + props.tournament.id"
              />
              <label :for="'tournament-location-' + props.tournament.id">Location</label>
            </FloatLabel>
          </div>
        </div>
        <div class="tournament-edit-row">
          <div class="grow">
            <FloatLabel>
              <DatePicker
                  :model-value="editedTournament.date.toJSDate()"
                  @update:model-value="(newDate:Date) => {
                    if(editedTournament)
                      editedTournament.date = DateTime.fromJSDate(newDate)
                  }"
                  dateFormat="yy-mm-dd"
                  :inputId="'tournament-date-' + props.tournament.id"
              />
              <label :for="'tournament-date-' + props.tournament.id">Date</label>
            </FloatLabel>
          </div>
          <div>
            <FloatLabel>
              <Select
                  v-model="editedTournament.region"
                  :options="store.publicStore.regions"
                  optionLabel="name"
                  optionValue="id"
                  :inputId="'tournament-region-' + props.tournament.id"
              />
              <label :for="'tournament-region-' + props.tournament.id">Region</label>
            </FloatLabel>
          </div>
        </div>
        <div class="tournament-edit-row">
          <div class="grow flex flex-row content-center">
            <Checkbox binary v-model="editedTournament.isLeague" :id="'isLeague-'+props.tournament.id"/>
            <label :for="'isLeague-'+props.tournament.id" class="ml-2">Is League Round</label>
          </div>
          <div v-if="editedTournament.isLeague">
            <FloatLabel>


              <DatePicker
                  :model-value="editedTournament.endDate?.toJSDate()"
                  @update:model-value="(newDate:Date) => {
                      editedTournament!!.endDate = DateTime.fromJSDate(newDate)
                    }"
                  dateFormat="yy-mm-dd"
                  :inputId="'tournament-end-date-' + props.tournament.id"
              /><label :for="'tournament-end-date-' + props.tournament.id">End date</label>
            </FloatLabel>
          </div>
        </div>

        <div class="tournament-edit-row">
          <div class="grow">
            <Button class="p-button-success" label="Save" @click.stop="save"/>
          </div>
          <div>
            <Button class="p-button-danger" label="Cancel" @click.stop="cancel"/>
          </div>
        </div>
      </template>
    </div>
    <SplitButton
        @click="goToCompleteTournamentReport(props.tournament.id)"
        class="w-56 m-2"
        label="Complete Tournament Report"
        :model="createTournamentButtonModel(props.tournament)"
    >

    </SplitButton>
    <Dialog v-model:visible="mergeDialogVisible" header="Merge Tournament" :modal="true" :closable="true">
      <p>Are you sure you want to merge this tournament with another?</p>
      <p>The tournament {{ props.tournament.name }} will be moved into the selected tournament.</p>
      <p>Choose the tournament to merge into:</p>
      <Select
          v-model="mergeWithTournament"
          :options="tournamentsExceptThis"
          :optionLabel="(t:DatabaseTournament) => t.name + ' in ' + t.location + ' on ' + t.date.toISODate()"
          placeholder="Select a tournament to merge into"
          filter
      />
      <br>
      <Button label="Merge" class="p-button-success" @click="mergeTournaments"/>
      <Button label="Cancel" class="p-button-danger" @click="mergeDialogVisible = false"/>
    </Dialog>
  </div>

</template>

<style scoped>
.single-tournament-row {
  @apply rounded-lg;
  @apply bg-surface-600;
  @apply p-2;
  @apply m-2;
  @apply flex;
  @apply flex-col;
  @apply transition-transform;
}

.tournament-edit-row {
  @apply flex flex-row m-2 mt-6;
}

.league {
  @apply bg-primary-950;
}

</style>
