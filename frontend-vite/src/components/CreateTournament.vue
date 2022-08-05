<script setup lang="ts">

import {computed, onMounted, ref} from "vue";
import type {DatabaseTournament, Tournament} from "@/types";
import {fromDateToDateString} from "@/utils/gobal_functions";

const props = defineProps<{
  preselectedDate: Date,
}>()

const emit = defineEmits<{
  (e: 'tournament_created', tournament: DatabaseTournament): void,
  (e: 'canceled') :void
}>()
const editedTournament = ref<Tournament>({
  date: new Date(), location: "", name: ""
})
const allowChangeDate = ref<Boolean>(false)
const isLoading = ref<Boolean>(true)
onMounted(() => {
  editedTournament.value.date = props.preselectedDate
})

async function save_tournament() {
  if (editedTournament.value.location && editedTournament.value.name) {
    const requestOptions = {
      method: "POST",
      headers: {
        'Content-Type': 'application/json;charset=utf-8'
      },
      body: JSON.stringify({
        name:editedTournament.value.name,
        location: editedTournament.value.location,
        date:fromDateToDateString(editedTournament.value.date)
      })
    };
    isLoading.value = true
    const response = await fetch("/api/tournament/new",requestOptions)
    const data = await response.json()
    data.date = new Date(data.date)
    isLoading.value = false
    emit('tournament_created',data as DatabaseTournament)
  }
}

const dateString = computed(() => {
  return fromDateToDateString(editedTournament.value.date)
})
</script>

<template>
  <Card>
    <template #title>
      Create new Tournament
    </template>
    <template #content>
      <template v-if="allowChangeDate">
        <Calendar id="dateformat" v-model="editedTournament.date" dateFormat="yy-mm-dd"/>
      </template>
      <template v-else> Date: {{ dateString }}
        <Button @click="allowChangeDate = true">Change</Button>
      </template>
      <br><br>
      <span class="p-float-label">
        <InputText v-model="editedTournament.name" id="name"/>
        <label for="name">Enter tournament name</label>
      </span><br>
      <span class="p-float-label">
        <InputText v-model="editedTournament.location" id="location"/>
        <label for="location">Enter tournament location</label>
      </span><br>
      <Button @click="save_tournament">Submit</Button>
      <Button @click="emit('canceled')" class="p-button-danger">Cancel</Button>
    </template>
  </Card>
</template>

<style scoped>

</style>