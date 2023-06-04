<script setup lang="ts">

import {DatabaseTournament, databaseTournamentToTournamentDAO, RegionDEO} from "@/types/tournament_types";
import {ref} from "vue";
import {useAdminStore} from "@/utils/admin_store";
import {updateTournamentOnServer} from "@/utils/api/admin_api";
import {DateTime} from "luxon";

let props = defineProps<{
  tournament: DatabaseTournament
}>();

let emit = defineEmits<{
  (e: 'tournament_updated', tournament: DatabaseTournament): void,
  (e: 'canceled'): void
}>()

let store = useAdminStore()

function regionIDToRegion(regionID: number): RegionDEO {
  return store.regions.filter(it => it.id == regionID)[0]
}

function startEdit() {
  // Weird hack to deep clone the object
  editedTournament.value = DatabaseTournament.parse(
      databaseTournamentToTournamentDAO(props.tournament)
  )
  currentlyEditing.value = true
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
</script>

<template>
  <div
      class="single-tournament-row"
      :class="{ 'cursor-pointer': !currentlyEditing }"
      v-on:click="startEdit"
  >
    <template v-if="!currentlyEditing">
      <div class="flex flex-row">
        <div class="grow">{{ props.tournament.name }}</div>
        <div>{{ props.tournament.location }}</div>
      </div>
      <div class="flex flex-row">
        <div class="grow">{{ props.tournament.date.toISODate() }}</div>
        <div>{{ regionIDToRegion(props.tournament.region).name }}</div>
      </div>
    </template>
    <template v-else-if="editedTournament">
      <div class="tournament-edit-row">
        <div class="grow">
        <span class="p-float-label">
            <InputText
                v-model="editedTournament.name"
                :inputId="'tournament-name-' + props.tournament.id"
            />
          <label :for="'tournament-name-' + props.tournament.id">Name</label>
        </span>
        </div>
        <div>
        <span class="p-float-label">
          <InputText
              v-model="editedTournament.location"
              :inputId="'tournament-location-' + props.tournament.id"
          />
          <label :for="'tournament-location-' + props.tournament.id">Location</label>
        </span>
        </div>
      </div>
      <div class="tournament-edit-row">
        <div class="grow">
        <span class="p-float-label">
          <Calendar
              :model-value="editedTournament.date.toJSDate()"
              @update:model-value="(newDate:Date) => {
                if(editedTournament)
                  editedTournament.date = DateTime.fromJSDate(newDate)
              }"
              dateFormat="yy-mm-dd"
              :inputId="'tournament-date-' + props.tournament.id"
          />
          <label :for="'tournament-date-' + props.tournament.id">Date</label>
        </span>
        </div>
        <div>
          <span class="p-float-label">
          <Dropdown
              v-model="editedTournament.region"
              :options="store.regions"
              optionLabel="name"
              optionValue="id"
              :inputId="'tournament-region-' + props.tournament.id"
          />
          <label :for="'tournament-region-' + props.tournament.id">Region</label>
          </span>
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

</template>

<style scoped>
.single-tournament-row {
  @apply rounded-lg;
  @apply bg-gray-200;
  @apply p-2;
  @apply m-2;
  @apply flex;
  @apply flex-col;
  @apply transition-transform;
}
.tournament-edit-row {
  @apply flex flex-row m-2 mt-6;
}

</style>