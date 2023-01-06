<script setup lang="ts">

import {computed, onMounted, ref} from "vue";
import type { Tournament} from "@/types";
import {DatabaseTournament} from "@/types";
import {DateTime} from "luxon";

const props = defineProps<{
  preselectedDate: Date,
}>()

const emit = defineEmits<{
  (e: 'tournament_created', tournament: DatabaseTournament): void,
  (e: 'canceled') :void
}>()
const editedTournament = ref<Tournament>({
  date: DateTime.now(), location: "", name: ""
})
const allowChangeDate = ref<Boolean>(false)
const isLoading = ref<Boolean>(true)
onMounted(() => {
  editedTournament.value.date = DateTime.fromJSDate( props.preselectedDate)
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
        date:editedTournament.value.date.toISODate()
      })
    };
    isLoading.value = true
    const response = await fetch("/api/tournament/new",requestOptions)
    const data = await response.json()
    let parseResult = DatabaseTournament.safeParse(data)
    if (parseResult.success) {
      emit('tournament_created',parseResult.data)
    } else {
      console.error(parseResult.error)
    }
    isLoading.value = false

  }
}

const dateString = computed(() => {
  return editedTournament.value.date.toISODate()
})
</script>

<template>
  <Card>
    <template #title>
      Create new Tournament
    </template>
    <template #content>
      <template v-if="allowChangeDate">
        <Calendar id="dateformat"
                  :model-value="editedTournament.date.toJSDate()"
                  @update:model-value="(newDate:Date) => {editedTournament.date = DateTime.fromJSDate(newDate)}"
                  dateFormat="yy-mm-dd"/>
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