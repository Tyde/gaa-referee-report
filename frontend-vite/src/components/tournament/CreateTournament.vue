<script setup lang="ts">

import {computed, onMounted, ref} from "vue";
import {DateTime} from "luxon";
import {DatabaseTournament, Tournament} from "@/types/tournament_types";
import {useReportStore} from "@/utils/edit_report_store";
import {uploadNewTournament} from "@/utils/api/tournament_api";

const props = defineProps<{
  preselectedDate: Date,
}>()

const store = useReportStore()

const emit = defineEmits<{
  (e: 'tournament_created', tournament: DatabaseTournament): void,
  (e: 'canceled') :void
}>()
const editedTournament = ref<Tournament>({
  date: DateTime.now(), location: "", name: "", region: 0
})
const allowChangeDate = ref<Boolean>(false)
const isLoading = ref<Boolean>(true)
onMounted(() => {
  editedTournament.value.date = DateTime.fromJSDate( props.preselectedDate)
})

async function save_tournament() {
  if (editedTournament.value.location && editedTournament.value.name && editedTournament.value.region) {

    isLoading.value = true


    uploadNewTournament(editedTournament.value)
        .then((newTournament) => emit('tournament_created',newTournament))
        .catch((e) => {
          store.newError(e)
        })
        .finally(() => {
          isLoading.value = false
        })


  } else {
    store.newError("Please fill out all fields")
  }
}

const dateString = computed(() => {
  return editedTournament.value.date.toISODate()
})
</script>

<template>
  <Card>
    <template #title>
      {{ $t('tournament.create') }}
    </template>
    <template #content>
      <template v-if="allowChangeDate">
        <Calendar id="dateformat"
                  :model-value="editedTournament.date.toJSDate()"
                  @update:model-value="(newDate:Date) => {editedTournament.date = DateTime.fromJSDate(newDate)}"
                  dateFormat="yy-mm-dd"/>
      </template>
      <template v-else> <h3>Date: {{ dateString }}</h3>
        <Button class="p-button-secondary" @click="allowChangeDate = true">{{ $t('tournament.changeDate') }}</Button>
      </template>
      <br><br>
      <span class="p-float-label">
        <InputText v-model="editedTournament.name" id="name"/>
        <label for="name">{{ $t('tournament.enterName') }}</label>
      </span><br>
      <span class="p-float-label">
        <InputText v-model="editedTournament.location" id="location"/>
        <label for="location">{{ $t('tournament.enterLocation') }}</label>
      </span><br>

        <Dropdown
            v-model="editedTournament.region"
            id="region"
            :options="store.regions"
            option-label="name"
            option-value="id"
            :placeholder="$t('tournament.selectRegion')"
            />
      <div class="flex flex-row">
        <div class="m-2">
          <Button @click="save_tournament">{{ $t('general.submit') }}</Button>
        </div>
        <div class="m-2">
          <Button @click="emit('canceled')" class="p-button-danger">{{ $t('general.cancel') }}</Button>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>
h3 {
  @apply m-2;
  @apply text-xl font-bold;
}
</style>
